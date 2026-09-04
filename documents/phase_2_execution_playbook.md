# Phase 2 â€” Senior Engineer Implementation Plan
**Branch:** `feature/phase-2` (branched off `main` post Phase 1 merge)  
**Duration:** 3 sprints Â· ~3 weeks  
**Team:** 6 members  
**Stack:** Spring Boot 4.1 Â· PostgreSQL Â· Next.js 15 Â· Cloudinary Â· Apache POI

---

## Section 1 â€” Locked Decisions & Rationale

| Decision | Choice | Why |
|---|---|---|
| CodeChef / AtCoder sync | **No sync. Link only.** User pastes their profile URL. | No stable public API. Scraping breaks on DOM changes. Not worth maintenance cost. |
| LeetCode sync | **Full sync via GraphQL API.** JIT on handle save + every 6h cron. | LeetCode exposes a public GraphQL endpoint with contest rating. Reliable. |
| Avatar | **Cloudinary Upload Widget.** URL saved to DB. | Account already exists. Eliminates storing binary blobs in PostgreSQL. |
| Phone number | **Mandatory on user profile.** Cannot be blank after saving profile. | Required for admin to add a student to an event without a separate form. |
| Event attendance | **Admin-only.** Admin searches by User ID. Details auto-fill. | No self-RSVP. Attendance is official record, not user-controlled. |
| Excel export | **Apache POI XSSF.** Server-side `.xlsx` streamed as binary download. | Most universal format for club administrators. |
| Soft delete for events | Events are **deactivated** (`is_active = false`), never hard deleted. | Preserves attendance history. Admin can reactivate if needed. |

---

## Section 2 â€” Implementation Stages & Dependencies

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
  Can run parallel with M5      Can run parallel with M4
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
> Every member branches off `feature/phase-2`, **not off `main`**. PRs are submitted back into `feature/phase-2`. Only when the entire Phase 2 is complete does `feature/phase-2` get merged into `main` via one final PR reviewed by M6.

---

## Section 3 â€” Stage 0: Database Migrations & Entity Layer
**Owner: Member 6**  
**PR: `phase2/stage-0-migrations`**  
**Estimated time: 1â€“2 days**  
**Blocks: All other members**

---

### 3.1 Migration V2 â€” Extend `users` Table

**File:** `backend/src/main/resources/db/migration/V2__extend_user_profile.sql`

Add the following **nullable** columns to `users`. All nullable â€” existing rows have no data for these fields, which is correct.

| Column Name | Data Type | Constraint | Reason |
|---|---|---|---|
| `avatar_url` | `VARCHAR(512)` | nullable | Cloudinary secure URL, up to 512 chars |
| `phone_number` | `VARCHAR(20)` | nullable | Stored even though UI enforces mandatory â€” allows admin-added users pre-migration |
| `leetcode_handle` | `VARCHAR(255)` | `UNIQUE`, nullable | For LeetCode sync |
| `leetcode_rating` | `INTEGER` | nullable | Populated by sync job |
| `codechef_url` | `VARCHAR(512)` | nullable | Profile link only |
| `atcoder_url` | `VARCHAR(512)` | nullable | Profile link only |
| `github_url` | `VARCHAR(512)` | nullable | Social link |
| `linkedin_url` | `VARCHAR(512)` | nullable | Social link |

**Index to add:**
- `CREATE INDEX idx_leetcode_handle ON users (leetcode_handle)` â€” the sync job looks up users by this field, needs to be fast.

---

### 3.2 Migration V3 â€” Create `events` Table

**File:** `backend/src/main/resources/db/migration/V3__create_events.sql`

| Column Name | Data Type | Constraint | Reason |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment |
| `title` | `VARCHAR(255)` | `NOT NULL` | Event name |
| `description` | `TEXT` | nullable | Optional long description |
| `event_date` | `TIMESTAMP` | `NOT NULL` | When the event happens |
| `location` | `VARCHAR(255)` | `NOT NULL` | Physical or online location |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Soft delete flag |
| `created_by` | `BIGINT` | `FK â†’ users.id NOT NULL` | Which admin created it |
| `created_at` | `TIMESTAMP(6)` | `NOT NULL DEFAULT NOW()` | Audit |
| `updated_at` | `TIMESTAMP(6)` | `NOT NULL DEFAULT NOW()` | Audit |

---

### 3.3 Migration V4 â€” Create `event_attendees` Table

**File:** `backend/src/main/resources/db/migration/V4__create_event_attendees.sql`

| Column Name | Data Type | Constraint | Reason |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment |
| `event_id` | `BIGINT` | `FK â†’ events.id ON DELETE CASCADE, NOT NULL` | Link to event |
| `user_id` | `BIGINT` | `FK â†’ users.id ON DELETE CASCADE, NOT NULL` | Link to student |
| `added_by` | `BIGINT` | `FK â†’ users.id NOT NULL` | Which admin added this student |
| `added_at` | `TIMESTAMP(6)` | `NOT NULL DEFAULT NOW()` | Audit â€” when attendance was recorded |

**Critical constraint:** `UNIQUE(event_id, user_id)` â€” enforced at the **database level**, not just in application code. Even if a bug in the service layer tries to insert a duplicate, the DB rejects it.

**Indexes:**
- `CREATE INDEX idx_attendees_event ON event_attendees(event_id)` â€” for fast "get all attendees for event" queries
- `CREATE INDEX idx_attendees_user ON event_attendees(user_id)` â€” for fast "which events has this student attended" queries

