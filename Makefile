include .env
export

.PHONY: dev down migrate migrate-down migrate-status tes lint

dev:
	docker compose up --build

down:
	docker compose down -v

migrate:
	docker compose run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		up
migrate-down:
	docker compose run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		down 1

migrate-status:
	docker compose run --rm migrate \
		-path=/migrations \
		-database "postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable" \
		version

test:
	cd backend && go test ./...

lint:
	cd backend && golangci-lint run