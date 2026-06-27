# Plan: Réouverture de session de test (admin only)

## Contexte
- L'endpoint `POST /test-sessions/{id}/request-close` existe déjà (fermeture par tout utilisateur authentifié + notification admins).
- Aucun endpoint de réouverture n'existe.
- La production est sur Railway (`itaccess-backend-production-5145.up.railway.app`).
- La migration V11 est déjà pushée pour corriger la colonne `resolved`.

## Décisions
- **Endpoint** : `POST /test-sessions/{id}/reopen`
- **Accès** : `@PreAuthorize("hasRole('admin')")` (admin uniquement, cohérent avec `UserController`)
- **Body** : aucun
- **Transition** : `CLOSED` → `OPEN` uniquement (rejet si statut ≠ `CLOSED`)
- **Notification** : notifier tous les admins (même logique que `requestCloseSession`)
- **Réponse** : `TestSessionDTO` complet mis à jour

## Tâches d'implémentation

1. **Service** (`TestSessionService.java`)
   - Ajouter `reopenSession(Long id)` :
     - Charger la session (404 si absente)
     - Vérifier `"CLOSED".equals(session.getStatut())` (400 sinon)
     - `session.setStatut("OPEN")` + save
     - Notifier tous les admins via `systemNotificationService`
     - Retourner `toDTOWithStats(updatedSession)`

2. **Controller** (`TestSessionController.java`)
   - Ajouter `POST /{id}/reopen` avec `@PreAuthorize("hasRole('admin')")`
   - Appeler `testSessionService.reopenSession(id)`
   - Retourner `ResponseEntity.ok()`

3. **Migration (si besoin)**
   - Aucune nouvelle colonne requise (réouverture = simple changement de statut)

4. **Vérification**
   - `mvn compile` passe
   - `mvn test` passe (tests existants + nouveau test unitaire `reopenSession`)
   - Vérifier que le endpoint est bien documenté Swagger

## Risques
- Si la session n'est pas `CLOSED`, l'erreur doit être claire (`IllegalStateException` → 400/409).
- La notification doit être cohérente avec `requestCloseSession` (type INFO, destinataires = admins).