---

### 3.4 Migration V5 â€” Create `weekly_snapshots` Table

**File:** `backend/src/main/resources/db/migration/V5__create_weekly_snapshots.sql`

| Column Name | Data Type | Constraint | Reason |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment |
| `user_id` | `BIGINT` | `FK â†’ users.id ON DELETE CASCADE, NOT NULL` | Whose rating this is |
| `platform` | `VARCHAR(20)` | `NOT NULL CHECK IN ('CODEFORCES','LEETCODE')` | Which platform |
| `rating` | `INTEGER` | `NOT NULL` | Rating at time of recording |
| `recorded_at` | `TIMESTAMP(6)` | `NOT NULL DEFAULT NOW()` | When snapshot was taken |

**Index:** `CREATE INDEX idx_snapshots_user_platform ON weekly_snapshots(user_id, platform)` â€” the chart query always filters by both user_id and platform, then orders by recorded_at.

---

### 3.5 Update `User.java` JPA Entity

**File:** `backend/src/main/java/com/cpclub/backend/user/entity/User.java`

Add 8 new private fields with `@Column` annotations that **exactly match** the migration column names. Also add the `leetcode_handle` index to `@Table(indexes = {...})`.

> [!WARNING]
> Column names in `@Column(name = "...")` must exactly match migration SQL. A mismatch causes `ddl-auto: validate` to fail on startup with a `SchemaManagementException`.

---

## Section 4 â€” Stage 1A: Backend Security
**Owner: Member 4**  
**PR: `phase2/backend-security`**  
**Estimated time: 1 day**  
**Depends on: Stage 0 merged**

---

### 4.1 Update `SecurityConfig.java`

**File:** `backend/src/main/java/com/cpclub/backend/security/config/SecurityConfig.java`

The `filterChain` method's `authorizeHttpRequests` block needs 3 new rules, inserted **before** the existing `anyRequest().authenticated()` catch-all.

**Rule 1 â€” Public event reads:**
```
GET /api/events         â†’ permitAll()
GET /api/events/{id}    â†’ permitAll()
```

**Rule 2 â€” Admin-only event management:**
```
/api/events/**          â†’ hasRole('ADMIN')
```

**Rule 3 â€” Admin-only user lookup:**
```
GET /api/users/{id}/lookup  â†’ hasRole('ADMIN')
```

**Why the order matters:** Spring Security evaluates rules top-to-bottom and stops at the first match. If Rule 2 (`/api/events/**` â†’ ADMIN) appears before Rule 1 (`GET /api/events` â†’ public), the public GET is blocked. Rule 1 must come first.

**Also add to the public snapshot endpoint:**
```
GET /api/snapshots/**   â†’ isAuthenticated()
```

---

### 4.2 Add Lookup Endpoint in `UserController.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/controller/UserController.java`

Add one new `@GetMapping("/{id}/lookup")` method:
- **Auth:** `@PreAuthorize("hasRole('ADMIN')")` â€” only admins can call this
- **Input:** `@PathVariable Long id`
- **Calls:** `userService.lookupUserById(id)` (defined by M5)
- **Returns:** `ResponseEntity<ApiResponse<UserLookupDto>>`
- **Error case:** If user not found, `ResourceNotFoundException` is thrown by service â†’ `GlobalExceptionHandler` returns `404`

---

## Section 5 â€” Stage 1B: Backend Data & APIs
**Owner: Member 5**  
**PR: `phase2/backend-data`**  
**Estimated time: 5â€“6 days**  
**Depends on: Stage 0 merged**  
**This is the largest backend task in Phase 2.**

---

### 5.1 Update `UserProfileUpdateRequest.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/dto/UserProfileUpdateRequest.java`

The existing record only has `name` and `codeforcesHandle`. Replace with a new record containing all fields. Validation rules:

| Field | Validation | Rule |
|---|---|---|
| `name` | `@NotBlank @Size(min=2, max=100)` | Required, 2â€“100 chars |
| `phoneNumber` | `@Size(max=20)` | Optional at DTO level; frontend enforces mandatory |
| `codeforcesHandle` | `@Size(max=100)` | Optional |
| `leetcodeHandle` | `@Size(max=100)` | Optional |
| `codechefUrl` | `@Size(max=512)` | Optional, should be URL but we don't validate format |
| `atcoderUrl` | `@Size(max=512)` | Optional |
| `githubUrl` | `@Size(max=512)` | Optional |
| `linkedinUrl` | `@Size(max=512)` | Optional |
| `avatarUrl` | `@Size(max=512)` | Optional; populated by Cloudinary on frontend |

---

### 5.2 Create `UserLookupDto.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/dto/UserLookupDto.java`

A new read-only DTO exclusively for the Admin event panel. Returns all data the admin needs in a single call. Contains:

| Field | Type | Source on User entity |
|---|---|---|
| `id` | Long | `user.getId()` |
| `name` | String | `user.getName()` |
| `email` | String | `user.getEmail()` |
| `phoneNumber` | String | `user.getPhoneNumber()` |
| `avatarUrl` | String | `user.getAvatarUrl()` |
| `codeforcesHandle` | String | `user.getCodeforcesHandle()` |
| `cfRating` | Integer | `user.getRating()` |
| `leetcodeHandle` | String | `user.getLeetcodeHandle()` |
| `leetcodeRating` | Integer | `user.getLeetcodeRating()` |
| `codechefUrl` | String | `user.getCodechefUrl()` |
| `atcoderUrl` | String | `user.getAtcoderUrl()` |
| `githubUrl` | String | `user.getGithubUrl()` |
| `linkedinUrl` | String | `user.getLinkedinUrl()` |

