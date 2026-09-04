# Phase 2 â€” Senior Engineer Implementation Plan (v3 â€” Updated)
**Branch:** `feature/phase-2` (branched off `main` post Phase 1 merge)
**Duration:** 3 sprints Â· ~3 weeks
**Team:** 6 members
**Stack:** Spring Boot 4.1 Â· PostgreSQL Â· Next.js 15 Â· Cloudinary Â· Apache POI

---

## ðŸš€ How to Get Started â€” Starter Prompts for Every Member

> [!IMPORTANT]
> Copy your prompt below and paste it into your AI coding assistant **while your project folder is open**. It will read the codebase, understand the plan, and guide you step by step.

---

### ðŸ‘‘ Member 6 â€” Team Lead & DevOps
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

---

### ðŸ›¡ï¸ Member 4 â€” Backend Security
**Branch:** `feature/M4-phase2-security`

```
I am Member 4 (Backend Security Engineer) on the CP Club Website project.
I need to implement Stage 1A of Phase 2.

IMPORTANT: Stage 0 (M6's work) must be merged into feature/phase-2 before I start. Branch from feature/phase-2.

Please do the following:
1. Read the full Phase 2 plan at: documents/phase_2_execution_playbook.md â€” focus on Section 5
2. Read SecurityConfig: backend/src/main/java/com/cpclub/backend/security/config/SecurityConfig.java
3. Read UserController: backend/src/main/java/com/cpclub/backend/user/controller/UserController.java

Then implement:
1. Add 8 new security rules to SecurityConfig.java in the correct order (public GET rules before Admin wildcard rules â€” Spring Security evaluates top-to-bottom)
2. Add GET /api/users/{id}/lookup to UserController with @PreAuthorize("hasRole('ADMIN')")
3. Add PUT /api/users/{id}/club-role to UserController with @PreAuthorize("hasRole('ADMIN')")

Run ./mvnw.cmd clean test and fix any failures before raising a PR into feature/phase-2.
```

---

### ðŸ“¦ Member 5 â€” Backend Data & APIs
**Branch:** `feature/M5-phase2-backend`

```
I am Member 5 (Backend Data & API Engineer) on the CP Club Website project.
I need to implement Stage 1B of Phase 2. This is the largest backend task.

IMPORTANT: Stage 0 (M6's work) must be merged into feature/phase-2 before I start. Branch from feature/phase-2.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md â€” focus on Sections 6.1 to 6.7
2. Read these files for context:
   - backend/src/main/java/com/cpclub/backend/user/dto/UserProfileUpdateRequest.java
   - backend/src/main/java/com/cpclub/backend/user/service/UserService.java
   - backend/src/main/java/com/cpclub/backend/user/dto/UserResponseDto.java
   - backend/src/main/java/com/cpclub/backend/codeforces/service/CodeforcesSyncService.java
   - backend/src/main/java/com/cpclub/backend/leaderboard/service/LeaderboardService.java
   - backend/pom.xml

Then implement in this order:
1. Update UserProfileUpdateRequest, UserResponseDto â€” add all new fields
2. Create UserLookupDto, UpdateClubRoleRequest â€” new DTOs
3. Update UserService â€” 3 new methods (updateClubRole, lookupUserById, update updateProfile)
4. Update UserRepository â€” 3 new query methods
5. Update LeaderboardService â€” platform and clubRole filter support
6. Create leetcode/ package â€” LeetCodeGraphQLResponse + LeetCodeSyncService
7. Update CodeforcesSyncService â€” call LeetCode bulk sync after CF sync
8. Create event/ package â€” entities, repos, all DTOs, EventService (with all guard clauses), EventExportService (Apache POI), EventController
9. Create gallery/ package â€” MemberGalleryPhoto entity, repo, DTOs, service, GalleryController
10. Create snapshot/ package â€” SnapshotService (weekly cron), SnapshotController
11. Add Apache POI dependency to pom.xml

Run ./mvnw.cmd clean test and fix all failures before raising a PR.
```

---

### ðŸŽ¨ Member 1 â€” Frontend UI/UX
**Branch:** `feature/M1-phase2-ui`

