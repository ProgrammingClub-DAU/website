# PHASE 1 — COMPLETE EXECUTION PLAN

## 1. Phase 1 Objective
To develop and deploy a functional, production-ready Minimum Viable Product (MVP) for the Competitive Programming (CP) Club. This includes secure user authentication, public user profiles, club member directory, static informational pages, and a dynamic leaderboard synchronized with Codeforces ratings.

## 2. Current Repository/Project State
**ANALYSIS RESULT:** Stage 0 (Initialization) is **Complete**.
*   **Frontend:** Next.js 16.3 + Tailwind CSS 4.x initialized in `frontend/`.
*   **Backend:** Java 17 + Spring Boot 4.1.0 boilerplate initialized in `backend/`.
*   **Database:** `docker-compose.yml` for PostgreSQL created at root.
*   **CI/CD:** GitHub Actions pipelines for Frontend (lint + build) and Backend (compile + test) are active.
*   **Git:** Repository initialized, `.gitignore`, `README.md`, and all boilerplate committed to `main` branch.

## 3. Team & Member Responsibilities
*   **Member 1 — Frontend UI/UX:** Primary owner of Next.js architecture, Tailwind styling, and static pages (Home, About).
*   **Member 2 — Frontend Auth:** Primary owner of React forms, Zod validation, Axios interceptors, and JWT state management.
*   **Member 3 — Frontend Dashboards:** Primary owner of dynamic data rendering (Profiles, Leaderboard UI, Member Directory).
*   **Member 4 — Backend Security:** Primary owner of Spring Security, BCrypt, JWT generation, and User entity definition.
*   **Member 5 — Backend APIs:** Primary owner of Spring Controllers/Services/Repositories for Content and DTO mapping.
*   **Member 6 — Team Leader (DevOps/Integrations):** Primary owner of Git Flow, Docker, PR Reviews, Codeforces cron jobs, and Deployment.

## 4. Phase 1 Architecture & Dependencies
*   **Frontend:** Next.js (App Router), React, Tailwind CSS, Zustand (State), Axios.
*   **Backend:** Java 17+, Spring Boot 4.1.0, Spring Security, Spring Data JPA, JWT, Springdoc OpenAPI (Swagger).
*   **Database:** PostgreSQL 15+ (Local via Docker Compose, Production via Managed DB).
*   **External Dependency:** Codeforces API (`https://codeforces.com/api/user.info`).

## 5. Master Dependency Graph
```text
[Stage 0] Project/Repo Initialization & Docker Setup
       ↓
[Stage 0] API Contract Definition (CRITICAL SYNC)
       ↓
       ├── [Stage 1] Frontend Tailwind & Boilerplate
       ├── [Stage 1] Backend Spring Security & DB Entities
       ↓
       ├── [Stage 2] Frontend Forms (Mock Data)
       ├── [Stage 2] Backend Auth APIs (/register, /login)
       ├── [Stage 2] Backend Data APIs (/users)
       ↓
[Stage 3] Integration Layer (Frontend connects to Backend via Axios)
       ↓
[Stage 4] Codeforces Sync Job (Backend)
       ↓
[Stage 4] Leaderboard Dashboard (Frontend)
       ↓
[Stage 5] Final Testing & Deployment
```

---

## 6. Stage 0 — Preparation

### Objective
Initialize the monorepo, set up the local developer environment, and finalize the API Contract.

### Members
All Members (Led by Member 6)

### Preconditions
None. Greenfield start.

### Execution Order
`SEQUENTIAL`
Member 6 → Repo Init
Member 6 → Docker Postgres Setup
All Members → API Contract Definition

### Detailed Tasks
*   **Task P1-S0-01 (Member 6):** Create GitHub repo. Create `frontend/` and `backend/` directories. Add `.gitignore`. Set branch protection rules on `main`.
*   **Task P1-S0-02 (Member 6):** Create `docker-compose.yml` in root for PostgreSQL. Add init scripts if necessary.
*   **Task P1-S0-03 (All Members):** Draft the API Contract document. Define exact JSON payloads for `/api/auth/register`, `/api/auth/login`, and `/api/users`. 

### Validation
Run `docker compose up -d`. Verify Postgres is running on port 5432. Verify API contract is documented.

### Handoff
Member 6 provides the Git clone URL to the team.

