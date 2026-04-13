.PHONY: dev down migrate test lint

dev:
	docker compose up --build

down:
	docker compose down -v

migrate:
	docker compose run --rm migrate

test:
	cd backend && go test ./...

lint:
	cd backend && golangci-lint run