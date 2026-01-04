# Core Control

API REST pour l'orchestration de stacks Docker basée sur le pattern CQRS.

## 🚀 Démarrage Rapide

```bash
# Démarrer l'application
mvn spring-boot:run

# Vérifier que l'API fonctionne
curl http://localhost:8080/worker
```

## 📚 Documentation

### Documentation OpenAPI Complète

Une documentation OpenAPI complète et professionnelle est disponible dans le dossier `openapi/` :

- **[Guide de Démarrage Rapide](openapi/quick-start.md)** - Démarrez en 5 minutes
- **[Spécification OpenAPI](openapi/core-control-api.yaml)** - Spécification complète

### Swagger UI

Interface interactive disponible une fois l'application démarrée :

```
http://localhost:8080/swagger-ui.html
```

### Spécification OpenAPI

- **JSON** : http://localhost:8080/v3/api-docs
- **YAML** : http://localhost:8080/v3/api-docs.yaml

## 🎯 Fonctionnalités

- ✅ Gestion de stacks Docker
- ✅ Versioning des configurations
- ✅ Commandes asynchrones (apply, rollback, deploy, start, stop, restart, delete)
- ✅ Suivi de l'exécution des commandes
- ✅ Consultation des logs
- ✅ Monitoring des conteneurs
- ✅ Pattern CQRS (Command Query Responsibility Segregation)

## 🏗️ Architecture

```
Client → REST API → Service Layer → Repository → SQLite
                         ↓
                   Command Worker → Docker Engine
```

### Pattern CQRS

- **Queries (GET)** : Synchrones, lecture seule
- **Commands (POST)** : Asynchrones, modification d'état

Voir [ARCHITECTURE.md](openapi/ARCHITECTURE.md) pour plus de détails.

## 📦 Technologies

- **Spring Boot 4.0.1** - Framework web
- **Java 21** - Langage
- **SQLite** - Base de données
- **Docker Java Client 3.4.0** - API Docker
- **SpringDoc OpenAPI 2.6.0** - Documentation
- **Maven** - Build

## 🔧 Installation

### Prérequis

- Java 21+
- Maven 3.8+
- Docker

### Build

```bash
mvn clean package
```

### Exécution

```bash
# Avec Maven
mvn spring-boot:run

# Avec le JAR
java -jar target/core-control-0.0.1-SNAPSHOT.jar

# Avec Docker
docker-compose up
```

## 📖 Exemples d'Utilisation

### Créer une stack

```bash
curl -X POST http://localhost:8080/stacks \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "my-app",
    "name": "My Application"
  }'
```

### Créer une version

```bash
curl -X POST http://localhost:8080/stacks/my-app/versions \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.0.0",
    "body": {
      "version": "3.8",
      "services": {
        "web": {
          "image": "nginx:alpine",
          "ports": ["80:80"]
        }
      }
    },
    "createdBy": "admin",
    "comment": "Initial version"
  }'
```

### Déployer la stack

```bash
curl -X POST http://localhost:8080/stacks/my-app/apply/v1.0.0
```

### Vérifier le statut

```bash
curl http://localhost:8080/stacks/my-app/status
```

Pour plus d'exemples, consultez :
- [Guide de Démarrage Rapide](openapi/quick-start.md)
- [Scénarios de Test](openapi/test-scenarios.md)
- [Exemples cURL](openapi/curl-examples.sh)

## 🧪 Tests

### Exécuter les tests

```bash
mvn test
```

### Scénarios de test

Consultez [test-scenarios.md](openapi/test-scenarios.md) pour des scénarios de test complets.

### Collection Postman

Importez [postman-collection.json](openapi/postman-collection.json) dans Postman pour des tests interactifs.

## 📊 API Endpoints

### Stacks
- `GET /stacks` - Liste toutes les stacks
- `POST /stacks` - Crée une nouvelle stack
- `GET /stacks/{stackId}` - Détails d'une stack

### Versions
- `GET /stacks/{stackId}/versions` - Liste les versions
- `POST /stacks/{stackId}/versions` - Crée une version
- `GET /stacks/{stackId}/versions/{version}` - Détails d'une version

### Commandes
- `POST /stacks/{stackId}/apply/{version}` - Applique une version
- `POST /stacks/{stackId}/rollback/{version}` - Rollback
- `POST /stacks/{stackId}/start` - Démarre la stack
- `POST /stacks/{stackId}/stop` - Arrête la stack
- `POST /stacks/{stackId}/restart` - Redémarre la stack
- `POST /stacks/{stackId}/delete` - Supprime la stack
- `POST /stacks/{stackId}/deploy` - Déploie une application

### Monitoring
- `GET /commands` - Liste les commandes
- `GET /commands/{id}` - Détails d'une commande
- `GET /commands/{id}/logs` - Logs d'une commande
- `GET /stacks/{stackId}/status` - Statut de la stack
- `GET /stacks/{stackId}/containers` - Liste les conteneurs
- `GET /stacks/{stackId}/logs` - Logs d'un service
- `GET /worker` - Statut du worker

Voir la [spécification OpenAPI complète](openapi/core-control-api.yaml) pour tous les détails.

## 🔐 Configuration

### application.properties

```properties
# Port du serveur
server.port=8080

# Base de données SQLite
spring.datasource.url=jdbc:sqlite:core-control.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Docker
docker.host=unix:///var/run/docker.sock
```

## 🐳 Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'
services:
  core-control:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./data:/app/data
```

## 📝 Licence

Ce projet est sous licence propriétaire STET.

## 👥 Contributeurs

- Équipe Core Control

## 📞 Support

Pour toute question :
- Consultez la [documentation OpenAPI](openapi/INDEX.md)
- Ouvrez une issue sur JIRA
- Contactez l'équipe de développement
---

**Core Control** - Orchestration de stacks Docker simplifiée
