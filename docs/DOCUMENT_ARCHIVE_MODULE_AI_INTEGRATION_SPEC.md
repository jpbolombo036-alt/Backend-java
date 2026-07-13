# Document Archive Module — Frontend Integration Spec (AI-agent ready)

> **Purpose:** Self-contained specification for an AI coding agent to implement the **Document Archive**
> module in the frontend, consuming the backend `Backend-java` `/document-archive/**` API. Hand this file
> to the agent together with the frontend repository. No further clarification from a human should be required.

---

## 1. Task

Add document archive management to the frontend (company PDF/Word documents):

- **Upload** a document (PDF or Word) with metadata (title required, plus description/category/tags/author).
- **List** documents (paginated) and filter by **category** or **search** by text.
- **Download** a document binary.
- **Update** a document (metadata and/or replace the binary).
- **Delete** a document (restricted to owner or admin).

The frontend never stores the binary; it uploads via multipart and retrieves it through the download
endpoint. Storage is B2 (if `B2_ENABLED=true`) or local disk (otherwise) — **transparent for the frontend**.

---

## 2. Backend API contract (authoritative)

**Base URL:** existing app API base (env var, e.g. `VITE_API_URL`). All paths below are relative.
**Auth:** `Authorization: Bearer <JWT>` on **every** request (`anyRequest().authenticated()`).
Errors return JSON `{ "error": "<message>" }`.

| # | Method | Path | Body / Params | Success | Error codes |
|---|--------|------|---------------|---------|-------------|
| 1 | POST | `/document-archive/upload` | `multipart/form-data` | `201` + `DocumentArchiveDTO` | `400`, `401` |
| 2 | GET | `/document-archive/download/{id}` | — | `200` + binary (`application/pdf` / Word / octet-stream) | `401`, `404` |
| 3 | GET | `/document-archive` | `page` (0-based, def 0), `size` (def 20) | `200` + `Page<DocumentArchiveDTO>` | `401` |
| 4 | GET | `/document-archive/{id}` | — | `200` + `DocumentArchiveDTO` | `401`, `404` |
| 5 | GET | `/document-archive/category/{category}` | — | `200` + `DocumentArchiveDTO[]` | `401` |
| 6 | GET | `/document-archive/search` | `q` (string) | `200` + `DocumentArchiveDTO[]` | `401` |
| 7 | PUT | `/document-archive/{id}` | `multipart/form-data` (file optional) | `200` + `DocumentArchiveDTO` | `400`, `401`, `403`, `404` |
| 8 | DELETE | `/document-archive/{id}` | — | `204` No Content | `401`, `403`, `404` |

### 2.1 `POST /document-archive/upload`  ⚠️ multipart requis

`Content-Type: multipart/form-data`. Fields:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `file` | File | yes | PDF (`.pdf`), Word (`.doc`, `.docx`) only. Other formats → `400`. |
| `title` | string | **yes** (`@NotBlank`) | sinon `400` (validation) |
| `description` | string | no | |
| `category` | string | no | |
| `tags` | string | no | |
| `author` | string | no | |

> ⚠️ **Erreur fréquente (`Current request is not a multipart request`)** : le front envoie la requête
> autrement qu'en `multipart/form-data` (souvent du JSON, ou un `Content-Type` écrasé par un intercepteur).
> Envoyer un **`FormData`** et **ne pas** forcer `Content-Type: application/json`. Voir §5.1.

### 2.2 `GET /document-archive` (pagination)

`?page=0&size=20`. Response is a Spring `Page` object:
```jsonc
{
  "content": [ /* DocumentArchiveDTO[] */ ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "last": false
}
```

### 2.3 `GET /document-archive/download/{id}`

Returns the binary. Headers include `Content-Disposition: attachment; filename="<originalFileName>"`
and the document's `contentType`. Each successful call increments `downloadCount`
(GET intentionally non-idempotent — expected, do not "fix" it).

### 2.4 `PUT /document-archive/{id}`

`multipart/form-data`, `file` **optional**. Metadata fields (`title`, `description`, `category`,
`tags`, `author`) are all optional; only provided ones are updated. If `file` is provided, it replaces
the binary (must be PDF/Word). Allowed only if caller is the uploader or `role == "admin"` → else `403`.

### 2.5 `DELETE /document-archive/{id}`

Removes the document (DB row + stored file). Allowed only if the caller is the uploader
(`uploadedBy == current user id`) **or** has `role == "admin"`. Otherwise → `403`.

---

## 3. Data model — `DocumentArchiveDTO`

