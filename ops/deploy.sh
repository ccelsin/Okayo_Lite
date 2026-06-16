#!/bin/bash
set -e

# =============================================================
# Script de déploiement Okayo Lite — VPS
# Usage : bash ops/deploy.sh
# =============================================================

echo "==> Installation de Docker..."
apt-get update -q
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -q
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "==> Docker installé : $(docker --version)"

echo ""
echo "==> ETAPES SUIVANTES :"
echo ""
echo "1. Clone le repo sur le VPS :"
echo "   git clone https://github.com/TON_REPO/Okayo_Lite.git && cd Okayo_Lite"
echo ""
echo "2. Copie et configure le .env :"
echo "   cp .env .env.prod"
echo "   nano .env.prod   # Change les mots de passe, JWT_SECRET, CORS_ALLOWED_ORIGINS"
echo ""
echo "3. Configure le Caddyfile avec ton IP VPS :"
echo "   IP=\$(curl -s ifconfig.me)"
echo "   sed -i \"s/VPS_IP/\$IP/g\" ops/caddy/Caddyfile"
echo "   echo \"Backend sera accessible sur : https://\$IP.nip.io\""
echo ""
echo "4. Lance la stack prod :"
echo "   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build"
echo ""
echo "5. Mets à jour le CORS dans .env.prod avec l'URL Netlify,"
echo "   puis redémarre le backend :"
echo "   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend"
