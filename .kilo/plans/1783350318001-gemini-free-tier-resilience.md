# Plan: Résilience Gemini gratuit et fin des 429

## Contexte confirmé
- `AiService` utilise bien `OPENAI_URL`/`OPENAI_MODEL`/`OPENAI_API_KEY` depuis `application.yml` + `.env`.
- Railway exécute l’appli avec la config de `railway.env.example`, donc l’appel va vers:
  - `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
  - `gemini-2.5-flash`
- Erreur observée: `429 Too Many Requests` avec dimension `generate_content_free_tier_requests = 20`.

## Objectif
- Garder Gemini gratuit.
- Supprimer les `429` exposés à l’utilisateur.
- Réduire la consommation du quota là où c’est possible.

## Décisions déjà prises
- Pas de changement de provider.
- On garde l’appel REST actuel.
- On améliore d’abord la robustesse côté backend avant de toucher au front.

## Plan d'exécution

### 1. Détection fine des erreurs quota
- Dans `AiService.chat(...)`, distinguer:
  - `429 RESOURCE_EXHAUSTED`
  - `5xx`
  - autres erreurs réseau
- Ajouter une réponse métier claire:
  - `AiChatResponse.error=true`
  - message utilisateur: “Quota IA temporairement atteint, réessayez dans environ N secondes.”

### 2. Backoff et file d'attente coté serveur
- Ajouter un mécanisme simple de **retry avec backoff** uniquement sur `429`/`5xx`.
- Limiter le nombre de tentatives et éviter de spammer l’API.

### 3. Réduction des appels inutiles
- Ne pas envoyer les tools quand la question ne semble pas nécessiter d’outil métier.
- Limiter le nombre d’outils envoyés par défaut, ou rendre `tool_choice` plus conservateur.

### 4. Réponse dégradée
- Si quota épuisé, répondre en mode “lecture seule limitée” sans appel externe:
  - afficher un message temporaire + les données déjà disponibles sans Gemini.
- Éviter de propager l’erreur brute au frontend.

### 5. Traçabilité
- Logger chaque `429` avec:
  - `model`
  - `userId`
  - heure
  - retard extrait depuis la réponse Gemini (`retryDelay`)
- Ça permet de mesurer la conso sans changer d’architecture.

### 6. Validation
- Vérifier qu’un message admin/utilisateur ne renvoie plus la brute erreur Gemini.
- Vérifier la nouvelle réponse dégradée en cas de `429`.
- Garder `./mvnw compile -DskipTests` vert.

## Hors scope
- Migration Spring AI.
- Changement de provider.
- Comportement de l’IA/cache de réponses long terme.