```jsonc
{
  "id": 7,
  "fileName": "9f1c...-a2.pdf",     // server-side unique name (DO NOT display)
  "originalFileName": "Contrat-2026.pdf",
  "fileSize": 245000,
  "contentType": "application/pdf",
  "title": "Contrat 2026",
  "description": "Contrat signé",
  "category": "Juridique",
  "tags": "contrat,2026",
  "author": "Jean",
  "uploadedBy": 1,
  "uploadedByUsername": "jdoe",
  "uploadDate": "2026-07-13T10:00:00",
  "updateDate": "2026-07-13T11:30:00", // null si jamais modifié
  "downloadCount": 3
}
```

TypeScript:

```ts
export interface DocumentArchiveDTO {
  id: number;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string | null;
  title: string;
  description: string | null;
  category: string | null;
  tags: string | null;
  author: string | null;
  uploadedBy: number;
  uploadedByUsername: string | null;
  uploadDate: string;
  updateDate: string | null;
  downloadCount: number;
}
```

> ⚠️ `fileName` is internal (server storage key). Never display it; use `originalFileName`.

---

## 4. Implementation instructions for the agent

1. **Explore the repo first.** Locate, and reuse:
   - The existing HTTP/API client and how the JWT is attached (auth context, axios interceptor,
     or a `getToken()` helper). **Do not create a new auth mechanism.**
   - Existing env config for the API base URL. Use it; fall back to a configurable constant.
   - The routing/navigation structure and component style (hooks, state lib, UI kit).
2. **Add API methods** to the existing API module (or a new `documentArchive` API file that imports
   the shared client). Mirror existing method signatures/styling.
3. **Add the UI** as a new "Documents" / "Archives" section consistent with the app's navigation.
4. **Handle errors** per the table in §6. Map `401` → existing "redirect to login" flow.
5. **Do not add new dependencies** unless strictly necessary; prefer what the repo already uses.

---

## 5. Ready-to-use code (adapt to repo conventions)

### 5.1 API client (`documentArchiveApi.ts`)