```
I am Member 1 (Frontend UI/UX Architect) on the CP Club Website project.
I need to implement Stage 2A of Phase 2 â€” new UI components and page layouts.

IMPORTANT: Stage 1 (M4 + M5 work) must be merged into feature/phase-2 before I start. Branch from feature/phase-2.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md â€” focus on Section 7
2. Look at existing components for the design language: frontend/src/components/site/ and frontend/src/app/events/page.tsx

Then create in this order:
1. components/ui/club-role-badge.tsx â€” small colored badge (Convenor=gold, Core=blue, Batch Rep=green, Ex-*=grey outline)
2. components/ui/data-table.tsx â€” generic table with loading skeleton and empty state
3. components/site/event-card.tsx â€” event card with cover image, title, date, location, status badge. Full card is a Link.
4. components/site/event-photo-grid.tsx â€” responsive photo grid with lightbox
5. components/site/member-gallery-grid.tsx â€” same grid for batch member photos
6. components/site/admin-tabs.tsx â€” tab nav with Members, Events, Galleries tabs
7. Modify app/events/page.tsx â€” replace hardcoded placeholders with server-side fetch from GET /api/events/upcoming
8. Create app/events/[id]/page.tsx â€” event detail page
9. Create app/gallery/page.tsx â€” member gallery with batch year dropdown filter
10. Create shell app/(dashboard)/admin/page.tsx â€” layout only, M3 fills data

Match the dark-mode design: glass-panel, rounded-panel, Eyebrow, Section components.
Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

### ðŸ§  Member 2 â€” Frontend Auth & State
**Branch:** `feature/M2-phase2-auth`

```
I am Member 2 (Frontend Auth & Logic Engineer) on the CP Club Website project.
I need to implement Stage 2B of Phase 2 â€” TypeScript types, store updates, and all API service files.

IMPORTANT: Stage 1 must be merged into feature/phase-2 before I start. Branch from feature/phase-2.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md â€” focus on Section 8
2. Read these files:
   - frontend/src/store/auth.ts
   - frontend/src/lib/services/dashboard.ts
   - frontend/src/lib/axios.ts

Then implement in this order:
1. Update store/auth.ts â€” add clubRole, batchYear, leetcodeHandle, phoneNumber, avatarUrl to the User interface
2. Update/create types/api.ts â€” add Event, EventDetail, EventAttendee, EventPhoto, UserLookup, MemberGalleryPhoto interfaces and ClubRole string union type
3. Create lib/services/events.ts â€” 17 functions covering all event, attendee, photo, and user lookup API calls. The exportAttendees function must use responseType: 'blob'.
4. Create lib/services/gallery.ts â€” 4 functions for member gallery
5. Update lib/services/dashboard.ts â€” mapUserToProfile() must map all new fields using ?? null fallback
6. Update lib/services/leaderboard.ts â€” pass ?platform= and ?filter= query params

Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

### ðŸ“Š Member 3 â€” Frontend Dashboards & Data
**Branch:** `feature/M3-phase2-dashboards`

```
I am Member 3 (Frontend Dashboards Engineer) on the CP Club Website project.
I need to implement Stage 2C of Phase 2 â€” all dashboard pages and data-heavy features.

IMPORTANT: Member 1 AND Member 2 PRs must both be merged into feature/phase-2 before I start. Branch from feature/phase-2 only after both are merged.

Please do the following:
1. Read the full Phase 2 plan: documents/phase_2_execution_playbook.md â€” focus on Section 9
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
   - Left panel: Student ID input â†’ Search â†’ preview card (show warning if no phone) â†’ Add to Event button
   - Right panel: attendee DataTable + Export to Excel button (blob download)

4. Update app/(dashboard)/leaderboard/page.tsx:
   - Add platform pill toggles: Codeforces | LeetCode
   - Add club filter pill toggles: All | Core | Batch Rep | Students
   - Add Club Role column with ClubRoleBadge

Run npm run build to verify zero TypeScript errors before raising a PR.
```

---

## Section 1 â€” Locked Decisions & Rationale

| Decision | Choice | Why |
|---|---|---|
| CodeChef / AtCoder sync | **No sync. Link only.** | No stable public API. Scraping breaks on DOM changes. |
| LeetCode sync | **Full GraphQL API sync.** JIT on handle save + every 6h cron. | Reliable public endpoint. |
| Avatar | **Cloudinary Upload Widget.** URL saved to DB. | Eliminates binary blobs in PostgreSQL. |
| Phone number | **Mandatory on user profile.** | Required before admin can add a student to an event. |
| Event attendance | **Admin-only.** Admin searches by User ID; details auto-fill from DB. | Official record, not self-controlled. |
| Excel export | **Apache POI XSSF.** Server-side `.xlsx` streamed as binary. | Universal format for club admins. |
| Soft delete for events | Events are **deactivated**, never hard deleted. | Preserves attendance + gallery history. |
| `clubRole` vs `role` | **Two separate fields.** `role` = platform permission (Admin/User). `clubRole` = club position (Core, Convenor, etc.). | Conflating them causes security issues â€” a Batch Rep should not have admin API access. |
| Gallery storage | **Cloudinary.** Admin uploads image via Upload Widget; URL stored in DB. | Consistent with avatar approach. No binary in DB. |

