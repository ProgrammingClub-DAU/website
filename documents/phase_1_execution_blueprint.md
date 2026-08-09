# PHASE 1 EXECUTION BLUEPRINT

## 1. Phase 1 Objective
To develop, integrate, test, and deploy a functional, production-ready Minimum Viable Product (MVP) for the Competitive Programming (CP) Club. The MVP provides secure user authentication (with Admin/Normal roles), public user profiles, a club member directory, static informational pages, and a dynamic leaderboard synchronized with live Codeforces ratings.

## 2. Phase 1 Scope
*   **Infrastructure:** Dockerized PostgreSQL database, Vercel frontend deployment, Render/AWS backend deployment.
*   **Authentication:** JWT-based stateless authentication, BCrypt password hashing, Spring Security.
*   **Roles:** `ROLE_USER` (can view profiles/leaderboards) and `ROLE_ADMIN` (can manage content in later phases).
*   **User Management:** Registration, login, and updating Codeforces handles on user profiles.
*   **Frontend UI:** Responsive, dark-mode Next.js (App Router) frontend using Tailwind CSS.
*   **Leaderboard:** Spring Boot cron job fetching data from the Codeforces API, displayed in a dynamic frontend table.
*   **Static Pages:** Home, About Us, Hall of Fame.

## 3. Out of Scope
*   Integration with LeetCode, CodeChef, or AtCoder (Phase 2).
*   Weekly Winners / Digital Badges (Phase 2).
*   Event Management / RSVPs (Phase 3).
*   Codeforces Mashup / Internal Club Contest tracking (Phase 3).
*   Problem of the Day (POTD) / Streaks (Phase 4).
*   Real-Time 1v1 Battles / WebSockets (Phase 4).
*   Native Forums / Chat channels (Phase 4).

## 4. Current Repository State
**ANALYSIS RESULT:** Stage 0 (Initialization) is **Complete**.
*   **Frontend:** Next.js + Tailwind CSS initialized in `frontend/`.
*   **Backend:** Java 17 + Spring Boot 4.1.0 boilerplate initialized in `backend/`.
*   **Database:** `docker-compose.yml` for PostgreSQL created at root.
*   **Git:** Repository initialized, root `.gitignore` and `README.md` created, and all boilerplate committed to `main` branch.
*   **Missing for Phase 1:** All actual business logic, UI components, API endpoints, database entities, and external API integrations are unwritten and ready for development.

## 4.1 Database Design (Phase 1)

> **Owner: Member 4** (Backend Security)

In Phase 1 there is **exactly one table**. Member 4 defines it as a JPA entity (`User.java`) and Hibernate auto-creates the table when the Spring Boot application starts.

### `users` table

| Column | Type | Constraint | Notes |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | |
| `name` | `VARCHAR(255)` | NOT NULL | |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Used for login |
| `password` | `VARCHAR(255)` | NOT NULL | Stored as BCrypt hash — NEVER plaintext |
| `codeforces_handle` | `VARCHAR(100)` | NULLABLE | Set by user after registration |
| `rating` | `INTEGER` | NULLABLE | Written by Member 6's cron job |
| `role` | `VARCHAR(50)` | NOT NULL, DEFAULT 'ROLE_USER' | Either `ROLE_USER` or `ROLE_ADMIN` |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Auto-managed by `@CreatedDate` (Spring JPA Auditing) |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Auto-managed by `@LastModifiedDate` (Spring JPA Auditing) |

### Who touches the DB in Phase 1?

| Member | DB Role | Action |
| :--- | :--- | :--- |
| **Member 4** | Schema Owner | Creates `User.java` entity — defines all columns |
| **Member 5** | Query Writer | Writes `UserRepository` methods (`findByEmail`, `findAll`, etc.) |
| **Member 6** | Data Writer | Cron job updates the `rating` column from Codeforces API |
| **Member 1, 2, 3** | No DB access | Frontend only — never touch the database directly |

> **Note:** No other tables are created in Phase 1. Tables for blogs, events, and contests belong to Phases 2, 3, and 4.

## 5. Team & Ownership

