# Phase 2 -- Senior Engineer Implementation Plan (v3 -- Updated)
**Branch model:** every member branches from `main` and raises PRs into `main`, merged one at a time by M6.
**Duration:** 3 sprints -- ~3 weeks
**Team:** 6 members
**Stack:** Spring Boot 4.1 -- PostgreSQL -- Next.js 15 -- Cloudinary -- Apache POI

---

## [START] How to Get Started -- Starter Prompts for Every Member

> [!IMPORTANT]
> Copy your prompt below and paste it into your AI coding assistant **while your project folder is open**. It will read the codebase, understand the plan, and guide you step by step.

---

### [M6] Member 6 -- Team Lead & DevOps
**Branch:** `feature/M6-phase2-stage0`

```
I am Member 6 (Team Lead) on the CP Club Website project.
I need to implement Stage 0 of Phase 2.

Please do the following:
1. Read the full Phase 2 plan at: documents/phase_2_execution_playbook.md
2. Read the existing DB migration: backend/src/main/resources/db/migration/V1__init.sql
3. Read the User entity: backend/src/main/java/com/cpclub/backend/user/entity/User.java

Then implement in this exact order:
- Create migration files V2 through V7 as specified in Section 4 of the plan
- Create the ClubRole enum and EventStatus enum as Java enums
- Update User.java with all 10 new fields
- Create all new JPA entities: Event, EventAttendee, EventPhoto, MemberGalleryPhoto, WeeklySnapshot

Rules:
- All new DB columns must be nullable (backward compatible)
- @Column names must exactly match SQL column names or ddl-auto: validate will fail on startup
- Run ./mvnw.cmd clean test at the end and fix any failures before raising a PR
```

