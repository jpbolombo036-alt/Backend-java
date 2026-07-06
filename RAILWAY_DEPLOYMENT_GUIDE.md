# Guide de Déploiement Railway - Backend Java Spring Boot

## Fichiers requis pour le déploiement

### 1. Dockerfile (déjà présent)
Le projet utilise un Dockerfile multi-stage pour optimiser la taille finale :

```dockerfile
# Étape 1 : Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Étape 2 : Exécution
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8000} -jar app.jar"]
```

### 2. Variables d'environnement Railway

Configurez ces variables dans le dashboard Railway (Settings → Variables) :

| Variable | Description | Exemple/Valeur par défaut |
|----------|-------------|---------------------------|
| `SPRING_DATASOURCE_URL` | URL de la base PostgreSQL Railway | `jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}` |
| `SPRING_DATASOURCE_DRIVER_CLASS` | Driver PostgreSQL | `org.postgresql.Driver` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur DB | `${PGUSER}` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe DB | `${PGPASSWORD}` |
| `JWT_SECRET` | Clé secrète JWT (256 bits min) | `votre-cle-jwt-secure` |
| `SERVER_PORT` | Port du serveur | `8000` (ou utilisez `${PORT}`) |
| `CORS_ALLOWED_ORIGINS` | URLs autorisées CORS | URLs de votre frontend + `https://your-app.up.railway.app` |
| `SMTP_HOST` | Serveur SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Port SMTP | `587` |
| `SMTP_USERNAME` | Email SMTP | `votre-email@gmail.com` |
| `SMTP_PASSWORD` | Mot de passe app SMTP | `votre-app-password` |
| `MAIL_FROM` | Expéditeur emails | `noreply@votre-domaine.com` |
| `FRONTEND_URL` | URL du frontend | `https://votre-frontend.vercel.app` |
| `UPLOAD_DIR` | Dossier uploads APK | `/tmp/uploads/apk` |
| `ATTACHMENTS_DIR` | Dossier pièces jointes | `/tmp/uploads/attachments` |
| `DOCUMENT_ARCHIVE_DIR` | Dossier documents | `/tmp/uploads/documents` |
| `MAX_FILE_SIZE` | Taille max fichiers | `150MB` |
| `B2_ENABLED` | Activation Backblaze B2 | `true` ou `false` |
| `B2_KEY_ID` | ID clé Backblaze | `votre-key-id` |
| `B2_APPLICATION_KEY` | Clé app Backblaze | `votre-application-key` |
| `B2_BUCKET` | Nom bucket B2 | `nom-bucket` |
| `B2_ENDPOINT` | Endpoint B2 S3 | `https://s3.us-east-005.backblazeb2.com` |
| `B2_REGION` | Région B2 | `us-east-005` |
| `B2_DOCUMENTS_PREFIX` | Préfixe documents B2 | `document-archive/` |
| `ADMIN_INIT_KEY` | Clé init admin | `votre-cle-init-secure` |

## 6. Configuration Maven (pom.xml)

Fichier pom.xml requis avec :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<properties>
    <java.version>17</java.version>
</properties>

<dependencies>
    <!-- Web & API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    
    <!-- Email -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    
    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Build plugin -->
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
</dependencies>
```

### 4. Fichier .env.example (optionnel pour développement)

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/votre_db
SPRING_DATASOURCE_DRIVER_CLASS=org.postgresql.Driver
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=dev-secret-key-change-in-production-min-256-bits

# Server
SERVER_PORT=8000

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=dev@example.com
SMTP_PASSWORD=dev-password
MAIL_FROM=noreply@local.dev

# Frontend
FRONTEND_URL=http://localhost:3000
```

### 5. Étapes de déploiement sur Railway

1. **Créer un compte** sur [railway.app](https://railway.app)
2. **Créer un nouveau projet** et lier le dépôt Git
3. **Ajouter le service PostgreSQL** depuis le marketplace Railway
4. **Configurer les variables d'environnement** dans Settings → Variables
5. **Railway détecte automatiquement le Dockerfile** et construit l'image
6. **Vérifier les logs** après déploiement pour s'assurer du démarrage

### 5. Configuration automatique PostgreSQL

Railway fournit automatiquement ces variables après ajout du service PostgreSQL :
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Utilisez-les dans vos variables :
```
SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
SPRING_DATASOURCE_USERNAME=${PGUSER}
SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
```

### 6. Volumes persistants (optionnel)

Pour les uploads persistants, ajoutez un volume :
1. Dans Railway, allez dans Settings → Volumes
2. Ajoutez un volume monté sur `/tmp/uploads`
3. Les variables `UPLOAD_DIR`, `ATTACHMENTS_DIR`, `DOCUMENT_ARCHIVE_DIR` pointeront vers ce volume

### 7. Commandes utiles

```bash
# Build local du projet
mvn clean package -Dmaven.test.skip=true

# Exécuter avec les variables d'environnement
java -Dserver.port=${PORT:-8000} -jar target/*.jar

# Vérifier les variables disponibles
env | grep PG
```

### 8. Dépannage

- **Erreur de connexion DB** : Vérifiez que le service PostgreSQL est ajouté et que les variables sont correctement liées
- **Erreur CORS** : Ajoutez l'URL de votre app Railway dans `CORS_ALLOWED_ORIGINS`
- **Erreur JWT** : Assurez-vous que `JWT_SECRET` est identique entre les environnements
- **Uploads perdus** : Activez un volume persistant ou utilisez B2 pour le stockage