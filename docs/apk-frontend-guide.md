# Guide d'intégration du module APK (côté frontend)

Ce document explique **comment ajouter le module APK** dans une application frontend qui consomme
le backend `Backend-java`. Il accompagne la référence API du `README.md` (section « Intégration
frontend ») et donne une démarche concrète, avec des exemples TypeScript/React.

> Backend concerné : endpoints `/apk/**`. Tous nécessitent un JWT (`Authorization: Bearer <token>`).

---

## 1. Vue d'ensemble

Le module APK permet de :

| Fonction      | Endpoint                         | Méthode |
|---------------|----------------------------------|---------|
| Uploader      | `/apk/upload`                    | POST    |
| Lister        | `/apk`                           | GET     |
| Lister (app)  | `/apk/application/{applicationId}` | GET  |
| Détail        | `/apk/{id}`                      | GET     |
| Télécharger   | `/apk/download/{id}`             | GET     |
| Supprimer     | `/apk/{id}`                      | DELETE  |

Le frontend ne stocke jamais le binaire : il l'upload via multipart et le récupère via le endpoint
de téléchargement. L'emplacement réel (disque local ou B2) est géré par le backend.

---

## 2. Modèle de données (`ApkFileDTO`)

```jsonc
{
  "id": 12,                       // Long
  "fileName": "3f2a...-e1.apk",   // nom unique côté serveur (à ne pas afficher)
  "originalFileName": "MonApp-2.3.1.apk", // nom lisible à afficher
  "fileSize": 18432000,           // octets
  "version": "2.3.1",             // nullable
  "packageName": "com.itaccess.monapp", // nullable
  "description": "Build de prod", // nullable
  "applicationId": 4,             // nullable (lien vers une Application)
  "uploadedBy": 1,                // Long (id utilisateur)
  "uploadDate": "2026-07-13T10:00:00",
  "downloadCount": 7              // incrémenté à chaque téléchargement
}
```

Types TypeScript suggérés :

```ts
export interface ApkFileDTO {
  id: number;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  version: string | null;
  packageName: string | null;
  description: string | null;
  applicationId: number | null;
  uploadedBy: number;
  uploadDate: string;
  downloadCount: number;
}
```

---

## 3. Client API (point de départ)

Créez un module `apkApi.ts` (ou équivalent). Exemple minimal :

```ts
const API = import.meta.env.VITE_API_URL ?? "https://<host>";
const headers = () => ({ Authorization: `Bearer ${getToken()}` });

export async function uploadApk(
  file: File,
  meta: { applicationId?: number; version?: string; packageName?: string; description?: string }
): Promise<ApkFileDTO> {
  const form = new FormData();
  form.append("file", file);
  if (meta.applicationId != null) form.append("applicationId", String(meta.applicationId));
  if (meta.version) form.append("version", meta.version);
  if (meta.packageName) form.append("packageName", meta.packageName);
  if (meta.description) form.append("description", meta.description);

  const res = await fetch(`${API}/apk/upload`, { method: "POST", headers: headers(), body: form });
  if (res.status === 400) throw new Error("Fichier invalide (extension .apk ou contenu non APK)");
  if (res.status === 401) throw new Error("Non authentifié");
  if (!res.ok) throw new Error(`Upload échoué (${res.status})`);
  return res.json();
}

export async function listApks(page = 0, size = 20): Promise<ApkFileDTO[]> {
  const res = await fetch(`${API}/apk?page=${page}&size=${size}`, { headers: headers() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function listApksByApplication(applicationId: number, page = 0, size = 20): Promise<ApkFileDTO[]> {
  const res = await fetch(`${API}/apk/application/${applicationId}?page=${page}&size=${size}`, { headers: headers() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function getApk(id: number): Promise<ApkFileDTO> {
  const res = await fetch(`${API}/apk/${id}`, { headers: headers() });
  if (res.status === 404) throw new Error("APK introuvable");
  if (!res.ok) throw new Error(`Erreur (${res.status})`);
  return res.json();
}

export async function deleteApk(id: number): Promise<void> {
  const res = await fetch(`${API}/apk/${id}`, { method: "DELETE", headers: headers() });
  if (res.status === 403) throw new Error("Suppression réservée à l'auteur ou un admin");
  if (res.status === 404) throw new Error("APK introuvable");
  if (!res.ok) throw new Error(`Suppression échouée (${res.status})`);
}
```

### Téléchargement (binaire)

Contrairement aux autres appels, le téléchargement renvoie un fichier, pas du JSON. Comme l'auth
est requise, on passe par `fetch` + `blob` (un simple `<a href>` ne transmettrait pas le JWT) :

