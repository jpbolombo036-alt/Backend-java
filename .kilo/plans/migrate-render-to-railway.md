# Plan — Migration Render vers Railway sans casser Render

## Objectif
Passer le backend Java/Spring Boot vers Railway en gardant Render et sa configuration actuelle intactes jusqu’à validation complète.

## État actuel observé
- `src/main/resources/application.yml` utilise déjà des variables d’environnement pour la base de données, le port, CORS, JWT, SMTP, etc.
- Des valeurs par défaut sont présentes dans `application.yml`, dont l’URL et le mot de passe Render PostgreSQL : il faudra les remplacer côté Railway par de vraies variables d’environnement.
- `render.yaml` configure déjà Render avec Docker et une base PostgreSQL Render.
- `Dockerfile` semble compatible avec un déploiement Docker sur Railway.

## Stratégie recommandée
1. Ne pas supprimer ni modifier `render.yaml` pendant la migration.
2. Déployer la même image Docker sur Railway avec des variables d’environnement Railway.
3. Garder la base Render active pendant les tests, ou créer une base Railway et migrer les données via dump/restore.
4. Ne basculer le frontend/API que quand Railway est validé.
5. Garder Render comme fallback jusqu’à confirmation que Railway fonctionne en production.

## Étapes d’exécution
1. Vérifier que Railway peut builder le projet avec le `Dockerfile` existant.
2. Créer une base PostgreSQL Railway ou choisir de réutiliser temporairement la base Render.
3. Ajouter les variables d’environnement Railway :
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SERVER_PORT=8000`
   - `JWT_SECRET` identique à Render si on veut garder les tokens valides
   - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
   - `MAIL_FROM`
   - `CORS_ALLOWED_ORIGINS` incluant l’URL Railway sans retirer les URLs Render/frontend existantes
   - `FRONTEND_URL`
   - `UPLOAD_DIR=/app/uploads/attachments` si les uploads doivent rester accessibles dans le conteneur
4. Déployer sur Railway.
5. Tester les endpoints critiques : login, activation, CORS, uploads, envoi d’email, endpoints protégés.
6. Si une nouvelle base Railway est utilisée, exporter/importer les données Render vers Railway avec `pg_dump`/`pg_restore`, puis exécuter Flyway.
7. Mettre à jour le frontend pour appeler Railway uniquement après validation.
8. Surveiller les logs Railway et garder Render actif en rollback.

## Risques à surveiller
- Les fichiers uploadés sont éphémères sur Railway si aucun volume ou stockage externe n’est configuré.
- Les tokens JWT expireront côté utilisateurs si `JWT_SECRET` change.
- Les variables par défaut dans `application.yml` contiennent des secrets Render : elles doivent rester des secours uniquement et idéalement être nettoyées plus tard.
- CORS doit inclure Railway pour éviter les blocages frontend.

## Rollback
- Reconfigurer le frontend sur l’URL Render.
- Ne pas supprimer la base Render avant validation.
- En cas d’erreur Railway, arrêter le trafic sans modifier Render.
