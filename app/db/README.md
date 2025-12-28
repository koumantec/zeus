# Database Layer - Phase 2

Cette couche implémente la persistance et la queue de commandes pour Core Monitor.

## Architecture

### Composants

1. **schema.py** : Initialisation du schéma SQLite
   - Tables : `stacks`, `stack_versions`, `commands`, `command_logs`
   - Index pour optimiser les requêtes
   - Contraintes d'intégrité référentielle

2. **repositories.py** : Couche d'accès aux données (DAO/Repository pattern)
   - `StackRepository` : CRUD pour les stacks
   - `StackVersionRepository` : Gestion des versions de stacks
   - `CommandRepository` : Queue de commandes avec opérations FIFO
   - `CommandLogRepository` : Logs d'exécution des commandes

3. **database.py** : Gestionnaire de base de données
   - Point d'entrée unique pour accéder aux repositories
   - Gestion du cycle de vie de la connexion
   - Support du context manager

## Schéma de Base de Données

### Table `stacks`
```sql
CREATE TABLE stacks (
    stack_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    current_version TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
)
```

### Table `stack_versions`
```sql
CREATE TABLE stack_versions (
    stack_id TEXT NOT NULL,
    version TEXT NOT NULL,
    parent_version TEXT,
    created_at TEXT NOT NULL,
    created_by TEXT NOT NULL,
    comment TEXT,
    compose_definition TEXT NOT NULL,  -- JSON
    runtime_mapping TEXT,              -- JSON (nullable)
    status TEXT,                       -- JSON (nullable)
    PRIMARY KEY (stack_id, version),
    FOREIGN KEY (stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
)
```

### Table `commands`
```sql
CREATE TABLE commands (
    command_id INTEGER PRIMARY KEY AUTOINCREMENT,
    stack_id TEXT NOT NULL,
    type TEXT NOT NULL,
    payload TEXT NOT NULL,             -- JSON
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL,
    started_at TEXT,
    ended_at TEXT,
    FOREIGN KEY (stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
)
```

### Table `command_logs`
```sql
CREATE TABLE command_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    command_id INTEGER NOT NULL,
    timestamp TEXT NOT NULL,
    level TEXT NOT NULL,
    message TEXT NOT NULL,
    FOREIGN KEY (command_id) REFERENCES commands(command_id) ON DELETE CASCADE
)
```

## Utilisation

### Initialisation

```python
from app.db.database import Database

# Base de données en mémoire (pour tests)
db = Database()
db.connect()

# Base de données persistante
db = Database(db_path="storage/core-monitor.db")
db.connect()

# Avec context manager
with Database(db_path="storage/core-monitor.db") as db:
    # Utiliser db...
    pass
```

### Opérations sur les Stacks

```python
from app.models.stack import Stack

# Créer une stack
stack = Stack(
    stack_id="stack-001",
    name="my-app",
    current_version=None
)
db.stacks.create(stack)

# Récupérer une stack
stack = db.stacks.get_by_id("stack-001")
stack = db.stacks.get_by_name("my-app")

# Lister toutes les stacks
stacks = db.stacks.list_all()

# Mettre à jour la version courante
db.stacks.update_current_version("stack-001", "v2")

# Supprimer une stack (cascade vers versions et commandes)
db.stacks.delete("stack-001")
```

### Opérations sur les Versions

```python
from datetime import datetime
from app.models.stack_version import (
    StackVersion, StackMetadata, ComposeDefinition
)

# Créer une version
version = StackVersion(
    stack_id="stack-001",
    version="v1",
    parent_version=None,
    metadata=StackMetadata(
        created_at=datetime.utcnow(),
        created_by="admin",
        comment="Initial version"
    ),
    compose=ComposeDefinition(
        version="3.8",
        services={
            "web": {"image": "nginx:latest"}
        }
    )
)
db.stack_versions.create(version)

# Récupérer une version
version = db.stack_versions.get_by_version("stack-001", "v1")

# Lister les versions d'une stack
versions = db.stack_versions.list_by_stack("stack-001")

# Mettre à jour le runtime mapping
runtime = {
    "networks": {"default": "net_abc123"},
    "volumes": {},
    "containers": {"web": {"id": "container_xyz"}}
}
db.stack_versions.update_runtime("stack-001", "v1", runtime)

# Mettre à jour le status
status = {
    "desired": "RUNNING",
    "actual": "RUNNING",
    "last_updated": datetime.utcnow().isoformat()
}
db.stack_versions.update_status("stack-001", "v1", status)
```

