# Module Frontend - Gestion des liens web des applications

## Objectif

Créer un nouveau module frontend permettant de gérer les liens web associés aux applications existantes du backend IT Access Manager.

Le module doit permettre à l’utilisateur connecté de :

- lister tous les liens web des applications ;
- lister les liens web d’une application précise ;
- créer un lien web pour une application ;
- modifier un lien web existant ;
- supprimer un lien web ;
- ouvrir un lien web dans un nouvel onglet ;
- intégrer le module dans la navigation existante si elle existe.

## Contexte backend

Le backend expose déjà un module `ApplicationLink` sous le préfixe API `/api`.

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

### 1. Lister tous les liens

```http
GET /api/application-links?page=0&size=10&sortBy=id&sortDir=asc
```

Réponse :

```json
{
  "content": [
    {
      "id": 1,
      "applicationId": 10,
      "nom": "Portail RH",
      "url": "https://rh.example.com",
      "type": "production",
      "description": "Portail interne RH",
      "dateCreation": "2026-06-15T18:21:59",
      "createdBy": 1,
      "application": {
        "id": 10,
        "nom": "Application RH"
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1
}
```

### 2. Lister les liens d’une application

```http
GET /api/application-links/applications/{applicationId}
```

Exemple :

```http
GET /api/application-links/applications/10
```

Réponse :

```json
[
  {
    "id": 1,
    "applicationId": 10,
    "nom": "Portail RH",
    "url": "https://rh.example.com",
    "type": "production",
    "description": "Portail interne RH",
    "dateCreation": "2026-06-15T18:21:59",
    "createdBy": 1,
    "application": {
      "id": 10,
      "nom": "Application RH"
    }
  }
]
```

### 3. Récupérer un lien par ID

```http
GET /api/application-links/{id}
```

Exemple :

```http
GET /api/application-links/1
```

### 4. Créer un lien

```http
POST /api/application-links
```

Body :

```json
{
  "applicationId": 10,
  "nom": "Portail RH",
  "url": "https://rh.example.com",
  "type": "production",
  "description": "Portail interne RH"
}
```

Réponse :

```http
201 Created
```

### 5. Modifier un lien

```http
PUT /api/application-links/{id}
```

Body :

```json
{
  "applicationId": 10,
  "nom": "Portail RH",
  "url": "https://rh.example.com",
  "type": "production",
  "description": "Portail interne RH"
}
```

### 6. Supprimer un lien

```http
DELETE /api/application-links/{id}
```

Réponse :

```http
204 No Content
```

## Modèle de données frontend

### ApplicationLink

```ts
export type ApplicationLink = {
  id?: number;
  applicationId: number;
  nom: string;
  url: string;
  type?: string;
  description?: string;
  dateCreation?: string;
  createdBy?: number;
  application?: {
    id?: number;
    nom?: string;
  };
};
```

### PageResponse

```ts
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
};
```

### Formulaire de lien

```ts
export type ApplicationLinkForm = {
  applicationId: number;
  nom: string;
  url: string;
  type: string;
  description: string;
};
```

## Pages recommandées

### 1. Liste des liens

Route recommandée :

```text
/application-links
```

Fonctionnalités :

- afficher un tableau des liens ;
- afficher le nom du lien ;
- afficher l’application associée ;
- afficher le type ;
- afficher la description ;
- afficher la date de création ;
- bouton pour ouvrir le lien ;
- bouton pour modifier ;
- bouton pour supprimer ;
- pagination ;
- tri si possible ;
- bouton pour créer un nouveau lien.

### 2. Création de lien

Route recommandée :

```text
/application-links/new
```

Fonctionnalités :

- sélectionner une application existante ;
- saisir le nom du lien ;
- saisir l’URL ;
- saisir le type ;
- saisir la description ;
- valider le formulaire ;
- afficher les erreurs backend ;
- rediriger vers la liste après création.

### 3. Modification de lien

Route recommandée :

```text
/application-links/:id/edit
```

Fonctionnalités :

- charger le lien par ID ;
- pré-remplir le formulaire ;
- permettre de changer l’application associée ;
- sauvegarder les modifications ;
- afficher les erreurs backend ;
- rediriger vers la liste après modification.

### 4. Détails optionnels

Route recommandée :

```text
/application-links/:id
```

Fonctionnalités :

