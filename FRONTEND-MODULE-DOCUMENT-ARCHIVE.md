# Module Frontend - Archive Documents (PDF / Word)

## Objectif

Créer un nouveau module frontend permettant de gérer l’archive documentaire de l’entreprise pour les fichiers PDF et Word.

Le module doit permettre à l’utilisateur connecté de :

- consulter la liste des documents archivés ;
- filtrer les documents par catégorie ;
- rechercher des documents par titre, description, tags ou auteur ;
- uploader un document PDF ou Word avec ses métadonnées ;
- prévisualiser un document dans le navigateur ;
- télécharger un document ;
- supprimer un document ;
- intégrer le module dans la navigation existante.

## Contexte backend

Le backend expose déjà un module `DocumentArchive` sous le préfixe API `/api`.

Base backend locale :

```text
http://localhost:8000/api
```

Swagger backend :

```text
http://localhost:8000/swagger-ui.html
```

## Formats acceptés

Seuls les formats suivants sont autorisés :

- `application/pdf` — fichiers `.pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` — fichiers `.docx`
- `application/msword` — fichiers `.doc`

Toute tentative d’upload d’un autre format retourne une erreur `400`.

## Endpoints disponibles

Toutes les requêtes doivent utiliser le token JWT stocké dans le frontend, généralement dans `localStorage`.

Header attendu :

```http
Authorization: Bearer <token>
Content-Type: application/json
```

Pour l’upload, le header `Content-Type` doit être `multipart/form-data` avec boundary généré automatiquement par le navigateur.

### 1. Lister les documents (paginé)

```http
GET /api/document-archive?page=0&size=20
```

Paramètres :

| Paramètre | Type    | Par défaut | Description                        |
|-----------|---------|------------|------------------------------------|
| `page`    | integer | `0`        | Numéro de page (commence à 0)      |
| `size`    | integer | `20`       | Nombre d’éléments par page         |

Réponse :

```json
{
  "content": [
    {
      "id": 1,
      "fileName": "a1b2c3d4-...",
      "originalFileName": "rapport_annuel.pdf",
      "fileSize": 5242880,
      "contentType": "application/pdf",
      "title": "Rapport Annuel 2024",
      "description": "Rapport financier annuel",
      "category": "Finance",
      "tags": "rapport,2024,annuel",
      "author": "Jean Dupont",
      "uploadedBy": 1,
      "uploadedByUsername": "admin",
      "uploadDate": "2026-06-24T10:30:00",
      "downloadCount": 12
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### 2. Récupérer un document par ID

```http
GET /api/document-archive/{id}
```

Exemple :

```http
GET /api/document-archive/1
```

Réponse :

```json
{
  "id": 1,
  "fileName": "a1b2c3d4-...",
  "originalFileName": "rapport_annuel.pdf",
  "fileSize": 5242880,
  "contentType": "application/pdf",
  "title": "Rapport Annuel 2024",
  "description": "Rapport financier annuel",
  "category": "Finance",
  "tags": "rapport,2024,annuel",
  "author": "Jean Dupont",
  "uploadedBy": 1,
  "uploadedByUsername": "admin",
  "uploadDate": "2026-06-24T10:30:00",
  "downloadCount": 12
}
```

### 3. Uploader un document

```http
POST /api/document-archive/upload
```

`Content-Type: multipart/form-data`

Champs du formulaire :

| Champ             | Type   | Requis | Description                            |
|------------------|--------|--------|----------------------------------------|
| `file`            | File   | Oui    | Fichier PDF ou Word                    |
| `title`           | string | Oui    | Titre du document                      |
| `description`     | string | Non    | Description du document                |
| `category`        | string | Non    | Catégorie du document                  |
| `tags`            | string | Non    | Tags séparés par des virgules          |
| `author`          | string | Non    | Auteur du document                     |

Exemple avec `curl` :

```bash
curl -X POST "http://localhost:8000/api/document-archive/upload" \
  -H "Authorization: Bearer <token>" \
  -F "file=@/chemin/vers/rapport.pdf" \
  -F "title=Rapport Annuel 2024" \
  -F "description=Rapport financier annuel" \
  -F "category=Finance" \
  -F "tags=rapport,2024,annuel" \
  -F "author=Jean Dupont"