### Exit Gate
Phase 1 may proceed only if:
- [ ] GitHub repository is cloned by all 6 members.
- [ ] PostgreSQL runs successfully locally for all backend members.
- [ ] API Contract is frozen and agreed upon.

## 7. Stage 1 — Foundation & Boilerplate

### Objective
Initialize frontend and backend frameworks and establish the core database schema.

### Members
Member 1, Member 4, Member 6

### Preconditions
Stage 0 completed.

### Execution Order
`PARALLEL`
├── Member 1 → Next.js & Tailwind setup
├── Member 4 → Spring Boot & Security setup
└── Member 6 → Setup Swagger Configuration

### Detailed Tasks
*   **Task P1-S1-01 (Member 1):** Run `npx create-next-app@latest frontend`. Configure `tailwind.config.ts` with CP-theme colors. Build layout shell (Navbar, Footer).
*   **Task P1-S1-02 (Member 4):** Generate Spring Boot project. Configure `application.yml` for Postgres. Create `User.java` JPA entity (including a `Role` enum for `ROLE_USER` and `ROLE_ADMIN`).
*   **Task P1-S1-03 (Member 6):** Add `springdoc-openapi` dependency to Spring Boot for automatic Swagger generation.

### Validation
Member 1 runs `npm run dev` and sees the themed layout. Member 4 runs the Spring Boot application and sees Hibernate auto-create the `users` table in Postgres.

### Handoff
Member 4 pushes `User` entity to Git so Member 5 can use it. Member 1 pushes layout so Members 2 & 3 can build inside it.

### Exit Gate
- [x] Next.js app compiles.
- [ ] Spring Boot app compiles and connects to DB.
- [ ] Base PRs reviewed and merged by Member 6.

## 8. Stage 2 — Independent API & UI Construction

### Objective
Build the raw APIs on the backend and the raw UI forms on the frontend (using mock data).

### Members
Member 2, Member 3, Member 4, Member 5

### Preconditions
Stage 1 merged to `main`.

### Execution Order
`PARALLEL`
├── Member 2 → Auth Forms (Frontend)
├── Member 3 → Profile UI (Frontend)
├── Member 4 → Auth APIs (Backend)
└── Member 5 → Data APIs (Backend)

### Detailed Tasks
*   **Task P1-S2-01 (Member 2):** Build `/login` and `/register` Next.js pages using `react-hook-form` and `zod`. Print to `console.log` on submit.
*   **Task P1-S2-02 (Member 3):** Build `/profile` and `/members` pages using mock JSON data.
*   **Task P1-S2-03 (Member 4):** Implement `JwtUtils`, `JwtAuthenticationFilter`, and `AuthController` (`/register`, `/login`). Ensure the JWT payload securely embeds the user's role (Admin vs Normal).
*   **Task P1-S2-04 (Member 5):** Create `UserController` and `UserService` for `GET /api/users` and `PUT /api/users/{id}/handle`. Create `UserResponseDto`.

### Validation
Frontend forms validate locally. Backend APIs return 200 OK or 400 Bad Request via Postman.

### Handoff
Member 4 & 5 provide Swagger UI URL (`localhost:8080/swagger-ui.html`) to Members 2 & 3.

### Exit Gate
- [x] Auth forms prevent invalid submissions (e.g., weak password).
- [x] Auth APIs successfully hash passwords and return valid JWTs.
- [ ] Member 6 tests APIs via Postman.

## 9. Stage 3 — The Integration Layer

### Objective
Connect the frontend to the backend. Real data flows through the application.

### Members
Member 2, Member 3, Member 6

### Preconditions
Stage 2 APIs verified by Member 6.

### Execution Order
`SEQUENTIAL`
Member 2 → Setup Axios Interceptors
       ↓
Member 2 → Connect Auth Forms
       ↓
Member 3 → Connect Profile & Member Directory
       ↓
Member 6 → Cross-Origin (CORS) Verification

