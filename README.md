# Competitive Programming Club Website

Source for the Programming Club @ DAU website — member directory, blog, club
history, and (from Phase 2) a leaderboard synced from Codeforces.

## Project Structure
- `frontend/`: Next.js web application. See [frontend/FRONTEND.md](frontend/FRONTEND.md).
- `backend/`: Spring Boot REST API.
- `documents/`: Project planning, architecture, and execution playbooks.

## Local Development

Requires Node.js 20+, Java 17+, and Docker Desktop.

The frontend runs entirely on its own — every page renders from local content
files with no API calls — so skip steps 1 and 3 unless you are working on the
backend.

### 1. Database
Run the PostgreSQL database locally using Docker Compose:
```bash
docker-compose up -d
```

### 2. Frontend
```bash
cd frontend
npm install
npm run dev      # localhost:3000
```

### 3. Backend
```bash
cd backend
./mvnw spring-boot:run   # localhost:8080, needs step 1
```

## Before opening a PR

CI is path-scoped: `frontend/**` changes run the frontend workflow,
`backend/**` the backend one. Run the matching check locally first.

```bash
cd frontend && npm run lint && npm run build
cd backend  && ./mvnw clean compile test -B
```

Branch from `main` as `feature/<member>-<feature>`, open a PR, and let the Team
Leader review — no self-merging, no force-pushing shared branches.

## Status

| Area | State |
| --- | --- |
| Frontend shell, design system, static pages | Built (Role 1) |
| Home, About, Events, Hall of Fame, Blog, Members | Built, placeholder content |
| Auth, login/register | Not started (Role 2) |
| Profiles, leaderboard UI | Not started (Role 3) |
| Backend entities, APIs, security | Boilerplate only (Roles 4–5) |
| Codeforces sync job, deployment | Not started (Role 6) |

Site content is placeholder pending confirmation from the club — see the
placeholder section of [frontend/FRONTEND.md](frontend/FRONTEND.md) before
publishing anything publicly.