Has a static factory method `fromEntity(User user)` for clean mapping in the service layer.

---

### 5.3 Update `UserResponseDto.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/dto/UserResponseDto.java`

Add all 8 new profile fields. This DTO is returned by `GET /api/users/profile` and `PUT /api/users/profile`. Without this update, the frontend receives `null` for all new fields even after saving.

---

### 5.4 Update `UserService.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/service/UserService.java`

**Changes to `updateProfile(Long userId, UserProfileUpdateRequest request)`:**
1. Map all new fields from the request onto the User entity.
2. For `codeforcesHandle`: existing uniqueness check stays. If changed, `codeforcesSyncService.syncSingleUser(user)` is called (already working from Phase 1).
3. For `leetcodeHandle`: new uniqueness check â€” if the handle is non-blank and already belongs to another user, throw `BadRequestException`. After saving, call `leetCodeSyncService.syncSingleUser(user)` for JIT sync.
4. Save and return the updated `UserResponseDto`.

**New method `lookupUserById(Long id)`:**
- Load user by ID. If not found, throw `ResourceNotFoundException("User not found with id: " + id)`.
- Return `UserLookupDto.fromEntity(user)`.

**Inject:** `LeetCodeSyncService` (via constructor injection â€” add to `@RequiredArgsConstructor` field list).

---

### 5.5 Update `UserRepository.java`

**File:** `backend/src/main/java/com/cpclub/backend/user/repository/UserRepository.java`

Add query method:
- `List<User> findByLeetcodeHandleIsNotNull()` â€” used by the bulk LeetCode sync to iterate only users who have a LeetCode handle set.

Also add:
- `boolean existsByLeetcodeHandle(String handle)` â€” for the uniqueness check in `UserService.updateProfile`.

---

### 5.6 Create LeetCode Sync Package

**Package:** `com.cpclub.backend.leetcode`

#### `leetcode/dto/LeetCodeGraphQLResponse.java`
A nested record structure that maps the JSON response from the LeetCode GraphQL API exactly.

The API response shape is:
```
{ "data": { "userContestRanking": { "rating": 1523.45 } } }
```
If the user has never participated in a contest, `userContestRanking` is `null`.

The DTO has three nested records: outer `LeetCodeGraphQLResponse` containing `data`, which contains `userContestRanking`, which contains `rating` as a `Double`. Null-safe at every level.

#### `leetcode/service/LeetCodeSyncService.java`

**Dependencies injected (constructor):** `UserRepository`, `RestTemplate`, `RateLimiter codeforcesRateLimiter` (the shared singleton bean from `AppConfig`).

**Methods:**

`syncSingleUser(User user)` â€” `@Transactional`
- Guard: if `user.getLeetcodeHandle()` is null or blank, return immediately without any HTTP call.
- Build GraphQL request body with the handle as the variable.
- Acquire one permit from the shared `RateLimiter` â€” blocks until a permit is available. This ensures LeetCode calls and Codeforces calls share the 0.5 req/sec gate.
- POST to `https://leetcode.com/graphql` with `Content-Type: application/json`.
- Parse response via `LeetCodeGraphQLResponse`.
- If `rating` is non-null: set `user.setLeetcodeRating((int) Math.round(rating))`, call `userRepository.save(user)`.
- If any exception occurs: log a warning with handle and message. Do NOT rethrow. Fail silently so a LeetCode outage never breaks profile saving.

`syncAllUsers()` â€” called by the cron
- Fetch all users via `userRepository.findByLeetcodeHandleIsNotNull()`.
- For each user, call the internal fetch logic (same as `syncSingleUser` but in a loop).
- Log total updated count at the end.

---

### 5.7 Update `CodeforcesSyncService.java`

**File:** `backend/src/main/java/com/cpclub/backend/codeforces/service/CodeforcesSyncService.java`

The existing `@Scheduled(cron = "0 0 */6 * * *")` method `syncCodeforcesRatings()` currently only syncs Codeforces. After it finishes, add a call to `leetCodeSyncService.syncAllUsers()`.

Inject `LeetCodeSyncService` via constructor injection.

---

### 5.8 Create Event Package

**Package:** `com.cpclub.backend.event`

#### `event/entity/Event.java`
JPA entity for the `events` table. Fields mirror Migration V3 exactly. Uses `@CreationTimestamp` and `@UpdateTimestamp` for audit fields. `createdBy` is a `@ManyToOne(fetch = LAZY)` to `User`.

#### `event/entity/EventAttendee.java`
JPA entity for `event_attendees`. Three `@ManyToOne(fetch = LAZY)` relationships: to `Event` (event), `User` (student being added), `User` (admin who added). `@UniqueConstraint` on `(event_id, user_id)` mirroring the DB constraint.

#### `event/repository/EventRepository.java`
Extends `JpaRepository<Event, Long>`. Query method:
- `List<Event> findByIsActiveTrueOrderByEventDateAsc()` â€” public event listing

#### `event/repository/EventAttendeeRepository.java`
Extends `JpaRepository<EventAttendee, Long>`. Query methods:
- `List<EventAttendee> findByEventIdOrderByAddedAtAsc(Long eventId)` â€” attendee list for a specific event
- `boolean existsByEventIdAndUserId(Long eventId, Long userId)` â€” duplicate check
- `Optional<EventAttendee> findByEventIdAndUserId(Long eventId, Long userId)` â€” for removal

