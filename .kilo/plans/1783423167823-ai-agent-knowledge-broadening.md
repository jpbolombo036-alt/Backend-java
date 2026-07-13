# Plan : Élargir la connaissance système de l'agent IA (exposition totale + injection de contexte)

## Contexte
L'agent `AiService` (src/main/java/com/itaccess/service/AiService.java) dispose déjà d'une résilience solide
(retry/backoff sur 429/5xx, sélection d'outils par mots-clés, réponse dégradée). Mais sa « connaissance du
système » est limitée : seules certaines entités sont exposées, et le `system prompt` ne décrit pas le domaine
métier. L'utilisateur veut (1) accès à plus de sources de données, (2) une meilleure « connaissance » du système,
(3) des capacités de synthèse/raisonnement accrues.

Décision clé : on **ne peut pas ré-entraîner Gemini**. La « connaissance » est donnée par injection de contexte
(léger, sans infra) + outils étendus, conformément aux réponses validées.

## Décisions validées
- **Mécanisme de connaissance** : injection de contexte légère — nouvel outil `get_system_knowledge` + `SYSTEM_PROMPT` enrichi (glossaire + règles métier). Pas de base vectorielle.
- **Périmètre outils** : exposition totale — lecture sur **tous** les repositories + écritures (create/update/delete) sur les entités pertinentes.
- **Garde-fous** : lectures ouvertes à tout utilisateur authentifié ; écritures/suppressions sensibles exigent le rôle `admin` ; le `system prompt` demande à l'IA de confirmer les actions irréversibles.
- **Raisonnement** : remplacer le cycle mono-outil par une boucle multi-outils (synthèse transversale).

## Travail préparatoire (champs vérifiés)
Entités pas encore exposées (lecture à ajouter) :
- `TestCase` : id, title, description, applicationId, createdBy, createdAt
- `ApplicationLink` : id, applicationId, nom, url, type, description, createdBy, dateCreation
- `Attachment` : id, fileName, originalFileName, filePath, fileSize, contentType, bugId, testStepId, messageId, createdBy, createdAt
- `Setting` : id, key, value
- `ReportGeneration` : id, reportType, title, status, generatedAt, generatedBy, generatedByUsername, content
- `Application` (création/maj/suppression) : nom, description, version, environnement
- `Compte` (création/maj/suppression) : applicationId, username, code, role, commentaire

Repositories à injecter dans `AiService` (manquants aujourd'hui) via champ `final` + `@RequiredArgsConstructor` :
`TestCaseRepository`, `ApplicationLinkRepository`, `AttachmentRepository`, `SettingRepository`
(`ApplicationRepository`, `CompteRepository`, `HabilitationRepository`, `ReportGenerationRepository` sont déjà présents).

## Tâches d'implémentation (ordre)

### 1. Injection de contexte (connaissance système)
- Enrichir `SYSTEM_PROMPT` avec : glossaire des entités, énumérations métier
  (Bug: `OPEN/IN_PROGRESS/RESOLVED/CLOSED` ; Attendance: `PRESENT/ABSENT/LATE` ; Todo ownership `createdBy` ;
  rôles `admin`/`user` ; types de rapport `SECURITY/ACCESS/TESTS/PERFORMANCE/COMPLIANCE` ;
  ApplicationLink, Setting, Attachment), et instruction de **synthétiser plusieurs outils** avant de répondre.
- Ajouter l'outil `get_system_knowledge` → renvoie un snapshot JSON compact :
  compteurs par entité (via les repositories déjà injectés), liste des types de rapport, des statuts Bug/Attendance,
  des rôles, et la signification des champs clés. C'est la « base de connaissances » dynamique du système.
- Ajouter `get_system_knowledge` aux outils de base **toujours envoyés** (avec `get_current_user`, `get_user_context`, `get_dashboard_stats`).

### 2. Nouveaux outils de LECTURE (tous rôles)
- `get_test_cases` (TestCaseRepository) — filtre optionnel `applicationId`.
- `get_application_links` (ApplicationLinkRepository) — filtre optionnel `applicationId`.
- `get_attachments` (AttachmentRepository) — filtre optionnel `bugId`/`testStepId`/`messageId`.
- `get_settings` (SettingRepository) — liste clés/valeurs ( éviter de logger les valeurs sensibles ).
- `get_reports` (ReportGenerationRepository) — liste des rapports générés (id, reportType, title, status, generatedAt, generatedByUsername).

### 3. Nouveaux outils d'ÉCRITURE (admin requis, sauf note)
Respecter les patterns existants (ex. `create_todo`, `create_bug`) : validation des champs obligatoires,
retour JSON `{success, message, id, ...}`, et **refus si `!"admin".equals(currentUser.getRole())`**.
- `create_application` / `update_application` / `delete_application` (admin) — champs Application.
- `create_compte` / `update_compte` / `delete_compte` (admin) — champs Compte.
- `create_application_link` / `update_application_link` / `delete_application_link` (admin).
- `create_test_case` / `update_test_case` / `delete_test_case` (admin).
- `create_setting` / `update_setting` / `delete_setting` (admin).
- `update_user` (admin) — ne modifier que `role` et `isActive` (setters Lombok sur `User`).
- `create_habilitation` / `update_habilitation` / `delete_habilitation` (admin) — champs Habilitation (compteId, permission).
- `delete_attachment` (admin) — suppression métadonnée (le fichier physique géré par l'endpoint d'upload existant).
- `delete_report` (admin) — suppression d'un `ReportGeneration`.

### 4. Raisonnement multi-outils (point 3)
- Remplacer `handleFunctionCalls` par `executeToolLoop(restClient, messages, currentUser)` :
  boucle qui, tant que la réponse contient `tool_calls` ET itérations < `MAX_TOOL_ITERATIONS` (ex. 5),
  exécute les outils, ajoute les résultats au tableau `messages`, et rappelle le modèle (via `postWithRetry`).
- Conserver le retry/backoff et la réponse dégradée (`buildDegradedResponse`) en cas d'échec dans la boucle.
- `processOpenAiResponse` appelle `executeToolLoop` au lieu de `handleFunctionCalls`.

### 5. Sélection d'outils étendue
- Ajouter chaque nouvel outil dans `TOOL_KEYWORDS` (map mots-clés français/anglais) pour que `selectToolNames`
  les inclue quand pertinent (ex. « test case », « lien », « paramètre », « pièce jointe », « rapport », « application », « compte », « habilitation », « utilisateur rôle »).
- Garder la détection « salutation seule → aucun outil ».

### 6. Sécurité / garde-fous
- Tout outil d'écriture sensible vérifie le rôle admin et renvoie `{"error":"Action réservée aux administrateurs."}`.
- Le `SYSTEM_PROMPT` précise : pour toute action de création/modification/suppression, l'IA doit résumer l'action
  et demander confirmation à l'utilisateur avant d'appeler l'outil (sauf si contexte explicite).
- Ne jamais exposer de secrets (clés API, mots de passe) dans les résultats d'outils ou les logs.

## Fichiers concernés
- `src/main/java/com/itaccess/service/AiService.java` — cœur des changements (prompt, outils, boucle, map mots-clés, champs repositories).
- `src/main/java/com/itaccess/dto/AiChatResponse.java` — inchangé (éventuellement ajouter `degraded` flag si utile, optionnel).
- Aucun nouveau repository/entité à créer (tous existent déjà).

## Validation
1. `.\mvnw.cmd compile -DskipTests` → BUILD SUCCESS.
2. Scénarios manuels (via `/ai/chat` avec un user authentifié) :
   - Question « connais-tu les différents types de rapports et statuts de bug ? » → l'IA utilise `get_system_knowledge`.
   - « résume les bugs ouverts et les présences du jour » → boucle multi-outils (bug + attendance) puis synthèse.
   - Admin : « crée l'application X » → `create_application` exécuté.
   - Non-admin : « supprime l'application 3 » → refus admin, pas d'action destructrice.
   - `429` toujours géré par la réponse dégradée (pas d'erreur brute remontée).
3. Vérifier que le payload envoyé ne contient que les outils pertinents (sélection par mots-clés) et que `get_system_knowledge` est présent par défaut.

## Risques / garde-fous
- **Taille de payload** : mitigée par la sélection d'outils et le jeu de base limité ; garder `get_system_knowledge` compact.
- **Sur-consommation quota** : la boucle multi-outils augmente les appels ; bornée par `MAX_TOOL_ITERATIONS` + sélection d'outils.
- **Actions destructrices** : admin-gating + confirmation dans le prompt ; pas d'outil de suppression exposé aux non-admin.
- **Données sensibles** : les outils ne renvoient pas de `hashedPassword`/clés ; `get_settings` ne loggue pas les valeurs.

## Hors scope
- RAG avec embeddings / base vectorielle (validé « injection légère » pour cette phase).
- Ré-entraînement du modèle (impossible via API).
- Persistance de l'historique de conversation (`conversationId` reste non utilisé pour l'instant).
