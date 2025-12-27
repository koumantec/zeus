Plan d’implémentation (pas à pas, contrôlé)

Objectif : livrer par incréments, chaque étape testée, stable, versionnée.

Phase 0 — Base projet (structure + qualité)

Repo + conventions (.editorconfig, ruff/black, mypy si souhaité)

Arborescence, packaging Python

CI (lint + tests)

Dockerfile backend + docker-compose dev

Livrable : squelette exécutable + pipeline qualité.

Phase 1 — Modèles & validation (contrats)

Implémenter modèles Pydantic :

Stack, StackVersion (compose + metadata), Command

Loader YAML → StackVersion

Tests unitaires validation (cas ok / cas KO)

Livrable : parsing/validation solide, sans API.

Phase 1 — Modèles & validation (contrats)

Implémenter modèles Pydantic :

Stack, StackVersion (compose + metadata), Command

Loader YAML → StackVersion

Tests unitaires validation (cas ok / cas KO)

Livrable : parsing/validation solide, sans API.

Phase 2 — Persistance & queue (SQLite)

Schéma DB minimal :

stacks

stack_versions

commands

command_logs

DAO/repository layer (abstraire SQLite)

Enqueue d’une commande + lecture de queue

Livrable : queue persistée mono-partition fonctionnelle.

Phase 3 — API Query (READ)

FastAPI endpoints READ :

stacks, versions, commandes

Pagination + filtres

Tests API (pytest + httpx)

Livrable : consultation complète sans exécution.

Phase 4 — API Command (WRITE) + machine d’état

Endpoints WRITE : create version, apply, rollback, delete, deploy (stubs)

Validation métier :

immutable versions

version increment

“une seule commande RUNNING à la fois”

Statuts : PENDING/RUNNING/DONE/FAILED/CANCELLED

Annulation (règles : uniquement si PENDING, ou mécanisme coopératif)

Livrable : commande asynchrone ordonnée (sans Docker réel).

Phase 5 — Worker d’exécution (sans Docker)

Worker loop :

pop prochaine commande PENDING

lock

RUNNING → DONE/FAILED

Gestion crash/reprise :

commandes RUNNING “stale” → FAILED/RETRY selon politique

Logs par commande

Livrable : exécution séquentielle fiable et observable.

Phase 6 — Docker SDK : primitives (networks/volumes/containers)

Connexion au Docker daemon (socket)

Création idempotente :

network ensure

volume ensure

container create (stopped)

Labels systématiques :

stack_id, version, service_name, command_id

Tests d’intégration (docker-in-docker ou runner local)

Livrable : primitives Docker stables.

Phase 7 — Convergence “compose-like” (APPLY)

Traduire compose.services → objets Docker

Graphe depends_on + démarrage ordonné

Healthchecks et “wait until healthy”

Mise à jour de runtime (container IDs, volume/network IDs)

Statuts stack/version (DEPLOYING → RUNNING)

Livrable : APPLY_STACK_VERSION complet.

Phase 8 — Rollback (ROLLBACK)

Commande rollback = convergence vers version ciblée

Politique sur containers existants :

stop/remove + recreate (simple)

ou reconcile fin (phase ultérieure)

Mise à jour current_version

Livrable : rollback fiable.

Phase 9 — Monitoring

Queries :

état containers, health, uptime

logs tail (option)

Événements Docker (option) → push vers UI via WebSocket/SSE

Livrable : dashboard de monitoring.

Phase 10 — Déploiement applicatif dans middleware (DEPLOY)

Définir abstractions :

DeployTarget (wildfly, tomcat, nginx…)

Implémenter stratégie par middleware :

docker exec + copie fichier

endpoint de management (si applicable)

Audit + logs + rollback applicatif (option)

Livrable : déploiement applicatif fonctionnel.

Phase 11 — Wizard & règles de cohérence

Catalogue composants (metadata)

Moteur de règles (dépendances, compatibilités)

UI wizard dynamique

Génération StackVersion depuis choix

Livrable : expérience utilisateur complète.

Phase 12 — Hardening (prod-ready)

AuthN/AuthZ (JWT/OIDC)

RBAC (rôles)

Sécurité Docker socket (agent/TLS)

Observabilité (Prometheus, logs structurés)

Migration SQLite → Postgres

Backups/restore

Tests end-to-end