```

Réponse (201 Created) :

```json
{
  "id": 1,
  "fileName": "a1b2c3d4-...",
  "originalFileName": "rapport_annuel.pdf",
  "fileSize": 5242880,
  "contentType": "application/pdf",
  "title": "Rapport Annuel 2024",
  "description": "Rapport financier annuel",
  "category": "Finance",
  "tags": "rapport,2024,annuel",
  "author": "Jean Dupont",
  "uploadedBy": 1,
  "uploadedByUsername": "admin",
  "uploadDate": "2026-06-24T10:30:00",
  "downloadCount": 0
}
```

Erreurs possibles :

- `400` — fichier vide ou format non supporté
- `401` — token manquant ou invalide
- `500` — erreur serveur lors de la sauvegarde

### 4. Télécharger un document

```http
GET /api/document-archive/download/{id}
```

Exemple :

```http
GET /api/document-archive/download/1
```

Headers de réponse :

```http
Content-Disposition: attachment; filename="rapport_annuel.pdf"
Content-Type: application/pdf
Content-Length: 5242880
Cache-Control: no-cache, no-store, must-revalidate
Pragma: no-cache
Expires: 0
```

Le navigateur télécharge le fichier avec son nom original. Le compteur de téléchargements est incrémenté automatiquement.

Pour la prévisualisation dans le navigateur, ouvrir l’URL dans un nouvel onglet ou dans un iframe. Le backend retourne le bon `Content-Type`, donc le navigateur affichera nativement le PDF ou tentera d’ouvrir le Word avec l’application appropriée.

### 5. Filtrer par catégorie

```http
GET /api/document-archive/category/{category}
```

Exemple :

```http
GET /api/document-archive/category/Finance
```

Réponse :

```json
[
  {
    "id": 1,
    "fileName": "a1b2c3d4-...",
    "originalFileName": "rapport_annuel.pdf",
    "fileSize": 5242880,
    "contentType": "application/pdf",
    "title": "Rapport Annuel 2024",
    "description": "Rapport financier annuel",
    "category": "Finance",
    "tags": "rapport,2024,annuel",
    "author": "Jean Dupont",
    "uploadedBy": 1,
    "uploadedByUsername": "admin",
    "uploadDate": "2026-06-24T10:30:00",
    "downloadCount": 12
  }
]
```

### 6. Rechercher des documents

```http
GET /api/document-archive/search?q=mot_cle
```

La recherche est effectuée sur les champs suivants : `title`, `description`, `tags`, `author`.

Exemple :

```http
GET /api/document-archive/search?q=rapport
```

Réponse :

```json
[
  {
    "id": 1,
    "fileName": "a1b2c3d4-...",
    "originalFileName": "rapport_annuel.pdf",
    "fileSize": 5242880,
    "contentType": "application/pdf",
    "title": "Rapport Annuel 2024",
    "description": "Rapport financier annuel",
    "category": "Finance",
    "tags": "rapport,2024,annuel",
    "author": "Jean Dupont",
    "uploadedBy": 1,
    "uploadedByUsername": "admin",
    "uploadDate": "2026-06-24T10:30:00",
    "downloadCount": 12
  }
]
```

### 7. Supprimer un document

```http
DELETE /api/document-archive/{id}
```

Exemple :

```http
DELETE /api/document-archive/1
```

Réponse :

```http
204 No Content
```

Le fichier physique est supprimé du serveur et l’enregistrement est retiré de la base de données.

## Modèle de données frontend

### DocumentArchive

```ts
export type DocumentArchive = {
  id?: number;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  title: string;
  description?: string;
  category?: string;
  tags?: string;
  author?: string;
  uploadedBy: number;
  uploadedByUsername: string;
  uploadDate: string;
  downloadCount?: number;
};
```

### DocumentArchiveRequest (upload)

```ts
export type DocumentArchiveRequest = {
  title: string;
  description?: string;
  category?: string;
  tags?: string;
  author?: string;
};
```

### DocumentArchiveForm

```ts
export type DocumentArchiveForm = {
  file: File | null;
  title: string;
  description: string;
  category: string;
  tags: string;
  author: string;
};
```

### PageResponse (pour la liste paginée)

```ts
export type PageResponse<T> = {
  content: T[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
```

## Pages recommandées

### 1. Liste des documents

Route recommandée :

```text
/document-archive
```

Fonctionnalités :

- afficher un tableau des documents ;
- afficher le titre ;
- afficher la catégorie ;
- afficher l’auteur ;
- afficher la date d’upload ;
- afficher la taille du fichier (convertie en Ko/Mo) ;
- afficher le nombre de téléchargements ;
- badge visuel selon le type (PDF / Word) ;
- bouton pour prévisualiser le document ;
- bouton pour télécharger ;
- bouton pour supprimer ;
- pagination ;
- champ de recherche par titre, description, tags ou auteur ;
- filtre par catégorie ;
- bouton pour uploader un nouveau document.

### 2. Upload de document

Route recommandée :

```text
/document-archive/upload
```

Fonctionnalités :

- champ de sélection de fichier (drag & drop si possible) ;
- champ titre (obligatoire) ;
- champ description (optionnel) ;
- champ catégorie (optionnel, input libre avec suggestions) ;
- champ tags (optionnel, input avec suggestion) ;
- champ auteur (optionnel) ;
- validation côté frontend avant envoi ;
- barre de progression si possible ;
- afficher les erreurs backend ;
- rediriger vers la liste après upload réussi.

#### Validation frontend recommandée avant upload

- le champ `file` est obligatoire ;
- le fichier doit être de type PDF ou Word ;
- la taille maximale doit être contrôlée côté frontend (ex. `150 Mo`) ;
- le champ `title` est obligatoire ;
- le champ `title` doit faire au maximum 255 caractères ;
- les champs `description` et `tags` doivent être limités en taille.

### 3. Prévisualisation

Route recommandée :

```text
/document-archive/{id}/preview
```

Fonctionnalités :

- afficher le document dans un composant de prévisualisation ;
- pour les PDF : utiliser l’API native du navigateur (`<embed>`, `<iframe>` ou `PDF.js`) ;
- pour les Word : afficher un message d’information invitant à télécharger le fichier, ou utiliser une librairie de conversion si disponible ;
- bouton pour télécharger depuis la prévisualisation ;
- bouton pour revenir à la liste.

### 4. Détail du document

Route recommandée :

```text
/document-archive/{id}
```

Fonctionnalités :

- afficher toutes les métadonnées du document ;
- afficher la taille du fichier ;
- afficher le type MIME ;
- afficher la catégorie ;
- afficher les tags ;
- afficher l’auteur ;
- afficher le nom de l’utilisateur qui a uploadé ;
- afficher la date d’upload ;
- afficher le nombre de téléchargements ;
- bouton prévisualiser ;
- bouton télécharger ;
- bouton supprimer.

## Champs du formulaire d’upload

### file

- Obligatoire.
- Type : `File` (input `type="file"`).
- Formats acceptés côté frontend : `.pdf`, `.doc`, `.docx`.
- Pour le drag & drop, vérifier l’extension et/ou le type MIME avant ajout.
- Contrôler la taille avant upload (ex. `150 * 1024 * 1024` octets).

### title

- Obligatoire.
- Type : `string`.
- Maximum 255 caractères.
- Exemple : `Rapport Annuel 2024`.

### description

- Optionnel.
- Type : `string`.
- Exemple : `Rapport financier annuel de l’exercice 2024`.

### category

- Optionnel.
- Type : `string`.
- Exemple : `Finance`, `RH`, `Juridique`, `Technique`, `Qualité`.
- Le frontend peut proposer une liste de suggestions, mais reste en `input libre` pour rester compatible avec le backend.

### tags

- Optionnel.
- Type : `string`.
- Saisir les tags separés par des virgules.
- Exemple : `rapport, 2024, annuel, financier`.
- Le frontend peut afficher les tags sous forme de badges après parsing.

### author

- Optionnel.
- Type : `string`.
- Exemple : `Jean Dupont`.

## Gestion des erreurs

Le frontend doit gérer au minimum :

- `400` — fichier vide, format non supporté, ou validation échouée ;
- `401` — utilisateur non connecté ou token expiré ;
- `403` — utilisateur non autorisé à supprimer ;
- `404` — document introuvable ;
- `500` — erreur serveur ;
- erreurs réseau.

Exemples de messages :

```text
Erreur lors de l’upload. Vérifiez le fichier et les champs puis réessayez.
```

```text
Format non supporté. Seuls les fichiers PDF et Word sont autorisés.
```

```text
Le titre est requis.
```

## Sécurité frontend

- Ne pas supprimer le token JWT.
- Ne pas logger le token.
- Ajouter le token dans le header `Authorization` pour toutes les requêtes.
- Supprimer le token et rediriger vers login en cas de `401`.
- Pour l’upload, utiliser `FormData` avec `fetch` ou `axios` ; ne pas envoyer le fichier en base64 pour éviter des payloads trop volumineux.
- Ne pas faire confiance aux données reçues du backend pour les règles d’autorisation : les boutons peuvent être affichés ou masqués, mais le backend reste la source de vérité.

## Intégration UI

Si le frontend possède déjà un layout avec sidebar ou navbar, ajouter un élément de navigation :

```text
Archive Documents
```

ou

```text
Bibliothèque
```

Icône recommandée si disponible :

```text
FileText
```

Route cible :

```text
/document-archive
```

## Exemples d’implémentation

### Upload avec FormData (vanilla JS / fetch)

```js
async function uploadDocument(file, metadata, token) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', metadata.title);
  if (metadata.description) formData.append('description', metadata.description);
  if (metadata.category) formData.append('category', metadata.category);
  if (metadata.tags) formData.append('tags', metadata.tags);
  if (metadata.author) formData.append('author', metadata.author);

  const response = await fetch('http://localhost:8000/api/document-archive/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || 'Erreur lors de l’upload');
  }

  return await response.json();
}
```

### Upload avec FormData (React)

```tsx
const uploadDocument = async (file, metadata, token) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', metadata.title);
  if (metadata.description) formData.append('description', metadata.description);
  if (metadata.category) formData.append('category', metadata.category);
  if (metadata.tags) formData.append('tags', metadata.tags);
  if (metadata.author) formData.append('author', metadata.author);

  const response = await fetch('http://localhost:8000/api/document-archive/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(error);
  }

  return await response.json();
};
```

### Upload avec FormData (axios)

```js
const uploadDocument = async (file, metadata, token) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', metadata.title);
  if (metadata.description) formData.append('description', metadata.description);
  if (metadata.category) formData.append('category', metadata.category);
  if (metadata.tags) formData.append('tags', metadata.tags);
  if (metadata.author) formData.append('author', metadata.author);

  const response = await axios.post(
    'http://localhost:8000/api/document-archive/upload',
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    }
  );

  return response.data;
};
```

### Téléchargement (nouvel onglet)

```ts
const openDocument = (id, token) => {
  const url = `http://localhost:8000/api/document-archive/download/${id}`;
  const link = document.createElement('a');
  link.href = url;
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  link.download = '';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
```

Ou avec `fetch` pour un téléchargement programmé :

```js
const downloadDocument = async (id, token) => {
  const response = await fetch(`http://localhost:8000/api/document-archive/download/${id}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (!response.ok) throw new Error('Erreur lors du téléchargement');

  const blob = await response.blob();
  const contentDisposition = response.headers.get('Content-Disposition');
  const fileName = contentDisposition
    ? contentDisposition.split('filename=')[1].replace(/"/g, '')
    : 'document';

  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
};
```

### Prévisualisation PDF dans un iframe

```tsx
function DocumentPreview({ document }) {
  if (document.contentType === 'application/pdf') {
    return (
      <iframe
        src={`http://localhost:8000/api/document-archive/download/${document.id}`}
        title={document.title}
        width="100%"
        height="600px"
        style={{ border: 'none' }}
      />
    );
  }

  if (
    document.contentType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
    document.contentType === 'application/msword'
  ) {
    return (
      <div className="preview-unavailable">
        <p>La prévisualisation n’est pas disponible pour ce type de fichier.</p>
        <a
          href={`http://localhost:8000/api/document-archive/download/${document.id}`}
          target="_blank"
          rel="noopener noreferrer"
          download
        >
          Télécharger le fichier
        </a>
      </div>
    );
  }

  return <p>Format non pris en charge.</p>;
}
```

## Structure frontend recommandée

Si le projet utilise une architecture React/Vite feature-based :

```text
src/
  features/
    document-archive/
      api/
        documentArchiveApi.ts
      components/
        DocumentArchiveTable.tsx
        DocumentUploadForm.tsx
        DocumentPreview.tsx
        DocumentCategoryFilter.tsx
        DocumentSearchBar.tsx
        DocumentActions.tsx
        DocumentTypeBadge.tsx
      pages/
        DocumentArchivePage.tsx
        DocumentUploadPage.tsx
        DocumentDetailPage.tsx
        DocumentPreviewPage.tsx
      types/
        documentArchiveTypes.ts
      utils/
        documentValidation.ts
        fileSizeFormatter.ts
        documentCategories.ts
