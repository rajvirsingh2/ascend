include .env
export

.PHONY: dev prod down migrate migrate-down migrate-status test lint

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