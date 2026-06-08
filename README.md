# Ascend — Real-Life RPG

A gamified personal development application inspired by the hit anime "Solo Leveling". Ascend turns your real life into an RPG where you are the main character. Complete real-world habits and goals to earn XP, level up your Hunter Rank, and unlock achievements — all powered by a personalised AI quest engine.

---

## Screenshots

<p align="center">
  <img src="docs/images/splash.png" width="22%" />
  <img src="docs/images/login.png" width="22%" />
  <img src="docs/images/register.png" width="22%" />
  <img src="docs/images/verification.png" width="22%" />
</p>

<p align="center">
  <img src="docs/images/focus.png" width="22%" />
  <img src="docs/images/dashboard.png" width="22%" />
  <img src="docs/images/goals.png" width="22%" />
  <img src="docs/images/notifications.png" width="22%" />
</p>

<p align="center">
  <img src="docs/images/profile.png" width="22%" />
  <img src="docs/images/stats.png" width="22%" />
  <img src="docs/images/share.png" width="22%" />
  <img src="docs/images/recover.png" width="22%" />
</p>

---

## Table of Contents

- [What is Ascend](#what-is-ascend)
- [Tech Stack](#tech-stack)
- [Custom Quest Generation Model](#custom-quest-generation-model)
- [Architecture Overview](#architecture-overview)
- [Module Map](#module-map)
- [Installation & Setup](#installation--setup)
- [API Reference](#api-reference)
- [Security Model](#security-model)

---

## What is Ascend

**Ascend turns your daily habits and personal goals into a fully-fledged RPG.** 

Instead of another boring to-do list, Ascend treats *you* as the main character. Every task you complete grants Experience Points (XP). Reach XP thresholds to level up your Hunter Rank and unlock achievements.

What truly sets Ascend apart is its **Personalised AI Quest Engine**. The system is powered by a **custom-trained Large Language Model (LLM)** hosted via a Python microservice on Hugging Face. It actively analyses your long-term goals, body metrics, and quest history to generate highly contextual daily and weekly challenges tailored specifically to where you are in your journey. 

### Highlight Features

- **GitHub-Style Heatmaps & Deep Analytics:** Visualize your consistency with beautiful activity heatmaps and track your distribution of effort across Health, Mind, and Wealth domains.
- **Offline-First Architecture:** Powered by a robust Room database locally, the app feels instantaneously snappy and seamlessly syncs to the backend in the background.
- **Dynamic Achievements & Sharing:** Earn stylish diamond badges for your milestones and share your Hunter Card with friends to show off your rank and streaks.

**Core loop:**
1. Set goals (fitness, learning, mindfulness, creativity)
2. Complete AI-generated quests and daily habits
3. Earn XP, level up, unlock titles
4. The AI remembers your history and dynamically evolves your next quests

### 🩸 HP & Death Mechanics
Ascend is designed to be punishing if you fall off track:
- **Free Skips**: You get 5 free quest skips per calendar month.
- **HP Damage**: Skipping quests beyond your free limit directly damages your Health Points (HP). The damage scales up with every additional skip.
- **Healing via Consistency**: Completing daily habits restores your HP. The longer your current streak, the more HP you heal!
- **Death Penalty**: If your HP drops to zero, you suffer a lethal penalty: your Level decreases by 1, and your core stats (Strength, Agility, Mana) are permanently reduced. You are then revived back to Max HP to start rebuilding.

---

## Custom Quest Generation Model

A core technical pillar of Ascend is its independent AI infrastructure. Rather than relying heavily on generic third-party AI APIs (which are expensive and rigid), Ascend features a **custom fine-tuned LLM** dedicated entirely to generating personalized RPG quests.

### The Machine Learning Journey
- **Synthetic Dataset Generation:** The process began with no real user data. A synthetic dataset of 100,000 instruction-tuning pairs was built using a custom script and Gemini, ensuring proper ChatML formatting, train/validation splits, and balanced domains (fitness, mindfulness, etc.).
- **Fine-tuning on Colab:** Using a free Colab GPU, Microsoft's **Phi-3 Mini** was fine-tuned using LoRA/QLoRA techniques via `SFTTrainer`. Key techniques included gradient accumulation and early stopping to prevent overfitting.
- **Serverless Deployment:** The resulting model was merged, quantized to GGUF format, and deployed on a free Hugging Face Space running `llama-cpp-python` and Gradio for API access.
- **Backend Integration:** The Go backend communicates with the Hugging Face Space via the Gradio protocol. It caches responses and includes a graceful fallback to a cloud LLM if the free tier space is cold-starting.
- **Continuous Improvement:** The architecture is designed for **Direct Preference Optimization (DPO)**. As users interact with quests (completing vs. skipping), preference pairs are collected to further align the model's reward signals in future iterations.

**Final State:**
- **Model:** Phi-3 Mini + LoRA (Quantized GGUF)
- **Hosted:** Hugging Face Spaces (CPU Free Tier)
- **Zero API Keys required** for the core quest generation loop.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Android client | Kotlin · Jetpack Compose · MVI · Room · Retrofit |
| Go API | Go 1.23 · Chi router · JWT |
| Database | PostgreSQL 16 (pgvector) |
| Cache & streams | Redis 7 · Redis Streams (async event processing) |
| Containerisation | Docker · Docker Compose |
| Cloud | AWS EC2 (free tier) · Nginx · GitHub Actions CD |

---

## Architecture Overview

```
Android app (Kotlin/Compose)
    │
    │  REST + WebSocket (HTTPS/WSS)
    ▼
Nginx Reverse Proxy (port 80/443)
    │
    ▼
Go API Gateway (port 8080)
    │  validates, persists, publishes
    ├──► Redis Streams ──► XP Worker (Go) ──► PostgreSQL
    │
    ├──► PostgreSQL (users, quests, habits, goals, progress_logs)
    │
    └──► Python LLM Quest Generation Service (port 8001)
```

**Key design decisions:**

- **API-first**: the Go backend is completely decoupled from the Android client via REST contracts
- **Async by default**: quest completion returns in <15ms; XP calculation and notifications are async via Redis Streams
- **Offline-first Android**: Room cache serves UI instantly; network sync happens in background

---


## Installation & Setup

Ascend is live! The backend is fully hosted on AWS, meaning you don't need to configure any servers or databases to play.

**To start playing:**
1. Download the latest `app-release.apk` from the [Releases](../../releases) page.
2. Install the APK on your Android device.
3. Create your account, set your goals, and start levelling up!

*(Note: If you are a developer and wish to self-host the backend infrastructure or contribute to the project, please refer to the **[Self-Hosting & Development Guide](SETUP_GUIDE.md)**.)*

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

---

## Project Structure

```
ascend/
├── .github/workflows/        CI/CD pipelines (backend, android, lint, release)
├── backend/                   Go API server
│   ├── cmd/
│   │   ├── server/            main.go — API entrypoint
│   │   └── worker/            worker entrypoint (XP consumer)
│   ├── internal/
│   │   ├── achievements/      achievement definitions + unlock logic
│   │   ├── auth/              JWT, bcrypt, session, OTP handlers
│   │   ├── avatar/            avatar generation + storage
│   │   ├── email/             SMTP sender
│   │   ├── events/            Redis Streams publisher + consumer
│   │   ├── game/              XP engine, levelling formula
│   │   ├── goal/              goal HTTP handlers
│   │   ├── habit/             habit HTTP handlers
│   │   ├── ingestion/         data ingestion pipeline
│   │   ├── interests/         user interest categories + onboarding
│   │   ├── middleware/        CORS, JWT guard, rate limit, HMAC, logger
│   │   ├── mlservice/         ML service client (quest generation)
│   │   ├── models/            shared domain models
│   │   ├── notifications/     push notification dispatch (FCM)
│   │   ├── otp/               OTP generate + verify
│   │   ├── physique/          body metrics tracking
│   │   ├── quest/             quest handlers + expiry worker
│   │   ├── server/            router wiring
│   │   ├── store/             repository interfaces + Postgres/Redis impls
│   │   │   ├── postgres/      SQL stores + migrations
│   │   │   └── redis/         cache + session stores
│   │   ├── user/              user profile handlers
│   │   └── workers/           XP background worker
│   └── pkg/
│       ├── config/            env config loader
│       ├── logger/            structured slog setup
│       ├── response/          JSON envelope helpers
│       └── validator/         input validation helpers
│
├── android/                   Kotlin/Compose app
│   └── app/src/main/java/com/ascend/app/
│       ├── data/
│       │   ├── local/         Room database, DAOs, entities, DataStore
│       │   ├── realtime/      WebSocketManager
│       │   ├── remote/        Retrofit services, DTOs, interceptors
│       │   └── repository/    offline-first repositories
│       ├── di/                Hilt modules
│       ├── domain/model/      pure Kotlin domain models
│       ├── notification/      FCM + local notification handling
│       ├── ui/
│       │   ├── attributes/    character attribute screens
│       │   ├── auth/          login, register, OTP screens
│       │   ├── components/    shared components (StatBar, QuestCard, etc.)
│       │   ├── dashboard/     main game screen
│       │   ├── goals/         goal management
│       │   ├── history/       quest + activity history
│       │   ├── interests/     interest selection onboarding
│       │   ├── levelup/       LevelUpModal with particle system
│       │   ├── navigation/    NavGraph, routes, bottom nav
│       │   ├── physique/      body metrics UI
│       │   ├── profile/       user profile + logout
│       │   ├── settings/      app settings
│       │   ├── splash/        auto-login routing
│       │   ├── stats/         analytics + heatmaps
│       │   └── theme/         colors, typography, shapes, gradients
│       ├── util/              extension functions + helpers
│       └── workers/           background sync workers
│
├── docs/images/               screenshots for README
├── nginx/                     reverse proxy configs (nginx.conf, ascend-proxy.inc)
├── scripts/                   deploy.sh, dev-tunnel.sh, seed.sql
├── docker-compose.yml         local stack
├── docker-compose.prod.yml    production stack
├── Makefile                   dev task runner
└── .env.example               environment template
```

---

*Ascend — Level up your real life.*
