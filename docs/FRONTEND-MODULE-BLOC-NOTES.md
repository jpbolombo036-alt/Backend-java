# Module Frontend - Bloc Notes QA

## Objectif

Créer un nouveau module frontend permettant de gérer les notes de qualification QA dans l’application IT Access Manager.

Le module doit permettre à l’utilisateur connecté de :

- consulter ses notes ;
- consulter toutes les notes si l’utilisateur est administrateur ;
- créer une note QA ;
- modifier une note existante ;
- supprimer une note ;
- associer une note à une application, une session de test ou un test ;
- gérer le statut de la note ;
- intégrer le module dans la navigation existante.

## Contexte backend

Le backend expose déjà un module `BlocNote` sous le préfixe API `/api`.

Base backend locale :

```text
http://localhost:8000/api
```

Swagger backend :

```text
http://localhost:8000/swagger-ui.html
```

## Endpoints disponibles

Toutes les requêtes doivent utiliser le token JWT stocké dans le frontend, généralement dans `localStorage`.

Header attendu :

```http
Authorization: Bearer <token>
Content-Type: application/json
```

### 1. Lister les notes

```http
GET /api/bloc-notes
```

Comportement backend :

- si l’utilisateur est `admin`, retourne toutes les notes ;
- sinon, retourne uniquement les notes créées par l’utilisateur connecté ;
- les notes utilisateur sont triées par `updatedAt` décroissant.

Réponse :

```json
[
  {
    "id": 1,
    "title": "Test login",
    "content": "Le login fonctionne correctement.",
    "applicationId": 10,
    "sessionId": 20,
    "testId": 30,
    "status": "DRAFT",
    "createdBy": 1,
    "createdByUsername": "qa_user",
    "createdAt": "2026-06-15T18:21:59",
    "updatedAt": "2026-06-15T18:30:00"
  }
]
```

### 2. Récupérer une note par ID

```http
GET /api/bloc-notes/{id}
```

Exemple :

```http
GET /api/bloc-notes/1
```

Réponse :

```json
{
  "id": 1,
  "title": "Test login",
  "content": "Le login fonctionne correctement.",
  "applicationId": 10,
  "sessionId": 20,
  "testId": 30,
  "status": "DRAFT",
  "createdBy": 1,
  "createdByUsername": "qa_user",
  "createdAt": "2026-06-15T18:21:59",
  "updatedAt": "2026-06-15T18:30:00"
}
```

### 3. Créer une note

```http
POST /api/bloc-notes
```

Body :

```json
{
  "title": "Test login",
  "content": "Le login fonctionne correctement.",
  "applicationId": 10,
  "sessionId": 20,
  "testId": 30,
  "status": "DRAFT"
}
```

Réponse :

```http
201 Created
```

### 4. Modifier une note

```http
PUT /api/bloc-notes/{id}
```

Body :

```json
{
  "title": "Test login mis à jour",
  "content": "Le login fonctionne correctement après correction.",
  "applicationId": 10,
  "sessionId": 20,
  "testId": 30,
  "status": "VALIDATED"
}
```

### 5. Supprimer une note

```http
DELETE /api/bloc-notes/{id}
```

Réponse :

```http
204 No Content
```

## Modèle de données frontend

### BlocNote

```ts
export type BlocNote = {
  id?: number;
  title?: string;
  content: string;
  applicationId?: number;
  sessionId?: number;
  testId?: number;
  status?: string;
  createdBy?: number;
  createdByUsername?: string;
  createdAt?: string;
  updatedAt?: string;
};
```

### BlocNoteForm

```ts
export type BlocNoteForm = {
  title: string;
  content: string;
  applicationId?: number;
  sessionId?: number;
  testId?: number;
  status: string;
};
```

### Statuts recommandés

```ts
export const BLOC_NOTE_STATUSES = [
  { value: 'DRAFT', label: 'Brouillon' },
  { value: 'IN_PROGRESS', label: 'En cours' },
  { value: 'VALIDATED', label: 'Validé' },
  { value: 'REJECTED', label: 'Rejeté' },
  { value: 'ARCHIVED', label: 'Archivé' }
];
```

Le backend accepte n’importe quelle valeur texte pour `status`. Si le frontend utilise une liste fixe, elle doit rester compatible avec le backend.

## Pages recommandées

### 1. Liste des notes

Route recommandée :

```text
/bloc-notes
```

Fonctionnalités :

- afficher la liste des notes ;
- afficher le titre ;
- afficher un extrait du contenu ;
- afficher le statut ;
- afficher l’auteur ;
- afficher la date de mise à jour ;
- afficher l’application associée si disponible ;
- afficher la session associée si disponible ;
- afficher le test associé si disponible ;
- bouton pour voir le détail ;
- bouton pour modifier ;
- bouton pour supprimer ;
- bouton pour créer une nouvelle note ;
- filtre optionnel par statut ;
- recherche optionnelle par titre.

### 2. Création de note

Route recommandée :

```text
/bloc-notes/new
```

Fonctionnalités :

- saisir un titre optionnel ;
- saisir le contenu obligatoire ;
- sélectionner une application optionnelle ;
- sélectionner une session de test optionnelle ;
- sélectionner un test optionnel ;
- sélectionner un statut ;
- valider le formulaire ;
- afficher les erreurs backend ;
- rediriger vers `/bloc-notes` après création.

### 3. Modification de note

Route recommandée :

```text
/bloc-notes/:id/edit
```

Fonctionnalités :

- charger la note par ID ;
- pré-remplir le formulaire ;
- permettre de modifier le contenu ;
- permettre de modifier les associations ;
- permettre de modifier le statut ;
- sauvegarder les modifications ;
- afficher les erreurs backend ;
- rediriger vers `/bloc-notes` après modification.

### 4. Détail de note

