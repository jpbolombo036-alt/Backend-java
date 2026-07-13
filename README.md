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

## Intégration frontend (API APK)

**Authentification** : tous les endpoints `/apk/**` sont protégés (`anyRequest().authenticated()`).
Le JWT doit être fourni dans le header `Authorization: Bearer <token>` sur chaque requête.

**Base URL** : `https://<host>` (ex. `https://itaccess-backend-production-5145.up.railway.app`).

### Format de l'objet `ApkFileDTO`

```json
{
  "id": 12,
  "fileName": "3f2a...-e1.apk",
  "originalFileName": "MonApp-2.3.1.apk",
  "fileSize": 18432000,
  "version": "2.3.1",
  "packageName": "com.itaccess.monapp",
  "description": "Build de prod",
  "applicationId": 4,
  "uploadedBy": 1,
  "uploadDate": "2026-07-13T10:00:00",
  "downloadCount": 7
}
```

> Le champ `filePath` est interne (jamais exposé en JSON). Le binaire est servi via l'endpoint
> de téléchargement, que le stockage soit local ou B2 — le frontend n'a pas à connaître l'emplacement.

### Référence des endpoints

| Méthode | Endpoint | Auth | Succès | Erreurs possibles |
|---------|----------|------|--------|-------------------|
| POST | `/apk/upload` | JWT | `201` + `ApkFileDTO` | `400` (extension/format invalide), `401` |
| GET | `/apk` | JWT | `200` + `ApkFileDTO[]` | `401` |
| GET | `/apk/application/{applicationId}` | JWT | `200` + `ApkFileDTO[]` | `401` |
| GET | `/apk/{id}` | JWT | `200` + `ApkFileDTO` | `401`, `404` |
| GET | `/apk/download/{id}` | JWT | `200` + binaire (`application/vnd.android.package-archive`) | `401`, `404` |
| DELETE | `/apk/{id}` | JWT | `204` (No Content) | `401`, `403` (non auteur/admin), `404` |

### Pagination

`GET /apk` et `GET /apk/application/{id}` acceptent `?page=` (0-based) et `?size=`.
Sans paramètres, **tous les résultats sont renvoyés** (comportement historique conservé).

### Exemples (fetch / TypeScript)

```ts
const API = "https://<host>";
const auth = () => ({ Authorization: `Bearer ${token}` });

// 1) Uploader un APK
async function uploadApk(file: File, meta: { applicationId?: number; version?: string }) {
  const form = new FormData();
  form.append("file", file);
  if (meta.applicationId) form.append("applicationId", String(meta.applicationId));
  if (meta.version) form.append("version", meta.version);

  const res = await fetch(`${API}/apk/upload`, { method: "POST", headers: auth(), body: form });
  if (res.status === 400) throw new Error("Fichier invalide (extension .apk ou contenu non APK)");
  if (!res.ok) throw new Error(`Upload échoué (${res.status})`);
  return res.json() as Promise<ApkFileDTO>; // 201
}

// 2) Lister les APK (pagination optionnelle)
async function listApks(page = 0, size = 20) {
  const res = await fetch(`${API}/apk?page=${page}&size=${size}`, { headers: auth() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json() as Promise<ApkFileDTO[]>;
}

// 3) Télécharger un APK (déclenche le téléchargement navigateur + incrémente le compteur)
function downloadApk(id: number) {
  // Le header d'auth est requis : on passe par un fetch puis un blob pour garder le token.
  fetch(`${API}/apk/download/${id}`, { headers: auth() })
    .then((r) => (r.ok ? r.blob() : Promise.reject(r.status)))
    .then((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `apk-${id}.apk`;
      a.click();
      URL.revokeObjectURL(url);
    })
    .catch((status) => alert(`Téléchargement impossible (${status})`));
}

// 4) Supprimer un APK (auteur ou admin uniquement -> 403 sinon)
async function deleteApk(id: number) {
  const res = await fetch(`${API}/apk/${id}`, { method: "DELETE", headers: auth() });
  if (res.status === 403) throw new Error("Suppression réservée à l'auteur ou un admin");
  if (res.status === 404) throw new Error("APK introuvable");
  if (!res.ok) throw new Error(`Suppression échouée (${res.status})`);
  // 204
}
```

### Points d'attention pour le frontend

- **Upload** : le champ doit s'appeler `file` (multipart). L'extension `.apk` est obligatoire et le
  contenu réel est vérifié côté serveur (signature ZIP) — un faux `.apk` est rejeté en `400`.
- **Suppression** : afficher un message adapté en `403` (l'utilisateur n'est ni l'auteur ni admin).
- **Téléchargement** : l'endpoint renvoie le binaire avec `Content-Disposition: attachment` ;
  l'incrément du compteur a lieu à chaque appel réussi (GET non idempotent, prévu).
- **Stockage B2** (prod/Railway) : transparent pour le frontend, le binaire vient toujours de
  l'endpoint `/apk/download/{id}`.

