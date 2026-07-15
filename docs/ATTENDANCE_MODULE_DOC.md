# Module Présences — Documentation Backend

## Résumé
Module de gestion des présences des agents (utilisateurs du rôle `user`).  
Permet le pointage d'arrivée / départ, le suivi des absences, retards et congés, et un dashboard de synthèse journalière.

---

## Entité `Attendance`

| Champ | Type | Description |
|-------|------|-------------|
| `id` | Long | Identifiant auto-incrémenté |
| `agentId` | Long | ID de l'agent (utilisateur) |
| `agentUsername` | String | Nom d'utilisateur de l'agent (dénormalisé) |
| `date` | LocalDate | Date de présence (unique par agent) |
| `checkInTime` | LocalTime? | Heure d'arrivée |
| `checkOutTime` | LocalTime? | Heure de départ |
| `status` | String | `PRESENT`, `LATE`, `ABSENT`, `LEAVE` |
| `reason` | String? | Motif (congé, absence, etc.) |
| `createdBy` | Long | ID de l'utilisateur qui a créé l'enregistrement |
| `createdAt` | LocalDateTime | Date de création automatique |

**Contrainte d'unicité** : `(agent_id, date)` — un seul enregistrement par agent et par jour.

---

## DTOs

### `AttendanceDTO` (sortie standard)
- `id`, `agentId`, `agentUsername`, `date`
- `checkInTime` (ISO `HH:mm:ss`), `checkOutTime`
- `status`, `reason`
- `createdBy`, `createdAt`

### `AttendanceReportDTO` (pour le dashboard)
Même champs que `AttendanceDTO` + :
- `duration` — durée de présence calculée (ex: `8h 30min`)

### `AttendanceDashboardDTO` (synthèse du jour)
- `date` — date du jour
- `totalPresent`, `totalAbsent`, `totalLate`, `totalOnLeave` — compteurs par statut
- `totalAgents` — nombre total d'agents
- `attendances` — liste de `AttendanceReportDTO`
- `statusDistribution` — Map<String, Integer> : `{PRESENT: 5, ABSENT: 1, LATE: 2, LEAVE: 1}`
- `attendanceRate` — taux de présence en % (présent + retard / total agents)

---

## Statuts possibles

| Valeur | Description | Condition d'attribution |
|--------|-------------|------------------------|
| `PRESENT` | Présent | Arrivée avant 9h00 |
| `LATE` | En retard | Arrivée après 9h00 |
| `ABSENT` | Absent | Défini manuellement ou absent toute la journée |
| `LEAVE` | Congé | Défini manuellement avec motif |

---

## Endpoints API

Tous les endpoints nécessitent un token JWT dans le header `Authorization: Bearer <token>` sauf mention contraire.

### 1. Dashboard du jour
```
GET /attendance-dashboard/today
```
**Réponse** : `AttendanceDashboardDTO`

---

### 2. CRUD Présences

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/attendances` | Authentifié | Toutes les présences, paginées (25/défaut) |
| `GET` | `/attendances/{id}` | Authentifié | Détail d'une présence |
| `GET` | `/attendances/agent/{agentId}` | Authentifié | Présences d'un agent, paginées |
| `GET` | `/attendances/agent/{agentId}/range?start=YYYY-MM-DD&end=YYYY-MM-DD` | Authentifié | Présences d'un agent sur une période |
| `GET` | `/attendances/date/{date}` | Authentifié | Présences d'une date donnée, paginées |
| `POST` | `/attendances` | Authentifié | Créer une présence manuellement |
| `PUT` | `/attendances/{id}` | Authentifié | Modifier une présence |
| `DELETE` | `/attendances/{id}` | Authentifié | Supprimer une présence |

---

### 3. Pointage (Check-in / Check-out)

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/attendances/check-in` | Authentifié | Pointer l'arrivée de l'agent connecté |
| `POST` | `/attendances/check-out` | Authentifié | Pointer le départ de l'agent connecté |

---