---

## Section 2 â€” New Features Added in This Version

The following features are **new additions** on top of the original Phase 2 scope:

1. **Club Role System** â€” `clubRole` enum column on `users`. Admin can assign any member a club position. Leaderboard filters (All, Core, Batch Rep, Students) become data-driven.
2. **Admin Event Management** â€” Admin creates, edits, and publishes upcoming events directly from the website (no hardcoding).
3. **Event Photo Gallery** â€” Admin uploads event photos (past events) to Cloudinary; displayed in a public gallery per event.
4. **Club Member Photo Gallery (Batch-wise)** â€” Admin uploads group/batch photos; displayed publicly, filterable by batch year.
5. **Completed Events** â€” Past events shown publicly with date, description, location, and their photo gallery.

---

## Section 3 â€” Implementation Stages & Dependencies

```
STAGE 0 â”€â”€â”€ DB Migrations + Entity Layer
              Owner: M6 Â· PR: phase2/stage-0
              Must merge before ANY other branch starts.
                    â”‚
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â–¼                       â–¼
STAGE 1A â”€ Backend Security   STAGE 1B â”€ Backend Data & APIs
  Owner: M4                     Owner: M5
  PR: phase2/backend-security   PR: phase2/backend-data
  Runs parallel with M5         Runs parallel with M4
        â”‚                               â”‚
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                    â–¼
     Both Stage 1 PRs merged into feature/phase-2
                    â”‚
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â–¼           â–¼           â–¼
STAGE 2A         STAGE 2B    STAGE 2C
Frontend UI      Auth/State  Dashboards
Owner: M1        Owner: M2   Owner: M3
                             (depends on M1 + M2 first)
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                    â–¼
STAGE 3 â”€â”€â”€ Integration, Tests, Polish, Deploy
              Owner: M6 Â· PR: phase2/integration
```

> [!IMPORTANT]
> Every member branches off `feature/phase-2`, **not off `main`**. PRs go back into `feature/phase-2`. Only when all Phase 2 work is done does `feature/phase-2` merge into `main` via one final PR reviewed by M6.

---

## Section 4 â€” Stage 0: Database Migrations & Entity Layer
**Owner: Member 6 (Team Lead)**
**PR: `phase2/stage-0-migrations`**
**Estimated time: 2 days**
**Blocks: All other members**

---

### 4.1 Migration V2 â€” Extend `users` Table

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
| `batch_year` | INTEGER | nullable | e.g. 2023, 2024 â€” used for batch gallery filtering |

**`club_role` allowed values (CHECK constraint):**
`CONVENOR`, `DEPUTY_CONVENOR`, `CORE`, `ASSOCIATE_CORE`, `BATCH_REPRESENTATIVE`, `EX_PC_MEMBER`, `EX_CORE`, `EX_CDC`, `STUDENT`

> [!NOTE]
> `STUDENT` is the default when `club_role` is set but the member holds no special position. `null` means unassigned (shown as "Member" on the frontend). This is distinct from `role` (ROLE_ADMIN / ROLE_USER) which controls API access.

**Indexes to add:**
- `CREATE INDEX idx_leetcode_handle ON users(leetcode_handle)`
- `CREATE INDEX idx_club_role ON users(club_role)` â€” for leaderboard filter queries

---

### 4.2 Migration V3 â€” Create `events` Table

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
| `created_by` | BIGINT | FK â†’ users.id NOT NULL | Admin who created it |
| `created_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | Audit |
| `updated_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | Audit |

> [!NOTE]
> `status` replaces the earlier `is_active` boolean. It gives 3 states: upcoming (public, accepting attendance), completed (public, read-only, gallery shown), cancelled (hidden from public).

---

### 4.3 Migration V4 â€” Create `event_attendees` Table

**File:** `V4__create_event_attendees.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `event_id` | BIGINT | FK â†’ events.id ON DELETE CASCADE, NOT NULL | |
| `user_id` | BIGINT | FK â†’ users.id ON DELETE CASCADE, NOT NULL | |
| `added_by` | BIGINT | FK â†’ users.id NOT NULL | Which admin added the student |
| `added_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |
| **UNIQUE(event_id, user_id)** | | DB-level constraint | Prevents double-attendance |

**Indexes:** on `event_id` and `user_id` separately.

---

### 4.4 Migration V5 â€” Create `event_photos` Table

