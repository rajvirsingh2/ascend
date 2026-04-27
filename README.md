# Ascend — Real-Life RPG

A gamified personal development application where you are the character. Complete real-world habits and goals to earn XP, level up, and unlock achievements — powered by a personalised AI quest engine.

---

## Table of Contents

- [What is Ascend](#what-is-ascend)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Module Map](#module-map)
- [Prerequisites](#prerequisites)
- [Setup Guide](#setup-guide)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [BYOK — Bring Your Own AI Key](#byok)
- [Development Commands](#development-commands)
- [Troubleshooting](#troubleshooting)

---

## What is Ascend

Ascend turns your daily habits and personal goals into an RPG. Every task you complete grants XP. Reach XP thresholds to level up. The AI quest engine analyses your goals and history to generate personalised daily and weekly challenges — quests tailored to where you are in your journey, not generic productivity advice.

**Core loop:**
1. Set goals (fitness, learning, mindfulness, creativity)
2. Complete AI-generated quests and daily habits
3. Earn XP, level up, unlock titles
4. The AI remembers your history and never repeats itself

---

## Tech Stack

| Layer | Technology |
|---|---|
| Android client | Kotlin · Jetpack Compose · MVI · Room · Retrofit |
| Go API | Go 1.23 · Chi router · JWT · AES-256-GCM |
| AI / RAG | Python · FastAPI · LangChain · OpenAI / Claude / Gemini |
| Database | PostgreSQL 16 · pgvector (semantic search) |
| Cache & streams | Redis 7 · Redis Streams (async event processing) |
| Containerisation | Docker · Docker Compose |
| Cloud (free) | Railway (no credit card required) |

---

## Architecture Overview

```
Android app (Kotlin/Compose)
    │
    │  REST + WebSocket (JWT auth)
    ▼
Go API Gateway (port 8080)
    │  validates, persists, publishes
    ├──► Redis Streams ──► XP Worker (Go) ──► PostgreSQL
    │                  ──► RAG Worker (Python) ──► pgvector
    │
    ├──► PostgreSQL (users, quests, habits, goals, progress_logs)
    │
    └──► Python RAG Service (port 8001)
             │  LangChain + vector similarity search
             └──► LLM API (OpenAI / Claude / Gemini via BYOK)
```

**Key design decisions:**

- **API-first**: the Go backend is completely decoupled from the Android client via REST contracts
- **Async by default**: quest completion returns in <15ms; XP calculation, embedding, and notifications are async via Redis Streams
- **Offline-first Android**: Room cache serves UI instantly; network sync happens in background
- **RAG memory**: every completed quest and goal is embedded into pgvector; the AI retrieves semantically relevant history before generating new quests — it never repeats itself
- **BYOK**: users supply their own LLM API key; the backend encrypts it with AES-256-GCM envelope encryption and decrypts it only during quest generation, never logging it

---

## Module Map

The project was built in 11 sequential modules:

| Module | What was built |
|---|---|
| M1 | Monorepo structure, Docker Compose, GitHub Actions CI |
| M2 | Go API core — auth, JWT, bcrypt, CORS, game engine (XP/levelling) |
| M3 | Android shell — MVI architecture, Navigation Compose, fake data |
| M4 | Database layer — PostgreSQL schema, pgvector, all migrations |
| M5 | Quest, habit, goal API endpoints, XP game loop |
| M6 | Android live integration — repositories, Room cache, full screens |
| M7 | RAG ingestion pipeline — Python embeddings, Redis queue worker |
| M8 | AI quest generation — LangChain chain, MMR retrieval, Go bridge |
| M9 | Observability, production Dockerfiles, deployment |
| M10 | Redis Streams (replacing Kafka), multi-provider AI adapter, BYOK |
| M11 | Anime-RPG design system, LevelUpModal, SettingsScreen, WebSocket |

---

## Prerequisites

Install these tools before starting:

```bash
# required
docker --version      # Docker Desktop 4.x+
go version            # Go 1.23+
python3 --version     # Python 3.12+
git --version         # any version

# for Android development
# Android Studio Ladybug or newer (developer.android.com/studio)
```

**Free accounts needed (no credit card):**

| Service | Purpose | URL |
|---|---|---|
| Railway | Cloud deployment | railway.app |
| ngrok | Local tunnel for device testing | ngrok.com |
| Brevo | SMTP email for OTP (300/day free) | brevo.com |

---

## Setup Guide

### 1. Clone the repository

```bash
git clone https://github.com/you/ascend.git
cd ascend
```

### 2. Configure environment

```bash
cp .env.example .env
```

Open `.env` and fill in:

```bash
# generate secrets
openssl rand -hex 32   # use for JWT_SECRET
openssl rand -hex 32   # use for MASTER_ENCRYPTION_KEY
openssl rand -hex 32   # use for HMAC_SECRET
```

Minimum required values for local development:

```env
DB_NAME=ascend_db
DB_USER=ascend_user
DB_PASSWORD=ascend_pass
DATABASE_URL=postgres://ascend_user:ascend_pass@localhost:5432/ascend_db?sslmode=disable
REDIS_URL=redis://localhost:6379
JWT_SECRET=<generated>
MASTER_ENCRYPTION_KEY=<generated>
HMAC_SECRET=<generated>
APP_ENV=development
APP_PORT=8080
ALLOWED_ORIGINS=http://localhost:3000
RAG_SERVICE_URL=http://localhost:8001
```

SMTP values (for email OTP) — leave blank to skip email in development:

```env
SMTP_HOST=smtp.brevo.com
SMTP_PORT=587
SMTP_USER=your@email.com
SMTP_PASSWORD=your_brevo_smtp_password
EMAIL_FROM=noreply@ascend.app
```

### 3. Start backend services

```bash
# start database and cache
docker compose up postgres redis -d

# run all migrations (creates all tables)
make migrate

# start all services
docker compose up --build
```

Expected output — all four containers running:

```
ascend_postgres   healthy
ascend_redis      healthy
ascend_backend    running on :8080
ascend_rag        running on :8001
ascend_xp_worker  consumer started
```

### 4. Verify services

```bash
curl http://localhost:8080/health   # {"data":{"status":"ok"}}
curl http://localhost:8001/health   # {"status":"ok","service":"rag"}
```

### 5. Load seed data

```bash
docker exec -i ascend_postgres \
  psql -U ascend_user -d ascend_db < scripts/seed.sql
```

This creates a test user: `test@ascend.app / password123` with sample goals, habits, and quests.

> **Note:** The seed user has `email_verified=true` set manually. For new registrations, the email OTP flow applies.

### 6. Run the Android app

1. Open Android Studio
2. `File → Open → select the android/ folder`
3. Wait for Gradle sync
4. Start an Android emulator (API 26 or higher)
5. Press Run

The app connects to `http://10.0.2.2:8080` — the emulator's alias for your machine's localhost.

### 7. Test on a real device (optional)

```bash
./scripts/dev-tunnel.sh
```

Copy the printed ngrok URL, paste it into `android/app/build.gradle.kts` in the `ngrok` build type, select `ngrokDebug` variant, and run on your physical device.

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | PostgreSQL connection string |
| `REDIS_URL` | Yes | Redis connection string |
| `JWT_SECRET` | Yes | 32-byte hex secret for JWT signing |
| `JWT_EXPIRY_MINUTES` | No | Access token TTL (default: 15) |
| `REFRESH_TOKEN_EXPIRY_DAYS` | No | Refresh token TTL (default: 7) |
| `MASTER_ENCRYPTION_KEY` | Yes | 32-byte hex key for BYOK encryption |
| `HMAC_SECRET` | Yes | 32-byte hex key for request signing |
| `APP_ENV` | No | development / production (default: development) |
| `APP_PORT` | No | API port (default: 8080) |
| `ALLOWED_ORIGINS` | Yes | Comma-separated CORS origins |
| `RAG_SERVICE_URL` | Yes | Internal URL of the Python RAG service |
| `SMTP_HOST` | No | SMTP server (required for email OTP) |
| `SMTP_PORT` | No | SMTP port (default: 587) |
| `SMTP_USER` | No | SMTP username |
| `SMTP_PASSWORD` | No | SMTP password |
| `EMAIL_FROM` | No | Sender address for OTP emails |

---

## API Reference

All endpoints are prefixed with `/api/v1`.

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | None | Register — sends email OTP |
| POST | `/auth/verify-email` | None | Verify OTP code |
| POST | `/auth/resend-otp` | None | Resend OTP code |
| POST | `/auth/login` | None | Login — returns access token |
| POST | `/auth/refresh` | Cookie | Rotate refresh token |
| POST | `/auth/logout` | Cookie | Invalidate session |

### User

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/me` | JWT | Get current user profile |

### Goals

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/goals` | JWT | List active goals |
| POST | `/goals` | JWT | Create a goal |
| PATCH | `/goals/:id` | JWT | Update a goal |
| DELETE | `/goals/:id` | JWT | Soft-delete a goal |

### Habits

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/habits` | JWT | List active habits |
| POST | `/habits` | JWT | Create a habit |
| POST | `/habits/:id/complete` | JWT | Check in (idempotent) |

### Quests

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/quests` | JWT | List active quests |
| POST | `/quests/:id/complete` | JWT | Complete a quest |
| POST | `/quests/:id/skip` | JWT | Skip a quest |
| POST | `/quests/generate` | JWT | AI-generate new quests (rate-limited: 3/day) |

### Settings

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/settings/api-key` | JWT | Store AI API key (encrypted) |
| GET | `/settings/api-key/status` | JWT | Check if key is stored |
| DELETE | `/settings/api-key` | JWT | Remove stored key |

### Health

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/health` | None | Liveness check |
| GET | `/ready` | None | Readiness check (DB + Redis) |

### WebSocket

| Endpoint | Auth | Description |
|---|---|---|
| `ws://host/api/v1/ws` | JWT header | Real-time event stream |

**WebSocket frame types:**

```json
{"type": "LEVEL_UP",    "payload": {"new_level": 13, "xp_awarded": 125}}
{"type": "XP_AWARDED",  "payload": {"amount": 40}}
{"type": "GUILD_QUEST", "payload": {"member_name": "...", "quest_title": "..."}}
```

---

## Security Model

### Authentication flow

```
Register → email OTP sent → verify OTP → account active → login → JWT + refresh cookie
```

- Access tokens: 15 minutes, HS256
- Refresh tokens: 7 days, stored as SHA-256 hash in Redis (never plaintext)
- Refresh rotation: old token is invalidated the moment a new one is issued
- Cookies: `HttpOnly`, `Secure`, `SameSite=Strict`

### Account protection

- **Login lockout**: 5 failed attempts within 15 minutes → account locked for 30 minutes
- **OTP rate limit**: max 3 OTP requests per 15-minute window per email
- **OTP single-use**: each code is deleted from Redis the moment it is verified
- **CORS**: exact allow-list only — no wildcard, `Vary: Origin` always set
- **HMAC request signing**: every mutating request includes `X-Timestamp` and `X-Signature`; backend rejects requests older than 5 minutes

### Password requirements

- Minimum 8 characters
- At least one uppercase letter
- At least one number
- Maximum 128 characters
- Stored as bcrypt hash at cost factor 12

### BYOK encryption

User-supplied AI API keys are never stored in plain text:

```
User submits key → TLS transit → Go handler
    → generates random 32-byte DEK
    → AES-256-GCM encrypts key with DEK
    → AES-256-GCM encrypts DEK with MEK (from env, never in DB)
    → stores {wrapped_dek, ciphertext} in postgres
    → plaintext key goes out of scope → GC eligible
```

At generation time:

```
Decrypt wrapped_dek with MEK → decrypt ciphertext with DEK
    → plaintext key in local variable
    → passed to RAG service
    → ZeroBytes() called on return → memory wiped
```

The plaintext key never appears in logs, never touches disk, and lives for one request stack frame.

---

## BYOK

To use AI quest generation, you need an API key from one of these providers:

| Provider | Get a key | Recommended free tier |
|---|---|---|
| OpenAI | platform.openai.com | gpt-4o-mini ($0.15/1M tokens) |
| Anthropic (Claude) | console.anthropic.com | claude-haiku-3 ($0.25/1M tokens) |
| Google (Gemini) | aistudio.google.com | gemini-1.5-flash (free tier available) |

In the Android app:

1. Go to **Settings** (bottom navigation)
2. Select your provider
3. Paste your API key
4. Tap **Save Key Securely**

The key is immediately encrypted on the server. You can remove it at any time from the same screen.

---

## Development Commands

```bash
# start all services
docker compose up --build

# start only database and cache (for running Go locally)
docker compose up postgres redis -d

# run migrations
make migrate

# roll back one migration
make migrate-down

# check migration version
make migrate-status

# run Go backend tests
cd backend && go test ./...

# run Go linter
cd backend && golangci-lint run

# run RAG service tests
cd rag-service && pytest -v

# view service logs
docker compose logs backend -f
docker compose logs rag-service -f
docker compose logs xp-worker -f

# connect to postgres
docker exec -it ascend_postgres psql -U ascend_user -d ascend_db

# connect to redis
docker exec -it ascend_redis redis-cli

# stop all containers
docker compose down

# stop and wipe all data (fresh start)
docker compose down -v

# start ngrok tunnel
./scripts/dev-tunnel.sh

# deploy to Railway
railway up
```

---

## Troubleshooting

**`go.sum` not found on first build**

```bash
cd backend && touch go.sum && go mod tidy
```

**Login returns "invalid credentials" on seeded user**

The seed script has a placeholder hash. Fix:

```bash
cd backend
go run /tmp/hashgen.go  # prints bcrypt hash for "password123"
# paste hash into:
docker exec -it ascend_postgres psql -U ascend_user -d ascend_db -c \
  "UPDATE users SET password_hash='<hash>' WHERE email='test@ascend.app';"
```

**Android cannot reach backend on emulator**

- Confirm backend is running: `curl http://localhost:8080/health`
- Confirm `BASE_URL` is `http://10.0.2.2:8080/api/v1/` (not `localhost`)
- Confirm `network_security_config.xml` allows cleartext to `10.0.2.2`

**Android cannot reach backend on physical device**

Use the ngrok tunnel: `./scripts/dev-tunnel.sh`

**pgvector extension not found**

The Docker image `pgvector/pgvector:pg16` includes pgvector pre-installed. If you see this error, the wrong Postgres image was used. Run `docker compose down -v && docker compose up postgres -d`.

**Quest generation returns "no AI API key configured"**

Go to Settings in the Android app and save your API key from OpenAI, Anthropic, or Google.

**RAG service shows "mock embedder" in logs**

This is correct when `OPENAI_API_KEY` is not set. The mock embedder returns fake vectors — quest generation still works but uses random embeddings instead of semantic ones. Set a real key in `.env` for real RAG behaviour.

**OTP email not arriving**

1. Check SMTP credentials in `.env`
2. Check spam folder
3. For development, read the OTP directly from Redis:
   ```bash
   docker exec -it ascend_redis redis-cli KEYS "otp:*"
   docker exec -it ascend_redis redis-cli GET "otp:your@email.com"
   ```

---

## Project Structure

```
ascend/
├── backend/                  Go API server
│   ├── cmd/server/           main.go — entrypoint
│   ├── cmd/worker/           worker entrypoint (XP consumer)
│   ├── internal/
│   │   ├── auth/             JWT, bcrypt, session, OTP handlers
│   │   ├── email/            SMTP sender
│   │   ├── events/           Redis Streams publisher + consumer
│   │   ├── game/             XP engine, levelling formula
│   │   ├── goal/             goal HTTP handlers
│   │   ├── habit/            habit HTTP handlers
│   │   ├── keyvault/         AES-256-GCM BYOK encryption
│   │   ├── middleware/        CORS, JWT guard, rate limit, HMAC, logger
│   │   ├── otp/              OTP generate + verify
│   │   ├── quest/            quest handlers + expiry worker
│   │   ├── server/           router wiring
│   │   ├── settings/         BYOK settings handlers
│   │   ├── store/            repository interfaces + Postgres/Redis impls
│   │   ├── validators/       input validation
│   │   └── workers/          XP background worker
│   └── pkg/
│       ├── config/           env config loader
│       ├── logger/           structured slog setup
│       └── response/         JSON envelope helpers
│
├── rag-service/              Python AI service
│   ├── app/
│   │   ├── providers/        OpenAI / Claude / Gemini adapters
│   │   ├── prompts/          versioned prompt templates
│   │   ├── context_builder   user context assembler
│   │   ├── document_builder  quest → embeddable text
│   │   ├── embedder          embedding model + pgvector store
│   │   ├── generate          full RAG pipeline
│   │   ├── retriever         cosine search + MMR reranking
│   │   └── worker            Redis queue consumer
│   └── tests/
│
├── android/                  Kotlin/Compose app
│   └── app/src/main/java/com/ascend/app/
│       ├── data/
│       │   ├── local/        Room database, DAOs, entities, DataStore
│       │   ├── realtime/     WebSocketManager
│       │   ├── remote/       Retrofit services, DTOs, interceptors
│       │   └── repository/   offline-first repositories
│       ├── di/               Hilt modules
│       ├── domain/model/     pure Kotlin domain models
│       └── ui/
│           ├── auth/         login, register, OTP screens
│           ├── components/   shared components (StatBar, QuestCard, etc.)
│           ├── dashboard/    main game screen
│           ├── goals/        goal management
│           ├── levelup/      LevelUpModal with particle system
│           ├── navigation/   NavGraph, routes, bottom nav
│           ├── profile/      user profile + logout
│           ├── settings/     BYOK key management
│           ├── splash/       auto-login routing
│           └── theme/        colors, typography, shapes, gradients
│
├── migrations/               numbered SQL migration files (golang-migrate)
├── scripts/                  seed.sql, dev-tunnel.sh
├── docker-compose.yml        full local stack
├── Makefile                  dev task runner
└── .env.example              environment template
```

---

*Ascend — Level up your real life.*