## Logique métier

### Check-in (`POST /attendances/check-in`)
- Cherche un enregistrement existant pour l'agent du jour.
- Si existant sans check-in → met à jour l'heure d'arrivée.
- Si inexistant → crée un nouvel enregistrement.
- **Statut automatique** : `LATE` si l'heure actuelle > 9h00, sinon `PRESENT`.
- Retourne l'enregistrement complet (même si déjà pointé).

### Check-out (`POST /attendances/check-out`)
- Nécessite un check-in existant pour le jour.
- Met à jour `checkOutTime` avec l'heure actuelle.
- Si déjà pointé le départ, retourne l'enregistrement sans modification.

### Création / Modification manuelle
- `admin` peut modifier/supprimer toute présence.
- Agent normal peut modifier/supprimer ses propres présences.
- Si `checkInTime` est fourni lors d'une modification et que le statut est `PRESENT`, le statut est recalculé automatiquement (LATE si après 9h).

---

## Authentification
Header requis :
```
Authorization: Bearer <access_token>
```
Le token est obtenu via `POST /auth/token` (JSON : `{"username":"...", "password":"..."}`).

Pour les endpoints `check-in` / `check-out`, l'agent est automatiquement identifié depuis le token (pas de paramètre `agentId` à fournir).

---

## Exemples d'appels

### Check-in
```bash
curl -X POST https://api.example.com/attendances/check-in \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

### Check-out
```bash
curl -X POST https://api.example.com/attendances/check-out \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

### Dashboard du jour
```bash
curl https://api.example.com/attendance-dashboard/today \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Présences d'un agent
```bash
curl "https://api.example.com/attendances/agent/42?page=0&size=25&sortBy=date&sortDir=desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Présences par plage de dates
```bash
curl "https://api.example.com/attendances/agent/42/range?start=2024-01-01&end=2024-01-31" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Pagination
- **Par défaut** : 25 éléments par page
- Paramètres : `page` (0-indexed), `size`, `sortBy`, `sortDir` (`asc`/`desc`)
- Réponse paginée (`PageResponse`) :
  ```json
  {
    "content": [...],
    "currentPage": 0,
    "pageSize": 25,
    "totalElements": 142,
    "totalPages": 6,
    "first": true,
    "last": false
  }
  ```

---

## Intégration Frontend — Points d'attention

1. **Rôle `admin`** vs **rôle `user`** :
   - L'admin voit toutes les présences, peut créer/modifier/supprimer n'importe quelle présence.
   - L'utilisateur standard peut pointer son arrivée/départ, modifier/supprimer ses propres présences.

2. **Heure limite de retard** : 9h00 (configurable dans `AttendanceService.java` ligne 112). Pour modifier, changer `LocalTime.of(9, 0)`.

3. **Création auto par admin** : Un admin peut créer une présence pour un agent avec un statut explicite (`ABSENT`, `LEAVE`, etc.).

4. **Dashboard temps réel** : L'endpoint `/attendance-dashboard/today` agrège toutes les présences du jour. Le frontend peut le poller périodiquement ou utiliser WebSocket si besoin d'actualisation instantanée.

5. **Durée de présence** : Calculée uniquement quand `checkInTime` ET `checkOutTime` sont présents. Format : `Xh Ymin`.

6. **Cas où l'agent n'a pas pointé** : Si un agent n'apparaît pas dans le dashboard du jour, c'est qu'il n'a pas encore pointé son arrivée (ABSENT non défini = absent mais non compté dans les stats jusqu'à un enregistrement explicite).

---

## Fichiers backend concernés

- `entity/Attendance.java`
- `repository/AttendanceRepository.java`
- `service/AttendanceService.java`
- `controller/AttendanceController.java`
- `controller/AttendanceDashboardController.java`
- `dto/AttendanceDTO.java`
- `dto/AttendanceReportDTO.java`
- `dto/AttendanceDashboardDTO.java`
- `db/migration/V8__create_attendance.sql`
