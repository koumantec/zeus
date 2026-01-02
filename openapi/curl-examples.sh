#!/bin/bash

# Core Control API - Exemples de requêtes cURL
# ============================================

BASE_URL="http://localhost:8080"

# Couleurs pour l'affichage
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_section() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

echo_command() {
    echo -e "${GREEN}$1${NC}"
}

# ==================== STACKS ====================

echo_section "STACKS - Gestion des stacks"

echo_command "1. Lister toutes les stacks"
curl -X GET "$BASE_URL/stacks" | jq .

echo_command "\n2. Créer une nouvelle stack"
curl -X POST "$BASE_URL/stacks" \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "web-app-prod",
    "name": "Application Web Production"
  }' | jq .

echo_command "\n3. Obtenir les détails d'une stack"
curl -X GET "$BASE_URL/stacks/web-app-prod" | jq .

# ==================== VERSIONS ====================

echo_section "VERSIONS - Gestion des versions"

echo_command "4. Créer une version simple (Nginx)"
curl -X POST "$BASE_URL/stacks/web-app-prod/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.0.0",
    "body": {
      "version": "3.8",
      "services": {
        "nginx": {
          "image": "nginx:1.25-alpine",
          "ports": ["80:80"]
        }
      }
    },
    "createdBy": "admin@example.com",
    "comment": "Version initiale avec Nginx"
  }' | jq .

echo_command "\n5. Créer une version complète (Web App)"
curl -X POST "$BASE_URL/stacks/web-app-prod/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v1.2.0",
    "body": {
      "version": "3.8",
      "services": {
        "nginx": {
          "image": "nginx:1.25-alpine",
          "ports": ["80:80", "443:443"],
          "volumes": [
            "./nginx.conf:/etc/nginx/nginx.conf:ro",
            "web-content:/usr/share/nginx/html"
          ],
          "networks": ["frontend"]
        },
        "app": {
          "image": "myapp:1.2.0",
          "environment": [
            "DB_HOST=postgres",
            "DB_PORT=5432",
            "REDIS_URL=redis://redis:6379"
          ],
          "depends_on": ["postgres", "redis"],
          "networks": ["frontend", "backend"]
        },
        "postgres": {
          "image": "postgres:16-alpine",
          "environment": [
            "POSTGRES_DB=myapp",
            "POSTGRES_USER=appuser",
            "POSTGRES_PASSWORD=secret123"
          ],
          "volumes": ["postgres-data:/var/lib/postgresql/data"],
          "networks": ["backend"]
        },
        "redis": {
          "image": "redis:7-alpine",
          "networks": ["backend"]
        }
      },
      "networks": {
        "frontend": {"driver": "bridge"},
        "backend": {"driver": "bridge"}
      },
      "volumes": {
        "web-content": {},
        "postgres-data": {}
      }
    },
    "createdBy": "admin@example.com",
    "comment": "Stack complète avec nginx, app, postgres et redis"
  }' | jq .

echo_command "\n6. Lister les versions d'une stack"
curl -X GET "$BASE_URL/stacks/web-app-prod/versions" | jq .

echo_command "\n7. Obtenir une version spécifique"
curl -X GET "$BASE_URL/stacks/web-app-prod/versions/v1.0.0" | jq .

# ==================== COMMANDES ====================

echo_section "COMMANDES - Opérations asynchrones"

echo_command "8. Appliquer une version"
APPLY_RESPONSE=$(curl -s -X POST "$BASE_URL/stacks/web-app-prod/apply/v1.0.0")
echo $APPLY_RESPONSE | jq .
COMMAND_ID=$(echo $APPLY_RESPONSE | jq -r '.commandId')
echo -e "${YELLOW}Command ID: $COMMAND_ID${NC}"

echo_command "\n9. Vérifier le statut de la commande"
sleep 2
curl -X GET "$BASE_URL/commands/$COMMAND_ID" | jq .

echo_command "\n10. Obtenir les logs de la commande"
curl -X GET "$BASE_URL/commands/$COMMAND_ID/logs?limit=100" | jq .

echo_command "\n11. Démarrer une stack"
curl -X POST "$BASE_URL/stacks/web-app-prod/start" | jq .

echo_command "\n12. Arrêter une stack"
curl -X POST "$BASE_URL/stacks/web-app-prod/stop" | jq .

echo_command "\n13. Redémarrer une stack"
curl -X POST "$BASE_URL/stacks/web-app-prod/restart" | jq .

echo_command "\n14. Rollback vers une version précédente"
curl -X POST "$BASE_URL/stacks/web-app-prod/rollback/v1.0.0" | jq .

echo_command "\n15. Déployer une application WAR"
curl -X POST "$BASE_URL/stacks/web-app-prod/deploy" \
  -H "Content-Type: application/json" \
  -d '{
    "targetService": "wildfly",
    "artifactPath": "/uploads/myapp-1.2.0.war",
    "strategy": "copy"
  }' | jq .

echo_command "\n16. Supprimer une stack (ATTENTION: destructif!)"
# curl -X POST "$BASE_URL/stacks/web-app-prod/delete" | jq .

echo_command "\n17. Annuler une commande en attente"
# curl -X POST "$BASE_URL/commands/10/cancel" | jq .