### Detailed Tasks
*   **Task P1-S3-01 (Member 2):** Create `src/lib/axios.ts` interceptor to attach JWT. Connect forms to `POST /api/auth/*`. Setup `zustand` to hold `user` state (including the user's role to toggle Admin UI).
*   **Task P1-S3-02 (Member 3):** Swap mock data with `axios.get('/api/users')`. Render real database profiles.
*   **Task P1-S3-03 (Member 6):** Ensure Spring Boot CORS configuration allows `localhost:3000`.

### Validation
User can register on frontend, login, see their name in the Navbar, navigate to Profile, and see their details.

### Handoff
Integration complete. Ready for external CP features.

### Exit Gate
- [ ] End-to-end Registration -> Login -> View Profile flow works.
- [ ] Refreshing the page keeps the user logged in (JWT in localStorage/cookies).
- [ ] 401 Unauthorized errors correctly redirect to `/login`.

## 10. Stage 4 — External Integrations (Codeforces)

### Objective
Implement the core CP feature: The live Codeforces leaderboard.

### Members
Member 3, Member 6

### Preconditions
Stage 3 Integration works. User handles can be saved to the database.

### Execution Order
`SEQUENTIAL`
Member 6 → Codeforces Cron Job
       ↓
Member 3 → Leaderboard UI

### Detailed Tasks
*   **Task P1-S4-01 (Member 6):** Create `@Scheduled(cron = "0 0 */6 * * *")` service in Spring Boot. Fetch `https://codeforces.com/api/user.info` for all users (Note: Sync also occurs instantaneously when a user updates their handle). Save `currentRating` to Postgres.
*   **Task P1-S4-02 (Member 3):** Create `/leaderboard` Next.js page. Fetch users sorted by rating.

### Validation
Force-trigger the cron job. Verify DB updates. Verify Leaderboard reflects accurate Codeforces ratings.

### Handoff
Feature complete. Proceed to final deployment.

### Exit Gate
- [ ] Codeforces API rate-limits handled gracefully (don't crash if CF is down).
- [ ] Leaderboard renders correctly on frontend.

---

## 11. Parallel Workstreams
**TRACK A (UI/UX):** Member 1 (Static pages, Layouts) runs continuously in parallel with backend architecture.
**TRACK B (Auth):** Member 2 (React Forms) and Member 4 (Spring Security) work in parallel until Stage 3 synchronization.
**TRACK C (Data):** Member 3 (Dashboards) and Member 5 (Data APIs) work in parallel using agreed-upon API Contracts until Stage 3 synchronization.

## 12. Git & Branching Strategy
*   **Main Branch:** `main` (Strictly protected, deployment branch).
*   **Feature Branches:** `feature/[role]-[feature-name]` (e.g., `feature/frontend-login`, `feature/backend-security`).
*   **Rules:**
    1. Checkout from `main`: `git checkout -b feature/xyz`
    2. Commit frequently with descriptive messages.
    3. Open a Pull Request (PR) to `main`.
    4. **Merge Order:** Backend PRs must merge before dependent Frontend PRs merge.

## 13. Code Review Strategy
**Reviewer:** Member 6 (Team Leader)
**Checklist:**
- [ ] No hardcoded passwords or secrets.
- [ ] Spring Boot Controllers strictly return DTOs (No Entities).
- [ ] Next.js components use 'use client' only when hooks are needed.
- [ ] No `console.log()` left in frontend production code.
- [ ] Swagger annotations are present on new APIs.

## 14. Testing Strategy
*   **Unit Testing:** Member 4 writes JUnit tests for JWT generation. Member 5 writes JUnit tests for Data mapping.
*   **API Testing:** Member 6 maintains a Postman Collection exported to the repository for all developers to use.
*   **End-to-End (Manual):** Create User -> Login -> Edit Profile -> Add Codeforces Handle -> Check Leaderboard.

## 15. Security Validation
- [x] **Authentication:** JWT is stateless. No session cookies stored in Spring Boot memory.
- [x] **Passwords:** BCrypt algorithm with complexity strength 12.
- [x] **CORS:** Only permit frontend origin domains (e.g., `localhost:3000`, `production-url.com`).
- [x] **Secrets:** `JWT_SECRET` and `DB_PASSWORD` are strictly in `.env` and never committed to Git.

## 16. Performance Validation
- [ ] **API Latency:** Codeforces Sync Job must batch requests if member count > 100 to avoid CF API bans.
- [ ] **Database:** Add SQL Index on `user.codeforces_handle` and `user.rating`.
- [ ] **Frontend:** Images (e.g., Club photos) must use Next.js `<Image>` component for optimization.

## 17. Documentation Plan
*   `README.md`: Must contain instructions on running `docker compose up`, `npm run dev`, and `mvn spring-boot:run`.
*   `API.md` (or Swagger): Living documentation of API routes.

## 18. Deployment Plan

> Every environment variable is documented in `backend/.env.example` and
> `frontend/.env.example`. Those files are the source of truth — if a variable is
> not in them, the code does not read it.

*   **Frontend (Vercel):** Connect Vercel to the GitHub `main` branch. Set
    `NEXT_PUBLIC_API_URL` (the deployed backend URL) and `NEXT_PUBLIC_SITE_URL`
    (this site's public origin). Both are inlined at build time, so changing
    either requires a redeploy.
*   **Backend (Render):** Connect to `main`. Set:
    *   `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
        `SPRING_DATASOURCE_PASSWORD` — **not** `DATABASE_URL`. Render injects a
        variable by that name automatically, but Spring does not read it and its
        `postgres://user:pass@host/db` form cannot be parsed by the JDBC driver.
        Rewrite it as `jdbc:postgresql://host:5432/db` and split the credentials
        into the two variables above.
    *   `JWT_SECRET` — required, no default; the app fails to start without it.
        Generate with `openssl rand -base64 48`.
    *   `CORS_ALLOWED_ORIGINS` — must include the Vercel URL, or reads will work
        while every browser-side write fails preflight.
    *   `SPRING_PROFILES_ACTIVE=prod` is baked into the Dockerfile, so it does not
        need setting — but verify it took effect (see below).
*   **Database (Render/Supabase):** Provision managed Postgres. The schema is
    created by Hibernate on first boot under the `default` profile; production runs
    `ddl-auto: validate` and will not alter it.

### Verifying a deployment

```bash
curl -i https://<backend-host>/v3/api-docs
```

A **404 confirms the `prod` profile is active.** A 200 means the app fell back to
the `default` profile — it will still serve traffic and look healthy while running
`ddl-auto: update`, logging every SQL statement, and exposing Swagger.

## 19. Failure Recovery / Rollback Plan
*   **Database Failure:** Follow [DATABASE-BACKUP.md](DATABASE-BACKUP.md) — take a
    dump of the broken state, restore the last good dump into a scratch database,
    verify it, then restore into production. **Do not drop the schema.** Production
    runs `ddl-auto: validate` with Flyway-managed migrations, so an empty schema
    means the application will not start at all, turning a partial failure into a
    total one.
*   **Bad Migration:** Write a forward migration that undoes the change, or restore
    a dump taken before it ran. Reverting the code alone does not revert the
    schema, and `validate` will then fail against the older entities. Take a backup
    before deploying any migration.
*   **Deployment Failure:** In Vercel/Render, use the "Rollback to previous deployment" button. Revert the bad commit in Git (`git revert <commit-hash>`).

---

## 20. Member-Wise Complete Execution Plans

### Member 1 — Frontend UI/UX & Layouts
**Responsibilities:** Global architecture, styling, and static pages.
**Execution Order:** Stage 1 → Stage 2
**Tasks:**
1. `npx create-next-app`
2. Define Tailwind colors.
3. Build `<Navbar>` and `<Footer>`.
4. Build `/` (Home) and `/about` pages.
**Validation:** Check responsive design on mobile viewport.
**Handoff:** Tell Member 2 the layout is ready for forms.
**Completion Checklist:**
- [x] App compiles.
- [x] Mobile responsive.

### Member 2 — Frontend Auth & Forms
**Responsibilities:** Login/Registration and API integration.
**Execution Order:** Stage 2 → Stage 3
**Tasks:**
1. Build Forms with Zod.
2. Setup Axios interceptor for JWT.
3. Hook forms to real API.
4. Setup Zustand for global auth state.
**Validation:** Verify invalid emails are rejected locally.
**Handoff:** Auth state works, Member 3 can now build private pages.
**Completion Checklist:**
- [x] Zod validation working.
- [ ] JWT stored securely.

### Member 3 — Frontend Data & Dashboards
**Responsibilities:** Profile, Directory, and Leaderboard UI.
**Execution Order:** Stage 2 → Stage 3 → Stage 4
**Tasks:**
1. Build Profile UI with mock data.
2. Connect to `GET /api/users`.
3. Build Leaderboard table connecting to backend data.
**Validation:** Table sorts correctly by rating.
**Handoff:** UI ready for production deployment.
**Completion Checklist:**
- [ ] Data renders from backend.
- [ ] Handles undefined/null ratings without crashing.

### Member 4 — Backend Security & Auth
**Responsibilities:** Spring Security, JWT, Auth Controllers.
**Execution Order:** Stage 1 → Stage 2
**Tasks:**
1. Create `User` Entity (with `ROLE_USER` / `ROLE_ADMIN`).
2. Setup `SecurityFilterChain` (restrict certain endpoints to Admins).
3. Implement `JwtUtils`.
4. Create `POST /register` and `POST /login`.
**Validation:** Hit endpoints with Postman.
**Handoff:** Swagger UI updated for Member 2.
**Completion Checklist:**
- [x] Passwords hashed.
- [x] JWT validates correctly.

### Member 5 — Backend Content APIs
**Responsibilities:** REST APIs and DTOs (implemented as Java Records).
**Execution Order:** Stage 2
**Tasks:**
1. Create `UserResponseDto`.
2. Create `UserService`.
3. Create `GET /api/users` and `PUT /api/users/{id}`.
**Validation:** Ensure password hashes NEVER return in JSON.
**Handoff:** Endpoints ready for Member 3.
**Completion Checklist:**
- [ ] DTOs implemented as immutable Java Records.
- [ ] Pagination added.

### Member 6 — Team Leader (DevOps, DB & Integrations)
**Responsibilities:** Code Reviews, Docker, Codeforces Sync, Deploy.
**Execution Order:** Stage 0 → Stage 3 → Stage 4 → Stage 5
**Tasks:**
1. Initialize Repo and Docker.
2. Review all PRs.
3. Write `@Scheduled` job for Codeforces API.
4. Deploy to Vercel/Render.
**Validation:** Full End-to-End user flow tested personally.
**Handoff:** Present working URL to the club.
**Completion Checklist:**
- [ ] CI/CD pipeline green.
- [ ] Production environment stable.

---

## 21. Master Execution Timeline

```text
DAY / STAGE
│
├── Stage 0 (Days 1-2)
│   ├── Member 6: Git Init, Docker Postgres
│   └── All: API Contract Finalization
│
├── Stage 1 (Days 3-5)
│   ├── Member 1: Next.js Layout Shell
│   └── Member 4: Spring Boot & Security Config
│
├── Stage 2 (Days 6-10) [PARALLEL]
│   ├── Member 2: Auth Forms (Mock)
│   ├── Member 3: Profile Dashboards (Mock)
│   ├── Member 4: Auth APIs
│   └── Member 5: Data APIs
│
├── Synchronization Point (Day 11)
│       ↓
│    Member 6: Postman QA Gate
│       ↓
├── Stage 3 (Days 12-15)
│   ├── Member 2: Axios Integration
│   └── Member 3: Dashboard Integration
│
├── Stage 4 (Days 16-20)
│   ├── Member 6: Codeforces Cron Job
│   └── Member 3: Leaderboard Integration
│
└── Stage 5 (Day 21)
        ↓
     Final Validation
        ↓
     Production Deployment
```

---

## 22. Phase 1 Final Acceptance Checklist

### Functional
- [ ] User can register and login.
- [ ] User can edit profile and add CP handles.
- [ ] Members directory is visible.
- [ ] Leaderboard syncs with Codeforces and sorts correctly.

### Technical
- [ ] Frontend uses Next.js App Router and Zustand.
- [ ] Backend strictly returns DTOs.
- [ ] Global Exception Handler intercepts all Java errors.

### Security
- [x] Stateless JWT authentication enforced.
- [x] Passwords hashed with BCrypt.
- [x] CORS policies restrict unauthorized domains.
- [x] Secrets isolated in `.env`.

### Git/Code Quality
- [ ] `main` branch is completely green.
- [ ] All PRs were reviewed and approved.

## 23. Phase 1 Sign-Off Procedure

**STAGE 5 EXIT GATE**

Phase 1 may proceed to production and Sign-Off only if:
1. Member 6 successfully runs a manual E2E test on the production URLs without errors.
2. Swagger documentation perfectly matches the live behavior.
3. The Database connection string points securely to the managed production database.

If ANY critical condition fails:
`STOP → Diagnosis → Fix Branch → Merge → Re-deploy → Re-validate → Proceed to Phase 2`