**File:** `V5__create_event_photos.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `event_id` | BIGINT | FK â†’ events.id ON DELETE CASCADE, NOT NULL | Which event this photo belongs to |
| `image_url` | VARCHAR(512) | NOT NULL | Cloudinary secure URL |
| `caption` | VARCHAR(255) | nullable | Optional photo caption |
| `uploaded_by` | BIGINT | FK â†’ users.id NOT NULL | Admin who uploaded |
| `uploaded_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |

**Index:** on `event_id` for fast gallery load.

---

### 4.5 Migration V6 â€” Create `member_gallery` Table

**File:** `V6__create_member_gallery.sql`

For batch-wise group photos of club members, separate from individual avatars.

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `batch_year` | INTEGER | NOT NULL | e.g. 2023, 2024 |
| `image_url` | VARCHAR(512) | NOT NULL | Cloudinary URL |
| `caption` | VARCHAR(255) | nullable | e.g. "Batch 2023 - Inauguration Day" |
| `uploaded_by` | BIGINT | FK â†’ users.id NOT NULL | Admin who uploaded |
| `uploaded_at` | TIMESTAMP(6) | NOT NULL DEFAULT NOW() | |

**Index:** on `batch_year` for fast batch-filter queries.

---

### 4.6 Migration V7 â€” Create `weekly_snapshots` Table

**File:** `V7__create_weekly_snapshots.sql`

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK â†’ users.id ON DELETE CASCADE, NOT NULL | |
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

## Section 5 â€” Stage 1A: Backend Security
**Owner: Member 4**
**PR: `phase2/backend-security`**
**Estimated time: 1â€“2 days**
**Depends on: Stage 0 merged**

### Files & What Changes

#### `SecurityConfig.java`
Add the following rules **before** `anyRequest().authenticated()`, in this exact order (top-to-bottom matters):

| Rule | Path Pattern | Auth Required |
|---|---|---|
| Public â€” list & view events | `GET /api/events`, `GET /api/events/{id}` | None |
| Public â€” event gallery | `GET /api/events/{id}/photos` | None |
| Public â€” member gallery | `GET /api/gallery/members` | None |
| Admin â€” all other event ops | `/api/events/**` | `hasRole('ADMIN')` |
| Admin â€” gallery upload | `/api/gallery/**` | `hasRole('ADMIN')` |
| Admin â€” user lookup for event | `GET /api/users/{id}/lookup` | `hasRole('ADMIN')` |
| Admin â€” assign club role | `PUT /api/users/{id}/club-role` | `hasRole('ADMIN')` |
| Authenticated â€” snapshots | `GET /api/snapshots/**` | `isAuthenticated()` |

#### `UserController.java`
Add two new endpoints:
1. `GET /api/users/{id}/lookup` â€” `@PreAuthorize("hasRole('ADMIN')")` â€” returns `UserLookupDto`. Calls `userService.lookupUserById(id)`.
2. `PUT /api/users/{id}/club-role` â€” `@PreAuthorize("hasRole('ADMIN')")` â€” body: `{ clubRole: "CORE" }`. Returns updated `UserResponseDto`. Calls `userService.updateClubRole(id, request)`.

---

## Section 6 â€” Stage 1B: Backend Data & APIs
**Owner: Member 5**
**PR: `phase2/backend-data`**
**Estimated time: 6â€“7 days**
**Depends on: Stage 0 merged**
**This is the largest single task in Phase 2.**

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
- `updateProfile()` â€” persist all new fields. If `leetcodeHandle` changed â†’ call `leetCodeSyncService.syncSingleUser()`. If `codeforcesHandle` changed â†’ call `codeforcesSyncService.syncSingleUser()`.
- `updateClubRole(Long userId, UpdateClubRoleRequest req)` â†’ loads user, sets `clubRole`, saves, returns `UserResponseDto`.
- `lookupUserById(Long id)` â†’ loads user, returns `UserLookupDto.fromEntity(user)`. Throws `ResourceNotFoundException` if missing.

#### `UserRepository.java` [MODIFY]
Add:
- `List<User> findByLeetcodeHandleIsNotNull()` â€” for bulk LeetCode sync
- `boolean existsByLeetcodeHandle(String handle)` â€” for uniqueness check
- `List<User> findByClubRoleIn(List<ClubRole> roles)` â€” for leaderboard filtering

---

### 6.2 Leaderboard Enhancement

#### `LeaderboardService.java` [MODIFY]
The existing leaderboard currently sorts by rating. Extend it to accept two query params:
- `platform` â€” `CODEFORCES` (default) or `LEETCODE`
- `filter` â€” `ALL` (default), `CORE`, `BATCH_REP`, `STUDENTS`

