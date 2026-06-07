#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────
# Ascend — Deploy / Redeploy
# Run from the project root on the EC2 instance:
#   bash scripts/deploy.sh
# ─────────────────────────────────────────────────────────
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"

echo "══════════════════════════════════════════════"
echo "  Ascend — Deploying"
echo "══════════════════════════════════════════════"

# ── Pre-flight checks ─────────────────────────────────
if [ ! -f "${ENV_FILE}" ]; then
    echo "✗ Missing ${ENV_FILE} — copy from .env.prod.example and fill in secrets"
    exit 1
fi

if ! docker compose version &>/dev/null; then
    echo "✗ Docker Compose not found. Run ec2-setup.sh first."
    exit 1
fi

# ── 1. Pull latest code ───────────────────────────────
echo "→ Pulling latest code..."
git pull origin master
echo "✓ Code updated"

# ── 2. Build images ───────────────────────────────────
echo "→ Building Docker images..."
docker compose -f "${COMPOSE_FILE}" build
echo "✓ Images built"

# ── 3. Stop worker first (graceful) ───────────────────
echo "→ Stopping XP worker..."
docker compose -f "${COMPOSE_FILE}" stop xp-worker 2>/dev/null || true

# ── 4. Start infra + run migrations ──────────────────
echo "→ Starting Postgres + Redis..."
docker compose -f "${COMPOSE_FILE}" up -d postgres redis
echo "→ Waiting for healthy services..."
sleep 5

echo "→ Running migrations..."
docker compose -f "${COMPOSE_FILE}" up migrate
echo "✓ Migrations complete"

# ── 5. Start everything ──────────────────────────────
echo "→ Starting all services..."
docker compose -f "${COMPOSE_FILE}" up -d
echo "✓ All services started"

# ── 6. Cleanup old images ────────────────────────────
echo "→ Cleaning up dangling images..."
docker image prune -f
echo "✓ Cleanup done"

# ── 7. Health check ──────────────────────────────────
echo "→ Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/health > /dev/null 2>&1; then
        echo "✓ Backend is healthy!"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "✗ Health check failed after 30s"
        echo "  Check logs: docker compose -f ${COMPOSE_FILE} logs backend"
        exit 1
    fi
    sleep 1
done

# ── 8. Readiness check (DB + Redis) ──────────────────
if curl -sf http://localhost:8080/ready > /dev/null 2>&1; then
    echo "✓ Readiness check passed (DB + Redis connected)"
else
    echo "⚠ Readiness check failed — DB or Redis may still be starting"
fi

echo ""
echo "══════════════════════════════════════════════"
echo "  ✓ Deploy complete!"
echo "══════════════════════════════════════════════"
echo ""
echo "  Status:  docker compose -f ${COMPOSE_FILE} ps"
echo "  Logs:    docker compose -f ${COMPOSE_FILE} logs -f"
echo "  Stop:    docker compose -f ${COMPOSE_FILE} down"
echo ""