#### `event/dto/EventCreateRequest.java`
Validated request body for create and update.

| Field | Validation |
|---|---|
| `title` | `@NotBlank @Size(max=255)` |
| `description` | no validation, optional |
| `eventDate` | `@NotNull @Future` â€” must be in the future |
| `location` | `@NotBlank @Size(max=255)` |

#### `event/dto/EventResponseDto.java`
What the API returns for a single event. Fields: `id`, `title`, `description`, `eventDate`, `location`, `isActive`, `createdByName` (just the admin's name string, not the full User object), `createdAt`. Has a static `fromEntity(Event)` factory method.

#### `event/dto/AddAttendeeRequest.java`
Tiny request body: `userId` (`@NotNull Long`). That's all.

#### `event/dto/EventAttendeeDto.java`
The full row shown in the admin attendee table. Fields:

| Field | Type | Where it comes from |
|---|---|---|
| `userId` | Long | `attendee.getUser().getId()` |
| `name` | String | `attendee.getUser().getName()` |
| `email` | String | `attendee.getUser().getEmail()` |
| `phoneNumber` | String | `attendee.getUser().getPhoneNumber()` |
| `avatarUrl` | String | `attendee.getUser().getAvatarUrl()` |
| `codeforcesHandle` | String | from user |
| `cfRating` | Integer | `user.getRating()` |
| `leetcodeHandle` | String | from user |
| `leetcodeRating` | Integer | from user |
| `codechefUrl` | String | from user |
| `atcoderUrl` | String | from user |
| `githubUrl` | String | from user |
| `linkedinUrl` | String | from user |
| `addedAt` | LocalDateTime | `attendee.getAddedAt()` |

Has a static `fromEntity(EventAttendee)` factory method. Eager-loads user via the entity relationship â€” no extra query.

#### `event/service/EventService.java`
**Dependencies injected:** `EventRepository`, `EventAttendeeRepository`, `UserRepository`.

**Methods and their exact behavior:**

`createEvent(EventCreateRequest req, String adminEmail)` â†’ `EventResponseDto`
- Load admin user by email. Throw `ResourceNotFoundException` if not found.
- Build and save `Event` entity. Return `EventResponseDto.fromEntity(saved)`.

`listActiveEvents()` â†’ `List<EventResponseDto>`
- Calls `eventRepository.findByIsActiveTrueOrderByEventDateAsc()`.
- Maps each to `EventResponseDto`. Returns the list.

`getEventById(Long id)` â†’ `EventResponseDto`
- `eventRepository.findById(id).orElseThrow(() â†’ ResourceNotFoundException)`.
- Returns `EventResponseDto.fromEntity(event)`.

`updateEvent(Long id, EventCreateRequest req)` â†’ `EventResponseDto`
- Load event. Throw `ResourceNotFoundException` if not found.
- Update fields from request. Save. Return DTO.

`deactivateEvent(Long id)`
- Load event. Set `isActive = false`. Save. Do NOT delete â€” preserves attendee history.

`addAttendee(Long eventId, Long userId, String adminEmail)` â†’ `EventAttendeeDto`
**This method has 4 guard clauses, in this exact order:**
1. Load event by `eventId`. Throw `ResourceNotFoundException("Event not found")` if missing.
2. Load user by `userId`. Throw `ResourceNotFoundException("Student not found with id: " + userId)` if missing.
3. Check `user.getPhoneNumber()` is not null and not blank. Throw `BadRequestException("Student has not added a phone number to their profile. Ask them to update it first.")` if missing.
4. Call `existsByEventIdAndUserId`. Throw `BadRequestException("This student is already registered for this event.")` if true.
5. Load admin by `adminEmail`. Build and save `EventAttendee`. Return `EventAttendeeDto.fromEntity(saved)`.

`removeAttendee(Long eventId, Long userId)`
- Find by `eventId` and `userId`. Throw `ResourceNotFoundException("Attendee not found")` if missing. Delete.

`getAttendees(Long eventId)` â†’ `List<EventAttendeeDto>`
- Load event first (throws 404 if not found, prevents "silent empty list for wrong ID" bug).
- Call `findByEventIdOrderByAddedAtAsc`. Map each to `EventAttendeeDto`. Return list.

#### `event/service/EventExportService.java`
**Dependency:** Apache POI XSSF (added to `pom.xml`).

Method: `exportToExcel(Long eventId, List<EventAttendeeDto> attendees)` â†’ `byte[]`

**What it builds:**
- Sheet name: `"Attendees"`
- Row 0 (header): Bold, 14 columns: ID, Name, Email, Phone Number, Avatar URL, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef URL, AtCoder URL, GitHub, LinkedIn, Added At
- Row 1+: One row per `EventAttendeeDto`, all fields in matching column order
- All columns auto-sized after data is written
- Writes to `ByteArrayOutputStream`, returns `byte[]`
- If anything goes wrong during generation: throw `RuntimeException("Failed to generate Excel export")` â€” controller will return 500

#### `event/controller/EventController.java`
**Dependencies injected:** `EventService`, `EventExportService`.  
**Base path:** `/api/events`

All endpoints, with exact auth and response format:

| Method | Path | Auth | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/events` | `hasRole('ADMIN')` | `EventCreateRequest` | `201 Created` + `ApiResponse<EventResponseDto>` |
| `GET` | `/api/events` | Public | â€” | `200 OK` + `ApiResponse<List<EventResponseDto>>` |
| `GET` | `/api/events/{id}` | Public | â€” | `200 OK` + `ApiResponse<EventResponseDto>` |
| `PUT` | `/api/events/{id}` | `hasRole('ADMIN')` | `EventCreateRequest` | `200 OK` + `ApiResponse<EventResponseDto>` |
| `DELETE` | `/api/events/{id}` | `hasRole('ADMIN')` | â€” | `200 OK` + `ApiResponse<Void>` |
| `POST` | `/api/events/{id}/attendees` | `hasRole('ADMIN')` | `AddAttendeeRequest` | `201 Created` + `ApiResponse<EventAttendeeDto>` |
| `DELETE` | `/api/events/{id}/attendees/{uid}` | `hasRole('ADMIN')` | â€” | `200 OK` + `ApiResponse<Void>` |
| `GET` | `/api/events/{id}/attendees` | `hasRole('ADMIN')` | â€” | `200 OK` + `ApiResponse<List<EventAttendeeDto>>` |
| `GET` | `/api/events/{id}/attendees/export` | `hasRole('ADMIN')` | â€” | `200 OK` + `byte[]` with `Content-Disposition: attachment` header |

The Excel download endpoint sets two response headers:
- `Content-Disposition: attachment; filename="attendees_<eventTitle>.xlsx"`
- `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

---

### 5.9 Create Snapshot Package

**Package:** `com.cpclub.backend.snapshot`

#### `snapshot/entity/WeeklySnapshot.java`
JPA entity for `weekly_snapshots`. Fields: id, user (ManyToOne to User), platform (String), rating (Integer), recordedAt (LocalDateTime with `@CreationTimestamp`).

#### `snapshot/repository/WeeklySnapshotRepository.java`
Method: `List<WeeklySnapshot> findByUserIdAndPlatformOrderByRecordedAtAsc(Long userId, String platform)`

#### `snapshot/service/SnapshotService.java`
**Scheduled:** `@Scheduled(cron = "0 0 0 * * MON")` â€” fires every Monday at midnight server time.

**Logic:**
- Fetch all users via `userRepository.findAll()`.
- For each user:
  - If `user.getRating() != null`, save a `WeeklySnapshot` with platform `"CODEFORCES"` and that rating.
  - If `user.getLeetcodeRating() != null`, save a `WeeklySnapshot` with platform `"LEETCODE"` and that rating.
- Log count of snapshots written.

**Important:** This does NOT re-fetch ratings from Codeforces/LeetCode â€” it just snapshots whatever is already in the DB at that moment. The 6-hour cron keeps the DB fresh; the Monday snapshot job just records the week's state.

#### `snapshot/controller/SnapshotController.java`
**Base path:** `/api/snapshots`  
**Auth:** Both endpoints require `isAuthenticated()`

| Method | Path | Returns |
|---|---|---|
| `GET` | `/api/snapshots/{userId}/codeforces` | `List<{ date: LocalDateTime, rating: Integer }>` |
| `GET` | `/api/snapshots/{userId}/leetcode` | `List<{ date: LocalDateTime, rating: Integer }>` |

Returns a simple list of `{date, rating}` pairs. The frontend maps these to the recharts `data` prop directly.

---

### 5.10 Update `pom.xml`

Add one new dependency for Excel generation:
- Group: `org.apache.poi`, Artifact: `poi-ooxml`, Version: `5.3.0`

---

## Section 6 â€” Stage 2A: Frontend UI/UX
**Owner: Member 1**  
**PR: `phase2/frontend-ui`**  
**Estimated time: 2â€“3 days**  
**Depends on: Stage 0 merged (to know the data shapes)**

---

### Files & Responsibilities

#### `frontend/src/components/site/event-card.tsx` [NEW]
Reusable card component for displaying a single event.

**Props:** `id`, `title`, `description`, `eventDate`, `location`, `isActive`

**Renders:**
- Title in large bold text
- Date formatted as `"Sat, 15 Nov 2025 Â· 3:00 PM"` using `date-fns format()`
- Location with a map pin icon
- Badge: green "Upcoming" if `eventDate` is in the future AND `isActive` is true; grey "Past" otherwise
- The whole card is a `<Link href={/events/${id}}>` so clicking navigates to the event detail

**Styling:** Follows existing glass-panel + rounded-panel design language from Phase 1.

#### `frontend/src/components/ui/data-table.tsx` [NEW]
Generic reusable table component used in Admin Members tab and Admin Event Attendees page.

**Props:** `columns: ColumnDef[]`, `data: T[]`, `isLoading: boolean`, `emptyMessage?: string`

**Behavior:**
- When `isLoading` is true: renders a skeleton with 5 animated placeholder rows
- When `data` is empty: renders `emptyMessage` (default: "No records found")
- Renders responsive table with zebra-stripe rows
- Columns are defined externally by the parent â€” the component is pure presentational

#### `frontend/src/components/site/admin-tabs.tsx` [NEW]
Tab navigation shell for the Admin Dashboard.

**Props:** `activeTab: 'members' | 'events'`, `onTabChange: (tab) => void`

Renders two tab buttons. Visual style: underline indicator on active tab. Does not fetch data â€” purely a nav component.

#### `frontend/src/app/events/page.tsx` [MODIFY]
Currently renders hardcoded placeholder content. Replace with:
- Server Component (`async` function, no `"use client"`)
- Fetch `GET /api/events` directly on the server using `fetch()` with `cache: 'no-store'` (always fresh)
- Map response to `<EventCard>` components
- If empty array: show "No upcoming events scheduled. Check back soon." message
- Keep existing page title, eyebrow, and section layout from Phase 1

---

## Section 7 â€” Stage 2B: Frontend Auth & State
**Owner: Member 2**  
**PR: `phase2/frontend-auth`**  
**Estimated time: 2 days**  
**Depends on: Stage 0 merged**

---

### Files & Responsibilities

#### `frontend/src/store/auth.ts` [MODIFY]
The `User` interface currently has: `id`, `email`, `fullName`, `role`, `codeforcesHandle`.

Add to the interface:
- `leetcodeHandle: string | null`
- `phoneNumber: string | null`
- `avatarUrl: string | null`

The `mapAuthResponseToUser()` function must also map these from the `AuthResponse` (but the login endpoint doesn't return them â€” that's fine. They default to `null` and are populated when `GET /api/users/profile` is called on the profile page).

#### `frontend/src/types/api.ts` [MODIFY]
Add new TypeScript types matching the backend DTOs:

`Event` type: `id`, `title`, `description`, `eventDate`, `location`, `isActive`, `createdByName`, `createdAt`

`EventAttendee` type: all 14 fields from `EventAttendeeDto`

`UserLookup` type: all 13 fields from `UserLookupDto`

Update the existing `Profile` type to add all 8 new user fields.

#### `frontend/src/lib/services/events.ts` [NEW]
All API calls for events and admin operations. Every function uses `apiClient` (Axios instance with auth interceptor).

| Function | HTTP call | Auth needed |
|---|---|---|
| `listEvents()` | `GET /api/events` | No |
| `getEvent(id)` | `GET /api/events/{id}` | No |
| `createEvent(data)` | `POST /api/events` | Yes (Admin) |
| `updateEvent(id, data)` | `PUT /api/events/{id}` | Yes (Admin) |
| `deactivateEvent(id)` | `DELETE /api/events/{id}` | Yes (Admin) |
| `addAttendee(eventId, userId)` | `POST /api/events/{eventId}/attendees` | Yes (Admin) |
| `removeAttendee(eventId, userId)` | `DELETE /api/events/{eventId}/attendees/{userId}` | Yes (Admin) |
| `getAttendees(eventId)` | `GET /api/events/{eventId}/attendees` | Yes (Admin) |
| `exportAttendees(eventId)` | `GET /api/events/{eventId}/attendees/export` â€” `responseType: 'blob'` | Yes (Admin) |
| `lookupUser(id)` | `GET /api/users/{id}/lookup` | Yes (Admin) |

For `exportAttendees`, the response type must be `'blob'` so Axios treats the binary response correctly.

#### `frontend/src/lib/services/dashboard.ts` [MODIFY]
The `mapUserToProfile()` function currently maps only the Phase 1 fields. Add mappings for all 8 new fields: `avatarUrl`, `phoneNumber`, `leetcodeHandle`, `leetcodeRating`, `codechefUrl`, `atcoderUrl`, `githubUrl`, `linkedinUrl`. Use `?? null` for fallback so missing fields default to `null` not `undefined`.

---

## Section 8 â€” Stage 2C: Frontend Dashboards & Data
**Owner: Member 3**  
**PR: `phase2/frontend-dashboards`**  
**Estimated time: 4â€“5 days**  
**Depends on: Member 1 AND Member 2 PRs merged into `feature/phase-2` first**

---

### Files & Responsibilities

#### `frontend/src/components/site/profile-dashboard.tsx` [MAJOR MODIFY]

**Change 1 â€” Avatar:**
- If `profile.avatarUrl` is set: render `<Image src={profile.avatarUrl} .../>` with `rounded-full` and the CF rank border color.
- If owner and no avatar: show the existing `<User>` icon placeholder PLUS a small "Upload photo" button beneath it.
- Clicking "Upload photo": opens the Cloudinary Upload Widget (loaded via `<Script src="https://upload-widget.cloudinary.com/global/all.js" />`). On success callback, extract `result.info.secure_url` and call `dashboardService.updateProfile({ avatarUrl: secure_url })`. On success, call `loadProfile()` to refresh.

**Change 2 â€” Platform links in header:**
Below the CF handle line, add a horizontal row of icon links (only shown if the URL is set):
- CodeChef icon â†’ `profile.codechefUrl`
- AtCoder icon â†’ `profile.atcoderUrl`
- GitHub icon â†’ `profile.githubUrl`
- LinkedIn icon â†’ `profile.linkedinUrl`

Each is an `<a href="..." target="_blank" rel="noopener noreferrer">` with the platform's icon from Lucide or a simple SVG.

**Change 3 â€” Edit Profile panel:**
The existing inline CF handle edit form gets replaced by a full "Edit Profile" drawer/panel (visible to owner only). Fields:
- Name (`text`, `required`)
- Phone Number (`tel`, `required`) â€” shown masked as `â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢` to non-owners
- Codeforces Handle (`text`)
- LeetCode Handle (`text`)
- CodeChef URL (`url`)
- AtCoder URL (`url`)
- GitHub URL (`url`)
- LinkedIn URL (`url`)

On "Save": call `PUT /api/users/profile`. On success: call `loadProfile()` to refresh. On error: show error message inline.

**Change 4 â€” Rating Charts:**
Replace the "Coming Soon" locked overlay on the rating chart section. Add two charts:
- **CF Chart:** Fetch `GET /api/snapshots/{userId}/codeforces`. If fewer than 2 data points, show "Not enough data yet â€” chart appears after the first weekly snapshot is recorded." Otherwise render `<LineChart>` from `recharts` with date on X-axis, rating on Y-axis.
- **LeetCode Chart:** Same pattern for `/api/snapshots/{userId}/leetcode`.

Use `recharts` (already in `package.json`). Reuse the existing chart styling (`stroke="var(--primary)"`, `CartesianGrid strokeDasharray="3 3"` etc.) to match the Phase 1 CF rating history chart.

---

#### `frontend/src/app/(dashboard)/admin/page.tsx` [NEW]

**Route guard:** At the top of the component, check `user?.role !== 'ROLE_ADMIN'`. If not admin, use `router.replace('/')` and return null. Do NOT rely only on the backend auth â€” protect the page client-side too.

**Tab 1 â€” Members:**
- On mount: fetch `GET /api/users/all`.
- Renders `<DataTable>` (from M1) with these columns:

| Column | Value | Action |
|---|---|---|
| Name | `user.name` | â€” |
| Email | `user.email` | â€” |
| Phone | `user.phoneNumber ?? 'â€”'` | â€” |
| CF Handle | `user.codeforcesHandle ?? 'â€”'` | â€” |
| CF Rating | `user.rating ?? 'â€”'` | â€” |
| LeetCode Handle | `user.leetcodeHandle ?? 'â€”'` | â€” |
| LeetCode Rating | `user.leetcodeRating ?? 'â€”'` | â€” |
| Role | badge: Admin / User | â€” |
| Actions | "Promote" / "Demote" + "Delete" | See below |

"Promote to Admin" button: calls `PUT /api/users/{id}/role` with `{ role: 'ROLE_ADMIN' }`. Refreshes list on success.
"Demote to User" button: same but `ROLE_USER`.
"Delete" button: shows a confirmation dialog. On confirm: calls `DELETE /api/users/{id}`. Refreshes list on success.

**Tab 2 â€” Events:**
- On mount: fetch `GET /api/events`.
- Top: "Create New Event" form with fields: Title, Description, Date+Time, Location. Submit calls `POST /api/events`. Refreshes list on success.
- Below: list of all events (active and inactive) as rows with: Title, Date, Location, Status badge (Active/Inactive), "Manage Attendees" link â†’ `/admin/events/{id}`, "Deactivate" button.

---

#### `frontend/src/app/(dashboard)/admin/events/[id]/page.tsx` [NEW]

Two-column layout (on desktop). Stacks vertically on mobile.

**Left column â€” Student Search Panel:**
1. Text input: "Enter Student ID (e.g. 42)"
2. "Search" button
3. On search: call `eventsService.lookupUser(id)`. Show loading state on button.
4. **Error states:**
   - If input is empty or not a number: show inline validation error, don't call API.
   - If API returns 404: show "No student found with this ID."
   - If API returns 403: show "Access denied." (shouldn't happen, page is admin-only, but defensive)
5. **Preview card (shown after successful search):**
   - Avatar (or placeholder icon)
   - Name, Email
   - Phone number (or red warning "âš ï¸ No phone number â€” ask the student to update their profile")
   - CF Handle + rating, LeetCode Handle + rating
   - CodeChef, AtCoder, GitHub, LinkedIn icon links
6. "Add to Event" button:
   - Disabled if: no student loaded, or student has no phone number, or student is already in the attendee list
   - If student already in list: show "Already added" chip instead
   - On click: call `eventsService.addAttendee(eventId, userId)`. On success: refresh attendee list, clear the search panel. On error: show the error message from the backend response.

**Right column â€” Attendee List:**
- Header row with event title, attendee count badge, and "Export to Excel" button.
- "Export to Excel" click:
  1. Call `eventsService.exportAttendees(eventId)` with `responseType: 'blob'`.
  2. Create a temporary object URL: `URL.createObjectURL(new Blob([response.data]))`.
  3. Create a temporary `<a>` tag, set `href` and `download="attendees_<eventTitle>.xlsx"`, click it programmatically.
  4. Revoke the object URL.
- Renders `<DataTable>` with these columns: Name, Email, Phone, CF Handle, CF Rating, LeetCode Handle, LeetCode Rating, CodeChef, AtCoder, GitHub, LinkedIn, Added At, Remove.
- "Remove" per row: confirmation dialog â†’ calls `eventsService.removeAttendee(eventId, userId)` â†’ refreshes list.

---

## Section 9 â€” Stage 3: Integration, Tests & Polish
**Owner: Member 6**  
**PR: `phase2/integration-and-tests`**  
**Estimated time: 2â€“3 days**

---

### 9.1 Unit Tests to Write

#### `EventServiceTest.java`
| Test | Scenario | Expected |
|---|---|---|
| `createEvent_success` | Valid request, admin exists | Event saved, returns EventResponseDto |
| `addAttendee_success` | Valid userId, has phone, not duplicate | Attendee row created, returns EventAttendeeDto |
| `addAttendee_missingPhone` | User exists but phoneNumber is null | Throws `BadRequestException` with phone message |
| `addAttendee_duplicate` | Student already in event | Throws `BadRequestException` with duplicate message |
| `addAttendee_userNotFound` | userId = 999 (doesn't exist) | Throws `ResourceNotFoundException` |
| `removeAttendee_success` | Attendee exists | Row deleted |
| `removeAttendee_notFound` | Attendee doesn't exist | Throws `ResourceNotFoundException` |
| `deactivateEvent_success` | Event exists | `isActive` set to false, not deleted |
| `getAttendees_success` | Event with 3 attendees | Returns list of 3 EventAttendeeDtos |

#### `LeetCodeSyncServiceTest.java`
| Test | Scenario | Expected |
|---|---|---|
| `syncSingleUser_success` | Mock HTTP returns rating 1523.45 | `user.leetcodeRating` set to 1523, saved |
| `syncSingleUser_nullHandle` | User has no leetcodeHandle | No HTTP call made (verify with mock) |
| `syncSingleUser_nullContestRanking` | API returns `userContestRanking: null` | Rating unchanged, no save |
| `syncSingleUser_httpError` | Mock HTTP throws exception | No exception propagated, rating unchanged |

#### `EventExportServiceTest.java`
| Test | Scenario | Expected |
|---|---|---|
| `exportToExcel_notEmpty` | 2 attendees | byte[] length > 0 |
| `exportToExcel_correctHeaders` | 1 attendee | Row 0, Cell 0 = "ID", Cell 1 = "Name", etc. |
| `exportToExcel_dataRow` | 1 attendee with known fields | Row 1 values match attendee DTO fields |

---

### 9.2 Leaderboard Platform Toggle

**Backend â€” update `LeaderboardService.java`:**
Add `platform` parameter to the method that fetches leaderboard data. When `platform = "LEETCODE"`, sort by `leetcode_rating` descending. Default (Codeforces) sorts by `rating` descending. The `LeaderboardController` passes the `?platform=` query param from the request.

**Frontend â€” update `leaderboard/page.tsx`:**
Add a state variable `activePlatform` (default: `"CODEFORCES"`). Render two pill toggle buttons: "Codeforces | LeetCode". On click, update state and re-fetch leaderboard with `?platform=<activePlatform>`. Show the platform name in the leaderboard table header.

---

### 9.3 Production Environment Variables

Add to Render frontend service (environment variables):
- `NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME` â€” used by the Cloudinary Upload Widget JS SDK

Note: Cloudinary API Key and Secret are **never sent to the frontend**. The Upload Widget uses unsigned presets or the cloud name only. The secret stays server-side (not needed for our use case since we're using unsigned uploads).

---

## Section 10 â€” Full API Contract Reference

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users` | Public | Member directory |
| `GET` | `/api/users/{id}` | Public | Public user profile |
| `GET` | `/api/users/{id}/lookup` | **Admin** | Full lookup for event panel |
| `GET` | `/api/users/profile` | Auth | Own full profile |
| `PUT` | `/api/users/profile` | Auth | Update own profile |
| `PUT` | `/api/users/{id}/role` | **Admin** | Change role |
| `DELETE` | `/api/users/{id}` | **Admin** | Delete user |
| `POST` | `/api/events` | **Admin** | Create event |
| `GET` | `/api/events` | Public | List active events |
| `GET` | `/api/events/{id}` | Public | Single event |
| `PUT` | `/api/events/{id}` | **Admin** | Update event |
| `DELETE` | `/api/events/{id}` | **Admin** | Deactivate event |
| `POST` | `/api/events/{id}/attendees` | **Admin** | Add attendee `{ userId }` |
| `DELETE` | `/api/events/{id}/attendees/{uid}` | **Admin** | Remove attendee |
| `GET` | `/api/events/{id}/attendees` | **Admin** | Attendee list (JSON) |
| `GET` | `/api/events/{id}/attendees/export` | **Admin** | Download `.xlsx` |
| `GET` | `/api/snapshots/{uid}/codeforces` | Auth | CF rating history |
| `GET` | `/api/snapshots/{uid}/leetcode` | Auth | LeetCode rating history |
| `GET` | `/api/leaderboard?platform=` | Public | Leaderboard (platform filter) |

---

## Section 11 â€” 6-Member Summary

| Member | Role | Stage | Owns |
|---|---|---|---|
| **M6** | Team Lead + DevOps | 0 + 3 | All 4 Flyway migrations, `User.java` entity update, LeetCode cron wiring, weekly snapshot cron, all unit tests, Render env vars, all PR reviews, final `feature/phase-2 â†’ main` merge |
| **M4** | Backend Security | 1A | `SecurityConfig.java` new route rules, `UserController` lookup endpoint |
| **M5** | Backend Data + APIs | 1B | `UserProfileUpdateRequest` update, `UserLookupDto`, `UserResponseDto` update, `UserService` update, `UserRepository` update, entire `leetcode/` package, entire `event/` package (entity, repo, DTO, service, export service, controller), entire `snapshot/` package, `pom.xml` POI |
| **M1** | Frontend UI/UX | 2A | `EventCard` component, `DataTable` component, `AdminTabs` component, Events page redesign, Admin page layout shell |
| **M2** | Frontend Auth + State | 2B | `auth.ts` User type, `types/api.ts` new types, `events.ts` service file, `dashboard.ts` mapper update |
| **M3** | Frontend Dashboards | 2C | Profile dashboard redesign (Cloudinary, platform links, edit panel, charts), Admin dashboard data (both tabs), Admin event attendee page (search panel + table + Excel download) |