Route recommandée :

```text
/bloc-notes/:id
```

Fonctionnalités :

- afficher le titre ;
- afficher le contenu complet ;
- afficher le statut ;
- afficher l’auteur ;
- afficher la date de création ;
- afficher la date de mise à jour ;
- afficher l’application associée ;
- afficher la session associée ;
- afficher le test associé ;
- bouton modifier ;
- bouton supprimer.

## Champs du formulaire

### title

- Optionnel côté backend.
- Maximum 255 caractères côté base.
- Exemple : `Test login`.
- Le frontend peut imposer une limite de 255 caractères.

### content

- Obligatoire côté backend.
- Type : texte long.
- Utiliser un `textarea`.
- Le frontend doit refuser un contenu vide.

### applicationId

- Optionnel.
- Correspond à l’ID d’une application.
- Le frontend doit charger la liste des applications depuis :

```http
GET /api/applications?page=0&size=1000&sortBy=id&sortDir=asc
```

ou depuis l’endpoint dédié déjà utilisé dans le frontend.

### sessionId

- Optionnel.
- Correspond à l’ID d’une session de test.
- Si le frontend possède déjà une liste des sessions, il doit la réutiliser.
- Sinon, utiliser :

```http
GET /api/test-sessions?page=0&size=1000&sortBy=id&sortDir=asc
```

si disponible.

### testId

- Optionnel.
- Correspond à l’ID d’un test.
- Si le frontend possède déjà une liste des tests, il doit la réutiliser.
- Sinon, utiliser :

```http
GET /api/tests?page=0&size=1000&sortBy=id&sortDir=asc
```

si disponible.

### status

- Optionnel côté backend.
- Valeur par défaut backend : `DRAFT`.
- Le frontend doit envoyer `DRAFT` par défaut si aucun statut n’est choisi.

## Gestion des erreurs

Le frontend doit gérer au minimum :

- `401` : utilisateur non connecté ou token expiré ;
- `403` : utilisateur non autorisé à modifier ou supprimer ;
- `404` : note introuvable ;
- erreurs de validation backend ;
- erreurs réseau.

Exemple de message d’erreur :

```text
Impossible de sauvegarder la note. Vérifiez les champs et réessayez.
```

## Sécurité frontend

- Ne pas supprimer le token JWT.
- Ne pas logger le token.
- Ajouter le token dans le header `Authorization`.
- Supprimer le token et rediriger vers login en cas de `401`.
- Ne pas faire confiance aux données reçues du backend pour les règles d’autorisation.
- Le backend reste la source de vérité pour les droits de modification et suppression.

## Intégration UI

Si le frontend possède déjà un layout avec sidebar ou navbar, ajouter un élément de navigation :

```text
Bloc Notes
```

ou

```text
Notes QA
```

Icône recommandée si disponible :

```text
Note
```

Route cible :

```text
/bloc-notes
```

## Intégration avec les applications, sessions et tests

Si le frontend possède déjà des pages détail pour une application, une session de test ou un test, ajouter une section :

```text
Notes QA
```

Dans cette section :

- afficher les notes associées ;
- bouton créer une note pour cette application, session ou test ;
- bouton voir le détail ;
- bouton modifier ;
- bouton supprimer.

Endpoints utiles :

```http
GET /api/bloc-notes
GET /api/bloc-notes/{id}
```

Le backend expose aussi des méthodes repository pour filtrer par `applicationId`, `sessionId` et `testId`, mais le controller actuel ne les expose pas encore. Si nécessaire, le backend devra être complété avec des endpoints dédiés.

## Validation frontend recommandée

Avant d’envoyer au backend :

- `content` est obligatoire ;
- `title` maximum 255 caractères ;
- `status` doit être une valeur texte ;
- `applicationId`, `sessionId`, `testId` doivent être des IDs numériques si renseignés.

## Structure frontend recommandée

Si le projet utilise une architecture React/Vite :

```text
src/
  features/
    bloc-notes/
      api/
        blocNotesApi.ts
      components/
        BlocNotesList.tsx
        BlocNoteCard.tsx
        BlocNoteForm.tsx
        BlocNoteActions.tsx
        BlocNoteStatusBadge.tsx
      pages/
        BlocNotesPage.tsx
        CreateBlocNotePage.tsx
        EditBlocNotePage.tsx
        BlocNoteDetailPage.tsx
      types/
        blocNoteTypes.ts
      utils/
        blocNoteValidation.ts
        blocNoteStatuses.ts
```

Si le projet utilise une architecture plus simple :

```text
src/
  pages/
    BlocNotesPage.jsx
    CreateBlocNotePage.jsx
    EditBlocNotePage.jsx
    BlocNoteDetailPage.jsx
  services/
    blocNoteService.js
  components/
    BlocNoteForm.jsx
    BlocNotesList.jsx
    BlocNoteCard.jsx
    BlocNoteStatusBadge.jsx
  utils/
    blocNoteValidation.js
    blocNoteStatuses.js
```

## Checklist de validation

- [ ] Le service API appelle bien `/api/bloc-notes`.
- [ ] Le token JWT est envoyé dans le header.
- [ ] La liste des notes s’affiche.
- [ ] Un utilisateur non admin voit uniquement ses notes.
- [ ] Un administrateur voit toutes les notes.
- [ ] La création fonctionne.
- [ ] La modification fonctionne.
- [ ] La suppression fonctionne.
- [ ] Le détail d’une note fonctionne.
- [ ] Les erreurs backend sont affichées.
- [ ] Les applications sont chargées pour le champ `applicationId`.
- [ ] Les sessions de test sont chargées si disponibles.
- [ ] Les tests sont chargés si disponibles.
- [ ] Le module est accessible depuis la navigation.
- [ ] Le module compile sans erreur.
