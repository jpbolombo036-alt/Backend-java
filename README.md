# Backend-java

Backend Spring Boot (Java 17) de l'application **IT Access Manager**.

## Modules principaux

- Auth / Users / Roles
- Applications, Comptes, Test steps (QA)
- **APK** : gestion des fichiers Android (`/apk`) — upload, téléchargement, liste, suppression
- Stockage objet (Backblaze B2), Audit, IA (OpenAI)

## Module APK

Le module stocke les binaires APK soit sur le disque local, soit sur le stockage objet **B2**
(Backblaze). Le mode est choisi par la variable `B2_ENABLED` :

- `B2_ENABLED=false` (défaut / dev) → fichiers sur disque (`app.upload.dir`, défaut `/tmp/uploads/apk`).
- `B2_ENABLED=true` (prod / Railway) → fichiers uploadés dans le bucket B2, clé `apk/<uuid>.apk`.

### Endpoints (`/apk`)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/apk/upload` | Upload d'un APK (auth requise, validation du contenu ZIP/APK) |
| GET | `/apk/download/{id}` | Téléchargement du binaire (incrémente le compteur) |
| GET | `/apk` | Liste paginée (`?page=&size=`, défaut = tous) |
| GET | `/apk/application/{applicationId}` | APK d'une application (paginné) |
| GET | `/apk/{id}` | Métadonnées d'un APK |
| DELETE | `/apk/{id}` | Suppression (auteur ou `admin` uniquement) |

### Sécurité

- L'upload accepte uniquement des fichiers dont le **contenu** est une archive ZIP (`PK\x03\x04`) — l'extension seule ne suffit pas.
- La suppression est restreinte à l'auteur de l'upload ou à un administrateur (`role=admin`).

## Configuration des variables d'environnement

| Variable | Défaut | Description |
|----------|--------|-------------|
| `B2_ENABLED` | `false` | Active le stockage objet B2 pour les APK/documents |
| `B2_KEY_ID` | — | ID de clé du compte B2 |
| `B2_APPLICATION_KEY` | — | Clé d'application B2 |
| `B2_BUCKET` | `itaccess-storage` | Nom du bucket |
| `B2_ENDPOINT` | — | Endpoint S3 B2 (ex. `https://s3.us-east-005.backblazeb2.com`) |
| `B2_REGION` | `us-east-005` | Région du bucket |
| `B2_DOCUMENTS_PREFIX` | `document-archive/` | Préfixe des objets documents |
| `UPLOAD_DIR` | `/tmp/uploads/apk` | Répertoire local (mode B2 désactivé) |
| `MAX_FILE_SIZE` | `150MB` | Taille max d'un upload |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/itaccessdb` | URL JDBC |
| `JWT_SECRET` | — | Clé de signature JWT (≥ 256 bits en prod) |

## Déploiement sur Railway

1. Créer le service depuis le `Dockerfile` (builder `DOCKERFILE`).
2. Ajouter une base PostgreSQL (Railway).
3. Définir les variables ci-dessus, **notamment `B2_ENABLED=true` + les clés B2**.
4. (Optionnel) Monter un volume sur `/tmp/uploads` si vous restez en mode disque local — sinon les fichiers locaux sont effacés à chaque redéploiement.

### Migration des APK locaux existants vers B2

Au démarrage, si `B2_ENABLED=true`, le composant `ApkStorageMigration` transfère automatiquement
vers B2 les APK encore référencés par un chemin local (les clés `apk/...` sont ignorées). Les
fichiers locaux absents sont simplement signalés et ignorés (aucun crash).

## Démarrage local

```bash
./mvnw clean package -Dmaven.test.skip=true
java -jar target/*.jar
```