### Queue de Commandes

```python
from datetime import datetime
from app.models.command import Command

# Enqueue une commande
cmd = Command(
    stack_id="stack-001",
    type="APPLY_STACK_VERSION",
    payload={"version": "v1"},
    status="PENDING",
    created_at=datetime.utcnow()
)
db.commands.enqueue(cmd)

# Récupérer la prochaine commande pending (FIFO)
next_cmd = db.commands.get_next_pending()

# Mettre à jour le statut
db.commands.update_status(
    next_cmd.command_id,
    "RUNNING",
    started_at=datetime.utcnow()
)

db.commands.update_status(
    next_cmd.command_id,
    "DONE",
    ended_at=datetime.utcnow()
)

# Lister les commandes par stack
commands = db.commands.list_by_stack("stack-001")

# Lister les commandes par statut
pending = db.commands.list_by_status("PENDING")
running = db.commands.list_by_status("RUNNING")
done = db.commands.list_by_status("DONE")
```

### Logs de Commandes

```python
from datetime import datetime

# Ajouter un log
db.command_logs.add_log(
    command_id=1,
    level="INFO",
    message="Starting command execution",
    timestamp=datetime.utcnow()
)

# Récupérer les logs d'une commande
logs = db.command_logs.get_logs(command_id=1)
for log in logs:
    print(f"[{log['level']}] {log['message']}")
```

## Comportement de la Queue

La queue de commandes suit ces principes :

1. **FIFO (First In, First Out)** : Les commandes sont traitées dans l'ordre de création
2. **Mono-partition** : Une seule commande `RUNNING` à la fois
3. **Persistée** : Survit aux redémarrages
4. **Ordonnée** : Index sur `created_at` pour garantir l'ordre

### Statuts de Commande

- `PENDING` : En attente d'exécution
- `RUNNING` : En cours d'exécution
- `DONE` : Terminée avec succès
- `FAILED` : Terminée en échec
- `CANCELLED` : Annulée

### Workflow Typique

```python
# Worker loop
while True:
    # Récupérer la prochaine commande
    cmd = db.commands.get_next_pending()
    if not cmd:
        break
    
    # Marquer comme running
    db.commands.update_status(cmd.command_id, "RUNNING", started_at=datetime.utcnow())
    db.command_logs.add_log(cmd.command_id, "INFO", "Starting execution")
    
    try:
        # Exécuter la commande
        execute_command(cmd)
        
        # Marquer comme done
        db.commands.update_status(cmd.command_id, "DONE", ended_at=datetime.utcnow())
        db.command_logs.add_log(cmd.command_id, "INFO", "Execution completed")
    except Exception as e:
        # Marquer comme failed
        db.commands.update_status(cmd.command_id, "FAILED", ended_at=datetime.utcnow())
        db.command_logs.add_log(cmd.command_id, "ERROR", f"Execution failed: {str(e)}")
```

## Tests

Les tests couvrent :

- ✅ Initialisation du schéma
- ✅ CRUD sur les stacks
- ✅ CRUD sur les versions
- ✅ Queue de commandes (enqueue, dequeue, FIFO)
- ✅ Logs de commandes
- ✅ Cascade delete (intégrité référentielle)
- ✅ Workflow complet d'intégration

Exécuter les tests :

```bash
pytest tests/test_database_schema.py -v
pytest tests/test_stack_repository.py -v
pytest tests/test_stack_version_repository.py -v
pytest tests/test_command_repository.py -v
pytest tests/test_command_log_repository.py -v
pytest tests/test_queue_integration.py -v
```

## Prochaines Étapes (Phase 3)

La phase 3 ajoutera l'API Query (READ) qui utilisera ces repositories pour exposer les données via FastAPI.
