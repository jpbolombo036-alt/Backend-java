# Intégration du stockage B2 dans le module APK

Ce document explique **comment** l'intégration du stockage objet Backblaze B2 a été réalisée dans le
module APK (`/apk`), pourquoi, et quelles décisions ont été prises. Il s'adresse aux développeurs
backend et à l'agent frontend chargé de consommer l'API.

---

## 1. Contexte et problème

Le module APK stockait initialement les binaires sur le **système de fichiers local**
(`app.upload.dir`, défaut `/tmp/uploads/apk`). Deux limites surviennent en production (ex. Railway) :

1. **Persistance** : le filesystem des containers est éphémère → les fichiers sont perdus à chaque
   redéploiement/redémarrage.
2. **Multi-instances** : un volume n'est pas partagé entre replicas → un APK uploadé sur une instance
   n'est pas trouvable sur une autre.

Le projet disposait **déjà** d'un `B2StorageService` complet (upload/download/delete/exists) et du SDK
S3 dans le `pom.xml`. L'objectif était donc d'**exploiter ce qui existait** plutôt que d'ajouter une
nouvelle dépendance (Cloudinary, etc.).

---

## 2. Décision : B2 et non Cloudinary

| Critère | B2 (choisi) | Cloudinary |
|---------|-------------|------------|
| Dépendance | déjà présente (`B2StorageService`) | nouvelle (SDK Cloudinary) |
| Adapté aux gros binaires | oui (stockage objet) | pensé pour images/vidéo, APK en `raw` |
| Coût bande passante | faible pour APK | potentiellement élevé (APK 50–200 Mo × N dl) |
| Partage multi-instances | oui (stockage partagé) | oui |
| Effort | nul (réutilisation) | moyen |

➡️ B2 est le meilleur choix pour des APK et était déjà implémenté.

---

## 3. Principe d'architecture

Un **abstraction de stockage à deux modes**, commutée par la configuration :

- `B2_ENABLED=true` → le binaire va dans le bucket B2 (clé `apk/<uuid>.<ext>`).
- `B2_ENABLED=false` (défaut/dev) → écriture disque local (comportement historique conservé).

Le champ `filePath` de l'entité `ApkFile` stocke soit la **clé B2**, soit le **chemin local**.
C'est transparent pour le frontend : il consomme toujours `/apk/download/{id}`.

```
Upload     : validate ZIP -> B2.upload(file, "apk/uuid.apk") | Files.copy(local)
Download   : B2.downloadAsResource(key) | new UrlResource(localPath)
Delete     : B2.delete(key) | Files.delete(localPath)
```

---

## 4. Implémentation (`ApkService.java`)

### 4.1 Dépendances injectées

- `B2StorageService b2StorageService` — service existant.
- `B2Properties b2Properties` — lit `app.storage.b2.*` (`enabled`, `bucket`, clés…).
- Constantes : `APK_OBJECT_PREFIX = "apk/"`, `APK_CONTENT_TYPE = "application/vnd.android.package-archive"`.

### 4.2 Upload (`uploadApk`)

1. Validation du **contenu** APK (signature ZIP `PK\x03\x04`) via `isZipArchive(byte[])` — avant tout
   stockage.
2. Branchement :
   - B2 actif → `b2StorageService.upload(file, "apk/" + uniqueFileName, APK_CONTENT_TYPE)`,
     `filePath = "apk/" + uniqueFileName`.
   - Sinon → `Files.copy` dans `resolveWritableUploadDirectory()`, `filePath = chemin absolu`.
3. Sauvegarde de l'entité (la clé/chemin est persistée).

### 4.3 Téléchargement (`downloadApk` + `loadApkResource`)

- `downloadApk(id)` vérifie l'existence (B2 `exists()` ou `Files.exists`) **avant** d'incrémenter
  `downloadCount`, puis renvoie le DTO.
- `loadApkResource(storageKey, originalFileName)` :
  - B2 actif → `b2StorageService.downloadAsResource(storageKey, originalFileName, APK_CONTENT_TYPE)`
    (retourne une `Resource` en mémoire).
  - Sinon → `new UrlResource(Paths.get(storageKey).toUri())`.

### 4.4 Suppression (`deleteApk`)

- Contrôle de **propriété** (auteur ou `role=admin`) → `SecurityException` (→ 403) si refusé.
- Suppression du binaire : `b2StorageService.delete(filePath)` ou `Files.delete(localPath)`.

### 4.5 Helper factorisé

`deleteStoredFile(storageKey)` centralise la suppression (B2 ou local) et est réutilisé par
`deleteApk` **et** `updateApk` (remplacement de binaire).

---

## 5. Migration des anciens fichiers locaux (`ApkStorageMigration.java`)

Composant `CommandLineRunner` exécuté au démarrage **si `B2_ENABLED=true`** :

- Sélectionne les `apk_files` dont `filePath` n'est pas une clé B2 (non préfixée `apk/`).
- Pour chacun, si le fichier local existe : `upload(localPath, "apk/" + fileName, …)` vers B2, puis
  met à jour `filePath` avec la clé.
- **Idempotent** : après migration, la clé `apk/...` fait qu'un prochain démarrage est un no-op.
- Fichiers locaux absents → simple log d'avertissement (aucun crash).

```
ApkFile (filePath="/tmp/uploads/apk/x.apk")
        │  ApkStorageMigration (B2_ENABLED=true)
        ▼
ApkFile (filePath="apk/x.apk")  + objet dans le bucket B2
```

---

## 6. Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|----------|--------|------|
| `B2_ENABLED` | `false` | Active le stockage objet B2 |
| `B2_KEY_ID` | — | ID de clé B2 |
| `B2_APPLICATION_KEY` | — | Clé d'application B2 |
| `B2_BUCKET` | `itaccess-storage` | Nom du bucket |
| `B2_ENDPOINT` | — | Endpoint S3 B2 |
| `B2_REGION` | `us-east-005` | Région |

Sur Railway : poser `B2_ENABLED=true` + les clés. Aucun volume disque requis.

---

## 7. Impact frontend

- **Aucun changement d'API** : le binaire arrive toujours via `GET /apk/download/{id}` avec
  `Content-Disposition: attachment`. Le frontend ignore où le fichier est stocké.
- Seule différence observable : les fichiers survivent aux redéploiements et sont partagés entre
  instances.

---

## 8. Fichiers concernés

| Fichier | Rôle |
|---------|------|
| `service/ApkService.java` | logique de stockage B2/local, validation, migration inline |
| `service/ApkStorageMigration.java` | transfert auto local → B2 au démarrage |
| `config/B2Properties.java` | configuration B2 (déjà présent) |
| `service/B2StorageService.java` | client B2/S3 (déjà présent, réutilisé) |
| `entity/ApkFile.java` | `filePath` = clé B2 ou chemin local |
| `db/migration/V12__apk_files_foreign_keys.sql` | FK sur `apk_files` |

---

## 9. Points de vigilance

- En mode B2, les **anciennes lignes** avec un chemin local ne sont pas téléchargeables tant que
  `ApkStorageMigration` ne les a pas transférées (ou qu'elles n'existent plus côté local).
- `B2StorageService.upload(Path, …)` charge le fichier en mémoire (`readAllBytes`) : adapté aux
  tailles APK (≤ 150 Mo par la limite multipart), mais à surveiller si la limite monte.
- Le fallback local reste utile en dev ; ne pas le supprimer sans raison.
