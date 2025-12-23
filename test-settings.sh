#!/bin/bash

# Script de test pour vérifier la configuration des paramètres

echo "=== Test de la configuration des paramètres ==="
echo ""

# 1. Vérifier que le dossier .core existe
echo "1. Vérification du dossier ~/.core"
if [ -d "$HOME/.core" ]; then
    echo "   ✓ Le dossier ~/.core existe"
else
    echo "   ✗ Le dossier ~/.core n'existe pas"
    echo "   → Création du dossier..."
    mkdir -p "$HOME/.core"
    echo "   ✓ Dossier créé"
fi
echo ""

# 2. Vérifier le fichier settings.conf
echo "2. Vérification du fichier settings.conf"
if [ -f "$HOME/.core/settings.conf" ]; then
    echo "   ✓ Le fichier settings.conf existe"
    echo "   Contenu actuel :"
    cat "$HOME/.core/settings.conf" | sed 's/^/   /'
else
    echo "   ✗ Le fichier settings.conf n'existe pas"
    echo "   → Il sera créé au premier démarrage de l'application"
fi
echo ""

# 3. Vérifier les permissions
echo "3. Vérification des permissions"
ls -la "$HOME/.core" | grep -E "^d|settings.conf" | sed 's/^/   /'
echo ""

# 4. Instructions pour tester
echo "=== Instructions de test ==="
echo ""
echo "Pour tester l'application :"
echo "1. Démarrer l'application : docker-compose up -d"
echo "2. Accéder à http://localhost:3000"
echo "3. Vous devriez être redirigé vers /settings si le fichier est vide ou absent"
echo "4. Renseigner les identifiants et enregistrer"
echo "5. Vérifier que le fichier ~/.core/settings.conf a été créé avec les valeurs"
echo ""
echo "Pour réinitialiser les paramètres :"
echo "   rm ~/.core/settings.conf"
echo ""