```

Si le projet utilise une architecture plus simple :

```text
src/
  pages/
    DocumentArchivePage.jsx
    DocumentUploadPage.jsx
    DocumentDetailPage.jsx
  services/
    documentArchiveService.js
  components/
    DocumentArchiveTable.jsx
    DocumentUploadForm.jsx
    DocumentPreview.jsx
    DocumentCategoryFilter.jsx
    DocumentSearchBar.jsx
    DocumentTypeBadge.jsx
  utils/
    documentValidation.js
    fileSizeFormatter.js
    documentCategories.js
  types/
    documentArchiveTypes.js
```

## Validation frontend recommandée

Avant d’envoyer au backend :

- `file` est obligatoire et doit être de type PDF ou Word ;
- `file` ne doit pas dépasser la taille maximum autorisée ;
- `title` est obligatoire ;
- `title` maximum 255 caractères ;
- `category` : longueur maximum 100 caractères ;
- `tags` : longueur maximum 65535 caractères ;
- `author` : longueur maximum 255 caractères.

Exemple de validation TypeScript :

```ts
const allowedTypes = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/msword'
];

const validateDocumentForm = (form) => {
  const errors = {};

  if (!form.file) {
    errors.file = 'Le fichier est requis.';
  } else if (!allowedTypes.includes(form.file.type)) {
    errors.file = 'Format non supporté. Seuls PDF et Word sont autorisés.';
  } else if (form.file.size > 150 * 1024 * 1024) {
    errors.file = 'Le fichier ne doit pas dépasser 150 Mo.';
  }

  if (!form.title || form.title.trim() === '') {
    errors.title = 'Le titre est requis.';
  } else if (form.title.length > 255) {
    errors.title = 'Le titre ne doit pas dépasser 255 caractères.';
  }

  return errors;
};
```

## Catégories recommandées

Le backend accepte n’importe quelle valeur texte pour `category`. Voici des suggestions pour le frontend :

```ts
export const DOCUMENT_CATEGORIES = [
  { value: '', label: 'Aucune catégorie' },
  { value: 'Finance', label: 'Finance' },
  { value: 'RH', label: 'Ressources Humaines' },
  { value: 'Juridique', label: 'Juridique' },
  { value: 'Technique', label: 'Technique' },
  { value: 'Qualité', label: 'Qualité' },
  { value: 'Sécurité', label: 'Sécurité' },
  { value: 'Marketing', label: 'Marketing' },
  { value: 'Procédures', label: 'Procédures' },
  { value: 'Formation', label: 'Formation' },
  { value: 'Autre', label: 'Autre' }
];
```

## Checklist de validation

- [ ] Le service API utilise bien le préfixe `/api/document-archive`.
- [ ] Le token JWT est envoyé dans le header `Authorization`.
- [ ] La liste des documents s’affiche avec pagination.
- [ ] L’upload fonctionne pour un fichier PDF.
- [ ] L’upload fonctionne pour un fichier Word (`.doc` et `.docx`).
- [ ] La validation frontend empêche l’upload d’un format non supporté.
- [ ] La taille du fichier est contrôlée côté frontend avant envoi.
- [ ] La prévisualisation PDF fonctionne.
- [ ] Le téléchargement fonctionne et utilise le nom original du fichier.
- [ ] La recherche par titre, description, tags et auteur fonctionne.
- [ ] Le filtre par catégorie fonctionne.
- [ ] La suppression fonctionne avec confirmation.
- [ ] Les erreurs backend sont affichées.
- [ ] Le module est accessible depuis la navigation.
- [ ] Le module compile sans erreur.
- [ ] Le drag & drop fonctionne pour l’upload (si implémenté).