| Member | Phase 1 Role | Owns | Depends On | Blocks | Deliverables |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Member 1** | Frontend UI Architect | Static Pages, Layouts, CSS | None | Member 2 & 3 (UI Shell) | `layout.tsx`, `page.tsx`, Theme configs |
| **Member 2** | Frontend Auth Engineer | Login/Register Forms, Zustand | Member 4 (APIs) | Member 3 (Auth State) | `login/page.tsx`, `axios.ts`, Zustand store |
| **Member 3** | Frontend Data Engineer | Profile UI, Leaderboard UI | Member 5 (APIs), Member 2 | Deployment | `profile/page.tsx`, `leaderboard/page.tsx` |
| **Member 4** | Backend Security | Spring Security, JWT, Auth APIs | None | Member 2 & 5 (Auth) | `User.java`, `JwtUtils`, `AuthController` |
| **Member 5** | Backend API Engineer | REST APIs, DTOs, Queries | Member 4 (Entity) | Member 3 (Data) | `UserController`, `UserResponseDto` |
| **Member 6** | Team Lead (DevOps) | Codeforces Sync, Docker, QA | Member 5 (DB) | Leaderboard UI | `CodeforcesSyncService`, Deployments, PRs |

## 6. Environment Setup
**Day 0 / Before Coding Checklist for ALL Members:**
- [ ] Install **Node.js 20+** (LTS) and **Java 17+**.
- [ ] Install **Docker Desktop** (must be running).
- [ ] Clone the repository: `git clone https://github.com/ProgrammingClub-DAU/website.git`.
- [ ] Start Database: Run `docker-compose up -d` in the root folder. Verify port 5432 is active.
- [ ] Start Backend: Open terminal in `backend/`, run `.\mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run` (Mac/Linux). Verify port 8080.
- [ ] Start Frontend: Open terminal in `frontend/`, run `npm install`, then `npm run dev`. Verify port 3000.
- [ ] Configure Git: `git config user.name "Your Name"` and `git config user.email "your.email@example.com"`.

## 7. Master Execution Order

```text
Phase 1
│
├── Stage 0 — Repository & Environment (COMPLETED)
│
├── Stage 1 — Foundation (UI Shell & DB Entity)
│
├── Stage 2 — Auth API & Auth UI Forms
│
├── Stage 3 — Integration (JWT + Zustand)
│
├── Stage 4 — Data APIs & Profile Dashboards
│
├── Stage 5 — Codeforces Integration
│
└── Stage 6 — Deployment & Sign-off
```

## 8. Dependency Graph

```text
[M1: UI Layouts] ─────────────┐
                              ↓
[M4: User Entity & Security] → [M5: Data APIs] ───→ [M3: Profiles & Leaderboard UI]
        ↓                                                    ↑
[M4: Auth APIs] ─────────────→ [M2: Integration] ────────────┘
        ↓
[M2: Auth Forms] ────────────┘

[M6: CF Sync Job] ───────────────────────────────────────────↗
```

## 9. Detailed Stage-by-Stage Execution

### Stage 1: Foundation (UI Shell & DB Entity)
**Tasks:**
*   **P1-001 (Member 1):** Create base Next.js layout (`Navbar`, `Footer`) and `globals.css` theming.
*   **P1-002 (Member 4):** Create `User.java` JPA entity (include `ROLE_USER` / `ROLE_ADMIN` enum, email, password, codeforces_handle, rating). Create `UserRepository`.

### Stage 2: Auth API & Auth UI Forms
*   **P1-003 (Member 4):** Configure `SecurityFilterChain`. Implement `JwtUtils`. Create `/api/auth/register` and `/api/auth/login`. Verify via Postman.
*   **P1-004 (Member 2):** Build `/login` and `/register` Next.js pages using `react-hook-form` + `zod`. (Use `console.log` for submission temporarily).

### Stage 3: Integration (JWT + Zustand)
*   **P1-005 (Member 2):** Create Axios interceptor (`src/lib/axios.ts`) to inject JWT headers. Hook forms to Member 4's APIs. Setup Zustand store to hold `{ user: User | null, role: string }`.

### Stage 4: Data APIs & Profile Dashboards
*   **P1-006 (Member 5):** Create `UserController` returning `UserResponseDto` (NEVER return raw entities/passwords). Create `GET /api/users` and `PUT /api/users/{id}/handle`.
*   **P1-007 (Member 3):** Build `/profile` page. Fetch user data via Axios. Allow updating Codeforces handle. Build `/members` directory page fetching all users.