> [!NOTE]
> **Stage 0 is complete** and merged into `main` (PR #50). It also
> carried a fix worth knowing about: Flyway had never actually run in this repo.
> Boot 4 moved `FlywayAutoConfiguration` into a separate
> `org.springframework.boot:spring-boot-flyway` module that was never declared,
> so `flyway-core` sat inert. Everyone must run `./mvnw clean install` after
> pulling, and recreate any local database built before the migrations
> (`docker compose down -v && docker compose up -d`).

**M6 also owned the `snapshot/` package in Stage 1** (Section 6.6), moved off M5
to shorten the critical path. **Stage 1C is complete** — repository, DTO,
service with the Monday cron, controller, and 7 unit tests, merged via PR #62.

---

### [M4] Member 4 -- Backend Security
**Branch:** `feature/M4-phase2-security`

```
I am Member 4 (Backend Security Engineer) on the CP Club Website project.
I need to implement Stage 1A of Phase 2.

IMPORTANT: Stage 0 (M6's work) must be merged into main before I start. Branch from main.

Please do the following:
1. Read the full Phase 2 plan at: documents/phase_2_execution_playbook.md -- focus on Section 5
2. Read SecurityConfig: backend/src/main/java/com/cpclub/backend/security/config/SecurityConfig.java
3. Read UserController: backend/src/main/java/com/cpclub/backend/user/controller/UserController.java

Then implement:
1. Add 8 new security rules to SecurityConfig.java in the correct order (public GET rules before Admin wildcard rules -- Spring Security evaluates top-to-bottom)
2. Add GET /api/users/{id}/lookup to UserController with @PreAuthorize("hasRole('ADMIN')")
3. Add PUT /api/users/{id}/club-role to UserController with @PreAuthorize("hasRole('ADMIN')")

Then, in the same branch, implement the gallery package described in Section 6.5:
4. Create gallery/ package -- MemberGalleryRepository, MemberGalleryPhotoDto,
   AddMemberPhotoRequest, MemberGalleryService, GalleryController
   (the MemberGalleryPhoto entity already exists from Stage 0)

The gallery package has no dependency on M5's work, so it can be built in
parallel with Stage 1B without coordination.

Run ./mvnw.cmd clean test and fix any failures before raising a PR into main.
```

---

### [M5] Member 5 -- Backend Data & APIs
**Branch:** `feature/M5-phase2-backend`

```
I am Member 5 (Backend Data & API Engineer) on the CP Club Website project.
I need to implement Stage 1B of Phase 2. This is the largest backend task.

IMPORTANT: Stage 0 (M6's work) must be merged into main before I start. Branch from main.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md -- focus on Sections 6.1 to 6.7
2. Read these files for context:
   - backend/src/main/java/com/cpclub/backend/user/dto/UserProfileUpdateRequest.java
   - backend/src/main/java/com/cpclub/backend/user/service/UserService.java
   - backend/src/main/java/com/cpclub/backend/user/dto/UserResponseDto.java
   - backend/src/main/java/com/cpclub/backend/codeforces/service/CodeforcesSyncService.java
   - backend/src/main/java/com/cpclub/backend/leaderboard/service/LeaderboardService.java
   - backend/pom.xml

Then implement in this order:
1. Update UserProfileUpdateRequest, UserResponseDto -- add all new fields
2. Create UserLookupDto, UpdateClubRoleRequest -- new DTOs
3. Update UserService -- 3 new methods (updateClubRole, lookupUserById, update updateProfile)
4. Update UserRepository -- 3 new query methods
5. Update LeaderboardService -- platform and clubRole filter support
6. Create leetcode/ package -- LeetCodeGraphQLResponse + LeetCodeSyncService
7. Update CodeforcesSyncService -- call LeetCode bulk sync after CF sync
8. Create event/ package -- repos, all DTOs, EventService (with all guard clauses), EventExportService (Apache POI), EventController
   (the Event, EventAttendee and EventPhoto entities already exist from Stage 0)
9. Add Apache POI dependency to pom.xml

NOT yours -- reassigned to balance the critical path:
- gallery/ package  -> M4, alongside Stage 1A security
- snapshot/ package -> M6

You DO still own leetcode/, because your UserService.updateProfile() and
CodeforcesSyncService both call into it.

Run ./mvnw.cmd clean test and fix all failures before raising a PR.
```

---

### [M1] Member 1 -- Frontend UI/UX
**Branch:** `feature/M1-phase2-ui`

```
I am Member 1 (Frontend UI/UX Architect) on the CP Club Website project.
I need to implement Stage 2A of Phase 2 -- new UI components and page layouts.

IMPORTANT: Stage 1 (M4 + M5 work) must be merged into main before I start. Branch from main.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md -- focus on Section 7
2. Look at existing components for the design language: frontend/src/components/site/ and frontend/src/app/events/page.tsx

Then create in this order:
1. components/ui/club-role-badge.tsx -- small colored badge (Convenor=gold, Core=blue, Batch Rep=green, Ex-*=grey outline)
2. components/ui/data-table.tsx -- generic table with loading skeleton and empty state
3. components/site/event-card.tsx -- event card with cover image, title, date, location, status badge. Full card is a Link.
4. components/site/event-photo-grid.tsx -- responsive photo grid with lightbox
5. components/site/member-gallery-grid.tsx -- same grid for batch member photos
6. components/site/admin-tabs.tsx -- tab nav with Members, Events, Galleries tabs
7. Modify app/events/page.tsx -- replace hardcoded placeholders with server-side fetch from GET /api/events/upcoming
8. Create app/events/[id]/page.tsx -- event detail page
9. Create app/gallery/page.tsx -- member gallery with batch year dropdown filter
10. Create shell app/(dashboard)/admin/page.tsx -- layout only, M3 fills data

Match the dark-mode design: glass-panel, rounded-panel, Eyebrow, Section components.
Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

### [M2] Member 2 -- Frontend Auth & State
**Branch:** `feature/M2-phase2-auth`

```
I am Member 2 (Frontend Auth & Logic Engineer) on the CP Club Website project.
I need to implement Stage 2B of Phase 2 -- TypeScript types, store updates, and all API service files.

IMPORTANT: Stage 1 must be merged into main before I start. Branch from main.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md -- focus on Section 8
2. Read these files:
   - frontend/src/store/auth.ts
   - frontend/src/lib/services/dashboard.ts
   - frontend/src/lib/axios.ts

Then implement in this order:
1. Update store/auth.ts -- add clubRole, batchYear, leetcodeHandle, phoneNumber, avatarUrl to the User interface
2. Update/create types/api.ts -- add Event, EventDetail, EventAttendee, EventPhoto, UserLookup, MemberGalleryPhoto interfaces and ClubRole string union type
3. Create lib/services/events.ts -- 17 functions covering all event, attendee, photo, and user lookup API calls. The exportAttendees function must use responseType: 'blob'.
4. Create lib/services/gallery.ts -- 4 functions for member gallery
5. Update lib/services/dashboard.ts -- mapUserToProfile() must map all new fields using ?? null fallback
6. Update lib/services/leaderboard.ts -- pass ?platform= and ?filter= query params

Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

### [M3] Member 3 -- Frontend Dashboards & Data
**Branch:** `feature/M3-phase2-dashboards`

```
I am Member 3 (Frontend Dashboards Engineer) on the CP Club Website project.
I need to implement Stage 2C of Phase 2 -- all dashboard pages and data-heavy features.

IMPORTANT: Member 1 AND Member 2 PRs must both be merged into main before I start. Branch from main only after both are merged.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md -- focus on Section 9
2. Read these files for context:
   - frontend/src/components/site/profile-dashboard.tsx
   - frontend/src/app/(dashboard)/leaderboard/page.tsx
   - frontend/src/app/(dashboard)/profile/page.tsx

Then implement in this order:

1. Rewrite profile-dashboard.tsx:
   - Avatar: show Cloudinary image if avatarUrl exists. If owner: show Cloudinary Upload Widget button. On upload success auto-save the URL.
   - Club role badge below member name using the ClubRoleBadge component from M1
   - Platform icon links row: CodeChef, AtCoder, GitHub, LinkedIn
   - Full "Edit Profile" panel (owner-only): name, phone (required), CF handle, LeetCode handle, CodeChef URL, AtCoder URL, GitHub URL, LinkedIn URL. Phone shown masked to visitors.
   - Two recharts LineCharts: CF rating history + LeetCode rating history from /api/snapshots

2. Fill in app/(dashboard)/admin/page.tsx (3 tabs):
   - Tab 1 Members: DataTable with all columns. Inline club role dropdown per row. Promote/Demote + Delete actions.
   - Tab 2 Events: create form + event list with status actions + Manage Attendees link per event
   - Tab 3 Galleries: member gallery upload (batchYear + Cloudinary widget + caption) + event gallery upload (select event + Cloudinary widget)

3. Create app/(dashboard)/admin/events/[id]/page.tsx:
   - Left panel: Student ID input -> Search -> preview card (show warning if no phone) -> Add to Event button
   - Right panel: attendee DataTable + Export to Excel button (blob download)

4. Update app/(dashboard)/leaderboard/page.tsx:
   - Add platform pill toggles: Codeforces | LeetCode
   - Add club filter pill toggles: All | Core | Batch Rep | Students
   - Add Club Role column with ClubRoleBadge

Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

## Section 1 -- Locked Decisions & Rationale

| Decision | Choice | Why |
|---|---|---|
| CodeChef / AtCoder sync | **No sync. Link only.** | No stable public API. Scraping breaks on DOM changes. |
| LeetCode sync | **Full GraphQL API sync.** JIT on handle save + every 6h cron. | Reliable public endpoint. |
| Avatar | **Cloudinary Upload Widget.** URL saved to DB. | Eliminates binary blobs in PostgreSQL. |
| Phone number | **Requested, not enforced.** UI marks it required; the backend warns but never blocks an event add. | Enforcing it blocked every pre-Phase-2 member (all have `phone_number = NULL`) from being marked present, and only the member themselves can fix it. Attendance is the official record; a sparse phone column is the lesser cost. |
| Event attendance | **Admin-only.** Admin searches by User ID; details auto-fill from DB. | Official record, not self-controlled. |
| Excel export | **Apache POI XSSF.** Server-side `.xlsx` streamed as binary. | Universal format for club admins. |
| Soft delete for events | Events are **deactivated**, never hard deleted. | Preserves attendance + gallery history. |
| `clubRole` vs `role` | **Two separate fields.** `role` = platform permission (Admin/User). `clubRole` = club position (Core, Convenor, etc.). | Conflating them causes security issues -- a Batch Rep should not have admin API access. |
| Gallery storage | **Cloudinary.** Admin uploads image via Upload Widget; URL stored in DB. | Consistent with avatar approach. No binary in DB. |

---

## Section 2 -- New Features Added in This Version

The following features are **new additions** on top of the original Phase 2 scope:

1. **Club Role System** -- `clubRole` enum column on `users`. Admin can assign any member a club position. Leaderboard filters (All, Core, Batch Rep, Students) become data-driven.
2. **Admin Event Management** -- Admin creates, edits, and publishes upcoming events directly from the website (no hardcoding).
3. **Event Photo Gallery** -- Admin uploads event photos (past events) to Cloudinary; displayed in a public gallery per event.
4. **Club Member Photo Gallery (Batch-wise)** -- Admin uploads group/batch photos; displayed publicly, filterable by batch year.
5. **Completed Events** -- Past events shown publicly with date, description, location, and their photo gallery.

---

## Section 3 -- Implementation Stages & Dependencies

```
STAGE 0 --- DB Migrations + Entity Layer            [DONE - PR #50]
              Owner: M6 -- PR: phase2/stage-0
              Must merge before ANY other branch starts.
                    |
        +-----------+-----------+-----------------+
        v                       v                 v
STAGE 1A - Security+Gallery  STAGE 1B - Data   STAGE 1C - Snapshots
  Owner: M4                    Owner: M5         Owner: M6
  PR: phase2/backend-security  PR: phase2/       PR: phase2/snapshots
                                  backend-data
  ~3-4 days                    ~4-5 days         ~1 day
  All three run in parallel. None of them calls into another.
        |                       |                 |
        +-----------+-----------+-----------------+
                    v
     All Stage 1 PRs merged into main
                    |
        +-----------+-----------+
        v           v           v
STAGE 2A         STAGE 2B    STAGE 2C
Frontend UI      Auth/State  Dashboards
Owner: M1        Owner: M2   Owner: M3
                             (depends on M1 + M2 first)
        +-----------+-----------+
                    v
STAGE 3 --- Integration, Tests, Polish, Deploy
              Owner: M6 -- PR: phase2/integration
```

> [!IMPORTANT]
> Every member branches off `main` and raises their PR back into `main`. M6
> reviews and merges them one at a time. There is no long-lived integration
> branch.
>
> **This replaces the `feature/phase-2` model the v3 plan described.** That
> branch existed, carried Stage 0 and Stage 1C, and has been merged into `main`
> and deleted. Two reasons for the change:
>
> 1. **CI only runs on `main`.** Both workflows in `.github/workflows/` are
>    configured with `branches: [ main ]`, so a PR into `feature/phase-2` ran no
>    backend tests and no frontend build — verified on PR #62, which got only
>    Vercel checks. Six people merging for three weeks with no automated
>    verification was the larger risk by far.
> 2. It matches how the team already works, so nobody branches from the wrong
>    place by habit.
>
> The trade-off accepted: `main` carries backend endpoints before the frontend
> wires them up. That is safe here — unreferenced endpoints are unreachable from
> the UI, and every Phase 2 migration column is nullable, so the schema stays
> backward compatible with whatever is currently deployed.

---

## Section 4 -- Stage 0: Database Migrations & Entity Layer
**Owner: Member 6 (Team Lead)**
**PR: `phase2/stage-0-migrations`**
**Estimated time: 2 days**
**Blocks: All other members**

---

### 4.1 Migration V2 -- Extend `users` Table

**File:** `V2__extend_user_profile.sql`

Add the following **nullable** columns to `users`:

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `avatar_url` | VARCHAR(512) | nullable | Cloudinary profile photo |
| `phone_number` | VARCHAR(20) | nullable | Mandatory in UI; nullable in DB for backward compat |
| `leetcode_handle` | VARCHAR(255) | UNIQUE, nullable | For LeetCode sync |
| `leetcode_rating` | INTEGER | nullable | Populated by sync |
| `codechef_url` | VARCHAR(512) | nullable | Link only, no sync |
| `atcoder_url` | VARCHAR(512) | nullable | Link only, no sync |
| `github_url` | VARCHAR(512) | nullable | Social link |
| `linkedin_url` | VARCHAR(512) | nullable | Social link |
| `club_role` | VARCHAR(50) | nullable, CHECK constraint | Club position (see enum below) |
| `batch_year` | INTEGER | nullable | e.g. 2023, 2024 -- used for batch gallery filtering |

**`club_role` allowed values (CHECK constraint):**
`CONVENOR`, `DEPUTY_CONVENOR`, `CORE`, `ASSOCIATE_CORE`, `BATCH_REPRESENTATIVE`, `EX_PC_MEMBER`, `EX_CORE`, `EX_CDC`, `STUDENT`

> [!NOTE]
> `STUDENT` is the default when `club_role` is set but the member holds no special position. `null` means unassigned (shown as "Member" on the frontend). This is distinct from `role` (ROLE_ADMIN / ROLE_USER) which controls API access.

**Indexes to add:**
- `CREATE INDEX idx_leetcode_handle ON users(leetcode_handle)`
- `CREATE INDEX idx_club_role ON users(club_role)` -- for leaderboard filter queries

---

### 4.2 Migration V3 -- Create `events` Table

**File:** `V3__create_events.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | Auto-increment |
| `title` | VARCHAR(255) | NOT NULL | Event name |
| `description` | TEXT | nullable | Long description |
| `event_date` | TIMESTAMP | NOT NULL | When the event happens |
| `location` | VARCHAR(255) | NOT NULL | Physical or online |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'UPCOMING' | `UPCOMING`, `COMPLETED`, `CANCELLED` |
| `cover_image_url` | VARCHAR(512) | nullable | Cloudinary URL for event banner |
| `created_by` | BIGINT | FK -> users.id NOT NULL | Admin who created it |
| `created_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | Audit |
| `updated_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | Audit |

> [!NOTE]
> `status` replaces the earlier `is_active` boolean. It gives 3 states: upcoming (public, accepting attendance), completed (public, read-only, gallery shown), cancelled (hidden from public).

---

### 4.3 Migration V4 -- Create `event_attendees` Table

**File:** `V4__create_event_attendees.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `event_id` | BIGINT | FK -> events.id ON DELETE CASCADE, NOT NULL | |
| `user_id` | BIGINT | FK -> users.id ON DELETE CASCADE, NOT NULL | |
| `added_by` | BIGINT | FK -> users.id NOT NULL | Which admin added the student |
| `added_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |
| **UNIQUE(event_id, user_id)** | | DB-level constraint | Prevents double-attendance |

**Indexes:** on `event_id` and `user_id` separately.

---

### 4.4 Migration V5 -- Create `event_photos` Table

**File:** `V5__create_event_photos.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `event_id` | BIGINT | FK -> events.id ON DELETE CASCADE, NOT NULL | Which event this photo belongs to |
| `image_url` | VARCHAR(512) | NOT NULL | Cloudinary secure URL |
| `caption` | VARCHAR(255) | nullable | Optional photo caption |
| `uploaded_by` | BIGINT | FK -> users.id NOT NULL | Admin who uploaded |
| `uploaded_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |

**Index:** on `event_id` for fast gallery load.

---

### 4.5 Migration V6 -- Create `member_gallery` Table

**File:** `V6__create_member_gallery.sql`

For batch-wise group photos of club members, separate from individual avatars.

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `batch_year` | INTEGER | NOT NULL | e.g. 2023, 2024 |
| `image_url` | VARCHAR(512) | NOT NULL | Cloudinary URL |
| `caption` | VARCHAR(255) | nullable | e.g. "Batch 2023 - Inauguration Day" |
| `uploaded_by` | BIGINT | FK -> users.id NOT NULL | Admin who uploaded |
| `uploaded_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |

**Index:** on `batch_year` for fast batch-filter queries.

---

### 4.6 Migration V7 -- Create `weekly_snapshots` Table

**File:** `V7__create_weekly_snapshots.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK -> users.id ON DELETE CASCADE, NOT NULL | |
| `platform` | VARCHAR(20) | NOT NULL CHECK IN ('CODEFORCES','LEETCODE') | |
| `rating` | INTEGER | NOT NULL | |
| `recorded_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |

**Index:** on `(user_id, platform)` combined.

---

### 4.7 Entity Layer Updates

**`User.java`:** Add all new fields with `@Column` annotations matching migration column names exactly. Add `ClubRole` enum (Java enum) with values: `CONVENOR`, `DEPUTY_CONVENOR`, `CORE`, `ASSOCIATE_CORE`, `BATCH_REPRESENTATIVE`, `EX_PC_MEMBER`, `EX_CORE`, `EX_CDC`, `STUDENT`. Store as `@Enumerated(EnumType.STRING)`. Also add `batchYear` Integer field.

**`Event.java`:** New entity. Add `EventStatus` enum with values: `UPCOMING`, `COMPLETED`, `CANCELLED`. Stored as `@Enumerated(EnumType.STRING)`. Include a `@OneToMany` to `EventPhoto` for convenience.

**`EventAttendee.java`:** New entity. Three `@ManyToOne` to `Event`, `User` (student), `User` (admin who added).

**`EventPhoto.java`:** New entity. `@ManyToOne` to `Event`. Fields: imageUrl, caption, uploadedBy, uploadedAt.

**`MemberGalleryPhoto.java`:** New entity. No foreign key to Event. Fields: batchYear, imageUrl, caption, uploadedBy, uploadedAt.

**`WeeklySnapshot.java`:** New entity. `@ManyToOne` to `User`. Fields: platform (String), rating (Integer), recordedAt.

---

## Section 5 -- Stage 1A: Backend Security + Gallery
**Owner: Member 4**
**PR: `phase2/backend-security`**
**Estimated time: 3-4 days**
**Depends on: Stage 0 merged**

> [!NOTE]
> M4 also owns the `gallery/` package (Section 6.5), moved off M5. Security work
> was 1-2 days against M5's 6-7, leaving M4 idle for most of Stage 1 while the
> critical path waited on a single person. Gallery has no dependency on M5's
> work, so it can be built in the same branch without coordination.

### Files & What Changes

#### `SecurityConfig.java`
Add the following rules **before** `anyRequest().authenticated()`, in this exact order (top-to-bottom matters):

| Rule | Path Pattern | Auth Required |
|---|---|---|
| Public -- list & view events | `GET /api/events`, `GET /api/events/{id}` | None |
| Public -- event gallery | `GET /api/events/{id}/photos` | None |
| Public -- member gallery | `GET /api/gallery/members` | None |
| Admin -- all other event ops | `/api/events/**` | `hasRole('ADMIN')` |
| Admin -- gallery upload | `/api/gallery/**` | `hasRole('ADMIN')` |
| Admin -- user lookup for event | `GET /api/users/{id}/lookup` | `hasRole('ADMIN')` |
| Admin -- assign club role | `PUT /api/users/{id}/club-role` | `hasRole('ADMIN')` |
| Authenticated -- snapshots | `GET /api/snapshots/**` | `isAuthenticated()` |

#### `UserController.java`
Add two new endpoints:
1. `GET /api/users/{id}/lookup` -- `@PreAuthorize("hasRole('ADMIN')")` -- returns `UserLookupDto`. Calls `userService.lookupUserById(id)`.
2. `PUT /api/users/{id}/club-role` -- `@PreAuthorize("hasRole('ADMIN')")` -- body: `{ clubRole: "CORE" }`. Returns updated `UserResponseDto`. Calls `userService.updateClubRole(id, request)`.

---

## Section 6 -- Stage 1B: Backend Data & APIs
**Owner: Member 5**
**PR: `phase2/backend-data`**
**Estimated time: 4-5 days** (was 6-7 before gallery and snapshot were reassigned)
**Depends on: Stage 0 merged**
**This is still the largest single task in Phase 2.**

> [!IMPORTANT]
> Two packages below are no longer M5's:
> - **6.5 `gallery/` -> M4**, built alongside Stage 1A security
> - **6.6 `snapshot/` -> M6**
>
> M5 keeps `leetcode/` (6.3), because `UserService.updateProfile()` and
> `CodeforcesSyncService` both call into it -- splitting it would have blocked
> M5 on another member's class.

---

### 6.1 User Profile Changes

#### `UserProfileUpdateRequest.java`
Replace existing record. Fields and validations:

| Field | Validation | Notes |
|---|---|---|
| `name` | `@NotBlank @Size(min=2, max=100)` | Required |
| `phoneNumber` | `@Size(max=20)` | Frontend enforces mandatory |
| `codeforcesHandle` | `@Size(max=100)` | Optional |
| `leetcodeHandle` | `@Size(max=100)` | Optional |
| `codechefUrl` | `@Size(max=512)` | Optional, URL format |
| `atcoderUrl` | `@Size(max=512)` | Optional |
| `githubUrl` | `@Size(max=512)` | Optional |
| `linkedinUrl` | `@Size(max=512)` | Optional |
| `avatarUrl` | `@Size(max=512)` | Set by Cloudinary, optional |

#### `UpdateClubRoleRequest.java` [NEW]
Single-field record: `clubRole` of type `ClubRole` enum. Annotated with `@NotNull`. Validated by the framework.

#### `UserLookupDto.java` [NEW]
Admin-only full view for the event attendance panel. Fields:
`id`, `name`, `email`, `phoneNumber`, `avatarUrl`, `codeforcesHandle`, `cfRating`, `leetcodeHandle`, `leetcodeRating`, `codechefUrl`, `atcoderUrl`, `githubUrl`, `linkedinUrl`, `clubRole`, `batchYear`.
Has static `fromEntity(User)` factory.

#### `UserResponseDto.java` [MODIFY]
Extend with all new fields: `avatarUrl`, `phoneNumber`, `leetcodeHandle`, `leetcodeRating`, `codechefUrl`, `atcoderUrl`, `githubUrl`, `linkedinUrl`, `clubRole`, `batchYear`.

#### `UserService.java` [MODIFY]
- `updateProfile()` -- persist all new fields. If `leetcodeHandle` changed -> call `leetCodeSyncService.syncSingleUser()`. If `codeforcesHandle` changed -> call `codeforcesSyncService.syncSingleUser()`.
- `updateClubRole(Long userId, UpdateClubRoleRequest req)` -> loads user, sets `clubRole`, saves, returns `UserResponseDto`.
- `lookupUserById(Long id)` -> loads user, returns `UserLookupDto.fromEntity(user)`. Throws `ResourceNotFoundException` if missing.

#### `UserRepository.java` [MODIFY]
Add:
- `List<User> findByLeetcodeHandleIsNotNull()` -- for bulk LeetCode sync
- `boolean existsByLeetcodeHandle(String handle)` -- for uniqueness check
- `List<User> findByClubRoleIn(List<ClubRole> roles)` -- for leaderboard filtering

---

### 6.2 Leaderboard Enhancement

#### `LeaderboardService.java` [MODIFY]
The existing leaderboard currently sorts by rating. Extend it to accept two query params:
- `platform` -- `CODEFORCES` (default) or `LEETCODE`
- `filter` -- `ALL` (default), `CORE`, `BATCH_REP`, `STUDENTS`

Filter logic:
- `ALL` -> no role filter, all users with a rating
- `CORE` -> `clubRole IN (CORE, ASSOCIATE_CORE, CONVENOR, DEPUTY_CONVENOR)`
- `BATCH_REP` -> `clubRole = BATCH_REPRESENTATIVE`
- `STUDENTS` -> `clubRole = STUDENT OR clubRole IS NULL`

#### `LeaderboardResponseDto.java` [MODIFY]
Add `clubRole` field (String) to the leaderboard row response so the frontend can display it as a badge next to the member's name.

#### `LeaderboardEntryProjection.java` [MODIFY]
Add `clubRole` to the projection interface used by the repository query.

#### `LeaderboardController.java` [MODIFY]
Update `GET /api/leaderboard` to accept `?platform=` and `?filter=` query params and pass them to the service.

---

### 6.3 LeetCode Sync Package [NEW]

**Package:** `com.cpclub.backend.leetcode`

#### `LeetCodeGraphQLResponse.java`
A nested record matching the LeetCode GraphQL API JSON shape:
`{ data: { userContestRanking: { rating: Double } } }`
All levels null-safe.

#### `LeetCodeSyncService.java`
**Injected:** `UserRepository`, `RestTemplate`, shared `RateLimiter` bean from `AppConfig`.

Methods:
- `syncSingleUser(User)` -- JIT sync on handle save. Guards: null handle -> skip. Acquires rate limiter permit. POSTs GraphQL query. Parses response. If `userContestRanking` is null (user never did a contest), sets rating to `0`. On any HTTP error: logs warning, does NOT throw -- fail-silently so profile saving still works.
- `syncAllUsers()` -- bulk sync called by cron. Iterates all users with non-null `leetcodeHandle`. Same logic per user. Logs total updated count.

#### `CodeforcesSyncService.java` [MODIFY]
After the existing `syncCodeforcesRatings()` cron method finishes, add a call to `leetCodeSyncService.syncAllUsers()`. Inject `LeetCodeSyncService` via constructor.

---

### 6.4 Event Package [NEW]

**Package:** `com.cpclub.backend.event`

#### Entities
- `Event.java` -- mirrors `events` table. Has `@Enumerated(EnumType.STRING) EventStatus status`. `createdBy` is `@ManyToOne(fetch=LAZY)` to User.
- `EventAttendee.java` -- mirrors `event_attendees`. Three `@ManyToOne` links: event, user (student), addedBy (admin). `@UniqueConstraint` on `(event_id, user_id)`.
- `EventPhoto.java` -- mirrors `event_photos`. `@ManyToOne` to Event. Fields: imageUrl, caption, uploadedBy, uploadedAt.

#### Repositories
- `EventRepository` -- `findByStatusOrderByEventDateAsc(EventStatus)`, `findAllByOrderByEventDateDesc()` (admin view), `findByStatusInOrderByEventDateDesc(List<EventStatus>)`.
- `EventAttendeeRepository` -- `findByEventIdOrderByAddedAtAsc(Long)`, `existsByEventIdAndUserId(Long, Long)`, `findByEventIdAndUserId(Long, Long)`.
- `EventPhotoRepository` -- `findByEventIdOrderByUploadedAtAsc(Long)`.

#### DTOs
- `EventCreateRequest` -- `title (@NotBlank)`, `description`, `eventDate (@NotNull)`, `location (@NotBlank)`, `coverImageUrl`.
- `EventResponseDto` -- `id`, `title`, `description`, `eventDate`, `location`, `status`, `coverImageUrl`, `createdByName`, `createdAt`. Static `fromEntity(Event)`.
- `EventDetailDto` -- extends `EventResponseDto` and adds `List<EventPhotoDto>` photos and `Integer attendeeCount`. Used for the public event detail page.
- `AddAttendeeRequest` -- single `userId (@NotNull Long)`.
- `EventAttendeeDto` -- 16 fields: userId, name, email, phoneNumber, hasPhone, avatarUrl, codeforcesHandle, cfRating, leetcodeHandle, leetcodeRating, codechefUrl, atcoderUrl, githubUrl, linkedinUrl, addedAt, clubRole. Static `fromEntity(EventAttendee)`.
  `hasPhone` is a boolean derived from `phoneNumber` being non-null and non-blank; it drives the missing-phone warning in the admin attendee table (see the note under `addAttendee`). The v3 plan said "14 fields" but listed 15; the correct count with `hasPhone` is 16.
- `EventPhotoDto` -- `id`, `imageUrl`, `caption`, `uploadedAt`. Static `fromEntity(EventPhoto)`.
- `AddEventPhotoRequest` -- `imageUrl (@NotBlank @Size(max=512))`, `caption`.

#### `EventService.java`
Methods and their exact behavior:

`createEvent(EventCreateRequest, String adminEmail)` -> `EventResponseDto`
- Loads admin by email. Creates Event with status=UPCOMING. Saves. Returns DTO.

`listUpcomingEvents()` -> `List<EventResponseDto>`
- Fetches events with `status = UPCOMING`, ordered by `eventDate ASC`.

`listCompletedEvents()` -> `List<EventResponseDto>`
- Fetches events with `status = COMPLETED`, ordered by `eventDate DESC`.

`getEventDetail(Long id)` -> `EventDetailDto`
- Loads event (404 if not found). Loads its photos. Gets attendee count. Returns combined DTO.

`updateEvent(Long id, EventCreateRequest)` -> `EventResponseDto`
- Loads event. Throws 404 if not found. Updates fields. Saves. Returns DTO.

`markEventCompleted(Long id)` -> `EventResponseDto`
- Loads event. Sets `status = COMPLETED`. Saves. Returns DTO.

`cancelEvent(Long id)` -> `EventResponseDto`
- Loads event. Sets `status = CANCELLED`. Saves. Returns DTO.

`addAttendee(Long eventId, Long userId, String adminEmail)` -> `EventAttendeeDto`
Guard clauses (in this exact order):
1. Load event. Throw `ResourceNotFoundException` if missing.
2. Check event status is `UPCOMING`. Throw `BadRequestException("Cannot add attendees to a completed or cancelled event.")` if not.
3. Load user. Throw `ResourceNotFoundException("Student not found")` if missing.
4. **Do NOT block on a missing phone number.** Set `hasPhone = false` on the
   returned DTO when `user.phoneNumber` is null or blank, and let the add
   proceed. See the note below.
5. Check `existsByEventIdAndUserId`. Throw `BadRequestException("Student is already registered.")` if true.
6. Load admin. Save `EventAttendee`. Return DTO.

> [!IMPORTANT]
> **Guard 4 was a hard `BadRequestException` in v3 and has been downgraded.**
>
> Every user created before Phase 2 has `phone_number = NULL` -- V2 added the
> column as nullable with no backfill. A hard block meant that on the first day
> this feature shipped, an admin standing at an event could add *nobody*, and
> could not fix it either: `PUT /api/users/profile` is self-only, so only the
> member can set their own phone.
>
> Attendance is the official club record. Losing it because someone had not
> filled in a phone number is a worse outcome than a sparse phone column.
>
> This also resolves a contradiction in v3: M3's admin panel was already
> specified to "show warning if no phone" and then offer an Add button, which
> only makes sense if the add is permitted.

`removeAttendee(Long eventId, Long userId)` -- finds attendee, deletes. Throws 404 if not found.

`getAttendees(Long eventId)` -> `List<EventAttendeeDto>` -- loads event first (404 guard), then fetches attendees.

`addEventPhoto(Long eventId, AddEventPhotoRequest, String adminEmail)` -> `EventPhotoDto`
- Loads event. Creates `EventPhoto` with imageUrl, caption, uploadedBy admin. Saves. Returns DTO.

`deleteEventPhoto(Long photoId)` -- finds photo, deletes. Throws 404 if not found.

`getEventPhotos(Long eventId)` -> `List<EventPhotoDto>` -- loads event (404 guard), fetches photos ordered by upload time.

#### `EventExportService.java`
Uses Apache POI XSSF. Method: `exportToExcel(List<EventAttendeeDto>)` -> `byte[]`.

Excel columns (15 -- the v3 plan said 14 but listed 15; `EventExportServiceTest` asserts on these headers, so the count matters): ID, Name, Email, Phone Number, Club Role, Avatar URL, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef URL, AtCoder URL, GitHub, LinkedIn, Added At.
- Row 0: bold headers.
- Row 1+: one row per attendee.
- All columns auto-sized.
- Returns `byte[]` from `ByteArrayOutputStream`.

#### `EventController.java`
Base path: `/api/events`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/events` | Admin | Create event |
| GET | `/api/events/upcoming` | Public | List upcoming events |
| GET | `/api/events/completed` | Public | List completed events |
| GET | `/api/events` | Admin | List ALL events (admin view) |
| GET | `/api/events/{id}` | Public | Single event detail with photos + attendee count |
| PUT | `/api/events/{id}` | Admin | Update event |
| PUT | `/api/events/{id}/complete` | Admin | Mark event as completed |
| PUT | `/api/events/{id}/cancel` | Admin | Cancel event |
| POST | `/api/events/{id}/attendees` | Admin | Add attendee `{ userId }` |
| DELETE | `/api/events/{id}/attendees/{uid}` | Admin | Remove attendee |
| GET | `/api/events/{id}/attendees` | Admin | Attendee list (JSON) |
| GET | `/api/events/{id}/attendees/export` | Admin | Download `.xlsx` |
| POST | `/api/events/{id}/photos` | Admin | Add photo `{ imageUrl, caption }` |
| DELETE | `/api/events/photos/{photoId}` | Admin | Delete a photo |
| GET | `/api/events/{id}/photos` | Public | Get all photos for an event |

---

### 6.5 Gallery Package [NEW] -- OWNER: M4 (not M5)

**Package:** `com.cpclub.backend.gallery`
**Built in M4's `feature/M4-phase2-security` branch, alongside Stage 1A.**
The `MemberGalleryPhoto` entity already exists from Stage 0.

#### Entities
- `MemberGalleryPhoto.java` -- mirrors `member_gallery` table. Fields: batchYear (Integer), imageUrl, caption, uploadedBy (ManyToOne to User), uploadedAt.

#### Repository
- `MemberGalleryRepository` -- `findByBatchYearOrderByUploadedAtAsc(Integer)`, `findDistinctBatchYearsOrderByDesc()` (returns `List<Integer>` of all unique years in the DB, for the frontend filter dropdown).

#### DTOs
- `MemberGalleryPhotoDto` -- `id`, `batchYear`, `imageUrl`, `caption`, `uploadedAt`. Static `fromEntity`.
- `AddMemberPhotoRequest` -- `batchYear (@NotNull @Min(2000) @Max(2100))`, `imageUrl (@NotBlank)`, `caption`.

#### `MemberGalleryService.java`
- `addPhoto(AddMemberPhotoRequest, String adminEmail)` -> `MemberGalleryPhotoDto`
- `deletePhoto(Long id)` -- 404 if not found
- `getPhotosByBatch(Integer batchYear)` -> `List<MemberGalleryPhotoDto>`
- `getAvailableBatchYears()` -> `List<Integer>` -- for frontend dropdown

#### `GalleryController.java`
Base path: `/api/gallery/members`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/gallery/members` | Admin | Upload a member gallery photo |
| DELETE | `/api/gallery/members/{id}` | Admin | Delete a photo |
| GET | `/api/gallery/members?batch={year}` | Public | Photos for a specific batch year |
| GET | `/api/gallery/members/batches` | Public | List of all available batch years |

---

### 6.6 Snapshot Package [NEW] -- OWNER: M6 (not M5)

**Package:** `com.cpclub.backend.snapshot`
**Built in M6's own branch, in parallel with Stage 1.**
The `WeeklySnapshot` entity already exists from Stage 0. This package reads the
`users` table and writes `weekly_snapshots`; nothing in M4's or M5's work calls
into it, so it can land in any order.

- `WeeklySnapshot.java` -- JPA entity.
- `WeeklySnapshotRepository.java` -- `findByUserIdAndPlatformOrderByRecordedAtAsc(Long, String)`.
- `SnapshotService.java` -- `@Scheduled(cron = "0 0 0 * * MON")`. Records current CF and LeetCode ratings for ALL users who have them into `weekly_snapshots`. Does NOT re-fetch from APIs -- snapshots the already-fresh DB values.
- `SnapshotController.java` -- `GET /api/snapshots/{userId}/codeforces` and `GET /api/snapshots/{userId}/leetcode`. Returns `List<{date, rating}>`. Auth: `isAuthenticated()`.

---

### 6.7 `pom.xml` Update
Add Apache POI XSSF: `org.apache.poi:poi-ooxml:5.3.0`.

---

## Section 7 -- Stage 2A: Frontend UI/UX
**Owner: Member 1**
**PR: `phase2/frontend-ui`**
**Estimated time: 3 days**

### Files & Responsibilities

| File | Action | What it does |
|---|---|---|
| `components/site/event-card.tsx` | NEW | Card for a single event. Shows: cover image (if any), title, date formatted as "Sat 15 Nov -- 3:00 PM", location, status badge (green=Upcoming, grey=Completed, red=Cancelled). Entire card is a `<Link href="/events/{id}">`. |
| `components/site/event-photo-grid.tsx` | NEW | Responsive photo grid for event gallery. Props: `photos: EventPhotoDto[]`. Lightbox on click. Shows caption below each photo. |
| `components/site/member-gallery-grid.tsx` | NEW | Same photo grid for batch photos. Props: `photos: MemberGalleryPhotoDto[]`. |
| `components/ui/data-table.tsx` | NEW | Generic reusable table. Props: `columns`, `data`, `isLoading`. Shows skeleton on load, "No records" if empty. |
| `components/ui/club-role-badge.tsx` | NEW | Small colored badge component for displaying club roles. Maps `clubRole` string to: Convenor=gold, Core=blue, Batch Rep=green, Student=grey, Ex-*=outline. |
| `components/site/admin-tabs.tsx` | NEW | Tab nav shell with tabs: Members, Events, Galleries. |
| `app/events/page.tsx` | MODIFY | Replace hardcoded placeholder. Server Component. Fetches `GET /api/events/upcoming`. Maps to `<EventCard>`. Shows "No upcoming events" if empty. |
| `app/events/[id]/page.tsx` | NEW | Event detail page. Shows: cover image, title, date, location, description, photo gallery grid, attendee count. Fetches `GET /api/events/{id}`. |
| `app/gallery/page.tsx` | NEW | Member gallery page. Shows batch year filter dropdown. Fetches `GET /api/gallery/members/batches` for years. Fetches photos by selected year. |
| `app/(dashboard)/admin/page.tsx` | CREATE SHELL | Admin dashboard shell with the tab layout. M3 fills the data. |

---

## Section 8 -- Stage 2B: Frontend Auth & State
**Owner: Member 2**
**PR: `phase2/frontend-auth`**
**Estimated time: 2 days**

### Files & Responsibilities

| File | Action | What it changes |
|---|---|---|
| `store/auth.ts` | MODIFY | Add `clubRole: string \| null`, `batchYear: number \| null`, `leetcodeHandle: string \| null`, `phoneNumber: string \| null`, `avatarUrl: string \| null` to the `User` interface. |
| `types/api.ts` | MODIFY | Add TypeScript types: `Event`, `EventDetail`, `EventAttendee`, `EventPhoto`, `UserLookup`, `MemberGalleryPhoto`. Update existing `Profile` type with all new fields. Add `ClubRole` string union type. |
| `lib/services/events.ts` | NEW | All event API calls: listUpcoming, listCompleted, getAllAdmin, getEventDetail, createEvent, updateEvent, markCompleted, cancelEvent, addAttendee, removeAttendee, getAttendees, exportAttendees (responseType: blob), addEventPhoto, deleteEventPhoto, getEventPhotos, lookupUser. |
| `lib/services/gallery.ts` | NEW | `addMemberPhoto(data)`, `deleteMemberPhoto(id)`, `getPhotosByBatch(year)`, `getAvailableBatchYears()`. |
| `lib/services/dashboard.ts` | MODIFY | Update `mapUserToProfile()` to map all 8 new fields plus `clubRole` and `batchYear`. Use `?? null` fallback for all. |
| `lib/services/leaderboard.ts` | MODIFY | Update the leaderboard API call to pass `?platform=` and `?filter=` params. Add `clubRole` to the leaderboard entry type. |

---

## Section 9 -- Stage 2C: Frontend Dashboards & Data
**Owner: Member 3**
**PR: `phase2/frontend-dashboards`**
**Depends on: M1 + M2 PRs merged**
**Estimated time: 5 days**

### Files & Responsibilities

#### `components/site/profile-dashboard.tsx` [MAJOR MODIFY]

**Change 1 -- Avatar:**
If `profile.avatarUrl` exists -> render image with CF rank border. If owner and no avatar -> show placeholder icon + "Upload photo" button. Click opens Cloudinary Upload Widget (`NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME`). On success -> `dashboardService.updateProfile({ avatarUrl: result.info.secure_url })` -> `loadProfile()`.

**Change 2 -- Club Role & Batch Year badge:**
Below the member's name in the profile header, show their `<ClubRoleBadge clubRole={profile.clubRole} />` (the M1 component). If no club role, show nothing.

**Change 3 -- Platform links in header:**
Row of icon links below CF handle. Only rendered if the URL is set: CodeChef, AtCoder, GitHub, LinkedIn. Each is `<a href="..." target="_blank" rel="noopener noreferrer">`.

**Change 4 -- Edit Profile panel:**
Replace the inline CF handle edit form with a full "Edit Profile" panel (owner-only). Fields: Name (required), Phone Number (required -- cannot save without it), Codeforces Handle, LeetCode Handle, CodeChef URL, AtCoder URL, GitHub URL, LinkedIn URL. Phone shown masked (`--------`) to visitors. On save: `PUT /api/users/profile`. On success: refresh. On error: show inline error.

**Change 5 -- Rating charts:**
Replace "Coming Soon" overlay with live charts using `recharts`. CF chart: `GET /api/snapshots/{userId}/codeforces`. LeetCode chart: `GET /api/snapshots/{userId}/leetcode`. If < 2 points: "Not enough data yet." Reuse Phase 1 chart styling.

---

#### `app/(dashboard)/admin/page.tsx` [FILL IN]

**Tab 1 -- Members:**
Fetches `GET /api/users/all`. Renders `<DataTable>` with columns:

| Column | Value | Actions |
|---|---|---|
| Avatar | small rounded image | -- |
| Name | `user.name` | -- |
| Email | `user.email` | -- |
| Phone | `user.phoneNumber \|\| '--'` | -- |
| CF Handle + Rating | -- | -- |
| LeetCode Handle + Rating | -- | -- |
| Club Role | `<ClubRoleBadge />` | "Change Role" dropdown |
| Batch Year | `user.batchYear \|\| '--'` | -- |
| Platform Role | Admin / User badge | "Promote" / "Demote" button |
| Actions | -- | "Delete" button with confirmation dialog |

"Change Club Role" -- inline dropdown with all `ClubRole` values. On select: calls `PUT /api/users/{id}/club-role`. Refreshes table.
"Promote/Demote" -- calls `PUT /api/users/{id}/role`. Refreshes table.
"Delete" -- confirmation dialog. Calls `DELETE /api/users/{id}`. Refreshes table.

**Tab 2 -- Events:**
- "Create New Event" form: Title (required), Description (optional), Date+Time (datetime-local input), Location (required), Cover Image (Cloudinary Upload Widget button -- inserts URL into hidden field). Submit -> `POST /api/events`. Refreshes list.
- Table of ALL events (admin view from `GET /api/events`). Columns: Title, Date, Location, Status badge, Cover Image thumbnail, Actions.
- Per-event actions: "Edit" (inline form), "Mark Completed", "Cancel", "Manage Attendees" -> navigates to `/admin/events/{id}`.

**Tab 3 -- Galleries:**
- **Member Gallery sub-tab:**
  - Batch year input (number) + Cloudinary upload button + Caption field. Submit -> `POST /api/gallery/members`. Refreshes grid.
  - Grid of uploaded photos, grouped by batch year. "Delete" button per photo.
- **Event Galleries sub-tab:**
  - Dropdown to select event. Once selected, shows existing photos for that event + upload form. Submit -> `POST /api/events/{id}/photos`. Delete per photo.

---

#### `app/(dashboard)/admin/events/[id]/page.tsx` [NEW]

Two-column layout (stacks on mobile):

**Left -- Student Search Panel:**
- Input: "Enter Student ID". Search button.
- On search: `eventsService.lookupUser(id)`. Shows loading state.
- Error states: non-numeric input -> inline validation. 404 -> "No student found with this ID." 
- Preview card after successful search: avatar, name, email, phone (or red warning "[WARN] No phone -- ask student to update profile"), CF + LeetCode data, club role badge, social links.
- "Add to Event" button: disabled if no student loaded, or already in attendee list. Shows "Already Added [DONE]" chip if already registered.
  **A missing phone number must NOT disable this button** -- show the red warning
  on the preview card and let the admin proceed. See the note under `addAttendee`
  in Section 6.4: blocking here would make the feature unusable for every member
  who joined before Phase 2, and the admin cannot fix another member's profile.
- On Add: `eventsService.addAttendee(eventId, userId)` -> show success toast -> clear search panel -> refresh attendee list.

**Right -- Attendee Table:**
- Header: event title, attendee count badge, "Export to Excel" button.
- Excel export: `eventsService.exportAttendees(eventId)` (responseType: blob) -> `URL.createObjectURL(new Blob([res.data]))` -> programmatic `<a>` click -> `URL.revokeObjectURL()`.
- `<DataTable>` with columns: Name, Email, Phone, Club Role, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef, AtCoder, GitHub, LinkedIn, Added At, Remove.
- "Remove" per row: confirmation dialog -> `eventsService.removeAttendee(eventId, userId)` -> refresh.

---

#### `app/(dashboard)/leaderboard/page.tsx` [MODIFY]

Add two sets of filter controls above the leaderboard table:

**Platform toggles:** "Codeforces | LeetCode" -- pill buttons. Updates `platform` state and re-fetches.

**Club filter toggles:** "All | Core | Batch Rep | Students" -- these are the **existing UI filters** the teammate mentioned. Now they become data-driven (not just cosmetic). Updates `filter` state and re-fetches with `?filter=CORE` etc.

In the leaderboard table, add a "Club Role" column showing `<ClubRoleBadge />` for each member.

---

## Section 10 -- Stage 3: Integration, Tests & Polish
**Owner: Member 6**
**PR: `phase2/integration-and-tests`**
**Estimated time: 2-3 days**

### Unit Tests

#### `EventServiceTest.java`

| Test | Scenario | Expected |
|---|---|---|
| `createEvent_success` | Valid request, admin exists | Event saved, status=UPCOMING |
| `addAttendee_success` | Valid userId, has phone, not duplicate | Attendee row created |
| `addAttendee_noPhone` | User has null phoneNumber | `BadRequestException` |
| `addAttendee_duplicate` | Student already registered | `BadRequestException` |
| `addAttendee_userNotFound` | userId doesn't exist | `ResourceNotFoundException` |
| `addAttendee_eventCompleted` | Event status is COMPLETED | `BadRequestException` |
| `removeAttendee_success` | Attendee exists | Row deleted |
| `markCompleted_success` | Event found | status set to COMPLETED |
| `addEventPhoto_success` | Valid eventId + url | Photo saved, DTO returned |
| `deleteEventPhoto_notFound` | photoId doesn't exist | `ResourceNotFoundException` |

#### `LeetCodeSyncServiceTest.java`

| Test | Scenario | Expected |
|---|---|---|
| `sync_success` | Mock HTTP returns rating 1523.45 | Rating set to 1523, saved |
| `sync_nullHandle` | User has no leetcodeHandle | Zero HTTP calls made |
| `sync_noContest` | `userContestRanking` is null in response | Rating set to 0 |
| `sync_httpError` | RestTemplate throws exception | No exception propagated, rating unchanged |

#### `EventExportServiceTest.java`

| Test | Scenario | Expected |
|---|---|---|
| `export_notEmpty` | 2 attendees | byte[] length > 0 |
| `export_correctHeaders` | Any attendees | Row 0, Cell 0 = "ID", Cell 1 = "Name", etc. |
| `export_clubRoleColumn` | Attendee has `clubRole = CORE` | Row 1, Club Role cell = "CORE" |

#### `MemberGalleryServiceTest.java`

| Test | Scenario | Expected |
|---|---|---|
| `addPhoto_success` | Valid batchYear + imageUrl | Photo saved, DTO returned |
| `deletePhoto_notFound` | ID doesn't exist | `ResourceNotFoundException` |
| `getByBatch_empty` | No photos for year 2020 | Empty list (not 404) |

### Environment Variables

**Frontend only (Vercel).** Both are documented in `frontend/.env.example`:
```
NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME=stdcydx1
NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET=cpclub_unsigned
```
These are `NEXT_PUBLIC_*`, so they are baked in at build time -- adding them to
Vercel does nothing until the site is redeployed.

**Nothing goes on Render.** The browser uploads straight to Cloudinary via the
Upload Widget and the backend only ever stores the returned URL string, so the
backend needs no Cloudinary credentials. The v3 plan listed a backend
`CLOUDINARY_CLOUD_NAME` "if needed"; it is not needed, and
`backend/.env.example` documents only variables the application actually reads.

### Carried-over follow-ups

1. **Add Testcontainers.** `src/test/resources/application-test.yml` runs H2 with
   `ddl-auto: create-drop` and Flyway **disabled**, so CI cannot detect a
   migration that has drifted from the entities -- a green suite proves nothing
   about the schema. Stage 0 was verified by booting against real PostgreSQL by
   hand; that should not stay a manual step.
2. **Harden the Cloudinary upload preset.** `cpclub_unsigned` is correctly set to
   Unsigned with folder `cpclub`, but `Allowed formats` and `Max file size` were
   not set. Verified consequence: a `.txt` file uploads successfully through
   `/raw/upload` using the public preset name. Set `Allowed formats` to
   `jpg,png,webp` and `Max file size` to `5000000` in the Cloudinary console.
3. **Backfill phone numbers.** Soft campaign, since the hard block was removed
   (Section 1). Size it with:
   `SELECT COUNT(*) AS total, COUNT(phone_number) AS have_phone FROM users;`

---

## Section 11 -- Full API Contract Reference

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users` | Public | Member directory (paginated) |
| GET | `/api/users/{id}` | Public | Public profile |
| GET | `/api/users/{id}/lookup` | **Admin** | Full lookup for event panel |
| GET | `/api/users/profile` | Auth | Own full profile |
| PUT | `/api/users/profile` | Auth | Update own profile (all new fields) |
| PUT | `/api/users/{id}/role` | **Admin** | Change platform role (ADMIN/USER) |
| PUT | `/api/users/{id}/club-role` | **Admin** | Change club role (CORE, CONVENOR, etc.) |
| DELETE | `/api/users/{id}` | **Admin** | Delete user |
| POST | `/api/events` | **Admin** | Create event |
| GET | `/api/events/upcoming` | Public | List upcoming events |
| GET | `/api/events/completed` | Public | List completed events |
| GET | `/api/events` | **Admin** | List ALL events (admin view) |
| GET | `/api/events/{id}` | Public | Event detail + photos + attendee count |
| PUT | `/api/events/{id}` | **Admin** | Update event |
| PUT | `/api/events/{id}/complete` | **Admin** | Mark as completed |
| PUT | `/api/events/{id}/cancel` | **Admin** | Cancel event |
| POST | `/api/events/{id}/attendees` | **Admin** | Add attendee `{ userId }` |
| DELETE | `/api/events/{id}/attendees/{uid}` | **Admin** | Remove attendee |
| GET | `/api/events/{id}/attendees` | **Admin** | View attendee list (JSON) |
| GET | `/api/events/{id}/attendees/export` | **Admin** | Download `.xlsx` |
| POST | `/api/events/{id}/photos` | **Admin** | Upload event photo `{ imageUrl, caption }` |
| DELETE | `/api/events/photos/{photoId}` | **Admin** | Delete event photo |
| GET | `/api/events/{id}/photos` | Public | Event photo gallery |
| POST | `/api/gallery/members` | **Admin** | Upload member gallery photo |
| DELETE | `/api/gallery/members/{id}` | **Admin** | Delete member gallery photo |
| GET | `/api/gallery/members?batch={year}` | Public | Photos by batch year |
| GET | `/api/gallery/members/batches` | Public | Available batch years |
| GET | `/api/snapshots/{uid}/codeforces` | Auth | CF rating history |
| GET | `/api/snapshots/{uid}/leetcode` | Auth | LeetCode rating history |
| GET | `/api/leaderboard?platform=&filter=` | Public | Leaderboard with platform + club filter |

---

## Section 12 -- 6-Member Summary

| Member | Role | Stage | Owns |
|---|---|---|---|
| **M6 (Lead)** | Foundation + DevOps | 0 + 1 + 3 | **[DONE]** All 6 Flyway migrations, all entity files, `ClubRole` enum, `EventStatus` enum, Flyway auto-config fix, Cloudinary env vars. **[Stage 1]** entire `snapshot/` package. **[Stage 3]** all unit tests, Render env vars, PR reviews, final merge |
| **M4** | Backend Security + Gallery | 1A | `SecurityConfig` new rules (all 8 rule blocks), `UserController` lookup endpoint, `UserController` club-role endpoint, entire `gallery/` package (2 DTOs + 1 repo + 1 service + 1 controller) |
| **M5** | Backend Data + APIs | 1B | `UserProfileUpdateRequest` update, `UpdateClubRoleRequest`, `UserLookupDto`, `UserResponseDto` update, `UserService` 3 new methods, `UserRepository` 3 new methods, leaderboard filter by `clubRole`, entire `leetcode/` package, entire `event/` package (7 DTOs + 3 repos + 2 services + 1 controller), `pom.xml` POI dep |
| **M1** | Frontend UI/UX | 2A | `EventCard`, `EventPhotoGrid`, `MemberGalleryGrid`, `DataTable`, `ClubRoleBadge`, `AdminTabs`, Events page redesign, Event detail page, Member Gallery page |
| **M2** | Frontend State + Auth | 2B | `auth.ts` new User fields, `types/api.ts` 6 new types + ClubRole union, `events.ts` service (17 functions), `gallery.ts` service (4 functions), `dashboard.ts` mapper update, `leaderboard.ts` params update |
| **M3** | Frontend Dashboards | 2C | Profile dashboard (Cloudinary avatar, club role badge, platform links, edit panel, rating charts), Admin dashboard all 3 tabs (Members with club role assignment, Events CRUD + gallery upload, Gallery management), Admin event attendee page (search + auto-fill + table + Excel download), Leaderboard platform + club filter toggles |
