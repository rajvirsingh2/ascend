#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────
# Ascend — First-Time EC2 Setup
# Run once on a fresh Ubuntu 24.04 EC2 instance:
#   chmod +x scripts/ec2-setup.sh && sudo ./scripts/ec2-setup.sh
# ─────────────────────────────────────────────────────────
set -euo pipefail

ASCEND_USER="${ASCEND_USER:-ubuntu}"
REPO_URL="${REPO_URL:-https://github.com/YOUR_USERNAME/ascend.git}"
INSTALL_DIR="/home/${ASCEND_USER}/ascend"

echo "══════════════════════════════════════════════"
echo "  Ascend — EC2 First-Time Setup"
echo "══════════════════════════════════════════════"

# ── 1. System updates ──────────────────────────────────
echo "→ Updating system packages..."
apt-get update -y && apt-get upgrade -y

# ── 2. Install Docker ──────────────────────────────────
echo "→ Installing Docker..."
if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sh
    usermod -aG docker "${ASCEND_USER}"
    systemctl enable docker
    systemctl start docker
    echo "✓ Docker installed"
else
    echo "✓ Docker already installed"
fi

# ── 3. Install Docker Compose plugin ──────────────────
echo "→ Ensuring Docker Compose plugin..."
if ! docker compose version &>/dev/null; then
    apt-get install -y docker-compose-plugin
    echo "✓ Docker Compose plugin installed"
else
    echo "✓ Docker Compose plugin already available"
fi

# ── 4. Install Nginx ──────────────────────────────────
echo "→ Installing Nginx..."
if ! command -v nginx &>/dev/null; then
    apt-get install -y nginx
    systemctl enable nginx
    echo "✓ Nginx installed"
else
    echo "✓ Nginx already installed"
fi

# ── 5. Install misc tools ─────────────────────────────
apt-get install -y git make ufw fail2ban

# ── 6. Firewall setup (UFW) ───────────────────────────
echo "→ Configuring firewall..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw --force enable
echo "✓ Firewall enabled (22, 80, 443)"

# ── 7. Enable swap (important for t3.micro) ───────────
echo "→ Setting up 1 GB swap..."
if [ ! -f /swapfile ]; then
    fallocate -l 1G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    # Tune swappiness for low-RAM server
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    echo 'vm.vfs_cache_pressure=50' >> /etc/sysctl.conf
    sysctl -p
    echo "✓ 1 GB swap enabled (swappiness=10)"
else
    echo "✓ Swap already exists"
fi

# ── 8. Clone repo ─────────────────────────────────────
echo "→ Cloning repository..."
if [ ! -d "${INSTALL_DIR}" ]; then
    sudo -u "${ASCEND_USER}" git clone "${REPO_URL}" "${INSTALL_DIR}"
    echo "✓ Repository cloned to ${INSTALL_DIR}"
else
    echo "✓ Repository already exists at ${INSTALL_DIR}"
fi

# ── 9. Copy Nginx config ──────────────────────────────
echo "→ Setting up Nginx config..."
cp "${INSTALL_DIR}/nginx/nginx.conf" /etc/nginx/sites-available/ascend
cp "${INSTALL_DIR}/nginx/ascend-proxy.inc" /etc/nginx/conf.d/ascend-proxy.inc
ln -sf /etc/nginx/sites-available/ascend /etc/nginx/sites-enabled/ascend
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx
echo "✓ Nginx configured"

# ── 10. Create systemd service for auto-start ─────────
echo "→ Creating systemd service..."
cat > /etc/systemd/system/ascend.service <<EOF
[Unit]
Description=Ascend Backend (Docker Compose)
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
User=${ASCEND_USER}
WorkingDirectory=${INSTALL_DIR}
ExecStart=/usr/bin/docker compose -f docker-compose.prod.yml up -d
ExecStop=/usr/bin/docker compose -f docker-compose.prod.yml down
TimeoutStartSec=120

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ascend.service
echo "✓ Systemd service created (auto-starts on reboot)"

# ── 11. Fail2Ban for SSH protection ───────────────────
echo "→ Configuring Fail2Ban..."
systemctl enable fail2ban
systemctl start fail2ban
echo "✓ Fail2Ban enabled"

echo ""
echo "══════════════════════════════════════════════"
echo "  ✓ Setup complete!"
echo "══════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. cd ${INSTALL_DIR}"
echo "  2. cp .env.prod.example .env.prod"
echo "  3. nano .env.prod              # fill in your secrets"
echo "  4. bash scripts/deploy.sh      # build & start everything"
echo ""
echo "Optional — Set up SSL with Let's Encrypt:"
echo "  sudo apt install certbot python3-certbot-nginx"
echo "  sudo certbot --nginx -d YOUR_DOMAIN"
echo ""