# ==================== REQUÊTES ====================

echo_section "REQUÊTES - Consultation de l'état"

echo_command "18. Lister toutes les commandes"
curl -X GET "$BASE_URL/commands?limit=20" | jq .

echo_command "\n19. Lister les commandes d'une stack"
curl -X GET "$BASE_URL/commands?stackId=web-app-prod&limit=10" | jq .

echo_command "\n20. Obtenir le statut d'une stack"
curl -X GET "$BASE_URL/stacks/web-app-prod/status" | jq .

echo_command "\n21. Lister les conteneurs d'une stack"
curl -X GET "$BASE_URL/stacks/web-app-prod/containers" | jq .

echo_command "\n22. Obtenir les logs d'un service"
curl -X GET "$BASE_URL/stacks/web-app-prod/logs?service=web-app-prod-nginx-1&tail=50" | jq .

echo_command "\n23. Vérifier le statut du worker"
curl -X GET "$BASE_URL/worker" | jq .

# ==================== WORKFLOW COMPLET ====================

echo_section "WORKFLOW COMPLET - Exemple de bout en bout"

echo_command "Étape 1: Créer une stack"
curl -X POST "$BASE_URL/stacks" \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "demo-app",
    "name": "Application de Démonstration"
  }' | jq .

echo_command "\nÉtape 2: Créer une version"
curl -X POST "$BASE_URL/stacks/demo-app/versions" \
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
    "createdBy": "demo-user",
    "comment": "Version initiale"
  }' | jq .

echo_command "\nÉtape 3: Appliquer la version"
DEMO_RESPONSE=$(curl -s -X POST "$BASE_URL/stacks/demo-app/apply/v1.0.0")
echo $DEMO_RESPONSE | jq .
DEMO_CMD_ID=$(echo $DEMO_RESPONSE | jq -r '.commandId')

echo_command "\nÉtape 4: Attendre et vérifier le statut"
sleep 5
curl -X GET "$BASE_URL/commands/$DEMO_CMD_ID" | jq .

echo_command "\nÉtape 5: Vérifier l'état de la stack"
curl -X GET "$BASE_URL/stacks/demo-app/status" | jq .

echo_command "\nÉtape 6: Consulter les logs de la commande"
curl -X GET "$BASE_URL/commands/$DEMO_CMD_ID/logs" | jq .

# ==================== EXEMPLES WILDFLY ====================

echo_section "EXEMPLE WILDFLY - Stack Java EE"

echo_command "Créer une stack WildFly"
curl -X POST "$BASE_URL/stacks" \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "wildfly-stack",
    "name": "WildFly Application Server"
  }' | jq .

echo_command "\nCréer une version WildFly avec PostgreSQL"
curl -X POST "$BASE_URL/stacks/wildfly-stack/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "v2.0.0",
    "body": {
      "version": "3.8",
      "services": {
        "wildfly": {
          "image": "quay.io/wildfly/wildfly:31.0.0.Final-jdk21",
          "ports": ["8080:8080", "9990:9990"],
          "environment": [
            "WILDFLY_USER=admin",
            "WILDFLY_PASSWORD=admin123"
          ],
          "volumes": [
            "./deployments:/opt/jboss/wildfly/standalone/deployments"
          ],
          "depends_on": ["postgres"],
          "networks": ["app-network"]
        },
        "postgres": {
          "image": "postgres:16-alpine",
          "environment": [
            "POSTGRES_DB=wildfly_db",
            "POSTGRES_USER=wildfly",
            "POSTGRES_PASSWORD=wildfly123"
          ],
          "volumes": ["postgres-data:/var/lib/postgresql/data"],
          "networks": ["app-network"]
        }
      },
      "networks": {
        "app-network": {"driver": "bridge"}
      },
      "volumes": {
        "postgres-data": {}
      }
    },
    "createdBy": "devops@example.com",
    "comment": "Configuration WildFly 31 avec PostgreSQL 16"
  }' | jq .

echo_command "\nAppliquer la version WildFly"
curl -X POST "$BASE_URL/stacks/wildfly-stack/apply/v2.0.0" | jq .

# ==================== TESTS D'ERREUR ====================

echo_section "TESTS D'ERREUR - Gestion des cas d'erreur"

echo_command "Tenter de créer une stack existante (409 Conflict)"
curl -X POST "$BASE_URL/stacks" \
  -H "Content-Type: application/json" \
  -d '{
    "stackId": "web-app-prod",
    "name": "Duplicate Stack"
  }' | jq .

echo_command "\nTenter d'obtenir une stack inexistante (404 Not Found)"
curl -X GET "$BASE_URL/stacks/non-existent-stack" | jq .

echo_command "\nTenter d'obtenir une version inexistante (404 Not Found)"
curl -X GET "$BASE_URL/stacks/web-app-prod/versions/v99.99.99" | jq .

echo_command "\nTenter d'annuler une commande terminée (409 Conflict)"
# curl -X POST "$BASE_URL/commands/1/cancel" | jq .

echo_section "Tests terminés!"
echo -e "${GREEN}Pour plus d'informations, consultez la documentation OpenAPI:${NC}"
echo -e "${YELLOW}http://localhost:8080/swagger-ui.html${NC}"