```ts
// Adapt: use the repo's existing API base + auth header helper.
const API = (import.meta as any).env?.VITE_API_URL ?? "https://<BACKEND_HOST>";
const authHeader = (): Record<string, string> => ({ Authorization: `Bearer ${getTokenFromApp()}` });

// ⚠️ Upload : FormData UNIQUEMENT. Ne pas setter Content-Type (le navigateur/axios ajoute la boundary).
export async function uploadDocument(
  file: File,
  meta: { title: string; description?: string; category?: string; tags?: string; author?: string }
): Promise<DocumentArchiveDTO> {
  const form = new FormData();
  form.append("file", file);
  form.append("title", meta.title);
  if (meta.description) form.append("description", meta.description);
  if (meta.category) form.append("category", meta.category);
  if (meta.tags) form.append("tags", meta.tags);
  if (meta.author) form.append("author", meta.author);

  const res = await fetch(`${API}/document-archive/upload`, { method: "POST", headers: authHeader(), body: form });
  if (res.status === 400) throw new Error("Requête invalide (titre requis ou format non supporté : PDF/Word)");
  if (res.status === 401) throw new Error("Non authentifié");
  if (!res.ok) throw new Error(`Upload échoué (${res.status})`);
  return res.json();
}

export async function getDocuments(page = 0, size = 20): Promise<{ content: DocumentArchiveDTO[]; totalElements: number; totalPages: number }> {
  const res = await fetch(`${API}/document-archive?page=${page}&size=${size}`, { headers: authHeader() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function getDocumentById(id: number): Promise<DocumentArchiveDTO> {
  const res = await fetch(`${API}/document-archive/${id}`, { headers: authHeader() });
  if (res.status === 404) throw new Error("Document introuvable");
  if (!res.ok) throw new Error(`Erreur (${res.status})`);
  return res.json();
}

export async function getByCategory(category: string): Promise<DocumentArchiveDTO[]> {
  const res = await fetch(`${API}/document-archive/category/${encodeURIComponent(category)}`, { headers: authHeader() });
  if (!res.ok) throw new Error(`Liste échouée (${res.status})`);
  return res.json();
}

export async function searchDocuments(q: string): Promise<DocumentArchiveDTO[]> {
  const res = await fetch(`${API}/document-archive/search?q=${encodeURIComponent(q)}`, { headers: authHeader() });
  if (!res.ok) throw new Error(`Recherche échouée (${res.status})`);
  return res.json();
}

export async function updateDocument(
  id: number,
  meta: { file?: File; title?: string; description?: string; category?: string; tags?: string; author?: string }
): Promise<DocumentArchiveDTO> {
  const form = new FormData();
  if (meta.file) form.append("file", meta.file);
  if (meta.title !== undefined) form.append("title", meta.title);
  if (meta.description !== undefined) form.append("description", meta.description);
  if (meta.category !== undefined) form.append("category", meta.category);
  if (meta.tags !== undefined) form.append("tags", meta.tags);
  if (meta.author !== undefined) form.append("author", meta.author);

  const res = await fetch(`${API}/document-archive/${id}`, { method: "PUT", headers: authHeader(), body: form });
  if (res.status === 400) throw new Error("Requête invalide");
  if (res.status === 403) throw new Error("Modification réservée à l'auteur ou un admin");
  if (res.status === 404) throw new Error("Document introuvable");
  if (!res.ok) throw new Error(`Mise à jour échouée (${res.status})`);
  return res.json();
}

export async function deleteDocument(id: number): Promise<void> {
  const res = await fetch(`${API}/document-archive/${id}`, { method: "DELETE", headers: authHeader() });
  if (res.status === 403) throw new Error("Suppression réservée à l'auteur ou un admin");
  if (res.status === 404) throw new Error("Document introuvable");
  if (!res.ok) throw new Error(`Suppression échouée (${res.status})`);
  // 204 on success
}

// Download: must send auth -> fetch + blob (a plain <a href> would NOT include the JWT).
export async function downloadDocument(id: number, fallbackName = `doc-${id}`): Promise<void> {
  const res = await fetch(`${API}/document-archive/download/${id}`, { headers: authHeader() });
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
function DocumentUploadForm({ onUploaded }: { onUploaded: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!file || !title) return;
    setBusy(true); setError(null);
    try {
      await uploadDocument(file, { title, category });
      setFile(null); setTitle(""); setCategory("");
      onUploaded();
    } catch (err) { setError((err as Error).message); }
    finally { setBusy(false); }
  }

  return (
    <form onSubmit={submit}>
      <input type="file" accept=".pdf,.doc,.docx" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
      <input placeholder="Titre (requis)" value={title} onChange={(e) => setTitle(e.target.value)} required />
      <input placeholder="Catégorie" value={category} onChange={(e) => setCategory(e.target.value)} />
      <button type="submit" disabled={busy || !file || !title}>{busy ? "Envoi…" : "Uploader le document"}</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </form>
  );
}

function DocumentList() {
  const [docs, setDocs] = useState<DocumentArchiveDTO[]>([]);
  const me = useCurrentUser(); // replace with repo's current-user hook/context
  const isAdmin = me.role === "admin";

  const refresh = () => getDocuments().then((p) => setDocs(p.content));
  useEffect(() => { refresh(); }, []);

  async function remove(id: number) {
    if (!confirm("Supprimer ce document ?")) return;
    try { await deleteDocument(id); refresh(); }
    catch (err) { alert((err as Error).message); }
  }

  return (
    <ul>
      {docs.map((d) => (
        <li key={d.id}>
          <span>{d.title} — {d.category ?? "?"} — {d.downloadCount} dl</span>
          <button onClick={() => downloadDocument(d.id, d.originalFileName)}>Télécharger</button>
          {(isAdmin || d.uploadedBy === me.id) && <button onClick={() => remove(d.id)}>Supprimer</button>}
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
| 400 | Format non supporté (pas PDF/Word), titre manquant, ou requête non-multipart | Inline error on form |
| 401 | Missing/expired JWT | Trigger existing "redirect to login" |
| 403 | Update/Delete by non-owner/non-admin | "Action non autorisée" |
| 404 | Document or file not found | "Introuvable"; remove from list |
| 500 | Server error | "Réessayez plus tard" |

---

## 7. Hard rules (must respect)

- Attach `Authorization: Bearer <token>` on **all** `/document-archive/**` calls.
- **Upload/Update must be `multipart/form-data`** (FormData). Do NOT send JSON for these endpoints,
  and do NOT manually set `Content-Type: application/json` (it breaks multipart parsing → 500
  `Current request is not a multipart request`).
- Download via `fetch` + `blob` (auth needed); do not use a raw `<a href>` to the endpoint.
- Delete/update button: show only when `isAdmin || uploadedBy === currentUserId`, but still handle `403`.
- Never display `fileName` (internal); display `originalFileName`.
- Pagination: `GET /document-archive` returns a `Page` object (`content` array + `totalElements`/`totalPages`).
- Do NOT "fix" the download counter increment (it is intended behavior).

---

## 8. Acceptance criteria (definition of done)

- [ ] API methods added reusing the existing client/auth (no new auth system).
- [ ] `DocumentArchiveDTO` type defined.
- [ ] Upload form: `multipart/form-data` (file + title required), PDF/Word only, shows `400` errors.
- [ ] List with pagination; category filter; text search.
- [ ] Download triggers a file save using the server filename; increments counter.
- [ ] Update endpoint (metadata and/or file replace) wired.
- [ ] Delete is conditional (owner/admin) and handles `403`/`404`.
- [ ] `401` routed through existing auth flow.
- [ ] Manual test passes: upload → list → download → update → delete.
