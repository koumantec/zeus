# Plan d'Implémentation (Pas à Pas, Contrôlé)

**Objectif :** Livrer par incréments, chaque étape testée, stable, versionnée.

---

## Phase 0 — Base Projet (Structure + Qualité)

### Objectifs
- Repo + conventions (`.editorconfig`, `ruff`/`black`, `mypy` si souhaité)
- Arborescence, packaging Python
- CI (lint + tests)
- Dockerfile backend + docker-compose dev

### Livrable
✅ Squelette exécutable + pipeline qualité

---

## Phase 1 — Modèles & Validation (Contrats)

### Objectifs
- Implémenter modèles Pydantic :
  - `Stack`
  - `StackVersion` (compose + metadata)
  - `Command`
- Loader YAML → `StackVersion`
- Tests unitaires validation (cas OK / cas KO)

### Livrable
✅ Parsing/validation solide, sans API

---

## Phase 2 — Persistance & Queue (SQLite)

### Objectifs
- Schéma DB minimal :
  - `stacks`
  - `stack_versions`
  - `commands`
  - `command_logs`
- DAO/repository layer (abstraire SQLite)
- Enqueue d'une commande + lecture de queue

### Livrable
✅ Queue persistée mono-partition fonctionnelle

---

## Phase 3 — API Query (READ)

### Objectifs
- FastAPI endpoints READ :
  - Stacks
  - Versions
  - Commandes
- Pagination + filtres
- Tests API (pytest + httpx)

### Livrable
✅ Consultation complète sans exécution

---

## Phase 4 — API Command (WRITE) + Machine d'État

### Objectifs
- Endpoints WRITE : create version, apply, rollback, delete, deploy (stubs)
- Validation métier :
  - Immutable versions
  - Version increment
  - "Une seule commande RUNNING à la fois"
- Statuts : `PENDING` / `RUNNING` / `DONE` / `FAILED` / `CANCELLED`
- Annulation (règles : uniquement si `PENDING`, ou mécanisme coopératif)

### Livrable
✅ Commande asynchrone ordonnée (sans Docker réel)

---

## Phase 5 — Worker d'Exécution (Sans Docker)

### Objectifs
- Worker loop :
  - Pop prochaine commande `PENDING`
  - Lock
  - `RUNNING` → `DONE` / `FAILED`
- Gestion crash/reprise :
  - Commandes `RUNNING` "stale" → `FAILED` / `RETRY` selon politique
- Logs par commande

### Livrable
✅ Exécution séquentielle fiable et observable

---

## Phase 6 — Docker SDK : Primitives (Networks/Volumes/Containers)

### Objectifs
- Connexion au Docker daemon (socket)
- Création idempotente :
  - Network ensure
  - Volume ensure
  - Container create (stopped)
- Labels systématiques :
  - `stack_id`
  - `version`
  - `service_name`
  - `command_id`
- Tests d'intégration (docker-in-docker ou runner local)

### Livrable
✅ Primitives Docker stables

---

## Phase 7 — Convergence "Compose-like" (APPLY)

### Objectifs
- Traduire `compose.services` → objets Docker
- Graphe `depends_on` + démarrage ordonné
- Healthchecks et "wait until healthy"
- Mise à jour de runtime (container IDs, volume/network IDs)
- Statuts stack/version (`DEPLOYING` → `RUNNING`)

### Livrable
✅ `APPLY_STACK_VERSION` complet

---

## Phase 8 — Rollback (ROLLBACK)

### Objectifs
- Commande rollback = convergence vers version ciblée
- Politique sur containers existants :
  - Stop/remove + recreate (simple)
  - Ou reconcile fin (phase ultérieure)
- Mise à jour `current_version`

### Livrable
✅ Rollback fiable

---

## Phase 9 — Monitoring

### Objectifs
- Queries :
  - État containers, health, uptime
  - Logs tail (option)
- Événements Docker (option) → push vers UI via WebSocket/SSE

### Livrable
✅ Dashboard de monitoring

---

## Phase 10 Déploiement Applicatif dans Middleware (DEPLOY)

### Objectifs
- Définir abstractions :
  - `DeployTarget` (wildfly, tomcat, nginx…)
- Implémenter stratégie par middleware :
  - `docker exec` + copie fichier
  - Endpoint de management (si applicable)
- Audit + logs + rollback applicatif (option)

### Livrable
✅ Déploiement applicatif fonctionnel

---

## Phase 11 — Wizard & Règles de Cohérence

### Objectifs
- Catalogue composants (metadata)
- Moteur de règles (dépendances, compatibilités)
- UI wizard dynamique
- Génération `StackVersion` depuis choix

### Livrable
✅ Expérience utilisateur complète

---

## Phase 12 — Hardening (Prod-Ready)

### Objectifs
- **AuthN/AuthZ** (JWT/OIDC)
- **RBAC** (rôles)
- **Sécurité Docker socket** (agent/TLS)
- **Observabilité** (Prometheus, logs structurés)
- **Migration SQLite → Postgres**
- **Backups/restore**
- **Tests end-to-end**

### Livrable
✅ Application production-ready

---

## Résumé des Phases

| Phase | Titre                                      | Statut |
|-------|--------------------------------------------|--------|
| 0     | Base Projet (Structure + Qualité)         | ✅     |
| 1     | Modèles & Validation (Contrats)            | ✅     |
| 2     | Persistance & Queue (SQLite)               | ✅     |
| 3     | API Query (READ)                           | ⏳     |
| 4     | API Command (WRITE) + Machine d'État       | ⏳     |
| 5     | Worker d'Exécution (Sans Docker)           | ⏳     |
| 6     | Docker SDK : Primitives                    | ⏳     |
| 7     | Convergence "Compose-like" (APPLY)         | ⏳     |
| 8     | Rollback (ROLLBACK)                        | ⏳     |
| 9     | Monitoring                                 | ⏳     |
| 10    | Déploiement Applicatif dans Middleware     | ⏳     |
| 11    | Wizard & Règles de Cohérence               | ⏳     |
| 12    | Hardening (Prod-Ready)                     | ⏳     |