### Stage 5: Codeforces Integration
*   **P1-008 (Member 6):** Create Spring `@Scheduled` cron job to fetch ratings from `https://codeforces.com/api/user.info` for all users in DB. Save to DB.
*   **P1-009 (Member 3):** Build `/leaderboard` UI. Fetch sorted users from backend and render table.

### Stage 6: Deployment & Sign-off
*   **P1-010 (Member 6):** Deploy Frontend to Vercel. Deploy Backend to Render. Provision Production Postgres.

## 10. Parallel Workstreams
**Parallel Group A (Stage 1 & 2):**
*   Member 1 (Layouts), Member 2 (Forms), and Member 4 (Security APIs) can work simultaneously.
*   *Why?* Member 1 touches CSS/Layout files. Member 2 touches isolated frontend pages (`/login`). Member 4 touches backend Java files. No file conflicts.

**Parallel Group B (Stage 4 & 5):**
*   Member 5 (Data APIs) and Member 6 (CF Sync Job) can work simultaneously on the backend.
*   *Why?* Member 5 builds Controllers. Member 6 builds a background Service. Both rely on Member 4's `User` entity, which is already merged.

## 11. Contracts & Interfaces

### CONTRACT P1-C01: Authentication API
*   **POST `/api/auth/register`**
    *   *Req:* `{ "email": "x@x.com", "password": "pass", "name": "John" }`
    *   *Res (200):* `{ "token": "jwt_string_here", "role": "ROLE_USER" }`
    *   *Res (400):* `{ "error": "Email already in use" }`

### CONTRACT P1-C02: User Data API
*   **GET `/api/users/{id}`**
    *   *Headers:* `Authorization: Bearer <token>`
    *   *Res (200):* `{ "id": 1, "name": "John", "codeforcesHandle": "tourist", "rating": 3900 }` *(Note: No password hash returned!)*

## 12. File-Level Ownership

| File/Directory | Owner | Purpose |
| :--- | :--- | :--- |
| `frontend/src/app/layout.tsx` | Member 1 | Global UI shell, Navbar |
| `frontend/src/app/(auth)/` | Member 2 | Login/Register routes and forms |
| `frontend/src/store/` | Member 2 | Zustand global state management |
| `frontend/src/app/(dashboard)/` | Member 3 | Profile, Leaderboard, Members routes |
| `backend/src/.../security/` | Member 4 | JWT Filters, Security Config |
| `backend/src/.../entity/User.java` | Member 4 | Database schema definition |
| `backend/src/.../controller/` | Member 5 | REST API Endpoints |
| `backend/src/.../service/CFSync.java` | Member 6 | Cron job logic for Codeforces API |

## 13. Git/GitHub Workflow
1.  **Sync:** `git pull origin main`
2.  **Branch:** `git checkout -b feature/M[X]-[feature-name]` (e.g., `feature/M2-login-forms`)
3.  **Develop:** Write code.
4.  **Test:** Run local compilation (`npm run build` or `mvn compile`).
5.  **Commit:** `git add .` -> `git commit -m "feat: added login form validation"`
6.  **Push:** `git push -u origin feature/M2-login-forms`
7.  **PR:** Open Pull Request targeting `main`.
8.  **Review:** Member 6 reviews. (No self-merging).
9.  **Merge:** Squash and merge into `main`. Delete branch.

## 14. Testing Strategy
*   **Backend Unit Testing:** Member 4 writes JUnit tests for `JwtUtils` (token generation/validation). Member 5 tests DTO mapping.
*   **Frontend Testing:** Member 2 verifies `zod` validation rejects weak passwords and invalid emails before API calls.
*   **API Testing:** Member 6 maintains a Postman Collection.
*   **Command (Backend):** `./mvnw test`
*   **Command (Frontend):** `npm run build` (Ensures no TypeScript errors before PR).

## 15. Integration Strategy
1.  **Backend Core:** Member 4 merges Security & DB Entity first.
2.  **Backend APIs:** Member 5 merges REST Controllers.
3.  **Frontend State:** Member 2 merges Axios + Zustand auth flow.
4.  **Frontend UI:** Member 3 merges Profile and Leaderboard UIs fetching from the live backend.
*   **Conflict Resolution:** If Member 3 needs an API change, they request Member 5 to update the DTO on a backend branch first.