- afficher les informations complètes du lien ;
- bouton ouvrir le lien ;
- bouton modifier ;
- bouton supprimer.

## Champs du formulaire

### applicationId

- Obligatoire.
- Type : `number`.
- Doit correspondre à l’ID d’une application existante.
- Le frontend doit charger la liste des applications depuis :

```http
GET /api/applications?page=0&size=1000&sortBy=id&sortDir=asc
```

ou depuis l’endpoint dédié si le frontend en possède déjà un.

### nom

- Obligatoire.
- Maximum 100 caractères.
- Exemple : `Portail RH`.

### url

- Obligatoire.
- Maximum 500 caractères.
- Doit idéalement commencer par `http://` ou `https://`.
- Le bouton d’ouverture doit utiliser cette URL.

### type

- Optionnel côté backend, mais recommandé au frontend.
- Exemples :
  - `production`
  - `recette`
  - `développement`
  - `documentation`
  - `support`
  - `administration`

### description

- Optionnel.
- Peut être un champ textarea.

## Gestion des erreurs

Le frontend doit gérer au minimum :

- `401` : utilisateur non connecté ou token expiré ;
- `403` : utilisateur non autorisé à modifier ou supprimer ;
- `404` : lien ou application introuvable ;
- erreurs de validation backend ;
- erreurs réseau.

Exemple de message d’erreur :

```text
Impossible de créer le lien. Vérifiez les champs et réessayez.
```

## Sécurité frontend

- Ne pas supprimer le token JWT.
- Ne pas logger le token.
- Ajouter le token dans le header `Authorization`.
- Supprimer le token et rediriger vers login en cas de `401`.
- Ne pas faire confiance aux données reçues du backend pour les règles d’autorisation : les boutons peuvent être affichés ou masqués, mais le backend reste la source de vérité.

## Intégration UI

Si le frontend possède déjà un layout avec sidebar ou navbar, ajouter un élément de navigation :

```text
Liens Applications
```

ou

```text
Liens Web
```

Icône recommandée si disponible :

```text
Link
```

Route cible :

```text
/application-links
```

## Intégration avec les applications

Si le frontend possède déjà une page détail application, ajouter une section :

```text
Liens web de l’application
```

Dans cette section :

- afficher les liens de l’application courante ;
- bouton créer un lien pour cette application ;
- bouton ouvrir le lien ;
- bouton modifier ;
- bouton supprimer.

Endpoint utile :

```http
GET /api/application-links/applications/{applicationId}
```

## Validation frontend recommandée

Avant d’envoyer au backend :

- `applicationId` requis ;
- `nom` requis ;
- `url` requis ;
- `url` doit commencer par `http://` ou `https://` ;
- `nom` maximum 100 caractères ;
- `url` maximum 500 caractères ;
- `type` maximum 100 caractères.

## Structure frontend recommandée

Si le projet utilise une architecture React/Vite :

```text
src/
  features/
    application-links/
      api/
        applicationLinksApi.ts
      components/
        ApplicationLinksTable.tsx
        ApplicationLinkForm.tsx
        ApplicationLinkActions.tsx
      pages/
        ApplicationLinksPage.tsx
        CreateApplicationLinkPage.tsx
        EditApplicationLinkPage.tsx
      types/
        applicationLinkTypes.ts
      utils/
        applicationLinkValidation.ts
```

Si le projet utilise une architecture plus simple :

```text
src/
  pages/
    ApplicationLinksPage.jsx
    CreateApplicationLinkPage.jsx
    EditApplicationLinkPage.jsx
  services/
    applicationLinkService.js
  components/
    ApplicationLinkForm.jsx
    ApplicationLinksTable.jsx
  types/
    applicationLinkTypes.js
```

## Checklist de validation

- [ ] Le service API appelle bien `/api/application-links`.
- [ ] Le token JWT est envoyé dans le header.
- [ ] La liste des liens s’affiche.
- [ ] La pagination fonctionne.
- [ ] La création fonctionne.
- [ ] La modification fonctionne.
- [ ] La suppression fonctionne.
- [ ] L’ouverture du lien fonctionne.
- [ ] Les erreurs backend sont affichées.
- [ ] La liste des applications est chargée pour le champ `applicationId`.
- [ ] Le module est accessible depuis la navigation.
- [ ] Le module compile sans erreur.
