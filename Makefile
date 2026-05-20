include .env
export

.PHONY: dev prod down migrate migrate-down migrate-status test lint seed shell-db shell-redis logs clean generate-secrets

## ── Local development (hot-reload, mounts source) ────────────────────────
dev:
	docker compose --profile dev up --build

## ── Production simulation (compiled binary, no volume mounts) ────────────
prod:
	docker compose --profile prod up --build

## ── Tear down (removes volumes too) ─────────────────────────────────────
down:
	docker compose down -v

## ── Migrations ───────────────────────────────────────────────────────────
migrate:
	docker compose --profile tools run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		up

migrate-down:
	docker compose --profile tools run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		down 1

migrate-status:
	docker compose --profile tools run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		version

## ── Testing & linting ────────────────────────────────────────────────────
test:
	cd backend && go test ./...

lint:
	cd backend && golangci-lint run

## ── Seed database with test data ─────────────────────────────────────
seed:
	@echo "→ Seeding database..."
	docker exec -i ascend_postgres psql -U ${DB_USER} -d ${DB_NAME} < scripts/seed.sql
	@echo "✓ Seed complete. Login: test@ascend.app / password123"

## ── Interactive shells ───────────────────────────────────────────────
shell-db:
	docker exec -it ascend_postgres psql -U ${DB_USER} -d ${DB_NAME}

shell-redis:
	docker exec -it ascend_redis redis-cli

## ── Logs ─────────────────────────────────────────────────────────────
logs:
	docker compose logs -f

## ── Nuclear reset (stops everything + wipes all data) ────────────────
clean:
	docker compose down -v
	@echo "✓ All containers stopped and volumes removed."

## ── Generate secrets ─────────────────────────────────────────────────
generate-secrets:
	@python -c "import secrets; print('JWT_SECRET=' + secrets.token_hex(32)); print('MASTER_ENCRYPTION_KEY=' + secrets.token_hex(32)); print('HMAC_SECRET=' + secrets.token_hex(32))"