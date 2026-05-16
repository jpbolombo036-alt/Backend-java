# 🧪 Tests Complets des Endpoints - IT Access Backend

## 📋 Table des matières
1. [Authentification](#authentification)
2. [Utilisateurs](#utilisateurs)
3. [Applications](#applications)
4. [Comptes](#comptes)
5. [Habilitations](#habilitations)
6. [Tests](#tests)
7. [Sessions de Test](#sessions-de-test)
8. [Todos](#todos)
9. [APK](#apk)

---

## 🔐 Authentification

### Base URL: `/auth`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| POST | `/token` | ❌ Non | Connexion utilisateur | username, password (body ou query) |
| GET | `/me` | ✅ Oui | Utilisateur actuel | Header: Authorization |
| POST | `/refresh-secret-key` | ❌ Non | Régénérer secret JWT | - |
| POST | `/forgot-password` | ❌ Non | Demande reset mot de passe | email (body) |
| POST | `/reset-password` | ❌ Non | Reset mot de passe | token, newPassword (body) |
| POST | `/init-admin` | ❌ Non | Créer admin initial | initKey (query) |
| POST | `/activate-user` | ✅ Admin | Activer utilisateur | username (query) |

### Tests Authentification

```bash
# 1. Initialiser l'admin (première fois)
POST http://localhost:8000/auth/init-admin?initKey=IT_ACCESS_INIT_2024_SECURE_KEY_CHANGE_ME
Response: Admin initial créé avec succès. Username: admin, Password: admin123

# 2. Connexion
POST http://localhost:8000/auth/token
Content-Type: application/json
{
  "username": "admin",
  "password": "admin123"
}
Response: { "accessToken": "jwt_token", "tokenType": "bearer" }

# 3. Obtenir utilisateur actuel
GET http://localhost:8000/auth/me
Authorization: Bearer jwt_token

# 4. Demande reset mot de passe
POST http://localhost:8000/auth/forgot-password
Content-Type: application/json
{
  "email": "admin@itaccess.com"
}

# 5. Reset mot de passe
POST http://localhost:8000/auth/reset-password
Content-Type: application/json
{
  "token": "reset_token",
  "newPassword": "newPassword123"
}

# 6. Activer utilisateur (admin only)
POST http://localhost:8000/auth/activate-user?username=testuser
Authorization: Bearer admin_jwt_token
```

---

## 👥 Utilisateurs

### Base URL: `/users`

| Méthode | Endpoint | Auth | Rôle | Description | Paramètres |
|---------|----------|------|------|-------------|------------|
| POST | `/users` | ✅ Oui | Admin | Créer utilisateur | UserDTO (body) |
| GET | `/users` | ✅ Oui | Admin | Liste utilisateurs | page, size, sortBy, sortDir |
| GET | `/users/{id}` | ✅ Oui | Admin | Utilisateur par ID | id (path) |
| PUT | `/users/{id}` | ✅ Oui | Admin | Modifier utilisateur | id (path), UserDTO (body) |
| DELETE | `/users/{id}` | ✅ Oui | Admin | Supprimer utilisateur | id (path) |
| GET | `/users/me` | ✅ Oui | Tous | Profil actuel | - |
| PUT | `/users/me` | ✅ Oui | Tous | Modifier profil | UserDTO (body) |
| PUT | `/users/me/password` | ✅ Oui | Tous | Changer mot de passe | PasswordChangeRequest (body) |

### Tests Utilisateurs

```bash
# 1. Créer utilisateur (admin only)
POST http://localhost:8000/users
Authorization: Bearer admin_jwt_token
Content-Type: application/json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "role": "user"
}

# 2. Lister tous les utilisateurs (admin only)
GET http://localhost:8000/users?page=0&size=10&sortBy=id&sortDir=asc
Authorization: Bearer admin_jwt_token

# 3. Obtenir utilisateur par ID (admin only)
GET http://localhost:8000/users/1
Authorization: Bearer admin_jwt_token

# 4. Modifier utilisateur (admin only)
PUT http://localhost:8000/users/1
Authorization: Bearer admin_jwt_token
Content-Type: application/json
{
  "username": "testuser",
  "email": "newemail@example.com",
  "role": "user"
}

# 5. Supprimer utilisateur (admin only)
DELETE http://localhost:8000/users/1
Authorization: Bearer admin_jwt_token

# 6. Obtenir profil actuel
GET http://localhost:8000/users/me
Authorization: Bearer jwt_token

# 7. Modifier profil actuel
PUT http://localhost:8000/users/me
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "username": "testuser",
  "email": "updated@example.com"
}

# 8. Changer mot de passe
PUT http://localhost:8000/users/me/password
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "oldPassword": "password123",
  "newPassword": "newPassword123"
}
```

---

## 📱 Applications

### Base URL: `/applications`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/applications` | ✅ Oui | Liste applications | page, size, sortBy, sortDir |
| GET | `/applications/{id}` | ✅ Oui | Application par ID | id (path) |
| POST | `/applications` | ✅ Oui | Créer application | ApplicationDTO (body) |
| PUT | `/applications/{id}` | ✅ Oui | Modifier application | id (path), ApplicationDTO (body) |
| DELETE | `/applications/{id}` | ✅ Oui | Supprimer application | id (path) |

### Tests Applications

```bash
# 1. Lister toutes les applications
GET http://localhost:8000/applications?page=0&size=10&sortBy=id&sortDir=asc
Authorization: Bearer jwt_token

# 2. Obtenir application par ID
GET http://localhost:8000/applications/1
Authorization: Bearer jwt_token

# 3. Créer application
POST http://localhost:8000/applications
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "nom": "Application Test",
  "description": "Description de l'application",
  "version": "1.0.0",
  "environnement": "production"
}

# 4. Modifier application
PUT http://localhost:8000/applications/1
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "nom": "Application Modifiée",
  "description": "Nouvelle description",
  "version": "2.0.0"
}

# 5. Supprimer application
DELETE http://localhost:8000/applications/1
Authorization: Bearer jwt_token
```

---

## 🔑 Comptes

### Base URL: `/comptes`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/comptes` | ✅ Oui | Liste comptes | page, size, sortBy, sortDir |
| GET | `/comptes/{id}` | ✅ Oui | Compte par ID | id (path) |
| POST | `/comptes` | ✅ Oui | Créer compte | CompteRequest (body) |
| PUT | `/comptes/{id}` | ✅ Oui | Modifier compte | id (path), CompteRequest (body) |
| DELETE | `/comptes/{id}` | ✅ Oui | Supprimer compte | id (path) |

### Tests Comptes

```bash
# 1. Lister tous les comptes
GET http://localhost:8000/comptes?page=0&size=10&sortBy=id&sortDir=asc
Authorization: Bearer jwt_token

# 2. Obtenir compte par ID
GET http://localhost:8000/comptes/1
Authorization: Bearer jwt_token

# 3. Créer compte
POST http://localhost:8000/comptes
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "applicationId": 1,
  "username": "compte_test",
  "code": "secret_code",
  "role": "admin",
  "commentaire": "Compte de test"
}

# 4. Modifier compte
PUT http://localhost:8000/comptes/1
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "applicationId": 1,
  "username": "compte_modifie",
  "role": "user"
}

# 5. Supprimer compte
DELETE http://localhost:8000/comptes/1
Authorization: Bearer jwt_token
```

---

## 🔒 Habilitations

### Base URL: `/habilitations`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/habilitations` | ❌ Non | Liste habilitations | - |
| POST | `/habilitations` | ❌ Non | Créer habilitation | HabilitationDTO (body) |
| DELETE | `/habilitations/{id}` | ❌ Non | Supprimer habilitation | id (path) |

### Tests Habilitations

```bash
# 1. Lister toutes les habilitations
GET http://localhost:8000/habilitations

# 2. Créer habilitation
POST http://localhost:8000/habilitations
Content-Type: application/json
{
  "compteId": 1,
  "permission": "read_write"
}

# 3. Supprimer habilitation
DELETE http://localhost:8000/habilitations/1
```

---

## 🧪 Tests

### Base URL: `/tests`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/tests` | ❌ Non | Liste tests | sessionId (query, optionnel) |
| GET | `/tests/{id}` | ❌ Non | Test par ID | id (path) |
| POST | `/tests` | ✅ Oui | Créer test | TestRequest (body) |
| PUT | `/tests/{id}` | ❌ Non | Modifier test | id (path), TestRequest (body) |
| DELETE | `/tests/{id}` | ❌ Non | Supprimer test | id (path) |

### Tests Tests

```bash
# 1. Lister tous les tests
GET http://localhost:8000/tests

# 2. Lister tests par session
GET http://localhost:8000/tests?sessionId=1

# 3. Obtenir test par ID
GET http://localhost:8000/tests/1

# 4. Créer test
POST http://localhost:8000/tests
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "sessionId": 1,
  "applicationId": 1,
  "fonction": "Test fonctionnalité X",
  "precondition": "Condition préalable",
  "etapes": "Étape 1, Étape 2",
  "resultatAttendu": "Résultat attendu"
}

# 5. Modifier test
PUT http://localhost:8000/tests/1
Content-Type: application/json
{
  "fonction": "Test modifié",
  "resultatAttendu": "Nouveau résultat"
}

# 6. Supprimer test
DELETE http://localhost:8000/tests/1
```

---

## 📋 Sessions de Test

### Base URL: `/test-sessions`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/test-sessions` | ❌ Non | Liste sessions | page, size, sortBy, sortDir |
| GET | `/test-sessions/{id}` | ❌ Non | Session par ID | id (path) |
| POST | `/test-sessions` | ✅ Oui | Créer session | TestSessionDTO (body) |
| PUT | `/test-sessions/{id}` | ✅ Oui | Modifier session | id (path), TestSessionDTO (body) |
| DELETE | `/test-sessions/{id}` | ✅ Oui | Supprimer session | id (path) |

### Tests Sessions

```bash
# 1. Lister toutes les sessions
GET http://localhost:8000/test-sessions?page=0&size=10

# 2. Obtenir session par ID
GET http://localhost:8000/test-sessions/1

# 3. Créer session
POST http://localhost:8000/test-sessions
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "nom": "Session Test 1",
  "description": "Description de la session",
  "applicationId": 1,
  "environnement": "production",
  "version": "1.0.0"
}

# 4. Modifier session
PUT http://localhost:8000/test-sessions/1
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "nom": "Session Modifiée",
  "statut": "completed"
}

# 5. Supprimer session
DELETE http://localhost:8000/test-sessions/1
Authorization: Bearer jwt_token
```

---

## ✅ Todos

### Base URL: `/todos`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| GET | `/todos` | ✅ Oui | Liste tâches | - |
| GET | `/todos/{id}` | ❌ Non | Tâche par ID | id (path) |
| POST | `/todos` | ✅ Oui | Créer tâche | TodoRequest (body) |
| PUT | `/todos/{id}` | ❌ Non | Modifier tâche | id (path), TodoRequest (body) |
| DELETE | `/todos/{id}` | ❌ Non | Supprimer tâche | id (path) |
| PATCH | `/todos/{id}/toggle` | ❌ Non | Basculer état | id (path) |

### Tests Todos

```bash
# 1. Lister toutes les tâches (admin voit toutes)
GET http://localhost:8000/todos
Authorization: Bearer jwt_token

# 2. Obtenir tâche par ID
GET http://localhost:8000/todos/1

# 3. Créer tâche
POST http://localhost:8000/todos
Authorization: Bearer jwt_token
Content-Type: application/json
{
  "title": "Tâche de test",
  "description": "Description de la tâche",
  "priority": "high",
  "dueDate": "2024-12-31"
}

# 4. Modifier tâche
PUT http://localhost:8000/todos/1
Content-Type: application/json
{
  "title": "Tâche modifiée",
  "completed": true
}

# 5. Supprimer tâche
DELETE http://localhost:8000/todos/1

# 6. Basculer état (toggle)
PATCH http://localhost:8000/todos/1/toggle
```

---

## 📦 APK

### Base URL: `/apk`

| Méthode | Endpoint | Auth | Description | Paramètres |
|---------|----------|------|-------------|------------|
| POST | `/apk/upload` | ✅ Oui | Upload APK | file (multipart), applicationId, version, packageName, description |
| GET | `/apk` | ❌ Non | Liste APK | - |
| GET | `/apk/{id}` | ❌ Non | APK par ID | id (path) |
| GET | `/apk/download/{id}` | ❌ Non | Télécharger APK | id (path) |
| GET | `/apk/application/{applicationId}` | ❌ Non | APK par application | applicationId (path) |
| DELETE | `/apk/{id}` | ❌ Non | Supprimer APK | id (path) |

### Tests APK

```bash
# 1. Upload APK
POST http://localhost:8000/apk/upload
Authorization: Bearer jwt_token
Content-Type: multipart/form-data
file: @application.apk
applicationId: 1
version: "1.0.0"
packageName: "com.example.app"
description: "Application de test"

# 2. Lister tous les APK
GET http://localhost:8000/apk

# 3. Obtenir APK par ID
GET http://localhost:8000/apk/1

# 4. Télécharger APK
GET http://localhost:8000/apk/download/1

# 5. Lister APK par application
GET http://localhost:8000/apk/application/1

# 6. Supprimer APK
DELETE http://localhost:8000/apk/1
```

---

## 🔐 Sécurité et Autorisations

### Rôles
- **admin** : Accès complet à tous les endpoints
- **user** : Accès limité à ses propres ressources

### Endpoints sans authentification
- `/auth/token` (connexion)
- `/auth/forgot-password`
- `/auth/reset-password`
- `/auth/init-admin` (avec clé de sécurité)
- `/habilitations/*`
- `/tests/*` (GET)
- `/todos/{id}` (GET)
- `/todos/{id}/toggle`
- `/apk/*` (GET, DELETE)

### Endpoints avec authentification admin
- `/users/*` (sauf `/me`)
- `/auth/activate-user`

### Endpoints avec authentification utilisateur
- `/applications/*`
- `/comptes/*`
- `/test-sessions/*` (POST, PUT, DELETE)
- `/tests/*` (POST)
- `/todos/*` (POST)
- `/apk/upload`

---

## 🚨 Problèmes de Sécurité Identifiés

### ❌ Critiques
1. **Habilitations sans authentification** : N'importe qui peut créer/supprimer des permissions
2. **Tests sans authentification** : Modifications possibles sans authentification
3. **Todos sans authentification** : Modifications possibles sans authentification
4. **APK sans authentification** : Suppression possible sans authentification

### ⚠️ Moyens
1. **Pas de rate limiting** : Attaques par force brute possibles
2. **Pas de validation email** : Comptes fake possibles

---

## ✅ Recommandations

### Immédiat
1. Ajouter `@PreAuthorize` sur les endpoints critiques sans authentification
2. Implémenter rate limiting sur les endpoints de connexion
3. Ajouter validation email avant activation de compte

### Court terme
1. Ajouter des tests unitaires
2. Implémenter l'audit trail
3. Ajouter des logs de sécurité

### Long terme
1. Implémenter 2FA
2. Ajouter monitoring
3. Optimiser les performances

---

## 📊 Statistiques

- **Total endpoints** : 45
- **Endpoints sans auth** : 15 (33%)
- **Endpoints avec auth admin** : 7 (16%)
- **Endpoints avec auth user** : 23 (51%)
- **Endpoints sécurisés** : 30 (67%)

**Score de sécurité** : 67/100 - Améliorations nécessaires
