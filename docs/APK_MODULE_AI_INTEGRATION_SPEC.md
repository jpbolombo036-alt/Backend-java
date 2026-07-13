# APK Module — Frontend Integration Spec (AI-agent ready)

> **Purpose:** Self-contained specification for an AI coding agent to implement the **APK module**
> in the frontend, consuming the backend `Backend-java` `/apk/**` API. Hand this file to the agent
> together with the frontend repository. No further clarification from a human should be required.

---

## 1. Task

Add APK (Android package) management to the frontend:

- **Upload** an APK file (with optional metadata).
- **List** uploaded APKs (all, or filtered by `applicationId`).
- **Download** an APK binary.
- **Delete** an APK (restricted to owner or admin).

The frontend never stores the binary; it uploads via multipart and retrieves it through the
download endpoint. The backend decides storage (local disk or B2) — frontend is unaffected.

---

## 2. Backend API contract (authoritative)

**Base URL:** existing app API base (env var, e.g. `VITE_API_URL`). All paths below are relative.
**Auth:** `Authorization: Bearer <JWT>` on **every** request (`anyRequest().authenticated()`).
Errors return JSON `{ "error": "<message>" }`.

| # | Method | Path | Body / Params | Success | Error codes |
|---|--------|------|---------------|---------|-------------|
| 1 | POST | `/apk/upload` | `multipart/form-data` | `201` + `ApkFileDTO` | `400`, `401` |
| 2 | GET | `/apk` | query `page`,`size` (optional) | `200` + `ApkFileDTO[]` | `401` |
| 3 | GET | `/apk/application/{applicationId}` | query `page`,`size` | `200` + `ApkFileDTO[]` | `401` |
| 4 | GET | `/apk/{id}` | — | `200` + `ApkFileDTO` | `401`, `404` |
| 5 | GET | `/apk/download/{id}` | — | `200` + binary (`application/vnd.android.package-archive`) | `401`, `404` |
| 6 | DELETE | `/apk/{id}` | — | `204` No Content | `401`, `403`, `404` |

### 2.1 `POST /apk/upload`

`Content-Type: multipart/form-data`. Fields:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `file` | File | yes | must end with `.apk`; content is also verified (ZIP signature). Invalid → `400`. |
| `applicationId` | number | no | links APK to an Application. |
| `version` | string | no | e.g. `2.3.1`. |
| `packageName` | string | no | e.g. `com.itaccess.app`. |
| `description` | string | no | free text. |

### 2.2 `GET /apk` and `GET /apk/application/{applicationId}`

Optional pagination: `?page=0&size=20`. **If omitted, ALL rows are returned** (no regression).
Response: JSON array of `ApkFileDTO`.

### 2.3 `GET /apk/download/{id}`

Returns the binary. Response headers include `Content-Disposition: attachment; filename="<originalFileName>"`
and `Content-Type: application/vnd.android.package-archive`. **Each successful call increments
`downloadCount`** (GET is intentionally non-idempotent — this is expected, do not "fix" it).

### 2.4 `DELETE /apk/{id}`

Removes the APK (DB row + stored file). Allowed only if the caller is the uploader
(`uploadedBy == current user id`) **or** has `role == "admin"`. Otherwise → `403`.

---

## 3. Data model — `ApkFileDTO`

```jsonc
{
  "id": 12,                       // number  (Long)
  "fileName": "3f2a...-e1.apk",   // string  — server-side unique name (DO NOT display)
  "originalFileName": "MonApp-2.3.1.apk", // string — human filename to display
  "fileSize": 18432000,           // number  — bytes
  "version": "2.3.1",             // string | null
  "packageName": "com.itaccess.monapp", // string | null
  "description": "Build de prod", // string | null
  "applicationId": 4,             // number | null
  "uploadedBy": 1,                // number  — uploader user id
  "uploadDate": "2026-07-13T10:00:00", // string (ISO-8601)
  "downloadCount": 7              // number
}
```

TypeScript:

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

> ⚠️ `fileName` is internal (server storage key). Never show it to users; use `originalFileName`.

---

## 4. Implementation instructions for the agent

1. **Explore the repo first.** Locate, and reuse:
   - The existing HTTP/API client and how the JWT is attached (auth context, axios interceptor,
     or a `getToken()` helper). **Do not create a new auth mechanism.**
   - Existing env config for the API base URL. Use it; fall back to a configurable constant.
   - The routing/navigation structure and component style (hooks, state lib, UI kit).
2. **Add API methods** to the existing API module (or a new `apk` API file that imports the shared
   client). Mirror existing method signatures/styling.
3. **Add the UI** as a new "APK" section/page consistent with the app's existing navigation
   (place it near "Applications" if such a concept exists).
4. **Handle errors** per the table in §6. Map `401` → existing "redirect to login" flow.
5. **Do not add new dependencies** unless strictly necessary; prefer what the repo already uses.

---

## 5. Ready-to-use code (adapt to repo conventions)

### 5.1 API client (`apkApi.ts`)