```ts
export async function downloadApk(id: number, fallbackName = `apk-${id}.apk`): Promise<void> {
  const res = await fetch(`${API}/apk/download/${id}`, { headers: headers() });
  if (res.status === 404) throw new Error("Fichier physique introuvable");
  if (!res.ok) throw new Error(`Téléchargement impossible (${res.status})`);

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  // Récupère le nom proposé par le serveur si présent
  const cd = res.headers.get("Content-Disposition");
  const nameMatch = cd?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  a.download = nameMatch ? decodeURIComponent(nameMatch[1]) : fallbackName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
```

---

## 4. Composants recommandés (React)

### 4.1 Upload

```tsx
function ApkUploadForm({ applicationId, onUploaded }: { applicationId?: number; onUploaded: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [version, setVersion] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!file) return;
    setBusy(true); setError(null);
    try {
      await uploadApk(file, { applicationId, version });
      setFile(null); setVersion("");
      onUploaded(); // refresh la liste
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input type="file" accept=".apk" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
      <input placeholder="Version (ex. 2.3.1)" value={version} onChange={(e) => setVersion(e.target.value)} />
      <button type="submit" disabled={busy || !file}>{busy ? "Envoi…" : "Uploader l'APK"}</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </form>
  );
}
```

### 4.2 Liste + téléchargement + suppression

```tsx
function ApkList({ applicationId }: { applicationId?: number }) {
  const [apks, setApks] = useState<ApkFileDTO[]>([]);
  const currentUserId = useCurrentUser().id;
  const isAdmin = useCurrentUser().role === "admin";

  const refresh = () => (applicationId ? listApksByApplication(applicationId) : listApks()).then(setApks);
  useEffect(() => { refresh(); }, [applicationId]);

  async function handleDelete(id: number) {
    if (!confirm("Supprimer cet APK ?")) return;
    try { await deleteApk(id); refresh(); }
    catch (err) { alert((err as Error).message); } // affiche le 403 si non autorisé
  }

  return (
    <ul>
      {apks.map((a) => (
        <li key={a.id}>
          <span>{a.originalFileName} — v{a.version ?? "?"} — {a.downloadCount} dl</span>
          <button onClick={() => downloadApk(a.id, a.originalFileName)}>Télécharger</button>
          {(isAdmin || a.uploadedBy === currentUserId) && (
            <button onClick={() => handleDelete(a.id)}>Supprimer</button>
          )}
        </li>
      ))}
    </ul>
  );
}
```

> Astuce UX : n'affichez le bouton « Supprimer » que si `isAdmin || a.uploadedBy === currentUserId`,
> mais gérez quand même le `403` (la source de vérité est le backend).

---

## 5. Gestion d'erreurs

| Status | Signification | Comportement UI suggéré |
|--------|---------------|--------------------------|
| 400 | Fichier non `.apk` ou contenu non APK | Message d'erreur sur le formulaire d'upload |
| 401 | JWT absent/expiré | Rediriger vers login |
| 403 | Suppression par non-auteur/non-admin | « Action non autorisée » |
| 404 | APK ou fichier introuvable | « Introuvable », retirer de la liste |
| 500 | Erreur serveur | « Réessayez plus tard » |

Le backend renvoie `{ "error": "message" }` en JSON sur les erreurs.

---

## 6. Points d'attention

- **Auth obligatoire** sur tous les endpoints `/apk/**` — pensez au header `Authorization`.
- **Upload** : champ `file` (multipart). Extension `.apk` + contenu réel vérifié (signature ZIP).
  Un faux `.apk` est rejeté en `400`.
- **Téléchargement** : incrémente `downloadCount` à chaque succès (GET prévu non idempotent).
- **Suppression** : réservée à l'auteur ou un `admin` (`role=admin`).
- **Pagination** : `?page=&size=`. Sans paramètres, tous les résultats sont renvoyés.
- **Stockage B2** (prod/Railway) : transparent — le binaire arrive toujours via `/apk/download/{id}`.

---

## 7. Checklist d'intégration

- [ ] Client API `/apk/**` avec header `Authorization` automatique
- [ ] Type `ApkFileDTO` défini
- [ ] Formulaire d'upload (`.apk`, métadonnées, gestion `400`)
- [ ] Liste / grille des APK avec pagination éventuelle
- [ ] Bouton téléchargement (blob + `Content-Disposition`)
- [ ] Bouton suppression conditionnel (auteur/admin) + gestion `403`/`404`
- [ ] Gestion global des `401` (redirection login)
- [ ] Tests manuels : upload → liste → download → delete
