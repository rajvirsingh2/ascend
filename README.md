# ⚔️ ASCEND — Your Life, Gamified

> **An Offline-First Android RPG powered by a Custom Fine-Tuned LLM and a Go Microservices Backend.**

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin)
![Go](https://img.shields.io/badge/Go-1.25-00ADD8?logo=go)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)

**Ascend** is a production-ready, gamified personal development application that turns real-world habits and goals into a high-stakes RPG. 

Built to showcase end-to-end full-stack engineering, Ascend features a resilient **Offline-First Android App** (Kotlin, Jetpack Compose, Room), a highly concurrent **Go API Gateway**, and a **Custom Fine-Tuned AI Engine** (Phi-3 Mini + LoRA) that dynamically generates personalized RPG quests based on a user's goals and biometric data.

### 🎮 Live Demo
Ascend is fully live and hosted on AWS! **[Download the latest APK Release here](../../releases)** to install it on your Android device and start playing.

---

## Screenshots

<table align="center">
  <tr>
    <td align="center"><img src="docs/images/splash.png" width="100%" /><br><b>Splash Screen</b></td>
    <td align="center"><img src="docs/images/login.png" width="100%" /><br><b>Login</b></td>
    <td align="center"><img src="docs/images/register.png" width="100%" /><br><b>Register</b></td>
    <td align="center"><img src="docs/images/verification.png" width="100%" /><br><b>OTP Verification</b></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/focus.png" width="100%" /><br><b>Focus Mode</b></td>
    <td align="center"><img src="docs/images/dashboard.png" width="100%" /><br><b>Dashboard & Quests</b></td>
    <td align="center"><img src="docs/images/goals.png" width="100%" /><br><b>Goal Setting</b></td>
    <td align="center"><img src="docs/images/notifications.png" width="100%" /><br><b>Push Notifications</b></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/profile.png" width="100%" /><br><b>User Profile</b></td>
    <td align="center"><img src="docs/images/stats.png" width="100%" /><br><b>GitHub-Style Heatmaps</b></td>
    <td align="center"><img src="docs/images/share.png" width="100%" /><br><b>Hunter Card Sharing</b></td>
    <td align="center"><img src="docs/images/recover.png" width="100%" /><br><b>HP Recovery</b></td>
  </tr>
</table>

---

## Table of Contents

- [What is Ascend](#what-is-ascend)
- [Android Engineering](#android-engineering-deep-dive)
- [Custom Quest Generation Model](#custom-quest-generation-model)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Testing & Quality Assurance](#testing--quality-assurance)
- [Application Metrics](#application-metrics)
- [API Reference](#api-reference)
- [Security Model](#security-model)

---

## The System (What is Ascend?)

**Ascend turns your daily habits and personal goals into a fully-fledged RPG.** 

Most productivity apps fail because they are fundamentally boring. Ascend treats *you* as the protagonist. Every task you complete grants Experience Points (XP). Reach XP thresholds to level up your Hunter Rank, unlock exclusive titles, and build your digital legacy.

What truly sets Ascend apart is its **Personalised AI Quest Engine**. We fine-tuned a custom Large Language Model (LLM) strictly for generating RPG quests. It actively analyzes your long-term goals, body metrics, and quest history to synthesize highly contextual daily and weekly challenges tailored specifically to where you are in your journey.

### 🩸 HP & Death Mechanics
Ascend is designed to be punishing if you fall off track:
- **Free Skips**: You get 5 free quest skips per calendar month.
- **HP Damage**: Skipping quests beyond your free limit directly damages your Health Points (HP). The damage scales up with every additional skip.
- **Healing via Consistency**: Completing daily habits restores your HP. The longer your current streak, the more HP you heal!
- **Death Penalty**: If your HP drops to zero, you suffer a lethal penalty: your Level decreases by 1, and your core stats (Strength, Agility, Mana) are permanently reduced. You are then revived back to Max HP to start rebuilding.

---

## Android Engineering Deep-Dive

While the backend powers the logic, the Android app is engineered to provide an instantaneous, highly-responsive, game-like experience. 

### Offline-First & Room Syncing
The app treats the **Room Database as the single source of truth**. 
- **Reads**: The UI strictly observes `Flow` streams directly from Room DAOs. When the database updates, the UI instantly reacts.
- **Writes**: Network interactions (like completing a quest) update the Room cache optimistically. When the device is online, the Retrofit API is hit first and the cache is updated on success.
- **Offline Write Queue**: If the device is offline, the write still applies locally and is recorded in a `pending_operations` Room table. A WorkManager `SyncWorker` (network-constrained, exponential backoff) replays the queue in order once connectivity returns. Server-rejected operations (e.g. an expired quest) are dropped rather than retried, and the queue is cleared on logout so one user's actions are never replayed under another's token.
- **Background Sync**: Network requests are managed asynchronously, ensuring the app remains fully functional and snappy even under degraded network conditions.

### MVI Architecture
To tame the complexity of RPG state management, the UI is built entirely on the **Model-View-Intent (MVI)** architectural pattern.
- State is deterministic and fully encapsulated within `ViewModel` flows.
- User actions are dispatched as discrete `Intent`s (e.g., `DashboardIntent.CompleteQuest`).
- One-off events (like a Level-Up particle animation trigger or a Snackbar) are routed cleanly through Kotlin `Channel`s as `Effect`s to prevent them from firing multiple times on configuration changes.

### Jetpack Compose Performance
Ascend is animation-heavy and UI-dense. To keep recompositions cheap:
- Data passed to Composables relies on immutable Domain Models (`@Immutable`).
- Complex screens, such as the **GitHub-Style Heatmap**, use calculated grid layouts with heavily localized state to ensure that scrolling through a year's worth of activity doesn't stutter the main thread.

### Push Notifications (FCM)
A dedicated `AscendFcmService` handles incoming Firebase Cloud Messaging data payloads. It constructs Android system tray notifications dynamically and intercepts deep-link routing. Crucially, it integrates with the `QuestRefreshWorker` to silently pre-fetch "Midnight Quest Drops", ensuring new daily quests are cached in the Room database before the user even opens the app.

### Resilient WebSocket Management
A global `WebSocketManager` maintains a persistent WSS connection with the Go API, automatically handling exponential backoff and reconnection if the device drops Wi-Fi. It streams real-time `LEVEL_UP` and `XP_AWARDED` events directly to the UI, bypassing the standard polling cycle.

On the server side, the `/api/v1/ws` endpoint is a dependency-free RFC 6455 implementation (`internal/realtime`) built directly on the Go standard library: JWT-guarded upgrade, a per-user connection hub with multi-device fan-out, and server-side ping keepalive. The XP worker publishes events through Redis Pub/Sub (`ascend:rt:<user>`), so whichever API instance holds the user's socket relays the frame — the worker and the API can scale independently.

---

## Custom Quest Generation Model

A core technical pillar of Ascend is its independent AI infrastructure. Rather than relying heavily on generic third-party AI APIs (which are expensive and rigid), Ascend features a **custom fine-tuned LLM** dedicated entirely to generating personalized RPG quests.

### The Machine Learning Journey
- **Synthetic Dataset Generation:** The process began with no real user data. A synthetic dataset of 10,000 instruction-tuning pairs was built using a custom script and Gemini, ensuring proper ChatML formatting, train/validation splits, and balanced domains.
- **Fine-tuning on Colab:** Using a free Colab GPU, Microsoft's **Phi-3 Mini** was fine-tuned from a base state using LoRA/QLoRA techniques via `SFTTrainer`.
- **Serverless Deployment:** The resulting model was merged, quantized to GGUF format, and deployed on a free Hugging Face Space running `llama-cpp-python` and Gradio for API access.
- **Fault-Tolerant Parsing:** LLMs occasionally truncate JSON due to token limits. The Go Backend features a custom "salvage" parsing algorithm that safely intercepts Hugging Face truncation errors and successfully rescues all valid JSON objects parsed up to the cutoff.
- **Continuous Improvement (DPO):** As users interact with quests (completing vs. skipping), preference pairs are collected to further align the model's reward signals in future Direct Preference Optimization iterations.

---

## Architecture & Tech Stack

| Layer | Technology |
|---|---|
| Android client | Kotlin · Jetpack Compose · MVI · Room · Retrofit · WorkManager (offline sync) · Firebase Cloud Messaging (FCM) |
| Go API | Go 1.25 · Chi router · JWT · stdlib WebSocket (RFC 6455) |
| Database | PostgreSQL 16 (pgvector) |
| Cache & streams | Redis 7 · Redis Streams (async events) · Redis Pub/Sub (realtime push) |
| Observability | Prometheus-format `/metrics` (request counts, latency histograms, live WS connections) |
| Containerisation | Docker · Docker Compose |
| Cloud | AWS EC2 (free tier) · Nginx · GitHub Actions CD |

```
Android app (Kotlin/Compose)
    │
    │  REST + WebSocket (HTTPS/WSS)
    ▼
Nginx Reverse Proxy (port 80/443)
    │
    ▼
Go API Gateway (port 8080) ──► /metrics (Prometheus, internal-only)
    │  validates, persists, publishes
    ├──► Redis queues ──► XP Worker (Go) ──► PostgreSQL
    │                          │
    │                          └──► Redis Pub/Sub ──► WS Hub ──► connected devices
    ├──► PostgreSQL (users, quests, habits, goals, progress_logs)
    │
    └──► Python LLM Quest Generation Service (port 8001)
```

---

## Testing & Quality Assurance

Ascend maintains a test-driven foundation to ensure the RPG mechanics and data syncing remain bug-free:

**Android** (`android/app/src/test`)
- **Frameworks**: **JUnit 4**, **MockK** (Kotlin object mocking), and `kotlinx-coroutines-test` for deterministic async testing.
- **Repository Mocks**: Repositories (`UserRepository`, `QuestRepository`, `HabitRepository`) are tested by mocking the Retrofit APIs to ensure the Room cache interacts correctly with network payloads.
- **Offline Queue Coverage**: Tests assert that transport failures enqueue replay operations and apply optimistic local updates, while server rejections do not — and that logout clears the queue.

**Go backend** (`go test ./...`)
- **WebSocket protocol** (`internal/realtime`): RFC 6455 handshake vectors, frame encode/decode (7/16-bit lengths), masked ping→pong, close echo, dead-connection pruning, multi-device fan-out, and concurrent-write integrity — all run with the race detector.
- **Metrics** (`internal/metrics`): counter/histogram exposition format and live gauges.
- **ML parsing** (`internal/mlservice`): the truncated-JSON "salvage" algorithm, including markdown-fenced and garbage LLM output.
- **Game math** (`internal/game`): XP curve monotonicity and difficulty-scaled reward floors.

---

## Application Metrics

| Metric | Measurement | Context |
|---|---|---|
| **APK Size** | **~15MB** | Optimized via R8 code shrinking and WebP vector assets. |
| **Cold Start Time** | **<600ms** | Hilt Dependency Injection optimized; lazy database initialization. |
| **Backend Latency** | **<15ms** | Non-blocking Go REST endpoints. |
| **Worker Processing** | **Async** | XP calculations pushed to Redis queues; UI is never blocked by complex game logic. |

### Observability
The Go API exposes Prometheus-format metrics at `/metrics` — per-route request counts, latency histograms (bucketed by chi route pattern, so `/quests/{id}/complete` is one series), uptime, and a live WebSocket connection gauge. The endpoint is blocked at the nginx layer (`404`) and the backend binds to `127.0.0.1`, so metrics are scrapeable only from inside the host.

---

## API Reference

All endpoints are prefixed with `/api/v1`.

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | None | Register — sends email OTP |
| POST | `/auth/login` | None | Login — returns access token |

### User, Goals & Quests
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/me` | JWT | Get current user profile |
| POST | `/goals` | JWT | Create a goal |
| GET | `/quests` | JWT | List active quests |
| POST | `/quests/:id/complete` | JWT | Complete a quest |
| POST | `/quests/:id/skip` | JWT | Skip a quest |
| POST | `/quests/generate` | JWT | AI-generate new quests (Rate Limited: 5/day) |

### Notifications
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/notifications` | JWT | Get user's notification inbox |
| PUT | `/notifications/{id}/read` | JWT | Mark a single notification as read |
| PUT | `/notifications/read-all` | JWT | Mark all notifications as read |
| DELETE | `/notifications/{id}` | JWT | Delete a notification |
| DELETE | `/notifications/clear-all` | JWT | Delete all notifications |

### WebSocket
| Endpoint | Auth | Description |
|---|---|---|
| `wss://host/api/v1/ws` | JWT header | Real-time event stream (server: `internal/realtime`, stdlib RFC 6455) |

**WebSocket frame types:**
```json
{"type": "LEVEL_UP",    "payload": {"new_level": 13, "xp_awarded": 125}}
{"type": "XP_AWARDED",  "payload": {"amount": 40}}
```

---

## Security Model

- **Authentication flow**: Register → email OTP sent → verify OTP → account active → login → JWT + refresh cookie
- Access tokens: 15 minutes, HS256. Refresh tokens: 7 days, stored as SHA-256 hash in Redis.
- **Account protection**: 5 failed attempts within 15 minutes → account locked for 30 minutes. OTP rate limit max 3 per 15-minute window.
- **Rate Limiting**: Auth endpoints are restricted to 10 requests per 15 minutes. AI Quest Generation is strictly limited to 5 requests per day per user to prevent abuse and API exhaustion.
- **CORS & CSRF**: Exact allow-list only. `HttpOnly`, `Secure`, `SameSite=Strict` cookies.
- **HMAC request signing**: Every mutating request includes `X-Timestamp` and `X-Signature` validated by the backend.

---

## Project Structure

```
ascend/
├── .github/workflows/        CI/CD pipelines (backend, android, lint, release)
├── backend/                   Go API server & workers
│   └── internal/
│       ├── realtime/          stdlib WebSocket server (hub, RFC 6455 framing) + tests
│       ├── metrics/           dependency-free Prometheus exposition + tests
│       ├── workers/           XP / punishment / quest workers (Redis queues)
│       └── store/postgres/    data access layer & migrations
├── android/                   Kotlin/Compose app
│   └── app/src/
│       ├── main/java/com/ascend/app/
│       │   ├── data/          Room DAOs, Retrofit APIs, offline Repositories
│       │   ├── domain/        Pure Kotlin business models (XP math)
│       │   ├── ui/            MVI architecture, Jetpack Compose screens
│       │   └── ...
│       ├── test/              JUnit & MockK Unit Tests (Repositories, Domain)
│       └── androidTest/       Instrumentation Tests
├── docs/images/               Screenshots
└── ...
```

---

### Author
Designed and Engineered by **Rajvir Singh**  
[LinkedIn](https://www.linkedin.com/in/vir-singh31/) | [GitHub](https://github.com/rajvirsingh2)

*Ascend — Level up your real life.*
