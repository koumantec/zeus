# Guide de Démarrage Rapide - Core Control API

Ce guide vous permet de démarrer rapidement avec l'API Core Control en 5 minutes.

## Prérequis

- Docker installé et en cours d'exécution
- L'application Core Control démarrée sur `http://localhost:8080`
- `curl` et `jq` installés (optionnel pour le formatage JSON)

## Démarrage en 5 minutes

### 1. Vérifier que l'API est accessible

```bash
curl http://localhost:8080/worker
```

Résultat attendu : `{"running":true}`

### 2. Créer votre première stack

```bash
curl -X POST http://localhost:8080/stacks \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "my-first-stack",
    "name": "Ma Première Stack"
  }'
```

### 3. Créer une version avec Nginx

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/versions \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.0.0",
    "body": {
      "version": "3.8",
      "services": {
        "web": {
          "image": "nginx:alpine",
          "ports": ["8080:80"]
        }
      }
    },
    "createdBy": "moi",
    "comment": "Ma première version"
  }'
```

### 4. Déployer la stack

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/apply/v1.0.0
```

Notez le `commandId` retourné (par exemple : `1`)

### 5. Vérifier le déploiement

```bash
# Attendre quelques secondes
sleep 5

# Vérifier le statut de la commande
curl http://localhost:8080/commands/1

# Vérifier que la stack est en cours d'exécution
curl http://localhost:8080/stacks/my-first-stack/status
```

### 6. Tester votre application

```bash
curl http://localhost:8080
```

Vous devriez voir la page d'accueil de Nginx !

## Commandes utiles

### Lister toutes les stacks

```bash
curl http://localhost:8080/stacks
```

### Voir les versions d'une stack

```bash
curl http://localhost:8080/stacks/my-first-stack/versions
```

### Voir les conteneurs en cours d'exécution

```bash
curl http://localhost:8080/stacks/my-first-stack/containers
```

### Voir les logs d'un conteneur

```bash
# D'abord, obtenir le nom du conteneur
curl http://localhost:8080/stacks/my-first-stack/containers

# Puis récupérer les logs (remplacer CONTAINER_NAME)
curl "http://localhost:8080/stacks/my-first-stack/logs?service=CONTAINER_NAME&tail=50"
```

### Arrêter la stack

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/stop
```

### Démarrer la stack

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/start
```

### Redémarrer la stack

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/restart
```

### Supprimer la stack

```bash
curl -X POST http://localhost:8080/stacks/my-first-stack/delete
```

## Exemples avancés

### Stack avec base de données

```bash
# 1. Créer la stack
curl -X POST http://localhost:8080/stacks \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "app-with-db",
    "name": "Application avec Base de Données"
  }'

# 2. Créer une version avec app + PostgreSQL
curl -X POST http://localhost:8080/stacks/app-with-db/versions \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.0.0",
    "body": {
      "version": "3.8",
      "services": {
        "app": {
          "image": "node:20-alpine",
          "environment": [
            "DB_HOST=postgres",
            "DB_PORT=5432"
          ],
          "depends_on": ["postgres"],
          "ports": ["3000:3000"]
        },
        "postgres": {
          "image": "postgres:16-alpine",
          "environment": [
            "POSTGRES_DB=myapp",
            "POSTGRES_USER=user",
            "POSTGRES_PASSWORD=password"
          ],
          "volumes": ["db-data:/var/lib/postgresql/data"]
        }
      },
      "volumes": {
        "db-data": {}
      }
    },
    "createdBy": "moi",
    "comment": "App avec PostgreSQL"
  }'

# 3. Déployer
curl -X POST http://localhost:8080/stacks/app-with-db/apply/v1.0.0
```

### Mise à jour d'une stack

```bash
# 1. Créer une nouvelle version
curl -X POST http://localhost:8080/stacks/my-first-stack/versions \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.1.0",
    "body": {
      "version": "3.8",
      "services": {
        "web": {
          "image": "nginx:1.25-alpine",
          "ports": ["8080:80"],
          "environment": ["NGINX_HOST=localhost"]
        }
      }
    },
    "createdBy": "moi",
    "comment": "Mise à jour vers nginx 1.25"
  }'

