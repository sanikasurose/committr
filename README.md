# Committr

A multi-user GitHub developer activity dashboard that surfaces real impact — lines shipped, language trajectory, coding hours, PR velocity, and team share — not just commit counts.

## Live demo

- **Frontend:** https://sanikasurose.github.io/committr/
- **Backend API:** https://committr-backend.onrender.com

> The Render free tier sleeps after inactivity. First request after a cold start may take ~30 seconds.

## Embed your stats badge

![Committr Stats](https://committr-backend.onrender.com/api/badge/sanikasurose)

Add this to any GitHub README — replace `your-username` with your GitHub login:

```md
![Committr Stats](https://committr-backend.onrender.com/api/badge/your-username)
```

The badge shows: **Lines Shipped · Streak · Top Language · Team Share**

## Stack

Spring Boot 3 / Java 21 · Angular 19 · PostgreSQL 16 · Redis 7 · Docker Compose · Flyway · Jenkins · GitHub OAuth

## Running locally

```bash
cp .env.example .env
# Fill in: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, ENCRYPTION_KEY (32-char AES key)

docker compose up --build
```

- Backend: http://localhost:8080
- Frontend: http://localhost:4200

## Running tests

```bash
# Unit tests (fast — no Docker required)
cd backend && ./mvnw test

# Unit + integration tests (requires Docker for Testcontainers)
cd backend && ./mvnw verify
```

## CI/CD (Jenkins)

4-stage pipeline defined in `Jenkinsfile`:

1. **Unit Tests** — `./mvnw test` (JUnit 5 + Mockito)
2. **Integration Tests** — `./mvnw verify` (Testcontainers + real PostgreSQL)
3. **Build** — Maven package + Angular build
4. **Docker Deploy** — `docker compose build`; triggers Render deploy hook on `main`