Filter logic:
- `ALL` â†’ no role filter, all users with a rating
- `CORE` â†’ `clubRole IN (CORE, ASSOCIATE_CORE, CONVENOR, DEPUTY_CONVENOR)`
- `BATCH_REP` â†’ `clubRole = BATCH_REPRESENTATIVE`
- `STUDENTS` â†’ `clubRole = STUDENT OR clubRole IS NULL`

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
- `syncSingleUser(User)` â€” JIT sync on handle save. Guards: null handle â†’ skip. Acquires rate limiter permit. POSTs GraphQL query. Parses response. If `userContestRanking` is null (user never did a contest), sets rating to `0`. On any HTTP error: logs warning, does NOT throw â€” fail-silently so profile saving still works.
- `syncAllUsers()` â€” bulk sync called by cron. Iterates all users with non-null `leetcodeHandle`. Same logic per user. Logs total updated count.

#### `CodeforcesSyncService.java` [MODIFY]
After the existing `syncCodeforcesRatings()` cron method finishes, add a call to `leetCodeSyncService.syncAllUsers()`. Inject `LeetCodeSyncService` via constructor.

---

### 6.4 Event Package [NEW]

**Package:** `com.cpclub.backend.event`

#### Entities
- `Event.java` â€” mirrors `events` table. Has `@Enumerated(EnumType.STRING) EventStatus status`. `createdBy` is `@ManyToOne(fetch=LAZY)` to User.
- `EventAttendee.java` â€” mirrors `event_attendees`. Three `@ManyToOne` links: event, user (student), addedBy (admin). `@UniqueConstraint` on `(event_id, user_id)`.
- `EventPhoto.java` â€” mirrors `event_photos`. `@ManyToOne` to Event. Fields: imageUrl, caption, uploadedBy, uploadedAt.

#### Repositories
- `EventRepository` â€” `findByStatusOrderByEventDateAsc(EventStatus)`, `findAllByOrderByEventDateDesc()` (admin view), `findByStatusInOrderByEventDateDesc(List<EventStatus>)`.
- `EventAttendeeRepository` â€” `findByEventIdOrderByAddedAtAsc(Long)`, `existsByEventIdAndUserId(Long, Long)`, `findByEventIdAndUserId(Long, Long)`.
- `EventPhotoRepository` â€” `findByEventIdOrderByUploadedAtAsc(Long)`.

#### DTOs
- `EventCreateRequest` â€” `title (@NotBlank)`, `description`, `eventDate (@NotNull)`, `location (@NotBlank)`, `coverImageUrl`.
- `EventResponseDto` â€” `id`, `title`, `description`, `eventDate`, `location`, `status`, `coverImageUrl`, `createdByName`, `createdAt`. Static `fromEntity(Event)`.
- `EventDetailDto` â€” extends `EventResponseDto` and adds `List<EventPhotoDto>` photos and `Integer attendeeCount`. Used for the public event detail page.
- `AddAttendeeRequest` â€” single `userId (@NotNull Long)`.
- `EventAttendeeDto` â€” 14 fields: userId, name, email, phoneNumber, avatarUrl, codeforcesHandle, cfRating, leetcodeHandle, leetcodeRating, codechefUrl, atcoderUrl, githubUrl, linkedinUrl, addedAt, clubRole. Static `fromEntity(EventAttendee)`.
- `EventPhotoDto` â€” `id`, `imageUrl`, `caption`, `uploadedAt`. Static `fromEntity(EventPhoto)`.
- `AddEventPhotoRequest` â€” `imageUrl (@NotBlank @Size(max=512))`, `caption`.

#### `EventService.java`
Methods and their exact behavior:

`createEvent(EventCreateRequest, String adminEmail)` â†’ `EventResponseDto`
- Loads admin by email. Creates Event with status=UPCOMING. Saves. Returns DTO.

`listUpcomingEvents()` â†’ `List<EventResponseDto>`
- Fetches events with `status = UPCOMING`, ordered by `eventDate ASC`.

`listCompletedEvents()` â†’ `List<EventResponseDto>`
- Fetches events with `status = COMPLETED`, ordered by `eventDate DESC`.

`getEventDetail(Long id)` â†’ `EventDetailDto`
- Loads event (404 if not found). Loads its photos. Gets attendee count. Returns combined DTO.

`updateEvent(Long id, EventCreateRequest)` â†’ `EventResponseDto`
- Loads event. Throws 404 if not found. Updates fields. Saves. Returns DTO.

`markEventCompleted(Long id)` â†’ `EventResponseDto`
- Loads event. Sets `status = COMPLETED`. Saves. Returns DTO.