# 2. Appliquer la nouvelle version
curl -X POST http://localhost:8080/stacks/my-first-stack/apply/v1.1.0
```

### Rollback

```bash
# Revenir à la version précédente
curl -X POST http://localhost:8080/stacks/my-first-stack/rollback/v1.0.0
```

## Interface Swagger UI

Pour une exploration interactive de l'API, ouvrez votre navigateur :

```
http://localhost:8080/swagger-ui.html
```

Vous pourrez :
- Voir tous les endpoints disponibles
- Tester les requêtes directement depuis le navigateur
- Voir les schémas de données
- Consulter les exemples

## Documentation complète

Pour plus de détails, consultez :

- **Spécification OpenAPI** : `openapi/core-control-api.yaml`
- **README** : `openapi/README.md`
- **Scénarios de test** : `openapi/test-scenarios.md`
- **Collection Postman** : `openapi/postman-collection.json`
- **Exemples cURL** : `openapi/curl-examples.sh`

## Workflow typique

```
1. Créer une stack
   ↓
2. Créer une version
   ↓
3. Appliquer la version (commande asynchrone)
   ↓
4. Suivre l'exécution via /commands/{id}
   ↓
5. Vérifier le statut via /stacks/{stackId}/status
   ↓
6. Consulter les logs si nécessaire
   ↓
7. Mettre à jour (créer nouvelle version + apply)
   ↓
8. Rollback si problème
```

## Résolution de problèmes

### La commande reste en PENDING

```bash
# Vérifier que le worker est actif
curl http://localhost:8080/worker

# Si running=false, redémarrer l'application
```

### La commande échoue (FAILED)

```bash
# Consulter les logs de la commande
curl http://localhost:8080/commands/{commandId}/logs

# Vérifier les logs Docker
docker logs [container-name]
```

### Le conteneur ne démarre pas

```bash
# Vérifier l'état des conteneurs
curl http://localhost:8080/stacks/{stackId}/containers

# Consulter les logs du conteneur
curl "http://localhost:8080/stacks/{stackId}/logs?service={service-name}&tail=100"
```

### Image Docker introuvable

```bash
# Vérifier que l'image existe sur Docker Hub
docker pull [image-name]

# Utiliser une image valide dans votre version
```

## Conseils

1. **Toujours vérifier le statut des commandes** : Les opérations sont asynchrones
2. **Utiliser des versions sémantiques** : v1.0.0, v1.1.0, v2.0.0, etc.
3. **Ajouter des commentaires** : Facilite le suivi des changements
4. **Consulter les logs** : En cas de problème
5. **Utiliser depends_on** : Pour gérer l'ordre de démarrage
6. **Définir des healthchecks** : Pour les services critiques

## Exemples de configurations Docker Compose

### Application Web Simple

```json
{
  "version": "3.8",
  "services": {
    "web": {
      "image": "nginx:alpine",
      "ports": ["80:80"]
    }
  }
}
```

### Application avec Base de Données

```json
{
  "version": "3.8",
  "services": {
    "app": {
      "image": "myapp:latest",
      "depends_on": ["db"],
      "environment": ["DB_HOST=db"]
    },
    "db": {
      "image": "postgres:16-alpine",
      "environment": ["POSTGRES_PASSWORD=secret"],
      "volumes": ["db-data:/var/lib/postgresql/data"]
    }
  },
  "volumes": {
    "db-data": {}
  }
}
```

### Stack Complète (Frontend + Backend + DB + Cache)

```json
{
  "version": "3.8",
  "services": {
    "frontend": {
      "image": "nginx:alpine",
      "ports": ["80:80"],
      "depends_on": ["backend"]
    },
    "backend": {
      "image": "node:20-alpine",
      "depends_on": ["db", "redis"],
      "environment": [
        "DB_HOST=db",
        "REDIS_URL=redis://redis:6379"
      ]
    },
    "db": {
      "image": "postgres:16-alpine",
      "environment": ["POSTGRES_PASSWORD=secret"],
      "volumes": ["db-data:/var/lib/postgresql/data"]
    },
    "redis": {
      "image": "redis:7-alpine"
    }
  },
  "volumes": {
    "db-data": {}
  }
}
```

## Support

Pour toute question :
- Consultez la documentation Swagger UI
- Vérifiez les logs de l'application
- Consultez les scénarios de test pour des exemples complets

Bon déploiement ! 🚀
