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

The backend suite includes Testcontainers tests against real PostgreSQL, so it
needs a running Docker daemon. CI has one. Without Docker locally those classes
fail to start rather than skip.

Branch from `main` as `feature/<member>-<feature>`, open a PR, and let the Team
Leader review — no self-merging, no force-pushing shared branches.

## Status

| Area | State |
| --- | --- |
| Frontend shell, design system, every public page | Built |
| Sign-in (Google, `@dau.ac.in` only) | Built |
| Profiles, leaderboards (Codeforces + LeetCode, club-role filters) | Built (Phase 2) |
| Events, attendance, Hall of Fame, gallery | Built (Phase 2) |
| CP Bingo, 1v1 compete | Built |
| Phase 3 backend: sync pipeline, stats, scores and awards, calendar | Merged |
| Phase 3 backend: club contests, blog workflow and comments, RSVP and reminders | Built, in review |
| Phase 3 frontend: stats pages, contests, calendar, practice, blog editor, RSVP | Not started |
| Phase 3 rollout (flags, backfill, mail) | Not started -- see Section 10 of the Phase 3 playbook |

The Phase 3 plan, and every decision behind it, is
[documents/phase_3_execution_playbook.md](documents/phase_3_execution_playbook.md).
Read its Amendments section first: it records what changed after the plan was
written, including what the Codeforces API actually allows today.

Site content is placeholder pending confirmation from the club -- see the
placeholder section of [frontend/FRONTEND.md](frontend/FRONTEND.md) before
publishing anything publicly.
