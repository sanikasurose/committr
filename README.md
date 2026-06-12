# Committr

A multi-user GitHub developer activity dashboard that surfaces real impact — lines shipped, language trajectory, coding hours, PR velocity, and team share — not just commit counts.

> **Not currently deployed.** Run it locally with the steps below.

## Stack

Spring Boot 3 / Java 21 · Angular 19 · PostgreSQL 16 · Redis 7 · Docker Compose · Flyway · GitHub OAuth

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (runs PostgreSQL, Redis, backend, and frontend together)
- A GitHub OAuth App — create one at [github.com/settings/developers](https://github.com/settings/developers)

## Setup

**1. Clone and configure**

```bash
git clone https://github.com/sanikasurose/committr.git
cd committr
cp .env.example .env
```

Open `.env` and fill in three required values:

| Variable | Where to get it |
|---|---|
| `GITHUB_CLIENT_ID` | Your GitHub OAuth App → Client ID |
| `GITHUB_CLIENT_SECRET` | Your GitHub OAuth App → Client secrets |
| `ENCRYPTION_KEY` | Any 32-character string, e.g. `openssl rand -base64 24` |

**2. Configure your GitHub OAuth App**

In your GitHub OAuth App settings, set:
- **Homepage URL:** `http://localhost:4200`
- **Authorization callback URL:** `http://localhost:8080/api/auth/callback`

**3. Start everything**

```bash
docker compose up --build
```

This starts PostgreSQL, Redis, the Spring Boot backend, and the Angular frontend. Flyway runs database migrations automatically on first boot.

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |

**4. Sign in**

Open http://localhost:4200, click **Continue with GitHub**, and you'll be redirected back to the dashboard after OAuth completes.

## Running tests

```bash
# Unit tests only (no Docker required)
cd backend && ./mvnw test

# Unit + integration tests (requires Docker for Testcontainers)
cd backend && ./mvnw verify
```

## Project structure

```
committr/
├── backend/          # Spring Boot 3 / Java 21
│   ├── src/main/java/com/committr/backend/
│   │   ├── controller/   # REST endpoints
│   │   ├── github/       # GitHub API client + OAuth
│   │   ├── analytics/    # Aggregation service + endpoints
│   │   └── ...
│   └── src/main/resources/db/migration/   # Flyway SQL
├── frontend/         # Angular 19 standalone components
│   └── src/app/
│       ├── features/ # repos, analytics, profile
│       ├── pages/    # landing, login
│       └── core/     # auth, guards, chart theme
├── docker-compose.yml
├── render.yaml       # deploy config (kept for reference, not active)
└── Jenkinsfile       # CI pipeline (kept for reference, not active)
```

## Features

- **GitHub OAuth** — sign in with your GitHub account; tokens encrypted at rest (AES-256)
- **Repo tracking** — add any GitHub repo you have access to; data is ingested automatically
- **Analytics dashboard** — team contribution share, language trend, coding-hours heatmap, commit frequency, PR velocity
- **Public profile + badge** — shareable stats page at `/u/:username` with embeddable SVG badge
- **Dark UI** — Tailwind CSS v4, Linear/Vercel-inspired design
