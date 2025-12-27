1. Objectif

Construire une application web containerisée permettant à un utilisateur :

de composer une stack applicative via un wizard (choix guidés et cohérents),

de versionner chaque modification de stack (historique complet),

de revenir en arrière (rollback) en sélectionnant une version précédente,

d’appliquer une stack sur un hôte Docker (création / mise à jour) en créant une constellation de containers,

de monitorer l’état des containers et de la stack,

d’exécuter des actions runtime (start/stop/restart/delete, déploiement applicatif dans middleware),

via une IHM et une API REST couvrant toutes les fonctionnalités.

L’application elle-même s’exécute dans un container Docker et pilote d’autres containers sur le même hôte via l’API Docker.



2. Principes d’architecture

2.1 Commandes vs Requêtes (CQRS pragmatique)

Deux catégories d’appels au backend :

Queries (READ) : ne modifient pas l’état du système (statut stack, liste versions, état commandes, monitoring containers…).
✅ exécutées immédiatement, sans ordonnancement strict.

Commands (WRITE) : susceptibles de modifier l’état (create/update/apply/deploy/delete/rollback…).
✅ persistées, ordonnées strictement, exécutées séquentiellement via un worker.

Règle : une commande WRITE ne démarre que si toutes les commandes WRITE précédentes sont DONE ou CANCELLED.

2.2 Versioning immuable des stacks

Une Stack est l’entité logique durable (identité).

Une StackVersion est un snapshot immuable de la définition déclarative de la stack à un instant donné.

Toute modification de stack crée une nouvelle StackVersion (jamais de modification “in place”).

Le rollback consiste à sélectionner une version existante et à déclencher une commande de convergence.

Analogie :

Stack = dépôt Git

StackVersion = commit Git


2.3 Définition “compose-like” (déclaratif)

Chaque StackVersion embarque une définition compatible docker-compose (langage déclaratif familier), utilisée comme “source of truth”.

L’orchestrateur effectue ensuite une convergence vers l’état désiré, via l’API Docker (SDK), sans dépendre du CLI docker compose.


2.4 Orchestration fiable par queue mono-partition persistée

Les commands WRITE sont stockées dans une queue persistée (mono-partition).

Un worker consomme la queue et exécute une commande à la fois, dans l’ordre.

Les queries READ ne passent pas par la queue.

Objectifs :

Ordonnancement strict

Reprise après crash (durabilité)

Audit / traçabilité (historique complet)

2.5 Convergence idempotente

L’exécution d’une StackVersion suit une logique de réconciliation :

Créer/vérifier networks et volumes

Créer les containers (stopped)

Démarrer selon dépendances (depends_on, healthchecks)

Enregistrer les identifiants runtime

Mettre à jour le statut

Cette approche permet :

relances sans effets de bord,

tolérance aux pannes,

rollback reproductible.

3. Architecture logique
   3.1 Composants

Frontend (IHM)

Wizard (construction de stack)

Dashboard (stacks, versions, commandes)

Monitoring et actions runtime

Utilise exclusivement l’API REST

Backend (Orchestrateur)

API Query (READ)

API Command (WRITE → enqueue)

Services de validation, versioning

Worker d’exécution (queue)

Intégration Docker SDK

Persistance : stacks, versions, commandes, logs

Docker Engine (host)

Exécute les containers de stacks

Exécute aussi le container “orchestrator”


3.2 Diagramme (simplifié)

Frontend ──REST──> Backend
|                 |
| Queries (READ)  | Commands (WRITE)
|                 v
|           Command Queue (DB)
|                 |
|               Worker
|                 |
└───────< Docker SDK / Docker API >─────── Docker Engine ──> Containers

4. Modèle de domaine
   4.1 Stack (identité)

stack_id

name

current_version (pointe vers la version active)

4.2 StackVersion (snapshot immuable)

stack_id

version (ex: v1, v2…)

parent_version

metadata (auteur, date, commentaire)

compose (définition compose-like)

runtime (mapping vers IDs Docker, généré)

status (desired/actual/last_updated)

4.3 Command (write)

command_id

stack_id

type

payload

status: PENDING|RUNNING|DONE|FAILED|CANCELLED

timestamps + logs

5. Choix d’implémentation (proposés)
   5.1 Backend

Python + FastAPI : API robuste, validation Pydantic, vitesse de dev.

Pydantic : contrats stricts pour StackVersion/Command.

Docker SDK (docker-py) : pilotage natif du runtime Docker.

SQLite au départ : queue + persistance simples, migrable vers Postgres.

Le backend tourne dans un container et accède à Docker via le socket /var/run/docker.sock (ou API TLS ultérieurement).

5.2 Frontend

SPA (React/Vue/Angular) — choix à finaliser ensuite.

Consomme uniquement l’API REST.

5.3 Format des définitions

YAML (lisible, proche docker-compose)

JSON possible côté API si besoin (conversion simple)

6. Sécurité (principes)

L’accès au socket Docker donne des privilèges élevés → limiter l’exposition :

orchestrateur sur réseau privé,

authentification/autorisation,

RBAC (phase ultérieure),

journalisation complète des actions.

À moyen terme : préférer Docker API TLS ou délégation via un agent.

7. API (esquisse)
   Queries (READ)

GET /stacks

GET /stacks/{id}

GET /stacks/{id}/versions

GET /stacks/{id}/versions/{version}

GET /stacks/{id}/status

GET /stacks/{id}/containers

GET /commands/{id}

GET /commands?stack_id=...

Commands (WRITE)

POST /stacks (crée Stack + version initiale)

POST /stacks/{id}/versions (crée une nouvelle StackVersion)

POST /stacks/{id}/apply/{version} (enqueue APPLY)

POST /stacks/{id}/rollback/{version} (enqueue ROLLBACK)

POST /stacks/{id}/deploy (enqueue DEPLOY)

POST /stacks/{id}/delete (enqueue DELETE)

POST /commands/{id}/cancel (annulation si possible)

8. Bonnes pratiques intégrées

Contrats stricts (Pydantic)

Séparation READ/WRITE (CQRS)

Immutabilité des versions

Queue persistée + worker séquentiel

Idempotence (convergence)

Logs structurés et auditables

Tests : unitaires, API, intégration Docker

CI/CD dès le début

Feature flags / versions API si nécessaire

Convention labels Docker pour traçabilité