`cancelEvent(Long id)` â†’ `EventResponseDto`
- Loads event. Sets `status = CANCELLED`. Saves. Returns DTO.

`addAttendee(Long eventId, Long userId, String adminEmail)` â†’ `EventAttendeeDto`
Guard clauses (in this exact order):
1. Load event. Throw `ResourceNotFoundException` if missing.
2. Check event status is `UPCOMING`. Throw `BadRequestException("Cannot add attendees to a completed or cancelled event.")` if not.
3. Load user. Throw `ResourceNotFoundException("Student not found")` if missing.
4. Check `user.phoneNumber` not null/blank. Throw `BadRequestException("Student has no phone number on their profile.")` if missing.
5. Check `existsByEventIdAndUserId`. Throw `BadRequestException("Student is already registered.")` if true.
6. Load admin. Save `EventAttendee`. Return DTO.

`removeAttendee(Long eventId, Long userId)` â€” finds attendee, deletes. Throws 404 if not found.

`getAttendees(Long eventId)` â†’ `List<EventAttendeeDto>` â€” loads event first (404 guard), then fetches attendees.

`addEventPhoto(Long eventId, AddEventPhotoRequest, String adminEmail)` â†’ `EventPhotoDto`
- Loads event. Creates `EventPhoto` with imageUrl, caption, uploadedBy admin. Saves. Returns DTO.

`deleteEventPhoto(Long photoId)` â€” finds photo, deletes. Throws 404 if not found.

`getEventPhotos(Long eventId)` â†’ `List<EventPhotoDto>` â€” loads event (404 guard), fetches photos ordered by upload time.

#### `EventExportService.java`
Uses Apache POI XSSF. Method: `exportToExcel(List<EventAttendeeDto>)` â†’ `byte[]`.

Excel columns (14): ID, Name, Email, Phone Number, Club Role, Avatar URL, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef URL, AtCoder URL, GitHub, LinkedIn, Added At.
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

### 6.5 Gallery Package [NEW]

**Package:** `com.cpclub.backend.gallery`

#### Entities
- `MemberGalleryPhoto.java` â€” mirrors `member_gallery` table. Fields: batchYear (Integer), imageUrl, caption, uploadedBy (ManyToOne to User), uploadedAt.

#### Repository
- `MemberGalleryRepository` â€” `findByBatchYearOrderByUploadedAtAsc(Integer)`, `findDistinctBatchYearsOrderByDesc()` (returns `List<Integer>` of all unique years in the DB, for the frontend filter dropdown).

#### DTOs
- `MemberGalleryPhotoDto` â€” `id`, `batchYear`, `imageUrl`, `caption`, `uploadedAt`. Static `fromEntity`.
- `AddMemberPhotoRequest` â€” `batchYear (@NotNull @Min(2000) @Max(2100))`, `imageUrl (@NotBlank)`, `caption`.

#### `MemberGalleryService.java`
- `addPhoto(AddMemberPhotoRequest, String adminEmail)` â†’ `MemberGalleryPhotoDto`
- `deletePhoto(Long id)` â€” 404 if not found
- `getPhotosByBatch(Integer batchYear)` â†’ `List<MemberGalleryPhotoDto>`
- `getAvailableBatchYears()` â†’ `List<Integer>` â€” for frontend dropdown

