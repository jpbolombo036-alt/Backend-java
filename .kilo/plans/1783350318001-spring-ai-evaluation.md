# Plan: Évaluation Spring AI pour IT Access Manager

## Constat contradictoire
- Le code `AiService` actuel appelle bien **OpenAI** via REST.
- Le `.env` et `application.yml` ne contiennent **aucune clé/con**fig Gemini.
- Pourtant l’erreur retournée est un **429 Gemini** (`generativelanguage.googleapis.com`).

➡️ Cela signifie probablement qu’il existe **un autre chemin AI** en production, ou que le déploiement ne reflète pas exactement cette version de code.

## Question de clarification
**D’où vient l’appel Gemini dans ton environnement ?**
- Y a-t-il un autre service/controller AI ?
- Le déploiement Railway/Render pointe-t-il vers une version antérieure du code ?
- Une variable d’environnement `GEMINI_*` existe-t-elle sur le serveur ?

Ma recommandation : **corriger d’abord l’origine de l’appel Gemini** avant de choisir Spring AI ou non, sinon on risque de migrer sur la mauvaise base.

## Comparaison : approche actuelle vs Spring AI

| Critère | Approche actuelle | Spring AI |
|---|---|---|
| Appel model | REST manuel OpenAI | Abstraction fournie |
| Function calling | JSON manuel | `@Tool` / `FunctionCallback` |
| Changement de provider | Modification du code/URL | Changement de propriété/Bean |
| Gestion d’erreurs/quota | Manuel | Built-in retry/backoff |
| Tests | Mock REST client difficile | Mocking plus simple |
| Boilerplate | Élevé | Réduit |
| Puissance/expressivité | Limitée à ce qu’on écrit | Écosystème structuré |

## Deux décisions à trancher
1. **Origine de Gemini** : résoudre d’abord l’incohérence.
2. **Cible AI** : OpenAI uniquement, ou abstraction multi-provider ?

## Proposition de branche d’implémentation
Si l’objectif est “même rôle, plus puissant/fiable” :
- Branche A : rester sur OpenAI, mais migrer vers Spring AI **OpenAI adapter**
- Branche B : ajouter un **fallback provider** via Spring AI pour réduire l’impact quota
- Branche C : garder REST manuel, mais ajouter **retry/circuit-breaker** et **clarté de config**

## Validation
- Identifier l’appel Gemini effectif en production
- Choisir la cible (OpenAI seul vs multi-provider)
- Si Spring AI retenu : vérifier compatibilité Spring Boot 3.2 / Java 17
