# Core Monitor - Orchestrateur de Stacks Docker

## 1. Objectif

Construire une application web containerisée permettant à un utilisateur :

- **Composer une stack applicative** via un wizard (choix guidés et cohérents)
- **Versionner chaque modification** de stack (historique complet)
- **Revenir en arrière** (rollback) en sélectionnant une version précédente
- **Appliquer une stack sur un hôte Docker** (création / mise à jour) en créant une constellation de containers
- **Monitorer l'état** des containers et de la stack
- **Exécuter des actions runtime** (start/stop/restart/delete, déploiement applicatif dans middleware)
- **Via une IHM et une API REST** couvrant toutes les fonctionnalités

L'application elle-même s'exécute dans un container Docker et pilote d'autres containers sur le même hôte via l'API Docker.

---

## 2. Principes d'Architecture

### 2.1 Commandes vs Requêtes (CQRS pragmatique)

Deux catégories d'appels au backend :

#### **Queries (READ)**
- Ne modifient pas l'état du système (statut stack, liste versions, état commandes, monitoring containers…)
- ✅ Exécutées immédiatement, sans ordonnancement strict

#### **Commands (WRITE)**
- Susceptibles de modifier l'état (create/update/apply/deploy/delete/rollback…)
- ✅ Persistées, ordonnées strictement, exécutées séquentiellement via un worker

**Règle :** Une commande WRITE ne démarre que si toutes les commandes WRITE précédentes sont `DONE` ou `CANCELLED`.

---

### 2.2 Versioning Immuable des Stacks

- Une **Stack** est l'entité logique durable (identité)
- Une **StackVersion** est un snapshot immuable de la définition déclarative de la stack à un instant donné
- Toute modification de stack crée une nouvelle `StackVersion` (jamais de modification "in place")
- Le rollback consiste à sélectionner une version existante et à déclencher une commande de convergence

**Analogie :**
```
Stack        = dépôt Git
StackVersion = commit Git
```

---

### 2.3 Définition "Compose-like" (Déclaratif)

- Chaque `StackVersion` embarque une définition compatible **docker-compose** (langage déclaratif familier)
- Utilisée comme **"source of truth"**
- L'orchestrateur effectue ensuite une convergence vers l'état désiré, via l'API Docker (SDK), sans dépendre du CLI `docker compose`

---

### 2.4 Orchestration Fiable par Queue Mono-partition Persistée

- Les **commands WRITE** sont stockées dans une queue persistée (mono-partition)
- Un **worker** consomme la queue et exécute une commande à la fois, dans l'ordre
- Les **queries READ** ne passent pas par la queue

**Objectifs :**
- ✅ Ordonnancement strict
- ✅ Reprise après crash (durabilité)
- ✅ Audit / traçabilité (historique complet)

---

### 2.5 Convergence Idempotente

L'exécution d'une `StackVersion` suit une logique de réconciliation :

1. Créer/vérifier networks et volumes
2. Créer les containers (stopped)
3. Démarrer selon dépendances (`depends_on`, healthchecks)
4. Enregistrer les identifiants runtime
5. Mettre à jour le statut

**Cette approche permet :**
- Relances sans effets de bord
- Tolérance aux pannes
- Rollback reproductible

---

## 3. Architecture Logique

### 3.1 Composants

#### **Frontend (IHM)**
- Wizard (construction de stack)
- Dashboard (stacks, versions, commandes)
- Monitoring et actions runtime
- Utilise exclusivement l'API REST

#### **Backend (Orchestrateur)**
- API Query (READ)
- API Command (WRITE → enqueue)
- Services de validation, versioning
- Worker d'exécution (queue)
- Intégration Docker SDK
- Persistance : stacks, versions, commandes, logs

#### **Docker Engine (host)**
- Exécute les containers de stacks
- Exécute aussi le container "orchestrator"

---

### 3.2 Diagramme (simplifié)

```
Frontend ──REST──> Backend
    |                 |
    | Queries (READ)  | Commands (WRITE)
    |                 v
    |           Command Queue (DB)
    |                 |
    |               Worker
    |                 |
    └───────< Docker SDK / Docker API >─────── Docker Engine ──> Containers
```

---

## 4. Modèle de Domaine

### 4.1 Stack (identité)

| Champ            | Description                          |
|------------------|--------------------------------------|
| `stack_id`       | Identifiant unique                   |
| `name`           | Nom de la stack                      |
| `current_version`| Pointe vers la version active        |