#### `GalleryController.java`
Base path: `/api/gallery/members`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/gallery/members` | Admin | Upload a member gallery photo |
| DELETE | `/api/gallery/members/{id}` | Admin | Delete a photo |
| GET | `/api/gallery/members?batch={year}` | Public | Photos for a specific batch year |
| GET | `/api/gallery/members/batches` | Public | List of all available batch years |

---

### 6.6 Snapshot Package [NEW]

**Package:** `com.cpclub.backend.snapshot`

- `WeeklySnapshot.java` â€” JPA entity.
- `WeeklySnapshotRepository.java` â€” `findByUserIdAndPlatformOrderByRecordedAtAsc(Long, String)`.
- `SnapshotService.java` â€” `@Scheduled(cron = "0 0 0 * * MON")`. Records current CF and LeetCode ratings for ALL users who have them into `weekly_snapshots`. Does NOT re-fetch from APIs â€” snapshots the already-fresh DB values.
- `SnapshotController.java` â€” `GET /api/snapshots/{userId}/codeforces` and `GET /api/snapshots/{userId}/leetcode`. Returns `List<{date, rating}>`. Auth: `isAuthenticated()`.

---

### 6.7 `pom.xml` Update
Add Apache POI XSSF: `org.apache.poi:poi-ooxml:5.3.0`.

---

## Section 7 â€” Stage 2A: Frontend UI/UX
**Owner: Member 1**
**PR: `phase2/frontend-ui`**
**Estimated time: 3 days**

### Files & Responsibilities

| File | Action | What it does |
|---|---|---|
| `components/site/event-card.tsx` | NEW | Card for a single event. Shows: cover image (if any), title, date formatted as "Sat 15 Nov Â· 3:00 PM", location, status badge (green=Upcoming, grey=Completed, red=Cancelled). Entire card is a `<Link href="/events/{id}">`. |
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

## Section 8 â€” Stage 2B: Frontend Auth & State
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

## Section 9 â€” Stage 2C: Frontend Dashboards & Data
**Owner: Member 3**
**PR: `phase2/frontend-dashboards`**
**Depends on: M1 + M2 PRs merged**
**Estimated time: 5 days**

### Files & Responsibilities

#### `components/site/profile-dashboard.tsx` [MAJOR MODIFY]

**Change 1 â€” Avatar:**
If `profile.avatarUrl` exists â†’ render image with CF rank border. If owner and no avatar â†’ show placeholder icon + "Upload photo" button. Click opens Cloudinary Upload Widget (`NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME`). On success â†’ `dashboardService.updateProfile({ avatarUrl: result.info.secure_url })` â†’ `loadProfile()`.

**Change 2 â€” Club Role & Batch Year badge:**
Below the member's name in the profile header, show their `<ClubRoleBadge clubRole={profile.clubRole} />` (the M1 component). If no club role, show nothing.

**Change 3 â€” Platform links in header:**
Row of icon links below CF handle. Only rendered if the URL is set: CodeChef, AtCoder, GitHub, LinkedIn. Each is `<a href="..." target="_blank" rel="noopener noreferrer">`.

**Change 4 â€” Edit Profile panel:**
Replace the inline CF handle edit form with a full "Edit Profile" panel (owner-only). Fields: Name (required), Phone Number (required â€” cannot save without it), Codeforces Handle, LeetCode Handle, CodeChef URL, AtCoder URL, GitHub URL, LinkedIn URL. Phone shown masked (`â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢`) to visitors. On save: `PUT /api/users/profile`. On success: refresh. On error: show inline error.

**Change 5 â€” Rating charts:**
Replace "Coming Soon" overlay with live charts using `recharts`. CF chart: `GET /api/snapshots/{userId}/codeforces`. LeetCode chart: `GET /api/snapshots/{userId}/leetcode`. If < 2 points: "Not enough data yet." Reuse Phase 1 chart styling.

---

#### `app/(dashboard)/admin/page.tsx` [FILL IN]

**Tab 1 â€” Members:**
Fetches `GET /api/users/all`. Renders `<DataTable>` with columns:

| Column | Value | Actions |
|---|---|---|
| Avatar | small rounded image | â€” |
| Name | `user.name` | â€” |
| Email | `user.email` | â€” |
| Phone | `user.phoneNumber \|\| 'â€”'` | â€” |
| CF Handle + Rating | â€” | â€” |
| LeetCode Handle + Rating | â€” | â€” |
| Club Role | `<ClubRoleBadge />` | "Change Role" dropdown |
| Batch Year | `user.batchYear \|\| 'â€”'` | â€” |
| Platform Role | Admin / User badge | "Promote" / "Demote" button |
| Actions | â€” | "Delete" button with confirmation dialog |

"Change Club Role" â€” inline dropdown with all `ClubRole` values. On select: calls `PUT /api/users/{id}/club-role`. Refreshes table.
"Promote/Demote" â€” calls `PUT /api/users/{id}/role`. Refreshes table.
"Delete" â€” confirmation dialog. Calls `DELETE /api/users/{id}`. Refreshes table.

**Tab 2 â€” Events:**
- "Create New Event" form: Title (required), Description (optional), Date+Time (datetime-local input), Location (required), Cover Image (Cloudinary Upload Widget button â€” inserts URL into hidden field). Submit â†’ `POST /api/events`. Refreshes list.
- Table of ALL events (admin view from `GET /api/events`). Columns: Title, Date, Location, Status badge, Cover Image thumbnail, Actions.
- Per-event actions: "Edit" (inline form), "Mark Completed", "Cancel", "Manage Attendees" â†’ navigates to `/admin/events/{id}`.

**Tab 3 â€” Galleries:**
- **Member Gallery sub-tab:**
  - Batch year input (number) + Cloudinary upload button + Caption field. Submit â†’ `POST /api/gallery/members`. Refreshes grid.
  - Grid of uploaded photos, grouped by batch year. "Delete" button per photo.
- **Event Galleries sub-tab:**
  - Dropdown to select event. Once selected, shows existing photos for that event + upload form. Submit â†’ `POST /api/events/{id}/photos`. Delete per photo.

---

#### `app/(dashboard)/admin/events/[id]/page.tsx` [NEW]

Two-column layout (stacks on mobile):

**Left â€” Student Search Panel:**
- Input: "Enter Student ID". Search button.
- On search: `eventsService.lookupUser(id)`. Shows loading state.
- Error states: non-numeric input â†’ inline validation. 404 â†’ "No student found with this ID." 
- Preview card after successful search: avatar, name, email, phone (or red warning "âš ï¸ No phone â€” ask student to update profile"), CF + LeetCode data, club role badge, social links.
- "Add to Event" button: disabled if no student loaded, no phone number, or already in attendee list. Shows "Already Added âœ“" chip if already registered.
- On Add: `eventsService.addAttendee(eventId, userId)` â†’ show success toast â†’ clear search panel â†’ refresh attendee list.

**Right â€” Attendee Table:**
- Header: event title, attendee count badge, "Export to Excel" button.
- Excel export: `eventsService.exportAttendees(eventId)` (responseType: blob) â†’ `URL.createObjectURL(new Blob([res.data]))` â†’ programmatic `<a>` click â†’ `URL.revokeObjectURL()`.
- `<DataTable>` with columns: Name, Email, Phone, Club Role, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef, AtCoder, GitHub, LinkedIn, Added At, Remove.
- "Remove" per row: confirmation dialog â†’ `eventsService.removeAttendee(eventId, userId)` â†’ refresh.

---

#### `app/(dashboard)/leaderboard/page.tsx` [MODIFY]

Add two sets of filter controls above the leaderboard table:

**Platform toggles:** "Codeforces | LeetCode" â€” pill buttons. Updates `platform` state and re-fetches.

**Club filter toggles:** "All | Core | Batch Rep | Students" â€” these are the **existing UI filters** the teammate mentioned. Now they become data-driven (not just cosmetic). Updates `filter` state and re-fetches with `?filter=CORE` etc.

In the leaderboard table, add a "Club Role" column showing `<ClubRoleBadge />` for each member.

---

## Section 10 â€” Stage 3: Integration, Tests & Polish
**Owner: Member 6**
**PR: `phase2/integration-and-tests`**
**Estimated time: 2â€“3 days**

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

### Environment Variables for Render
```
CLOUDINARY_CLOUD_NAME=...          (backend, if needed)
NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME= (frontend Upload Widget)
```

---

## Section 11 â€” Full API Contract Reference

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

## Section 12 â€” 6-Member Summary

| Member | Role | Stage | Owns |
|---|---|---|---|
| **M6 (Lead)** | Foundation + DevOps | 0 + 3 | All 6 Flyway migrations, all entity files, `ClubRole` enum, `EventStatus` enum, LeetCode cron wiring, snapshot cron, all unit tests, Render env vars, PR reviews, final merge |
| **M4** | Backend Security | 1A | `SecurityConfig` new rules (all 8 rule blocks), `UserController` lookup endpoint, `UserController` club-role endpoint |
| **M5** | Backend Data + APIs | 1B | `UserProfileUpdateRequest` update, `UpdateClubRoleRequest`, `UserLookupDto`, `UserResponseDto` update, `UserService` 3 new methods, `UserRepository` 3 new methods, leaderboard filter by `clubRole`, entire `leetcode/` package, entire `event/` package (5 DTOs + 3 repos + 2 services + 1 controller), entire `gallery/` package (2 DTOs + 1 repo + 1 service + 1 controller), entire `snapshot/` package, `pom.xml` POI dep |
| **M1** | Frontend UI/UX | 2A | `EventCard`, `EventPhotoGrid`, `MemberGalleryGrid`, `DataTable`, `ClubRoleBadge`, `AdminTabs`, Events page redesign, Event detail page, Member Gallery page |
| **M2** | Frontend State + Auth | 2B | `auth.ts` new User fields, `types/api.ts` 6 new types + ClubRole union, `events.ts` service (17 functions), `gallery.ts` service (4 functions), `dashboard.ts` mapper update, `leaderboard.ts` params update |
| **M3** | Frontend Dashboards | 2C | Profile dashboard (Cloudinary avatar, club role badge, platform links, edit panel, rating charts), Admin dashboard all 3 tabs (Members with club role assignment, Events CRUD + gallery upload, Gallery management), Admin event attendee page (search + auto-fill + table + Excel download), Leaderboard platform + club filter toggles |