## 16. Blockers & Recovery Procedures
*   **Blocker:** Codeforces API is down or rate-limiting.
    *   **Handler:** Member 6.
    *   **Action:** Wrap the cron job in a `try/catch`. Log the error. Do NOT crash the backend. Frontend Leaderboard should gracefully display the last known ratings.
*   **Blocker:** Member 3 needs data, but Member 5's API isn't merged yet.
    *   **Action:** Member 3 creates a `mockUsers.json` file to build the UI against, swapping to Axios only when Member 5 merges.

## 17. Milestones
*   **M1 — Foundation Ready:** Next.js layout and Spring Security merged.
*   **M2 — APIs Live:** Backend fully tested via Postman.
*   **M3 — End-to-End Auth:** Users can register and login on the frontend.
*   **M4 — Codeforces Sync:** Cron job successfully updates DB automatically.
*   **M5 — MVP Complete:** Production deployment successful.

## 18. Member-Wise Checklists

### Member 1 Checklist
- [x] Checkout `feature/M1-layout`
- [x] Configure `tailwind.config.ts` / CSS theme
- [x] Build `<Navbar>` and `<Footer>`
- [x] Open PR, request Member 6 review

### Member 2 Checklist
- [ ] Checkout `feature/M2-auth-forms`
- [ ] Build React forms with Zod
- [ ] Wait for P1-C01 Contract to be merged
- [ ] Implement Axios interceptor and Zustand store
- [ ] Open PR, request Member 6 review

### Member 3 Checklist
- [ ] Checkout `feature/M3-dashboards`
- [ ] Build UI for Profile using mock data
- [ ] Wait for Member 5's APIs
- [ ] Connect Profile and Leaderboard to Axios
- [ ] Open PR, request Member 6 review

### Member 4 Checklist
- [ ] Checkout `feature/M4-security`
- [x] Build `User` entity with Roles *(Done by M5)*
- [ ] Build `JwtUtils` and `SecurityFilterChain`
- [ ] Open PR, request Member 6 review

### Member 5 Checklist
- [x] Checkout `feature/M5-data-apis` (Wait for Member 4's `User.java`)
- [x] Build `UserResponseDto` and `UserController`
- [x] Open PR, request Member 6 review

### Member 6 Checklist (Leader)
- [ ] Review and merge all PRs daily.
- [ ] Checkout `feature/M6-cf-sync`
- [ ] Implement `@Scheduled` Codeforces API fetcher
- [x] Configure Render and Vercel deployments

## 19. Master Task Tracker

| ID | Task | Owner | Depends On | Parallel With | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| P1-001 | Build UI Layout Shell | M1 | None | P1-002, P1-004 | P1-007 | **Done** |
| P1-002 | `User` Entity & JPA Repo | M4 *(Done by M5)* | None | P1-001, P1-004 | P1-003, P1-006, P1-008 | **Done** |
| P1-003 | Auth APIs & JWT Config | M4 | P1-002 | P1-004 | P1-005 | Todo |
| P1-004 | React Auth Forms (Mock) | M2 | None | P1-001, P1-002 | P1-005 | Todo |
| P1-005 | Auth Integration (Axios/Zustand) | M2 | P1-003, P1-004 | None | P1-007 | Todo |
| P1-006 | Data APIs & DTOs | M5 | P1-002 | P1-008 | P1-007, P1-009 | **Done** |
| P1-007 | Profile & Members UI | M3 | P1-001, P1-005, P1-006 | P1-008 | P1-010 | Todo |
| P1-008 | Codeforces Cron Sync | M6 | P1-002 | P1-006, P1-007 | P1-009 | Todo |
| P1-009 | Leaderboard UI | M3 | P1-008, P1-006 | None | P1-010 | Todo |
| P1-010 | Production Deployment | M6 | All Tasks | None | Phase 1 Sign-Off | **Done** |

## 20. Phase 1 Sign-Off Checklist
*   [ ] All `Todo` items in Master Task Tracker are complete.
*   [ ] `main` branch compiles locally on a clean clone.
*   [ ] JWT authorization headers are securely functioning.
*   [ ] Admin endpoints successfully reject Normal users (403 Forbidden).
*   [ ] Live deployment URLs are accessible.

## 21. Phase 1 Completion Criteria
Phase 1 is complete ONLY when Member 6 successfully registers an account on the live production URL, updates their Codeforces handle on their profile, and sees their rating automatically appear on the public leaderboard.