---

### 4.2 StackVersion (snapshot immuable)

| Champ            | Description                                    |
|------------------|------------------------------------------------|
| `stack_id`       | Référence à la stack parente                   |
| `version`        | Numéro de version (ex: v1, v2…)                |
| `parent_version` | Version précédente                             |
| `metadata`       | Auteur, date, commentaire                      |
| `compose`        | Définition compose-like                        |
| `runtime`        | Mapping vers IDs Docker (généré)               |
| `status`         | desired/actual/last_updated                    |

---

### 4.3 Command (write)

| Champ        | Description                                              |
|--------------|----------------------------------------------------------|
| `command_id` | Identifiant unique                                       |
| `stack_id`   | Stack concernée                                          |
| `type`       | Type de commande                                         |
| `payload`    | Données de la commande                                   |
| `status`     | `PENDING` \| `RUNNING` \| `DONE` \| `FAILED` \| `CANCELLED` |
| `timestamps` | Horodatage + logs                                        |

---

## 5. Choix d'Implémentation (Proposés)

### 5.1 Backend

- **Python + FastAPI** : API robuste, validation Pydantic, vitesse de dev
- **Pydantic** : Contrats stricts pour StackVersion/Command
- **Docker SDK (docker-py)** : Pilotage natif du runtime Docker
- **SQLite** au départ : Queue + persistance simples, migrable vers Postgres
- Le backend tourne dans un container et accède à Docker via le socket `/var/run/docker.sock` (ou API TLS ultérieurement)

---

### 5.2 Frontend

- **SPA** (React/Vue/Angular) — choix à finaliser ensuite
- Consomme uniquement l'API REST

---

### 5.3 Format des Définitions

- **YAML** (lisible, proche docker-compose)
- **JSON** possible côté API si besoin (conversion simple)

---

## 6. Sécurité (Principes)

L'accès au socket Docker donne des privilèges élevés → **limiter l'exposition** :

- ✅ Orchestrateur sur réseau privé
- ✅ Authentification/autorisation
- ✅ RBAC (phase ultérieure)
- ✅ Journalisation complète des actions

**À moyen terme :** Préférer Docker API TLS ou délégation via un agent.

---

## 7. API (Esquisse)

### Queries (READ)

| Méthode | Endpoint                                | Description                          |
|---------|-----------------------------------------|--------------------------------------|
| `GET`   | `/stacks`                               | Liste toutes les stacks              |
| `GET`   | `/stacks/{id}`                          | Détails d'une stack                  |
| `GET`   | `/stacks/{id}/versions`                 | Liste des versions d'une stack       |
| `GET`   | `/stacks/{id}/versions/{version}`       | Détails d'une version                |
| `GET`   | `/stacks/{id}/status`                   | Statut actuel de la stack            |
| `GET`   | `/stacks/{id}/containers`               | Liste des containers de la stack     |
| `GET`   | `/commands/{id}`                        | Détails d'une commande               |
| `GET`   | `/commands?stack_id=...`                | Liste des commandes (filtrées)       |

---

### Commands (WRITE)

| Méthode | Endpoint                                | Description                                    |
|---------|-----------------------------------------|------------------------------------------------|
| `POST`  | `/stacks`                               | Crée Stack + version initiale                  |
| `POST`  | `/stacks/{id}/versions`                 | Crée une nouvelle StackVersion                 |
| `POST`  | `/stacks/{id}/apply/{version}`          | Enqueue APPLY                                  |
| `POST`  | `/stacks/{id}/rollback/{version}`       | Enqueue ROLLBACK                               |
| `POST`  | `/stacks/{id}/deploy`                   | Enqueue DEPLOY                                 |
| `POST`  | `/stacks/{id}/delete`                   | Enqueue DELETE                                 |
| `POST`  | `/commands/{id}/cancel`                 | Annulation si possible                         |

---

## 8. Bonnes Pratiques Intégrées

- ✅ **Contrats stricts** (Pydantic)
- ✅ **Séparation READ/WRITE** (CQRS)
- ✅ **Immutabilité des versions**
- ✅ **Queue persistée + worker séquentiel**
- ✅ **Idempotence** (convergence)
- ✅ **Logs structurés et auditables**
- ✅ **Tests** : unitaires, API, intégration Docker
- ✅ **CI/CD** dès le début
- ✅ **Feature flags / versions API** si nécessaire
- ✅ **Convention labels Docker** pour traçabilité
