# Plan: Mode runner tableau pour sessions de test QA

## Contexte
- Le QA valide des étapes de test (`TestStep`) au sein d'une `TestSession`.
- Le backend supporte déjà `resolved` (booléen libre) et le statut (`OK`, `BUG`, `EN COURS`).
- L'endpoint `PUT /tests/{id}` accepte une mise à jour partielle.
- La migration V11 est déployée sur Railway (colonne `resolved` sur `test_steps`).

## Décisions
- **Vue** : tableau complet des étapes de la session (option B validée).
- **Interaction** : cases à cocher `resolved` + dropdown `statut` par ligne, sauvegarde automatique au changement (option A validée).
- **Logique** : `resolved` est indépendant de `statut`. L'utilisateur le manœuvre explicitement via la case à cocher.

## Tâches d'implémentation

### 1. Frontend — Vue tableau session (`src/pages/Tests/index.tsx`)
- Charger `sessionId` courant + appeler `GET /test-sessions/{id}` (ou `GET /tests?sessionId={id}`) pour récupérer les tests avec `resolved` et `statut`.
- Afficher chaque `TestStep` en tableau :
  - Colonne `#` : `testNumber`
  - Colonne `Fonction` : `fonction`
  - Colonne `Statut` : dropdown `<select>` lié à `statut` (valeurs : `OK`, `BUG`, `EN COURS`)
  - Colonne `Résolu` : case à cocher `<input type="checkbox">` liée à `resolved`
  - Style : ligne barrée si `resolved === true`
- Au changement (`onChange`) :
  - Mettre à jour le state local (optimistic update)
  - Appeler `PUT /tests/{id}` avec le body partiel `{ statut: "...", resolved: true/false }` selon le champ modifié
  - En cas d'erreur API, rollback du state local + affichage toast/erreur

### 2. Frontend — Types DTO (`src/api/dashboardApi.ts` ou équivalent)
- Vérifier que `TestDTO` inclut `resolved: boolean`
- Vérifier que `TestSessionDTO` inclut `plateforme`, `testsCount`, `testsResolvedCount`, etc. (déjà côté backend)

### 3. Frontend — Appels API
- S'assurer que la requête PUT envoie `resolved` + `statut` séparément (pas de champ manquant écrasé à `null` grâce à la mise à jour partielle backend).

### 4. UX / UI
- Ligne barrée (text-decoration: line-through) quand `resolved === true`.
- Dropdown stylisé pour le statut (couleurs : vert OK / rouge BUG / jaune EN COURS).
- Indicateur visuel de sauvegarde en cours (spinner) pendant l'appel API.

## Validation
- Vérifier que modifier le statut d'une ligne déclenche bien `PUT /tests/{id}` avec `{ statut: "OK" }` (sans toucher à `resolved`).
- Vérifier que cocher/décocher `resolved` déclenche bien `PUT /tests/{id}` avec `{ resolved: true/false }` (sans toucher à `statut`).
- Vérifier que la ligne reste barrée après refresh (persistance Railway).
- Vérifier que les erreurs réseau n'écrasent pas les données locales sans confirmation.

## Risques
- Si le frontend envoie `null` pour un champ non modifié, le backend (mise à jour partielle) l'ignore OK. Pas de risque de perte.
- Si la colonne `resolved` n'est pas présente sur Railway, l'API retourne 500 (V11 doit être appliquée).
- performances : si une session a beaucoup de tests, batch PUT possible mais pas requis pour un MVP.