```ts
// Adapt: use the repo's existing API base + auth header helper.
const API = (import.meta as any).env?.VITE_API_URL ?? "https://<BACKEND_HOST>";
const authHeader = (): Record<string, string> => ({
  Authorization: `Bearer ${getTokenFromApp()}`, // replace with repo's token accessor
});

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

  const res = await fetch(`${API}/apk/upload`, { method: "POST", headers: authHeader(), body: form });
  if (res.status === 400) throw new Error("Fichier invalide (extension .apk ou contenu non APK)");
  if (res.status === 401) throw new Error("Non authentifié");
  if (!res.ok) throw new Error(`Upload échoué (${res.status})`);
  return res.json();
}

export async function listApks(page = 0, size = 20): Promise<ApkFileDTO[]> {
  const res = await fetch(`${API}/apk?page=${page}&size=${size}`, { headers: authHeader() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function listApksByApplication(applicationId: number, page = 0, size = 20): Promise<ApkFileDTO[]> {
  const res = await fetch(`${API}/apk/application/${applicationId}?page=${page}&size=${size}`, { headers: authHeader() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function getApk(id: number): Promise<ApkFileDTO> {
  const res = await fetch(`${API}/apk/${id}`, { headers: authHeader() });
  if (res.status === 404) throw new Error("APK introuvable");
  if (!res.ok) throw new Error(`Erreur (${res.status})`);
  return res.json();
}

export async function deleteApk(id: number): Promise<void> {
  const res = await fetch(`${API}/apk/${id}`, { method: "DELETE", headers: authHeader() });
  if (res.status === 403) throw new Error("Suppression réservée à l'auteur ou un admin");
  if (res.status === 404) throw new Error("APK introuvable");
  if (!res.ok) throw new Error(`Suppression échouée (${res.status})`);
  // 204 on success
}

// Download: must send auth -> fetch + blob (a plain <a href> would NOT include the JWT).
export async function downloadApk(id: number, fallbackName = `apk-${id}.apk`): Promise<void> {
  const res = await fetch(`${API}/apk/download/${id}`, { headers: authHeader() });
  if (res.status === 404) throw new Error("Fichier physique introuvable");
  if (!res.ok) throw new Error(`Téléchargement impossible (${res.status})`);

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  const cd = res.headers.get("Content-Disposition");
  const m = cd?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  a.download = m ? decodeURIComponent(m[1]) : fallbackName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
```

### 5.2 React components (example)

```tsx
function ApkUploadForm({ applicationId, onUploaded }: { applicationId?: number; onUploaded: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [version, setVersion] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!file) return;
    setBusy(true); setError(null);
    try {
      await uploadApk(file, { applicationId, version });
      setFile(null); setVersion("");
      onUploaded();
    } catch (err) { setError((err as Error).message); }
    finally { setBusy(false); }
  }

  return (
    <form onSubmit={submit}>
      <input type="file" accept=".apk" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
      <input placeholder="Version (ex. 2.3.1)" value={version} onChange={(e) => setVersion(e.target.value)} />
      <button type="submit" disabled={busy || !file}>{busy ? "Envoi…" : "Uploader l'APK"}</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </form>
  );
}

function ApkList({ applicationId }: { applicationId?: number }) {
  const [apks, setApks] = useState<ApkFileDTO[]>([]);
  const me = useCurrentUser(); // replace with repo's current-user hook/context
  const isAdmin = me.role === "admin";

  const refresh = () =>
    (applicationId ? listApksByApplication(applicationId) : listApks()).then(setApks);
  useEffect(() => { refresh(); }, [applicationId]);

  async function remove(id: number) {
    if (!confirm("Supprimer cet APK ?")) return;
    try { await deleteApk(id); refresh(); }
    catch (err) { alert((err as Error).message); }
  }

  return (
    <ul>
      {apks.map((a) => (
        <li key={a.id}>
          <span>{a.originalFileName} — v{a.version ?? "?"} — {a.downloadCount} téléchargements</span>
          <button onClick={() => downloadApk(a.id, a.originalFileName)}>Télécharger</button>
          {(isAdmin || a.uploadedBy === me.id) && (
            <button onClick={() => remove(a.id)}>Supprimer</button>
          )}
        </li>
      ))}
    </ul>
  );
}
```

---

## 6. Error handling map

| Status | Meaning | UI behavior |
|--------|---------|-------------|
| 400 | Not `.apk` or content not a valid APK | Inline error on upload form |
| 401 | Missing/expired JWT | Trigger existing "redirect to login" |
| 403 | Delete by non-owner/non-admin | "Action non autorisée" |
| 404 | APK or file not found | "Introuvable"; remove from list |
| 500 | Server error | "Réessayez plus tard" |

---

## 7. Hard rules (must respect)

- Attach `Authorization: Bearer <token>` on **all** `/apk/**` calls.
- Download via `fetch` + `blob` (auth needed); do not use a raw `<a href>` to the endpoint.
- Delete button: show only when `isAdmin || apk.uploadedBy === currentUserId`, but still handle `403`.
- Never display `fileName` (internal); display `originalFileName`.
- Pagination is optional; omitting `page`/`size` returns everything.
- Do NOT "fix" the download counter increment (it is intended behavior).

---

## 8. Acceptance criteria (definition of done)

- [ ] API methods added reusing the existing client/auth (no new auth system).
- [ ] `ApkFileDTO` type defined.
- [ ] Upload form: `.apk` only, optional metadata, shows `400` errors.
- [ ] List/grid shows APKs with pagination support; uses `originalFileName`.
- [ ] Download triggers a file save using the server filename; increments counter.
- [ ] Delete is conditional (owner/admin) and handles `403`/`404`.
- [ ] `401` routed through existing auth flow.
- [ ] Manual test passes: upload → list → download → delete.
