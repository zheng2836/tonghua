#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/zheng2836/tonghua.git"
INSTALL_DIR="/opt/tonghua"
BRANCH="main"
DOMAIN_DEFAULT="joker404.xyz"
TURN_USER_DEFAULT="turnuser"
TURN_PASS_DEFAULT="turnpass"

DOMAIN="${1:-$DOMAIN_DEFAULT}"
TURN_USER="${TURN_USER:-$TURN_USER_DEFAULT}"
TURN_PASS="${TURN_PASS:-$TURN_PASS_DEFAULT}"
FCM_SERVICE_ACCOUNT_FILE="${FCM_SERVICE_ACCOUNT_FILE:-/opt/tonghua/firebase-service-account.json}"

if [[ $EUID -ne 0 ]]; then
  echo "Please run as root"
  exit 1
fi

if command -v dnf >/dev/null 2>&1; then
  PKG=dnf
else
  PKG=yum
fi

$PKG -y install git curl ca-certificates nginx
systemctl enable --now nginx

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

mkdir -p /usr/local/lib/docker/cli-plugins
if ! docker compose version >/dev/null 2>&1; then
  curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

if [[ -d "$INSTALL_DIR/.git" ]]; then
  git -C "$INSTALL_DIR" fetch origin
  git -C "$INSTALL_DIR" checkout "$BRANCH"
  git -C "$INSTALL_DIR" pull --ff-only origin "$BRANCH"
else
  git clone --branch "$BRANCH" "$REPO_URL" "$INSTALL_DIR"
fi

mkdir -p "$INSTALL_DIR/deploy"
if [[ ! -f "$INSTALL_DIR/deploy/server.env" ]]; then
  cp "$INSTALL_DIR/deploy/server.env.example" "$INSTALL_DIR/deploy/server.env"
fi

if ! grep -q '^FCM_SERVICE_ACCOUNT_FILE=' "$INSTALL_DIR/deploy/server.env"; then
  echo "FCM_SERVICE_ACCOUNT_FILE=$FCM_SERVICE_ACCOUNT_FILE" >> "$INSTALL_DIR/deploy/server.env"
else
  sed -i "s|^FCM_SERVICE_ACCOUNT_FILE=.*|FCM_SERVICE_ACCOUNT_FILE=$FCM_SERVICE_ACCOUNT_FILE|" "$INSTALL_DIR/deploy/server.env"
fi

cat > "$INSTALL_DIR/deploy/coturn/turnserver.conf" <<EOF
listening-port=3478
fingerprint
lt-cred-mech
realm=$DOMAIN
user=$TURN_USER:$TURN_PASS
min-port=49160
max-port=49200
no-cli
EOF

cat > /etc/nginx/conf.d/tonghua.conf <<EOF
server {
    listen 80;
    server_name $DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

nginx -t
systemctl reload nginx

cd "$INSTALL_DIR/deploy"
docker compose up -d --build

echo
echo "Installed tonghua to $INSTALL_DIR"
echo "HTTP base URL: http://$DOMAIN"
echo "TURN URL: turn:$DOMAIN:3478?transport=udp"
echo "TURN username: $TURN_USER"
echo "TURN password: $TURN_PASS"
echo "FCM service account expected at: $FCM_SERVICE_ACCOUNT_FILE"
echo
echo "If Firebase service account JSON is not present yet, place it at:"
echo "  $FCM_SERVICE_ACCOUNT_FILE"
