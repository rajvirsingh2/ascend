# Ascend — Complete Setup Guide (Tested & Verified)

> [!TIP]
> **Ascend is currently live and hosted on AWS!** If you just want to play the game, you do **not** need to follow this guide. Simply download the latest APK from the [Releases](https://github.com/rajvirsingh2/ascend/releases) page.
> 
> This guide is intended solely for developers who want to run the backend locally, contribute to the codebase, or host their own private instance of the game.

> This guide provides instructions for **Windows (PowerShell), macOS (zsh), and Linux (bash)**.
> Last verified: 2026-05-11.

---

## What you will have running

| Service | Container | Port |
|---|---|---|
| PostgreSQL 16 + pgvector | `ascend_postgres` | `localhost:5432` |
| Redis 7 | `ascend_redis` | `localhost:6379` |
| Go API server | `ascend_backend` | `localhost:8080` |
| Python RAG service | `ascend_rag` | `localhost:8001` |
| XP background worker | `ascend_xp_worker` | *(internal — no port)* |
| Android app | emulator or device | — |

---

## PART 1 — Prerequisites

### 1.1 — Docker Desktop

Docker runs the **entire** backend. Install it first.

1. Download from [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)
2. Run the installer — accept all defaults
3. Open Docker Desktop from Start menu
4. Wait until the whale icon says **"Engine running"** (green)
5. **Keep Docker Desktop open** while developing

**Verify:**
```powershell
docker --version          # Docker version 27.x.x
docker compose version    # Docker Compose version v2.x.x
```

---

### 1.2 — Git

1. Download from [git-scm.com/download/win](https://git-scm.com/download/win)
2. Use all defaults except:
   - PATH: choose **"Git from the command line and also from 3rd-party software"**

**Verify:**
```powershell
git --version    # git version 2.x.x
```

---

### 1.3 — GNU Make

**Windows (Chocolatey):**
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
iwr https://chocolatey.org/install.ps1 -UseBasicParsing | iex
# Close and reopen PowerShell, then:
choco install make
```

**macOS (Homebrew):**
```bash
brew install make
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update && sudo apt install make
```

**Verify:**
```powershell
make --version    # GNU Make 4.x.x
```

---

### 1.4 — Python 3.12+

**Windows:**
1. Download from [python.org/downloads](https://www.python.org/downloads/)
2. **Check "Add Python to PATH"** before clicking Install

**macOS:**
```bash
brew install python
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update && sudo apt install python3 python3-pip
```

**Verify:**
```powershell
python --version    # Python 3.12.x
```

> [!NOTE]
> Python is used by the RAG service (runs inside Docker) and by `make generate-secrets` on your host machine.

---

### 1.5 — Go 1.23+ *(optional — only for local development outside Docker)*

**Windows / macOS:**
1. Download from [go.dev/dl](https://go.dev/dl/) → `.msi` (Windows) or `.pkg` (macOS) installer.
2. Run installer — it sets PATH automatically.

**Linux (Ubuntu/Debian):**
Follow the official instructions at [go.dev/doc/install](https://go.dev/doc/install) or use snap:
```bash
sudo snap install go --classic
```

**Verify:**
```powershell
go version    # go version go1.23.x (or higher)
```

> [!TIP]
> Go is **not required** to run the app — it compiles inside Docker. You only need it if you want to run `go test` or `go build` locally on your host.

---

### 1.6 — Android Studio

1. Download from [developer.android.com/studio](https://developer.android.com/studio)
2. Run installer → accept defaults
3. On first launch, choose **"Standard"** installation → accept all licenses → Finish
4. After setup: **More Actions → SDK Manager**:
   - **SDK Platforms** tab → check **Android 14.0 (API 34)** or higher → Apply
   - **SDK Tools** tab → ensure these are checked:
     - Android SDK Build-Tools
     - Android Emulator
     - Android SDK Platform-Tools

**Create an emulator:**
1. More Actions → **Virtual Device Manager** → Create Device
2. Choose **Pixel 8** → Next → **API 34** (download if needed) → Next → Finish
3. Press ▶ Play — first boot takes 2–3 minutes

---

## PART 2 — Get the code

```powershell
git clone https://github.com/YOUR_ORG/ascend.git
cd ascend
```

### Expected folder structure
```
ascend/
├── backend/           # Go API + worker
├── rag-service/       # Python RAG service
├── android/           # Android app (Kotlin + Compose)
├── scripts/           # seed.sql, dev-tunnel.sh
├── .github/           # CI workflows
├── docker-compose.yml
├── .env.example
├── Makefile
└── .gitignore
```

---

## PART 3 — Configure environment variables

### 3.1 — Copy the template

**Windows (PowerShell):**
```powershell
Copy-Item .env.example .env
```

**macOS / Linux:**
```bash
cp .env.example .env
```

### 3.2 — Generate secrets

```powershell
make generate-secrets
```

Output:
```
JWT_SECRET=a1b2c3d4...
HMAC_SECRET=c9d0e1f2...
```

### 3.3 — Edit `.env`

Open `.env` in VS Code or Notepad and paste the generated values:

```env
# ── Database (these defaults work as-is) ──────────────────────────────
DB_NAME=ascend_db
DB_USER=ascend_user
DB_PASSWORD=ascend_pass
DB_HOST=postgres
DB_PORT=5432
DATABASE_URL=postgres://ascend_user:ascend_pass@postgres:5432/ascend_db?sslmode=disable

# ── Redis (works as-is) ──────────────────────────────────────────────
REDIS_URL=redis://redis:6379

# ── Auth (paste generated values) ────────────────────────────────────
JWT_SECRET=<paste from generate-secrets>
JWT_EXPIRY_MINUTES=15
REFRESH_TOKEN_EXPIRY_DAYS=7

# ── Encryption (paste generated values) ──────────────────────────────
HMAC_SECRET=<paste from generate-secrets>

# ── App ──────────────────────────────────────────────────────────────
APP_ENV=development
APP_PORT=8080
ALLOWED_ORIGINS=http://localhost:3000

# ── Services ─────────────────────────────────────────────────────────
RAG_SERVICE_URL=http://rag-service:8001
WORKER_TYPE=xp

# ── 3rd Party Integrations (Optional) ────────────────────────────────
RESEND_API_KEY=
EMAIL_FROM="Ascend <onboarding@resend.dev>"

CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

FCM_PROJECT_ID=
FCM_CREDENTIALS_JSON=./firebase-service-account.json
```

> [!IMPORTANT]
> The `.env` file is already in `.gitignore` — it will **never** be committed to git.
> Variables like `RESEND_API_KEY`, `CLOUDINARY_*`, and `FCM_*` are optional for local development but required for their respective features to work.

---

## PART 3B — Setting up 3rd Party Services (Optional but Recommended)

These services are now fully integrated. To enable their features, follow these one-time setup steps and add the keys to your `.env` file.

### 1. Resend (Email & OTPs)
Used for sending verification codes and welcome emails.
1. Sign up at [resend.com](https://resend.com) (free tier is sufficient).
2. Go to **API Keys** and create a new key.
3. Copy the key and add it to your `.env` as `RESEND_API_KEY`.
*(Note: Use the `Ascend <onboarding@resend.dev>` address for `EMAIL_FROM` if you haven't verified a custom domain yet).*

### 2. Cloudinary (Avatar Uploads)
Used for uploading and transforming user profile pictures.
1. Sign up at [cloudinary.com](https://cloudinary.com).
2. Go to your **Dashboard** and copy your Cloud Name, API Key, and API Secret.
3. Add them to your `.env` under the respective `CLOUDINARY_*` keys.

### 3. Firebase Cloud Messaging (FCM)
Used for daily push reminders and level-up notifications.
1. Go to the [Firebase Console](https://console.firebase.google.com).
2. Create a project or select an existing one.
3. Navigate to **Project Settings → Service Accounts** and click **Generate new private key**.
4. Download the JSON file and place it in the `backend/` directory as `firebase-service-account.json`.
5. Add your Firebase Project ID to your `.env` as `FCM_PROJECT_ID` (the `FCM_CREDENTIALS_JSON` path should point to the JSON file you just downloaded).

---

## PART 4 — Start Docker and run migrations

### 4.1 — Start only database + Redis first

```powershell
docker compose up postgres redis -d
```

Wait for healthy status:
```powershell
docker compose ps
```

Expected:
```
NAME              STATUS
ascend_postgres   running (healthy)
ascend_redis      running (healthy)
```

> [!TIP]
> Takes 15–30 seconds. If postgres isn't healthy yet, wait and retry `docker compose ps`.

### 4.2 — Run database migrations

```powershell
make migrate
```

Expected output:
```
1/u create_extensions
2/u create_users
3/u create_goals
...
18/u physique_profile
```

If it says `no change` — that's fine, migrations already ran.

### 4.3 — Verify tables were created

```powershell
docker exec ascend_postgres psql -U ascend_user -d ascend_db -c "\dt"
```

You should see 15 tables including `users`, `quests`, `habits`, `physique_profiles`, `user_memories`.

### 4.4 — Load test seed data

```powershell
make seed
```

Output:
```
→ Seeding database...
BEGIN
DELETE ...
INSERT ...
COMMIT
✓ Seed complete. Login: test@ascend.app / password123
```

---

## PART 5 — Start the full stack

### 5.1 — Start all services

```powershell
docker compose up --build
```

> [!NOTE]
> **First run takes 3–5 minutes** — Docker downloads images and compiles Go.
> Subsequent starts use cache and take ~10–30 seconds.

Watch for all services to show as running:
```
ascend_postgres    ✓ healthy
ascend_redis       ✓ healthy
ascend_backend     ✓ started
ascend_rag         ✓ started
ascend_xp_worker   ✓ started
```

### 5.2 — Verify each service (open a new terminal)

```powershell
# Backend health
Invoke-RestMethod http://localhost:8080/health
# → @{data=@{status=ok}}

# Backend readiness (confirms DB + Redis connected)
Invoke-RestMethod http://localhost:8080/ready
# → @{data=@{status=ready}}

# RAG service health
Invoke-RestMethod http://localhost:8001/health
# → @{status=ok; service=rag}
```

> [!TIP]
> If `/health` returns nothing, the backend is still compiling. Wait 30 seconds and retry.

---

## PART 6 — Test the API

### 6.1 — Login with the test user

**Windows (PowerShell):**
```powershell
$body = @{email='test@ascend.app'; password='password123'} | ConvertTo-Json
$login = Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/login `
    -Method POST -Body $body -ContentType 'application/json'
$login.data
```

**macOS / Linux (Bash):**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@ascend.app","password":"password123"}'
```

Expected:
```
access_token : eyJhbGciOiJIUzI1NiIs...
token_type   : Bearer
```

### 6.2 — Save the token and call protected endpoints

**Windows (PowerShell):**
```powershell
$token = $login.data.access_token
$headers = @{Authorization = "Bearer $token"}

# Get user profile
Invoke-RestMethod http://localhost:8080/api/v1/me -Headers $headers | ConvertTo-Json -Depth 5
```

**macOS / Linux (Bash):**
```bash
TOKEN="your_access_token_here"
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me
```

Expected response:
```json
{
    "data": {
        "username": "TestHero",
        "level": 3,
        "current_xp": 50,
        "xp_to_next": 800,
        "hp": 87,
        "max_hp": 100
    }
}
```

### 6.3 — Test quests, habits, goals

**Windows (PowerShell):**
```powershell
# Get active quests
Invoke-RestMethod http://localhost:8080/api/v1/quests -Headers $headers | ConvertTo-Json -Depth 5

# Get habits
Invoke-RestMethod http://localhost:8080/api/v1/habits -Headers $headers | ConvertTo-Json -Depth 5

# Get goals
Invoke-RestMethod http://localhost:8080/api/v1/goals -Headers $headers | ConvertTo-Json -Depth 5
```

**macOS / Linux (Bash):**
```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/quests
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/habits
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/goals
```

All three should return arrays with 2 items each (seeded data).



> [!IMPORTANT]
> **Backend is fully working if all the above return data.** ✅

---

## PART 7 — Run the Android app

### 7.1 — Open the project

1. Open Android Studio → click **"Open"** (not "New Project")
2. Navigate to `ascend/android/` → click OK
3. Wait for **Gradle sync** to finish (3–5 minutes first time)
4. Done when bottom bar says "Gradle sync finished" with no errors

### 7.2 — Verify `BASE_URL` is correct

Open [android/app/build.gradle.kts](file:///d:/rajvir/ascend/android/app/build.gradle.kts). The debug block should have:

```kotlin
debug {
    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/v1/\"")
}
```

> [!WARNING]
> `10.0.2.2` is the Android emulator's special alias for your host machine's `localhost`.
> Do **NOT** use `localhost` or `127.0.0.1` — they refer to the emulator itself, not your PC.

### 7.3 — Verify network security config exists

File: `android/app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
    </domain-config>
</network-security-config>
```

And `AndroidManifest.xml` must have:
```xml
android:networkSecurityConfig="@xml/network_security_config"
```

Both are already present in the codebase. ✅

### 7.4 — Run the app

1. Start your emulator from the Device Manager
2. Select the emulator in the device dropdown (top toolbar)
3. Press the green ▶ **Run** button (or Shift+F10)
4. First compile takes ~1–2 minutes
5. App launches to the **Login screen**

### 7.5 — Log in

| Field | Value |
|---|---|
| Email | `test@ascend.app` |
| Password | `password123` |

You'll land on the Dashboard showing the hero panel, active quests, and habits.

---

## PART 8 — Quest generation (ML)

Quests are generated using a custom ML model hosted on HuggingFace Spaces. 

### 8.1 — Configuration

The backend connects to the ML model via the `ML_SERVICE_URL` variable in your `.env`.

### 8.2 — Generate quests

1. Go to **Dashboard** → tap **"GENERATE NEW QUESTS"**
2. Wait 2–5 seconds
3. Three AI-generated quests should appear

**If it fails:**
```powershell
docker compose logs ascend_backend --tail 30
```

### 8.3 — Export Preference Data (DPO)

The backend automatically logs user interactions (completed vs skipped quests). Once enough data is collected, you can export it to train the next version of the model:

```powershell
# Requires Python and psycopg2
pip install psycopg2
python scripts/ml/export_preferences.py --db "postgres://ascend_user:ascend_pass@localhost:5432/ascend_db?sslmode=disable"
```
This generates `data/preferences.jsonl` which can be uploaded to Google Drive to run the Colab training notebook located at `scripts/ml/notebooks/Ascend_Phase5_DPO.ipynb`.

---

## PART 9 — Daily workflow

**Windows:**
```powershell
cd d:\rajvir\ascend
docker compose up --build

# In a new terminal:
Invoke-RestMethod http://localhost:8080/health
```

**macOS / Linux:**
```bash
cd /path/to/ascend
docker compose up --build

# In a new terminal:
curl http://localhost:8080/health
```

*(To stop everything, press `Ctrl+C` or run `docker compose down` in any OS).*

---

## PART 10 — Make commands reference

| Command | What it does |
|---|---|
| `docker compose up --build` | Start all services (keep running) |
| `docker compose down` | Stop all services |
| `make migrate` | Run pending database migrations |
| `make migrate-down` | Roll back the last migration |
| `make migrate-status` | Show current migration version |
| `make seed` | Load test data (test@ascend.app / password123) |
| `make shell-db` | Open interactive PostgreSQL shell |
| `make shell-redis` | Open interactive Redis shell |
| `make logs` | Tail logs from all containers |
| `make clean` | Stop containers + **wipe all data** |
| `make test` | Run Go unit tests |
| `make generate-secrets` | Generate fresh JWT/HMAC keys |

---

## PART 11 — Troubleshooting

### "docker: command not found"
Docker Desktop is not installed or not running. Open Docker Desktop and wait for the green status.

### "port 5432 already in use"
```powershell
# Find what's using it
netstat -ano | findstr :5432
# Kill the process
Stop-Process -Id <PID> -Force
```

### "port 8080 already in use"
```powershell
netstat -ano | findstr :8080
Stop-Process -Id <PID> -Force
```

### Migrations fail with "connection refused"
PostgreSQL isn't ready yet:
```powershell
docker compose ps    # Wait until ascend_postgres shows (healthy)
make migrate         # Retry
```

### Login returns "invalid credentials"
The seed data may be stale. Re-seed:
```powershell
# Clear any rate-limit lockouts first
docker exec ascend_redis sh -c "redis-cli FLUSHDB"
# Re-seed
make seed
```

### Login returns "email not verified"
The seed script sets `email_verified=true` automatically. If you registered a **new** user via the API, you need to verify the email. Read the OTP from Redis:
```powershell
docker exec ascend_redis sh -c "redis-cli KEYS 'otp:*'"
docker exec ascend_redis sh -c "redis-cli GET otp:your@email.com"
```
Copy the 6-digit code and call the verify endpoint:
```powershell
$verifyBody = @{email='your@email.com'; otp='123456'} | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/verify-email `
    -Method POST -Body $verifyBody -ContentType 'application/json'
```

### "account temporarily locked"
Too many failed login attempts. Clear the lockout:
```powershell
docker exec ascend_redis sh -c "redis-cli FLUSHDB"
```

### Android shows "Network Error"
1. Confirm backend is running: `Invoke-RestMethod http://localhost:8080/health`
2. Confirm `BASE_URL` is `http://10.0.2.2:8080/api/v1/` (not `localhost`)
3. Confirm `network_security_config.xml` exists and allows cleartext to `10.0.2.2`
4. In Android Studio: Build → Clean Project → Run again

### Android Gradle sync failed
1. File → Invalidate Caches → Invalidate and Restart
2. Let sync finish after restart
3. Check internet connection (Gradle downloads dependencies)

### RAG service shows "mock embedder" in logs
Normal. The app primarily relies on the custom ML model for quest generation. RAG is available as a fallback.

### Docker builds are very slow
First build downloads + compiles everything. Subsequent builds use cache (10–30 seconds).

### Complete nuclear reset
```powershell
make clean       # Stops containers + wipes all database data
make migrate     # Re-creates all tables from scratch
make seed        # Re-loads test data
```

---

## PART 12 — Deploy to Railway

### 12.1 — Install Railway CLI
```powershell
npm install -g @railway/cli
```

### 12.2 — Sign up and link
```powershell
railway login     # Opens browser → sign in with GitHub
railway init      # Links folder to a new Railway project
```

### 12.3 — Add managed services
```powershell
railway add --plugin postgresql
railway add --plugin redis
```

### 12.4 — Set secrets
```powershell
# Generate and set in one go
$jwt = python -c "import secrets; print(secrets.token_hex(32))"
$hmac = python -c "import secrets; print(secrets.token_hex(32))"
railway variables set JWT_SECRET="$jwt" HMAC_SECRET="$hmac" APP_ENV="production" ALLOWED_ORIGINS="*"
```

### 12.5 — Deploy
```powershell
railway up
```

### 12.6 — Update Android for production

In `android/app/build.gradle.kts`, update the release block:
```kotlin
release {
    buildConfigField("String", "BASE_URL",
        "\"https://YOUR-APP.up.railway.app/api/v1/\"")
}
```

---

## PART 13 — Test on physical device (ngrok)

### 13.1 — Install ngrok
1. Sign up at [ngrok.com](https://ngrok.com) (free)
2. Download for Windows
3. Run: `ngrok config add-authtoken YOUR_TOKEN`

### 13.2 — Start the tunnel
```powershell
ngrok http 8080
```
Copy the printed `https://` URL.

### 13.3 — Update Android
In `android/app/build.gradle.kts`:
```kotlin
create("ngrok") {
    buildConfigField("String", "BASE_URL",
        "\"https://YOUR-NGROK-URL.ngrok-free.app/api/v1/\"")
}
```

In Android Studio: View → Tool Windows → Build Variants → change to `ngrokDebug` → Run.

---

## Appendix — Files changed during setup verification

| File | Change |
|---|---|
| [Makefile](file:///d:/rajvir/ascend/Makefile) | Added missing targets: `seed`, `shell-db`, `shell-redis`, `logs`, `clean`, `generate-secrets` |
| [scripts/seed.sql](file:///d:/rajvir/ascend/scripts/seed.sql) | Fixed: sets `email_verified=true`, correct bcrypt hash, proper cleanup, includes `hp`/`max_hp` |
| [.env.example](file:///d:/rajvir/ascend/.env.example) | Updated to include all actual env vars (`HMAC_SECRET`, `WORKER_TYPE`) |
| [.gitignore](file:///d:/rajvir/ascend/.gitignore) | Removed `.env.example` from ignore (it should be tracked) |
| [server.go](file:///d:/rajvir/ascend/backend/internal/server/server.go) | Fixed physique route: `Save` changed from `GET` to `POST` |

---

## Appendix — Known limitations

*(Note: The XP Worker, Email OTPs via Resend, Avatar Uploads via Cloudinary, and Push Notifications via FCM have now been fully implemented and are no longer limitations!)*
