# Plan : Corriger les valeurs par défaut B2 dans le code

## Contexte
Le bucket Backblaze B2 réel est `itaccess-storage`, région `us-east-005`. Mais plusieurs fichiers contiennent encore d'anciennes valeurs par défaut (`taccess-storage`, `us-west-002`) qui risquent d'être utilisées si une variable d'environnement Railway est manquante.

## Tâches

1. Corriger `B2Properties.java` :
   - `bucket` default : `"taccess-storage"` → `"itaccess-storage"`
   - `region` default : `"us-west-002"` → `"us-east-005"`

2. Corriger `application.yml` ligne 71 :
   - `${B2_BUCKET:taccess-storage}` → `${B2_BUCKET:itaccess-storage}`

3. Vérifier et corriger `.env` et `.env.example` :
   - `B2_BUCKET` : `taccess-storage` → `itaccess-storage`
   - `R2_BUCKET` (si présent) : vérifier cohérence

4. Vérifier `docker-compose.yml` :
   - `B2_BUCKET` : `taccess-storage` → `itaccess-storage`

## Validation
- `mvn compile` doit passer sans erreur
- Tous les fichiers de config B2 doivent référencer `itaccess-storage` et `us-east-005`
