# Backend Audit

**Date:** 11 August 2026
**Commit:** `64b5237`
**Scope:** `backend/` — correctness, data integrity, API contracts, performance, tests

Security concerns (auth, secrets, injection, data exposure) are covered separately in
[SECURITY-AUDIT.md](SECURITY-AUDIT.md) and are not repeated here.

**Test suite: 56 tests, 0 failures, 0 skipped, ~35s. `BUILD SUCCESS`.**

---

## Summary

| # | Severity | Issue |
| --- | --- | --- |
| 1 | **Critical** | One bad Codeforces handle permanently freezes rating sync for the whole club |
| 2 | **Critical** | No migration tooling under `ddl-auto: validate` — first schema change bricks the deploy |
| 3 | Important | Untimed network call inside a transaction, on a single-threaded scheduler |
| 4 | Important | No batching in CF sync — a stated playbook requirement is unimplemented |
| 5 | Important | Catch-all exception handler turns client errors (400/405) into 500s |
| 6 | Important | Blog pagination unbounded and unvalidated |
| 7 | Important | Expired sessions return 403 not 401, so the frontend never redirects to login |
| 8 | Important | N+1 queries on the leaderboard — 22 round trips per page |
| 9 | Important | CF handle uniqueness is case-sensitive; sync then silently drops the duplicate |
| 10 | Important | Frontend truncates member directory and leaderboard at 20 rows |

---

## Things that are right (verified, not assumed)

Several things worth *not* re-investigating, because they were checked against generated
SQL and compiled bytecode rather than annotations alone:

- **Leaderboard NULL ordering is correct.** `UserRepository.java:84` uses explicit
  `ORDER BY u.rating DESC NULLS LAST, u.id ASC`. Emitted SQL under `PostgreSQLDialect`
  confirms `order by u1_0.rating desc nulls last, u1_0.id`. An earlier review suspected
  unrated members would sort to the top on Postgres — they do not. The `id ASC` tiebreak
  also makes pagination deterministic.
- **Playbook §16 indexes exist in the DDL**, not just in annotations:
  `create index idx_codeforces_handle on users (codeforces_handle);` and
  `create index idx_rating on users (rating);`
- **Audit timestamps genuinely populate.** Hibernate's `@CreationTimestamp` /
  `@UpdateTimestamp` are self-contained. Blueprint §4.1 prescribes Spring Data's
  `@CreatedDate`/`@LastModifiedDate`, but there is no `@EnableJpaAuditing` anywhere —
  following the doc would have produced silent nulls. **The code is right and the doc
  is wrong.**
- **The `@Builder.Default` trap is absent.** Disassembly confirms `User()` calls
  `$default$role()`, so `new User()` yields `ROLE_USER`, not null.
- **A Codeforces outage cannot wipe ratings** — `CodeforcesSyncService.java:82` guards
  on `rating != null`, and the catch leaves prior values intact.
- **Rank computation is real standard competition ranking**, covered by 7 focused tests
  including ties spanning a page boundary. The best-tested code in the repo.
- `open-in-view: false`, `ddl-auto: validate` in prod, Swagger disabled in prod,
  non-root Docker user, and a response envelope applied consistently across all six
  controllers.

---

## Critical

### 1. One bad Codeforces handle freezes the leaderboard for everyone

**Where:** `codeforces/service/CodeforcesSyncService.java:63-70`

```java
String handlesQueryParam = String.join(";", handles);
String requestUrl = CODEFORCES_API_URL + handlesQueryParam;
```

Every handle goes into a single `user.info` request, and that API is **all-or-nothing**:
if any one handle is invalid — a typo, a renamed account, a deleted account — the whole
response is `status: "FAILED"` with `result: null`. The code falls to the `else` branch,
logs a warning, and updates **zero** users.

Nothing validates a handle against Codeforces when it is set —
`UserService.updateCodeforcesHandle:117-123` only trims it and checks local uniqueness.
So one member typing `tourrist` silently freezes the leaderboard for the entire club,
indefinitely, with a single WARN line as the only evidence.

Blueprint §21 makes a working leaderboard the Phase 1 completion criterion, so this is
core functionality broken by a one-character user mistake.

**Fix:** chunk handles into batches (see issue 4), and on a `FAILED` response fall back
to per-handle requests so one bad handle only skips itself. Log the offending handle so
an admin can clear it.

### 2. No migration path — the first schema change breaks the deploy

**Where:** `application-prod.yml:11`, `pom.xml`, `Dockerfile:15`

`ddl-auto: validate` is the correct production setting, but **there is no Flyway or
Liquibase dependency and no SQL migration directory anywhere in the repo** (verified: 0
matches in `pom.xml`, no `.sql` files). The consequence: the moment anyone adds a field
to an entity, the production app refuses to start, and nothing in the repo provides a
forward path.

The documented recovery makes it worse — playbook §19 says *"drop schema and restore
from local backup SQL dump."* Dropping the schema on a live deployment **is** the
data-loss scenario, and it is currently the only written procedure.

Compounding it, `Dockerfile:15` never sets the profile:

```dockerfile
ENTRYPOINT ["java","-jar","app.jar"]
```

If `SPRING_PROFILES_ACTIVE=prod` is not set in the hosting dashboard, the container
silently falls back to `application.yml` — `ddl-auto: update`, `show-sql: true`, Swagger
exposed, and a datasource pointing at `localhost:5432`. Nothing in the repo records that
this variable is mandatory.

**Fix:** add Flyway, baseline the current schema as `V1__init.sql`, keep `validate`. Add
`ENV SPRING_PROFILES_ACTIVE=prod` to the Dockerfile so the safe profile is the default
rather than an unwritten deployment step.

---

## Important

### 3. Network call inside a transaction, untimed, on a one-thread scheduler

`CodeforcesSyncService.java:41-42,68` and `common/config/AppConfig.java:19-21`

```java
@Scheduled(cron = "...")
@Transactional
public void syncCodeforcesRatings() { ... restTemplate.getForObject(requestUrl, ...); }
```
```java
public RestTemplate restTemplate() { return new RestTemplate(); }
```

Three problems stack: a DB connection is held for the whole external HTTP call;
`new RestTemplate()` has **no connect or read timeout**, so a half-open connection blocks
forever; and Spring's default scheduler pool is one thread, so a blocked sync means the
job never runs again until restart. `CodeforcesController.java:37` invokes it
synchronously too, so an admin clicking "sync" can hang an HTTP worker.

**Fix:** set connect/read timeouts (5s/15s); fetch outside the transaction and open a
short one to write; give `@Scheduled` its own pool.

### 4. No batching — playbook requirement unimplemented

`CodeforcesSyncService.java:63`. Playbook §16 requires batching above 100 members. All
handles concatenate into one URL unconditionally; a few hundred members will exceed the
common 8 KB request-line limit and fail wholesale. There is also no 429 handling — a
rate-limit response is caught by the generic `catch (Exception)` and treated like a DNS
failure.

**Fix:** partition into chunks of ~100, sleep ~200ms between chunks, and special-case
`TooManyRequests` with backoff.

### 5. Catch-all handler turns client errors into 500s

`common/exception/GlobalExceptionHandler.java:102-107`

`@ExceptionHandler(Exception.class)` runs before Spring's `DefaultHandlerExceptionResolver`
and matches everything, so framework exceptions never reach their correct status:

| Request | Returns | Should be |
| --- | --- | --- |
| `GET /api/users?size=500` | 500 | 400 |
| `GET /api/users/abc` | 500 | 400 |
| Malformed JSON body | 500 | 400 |
| Wrong HTTP method | 500 | 405 |

Invisible to the tests because `GlobalExceptionHandlerTest` calls handler methods
directly rather than dispatching through MockMvc.

**Fix:** extend `ResponseEntityExceptionHandler` so framework exceptions keep their
statuses; add explicit handlers for `ConstraintViolationException` and
`IllegalArgumentException` → 400.

### 6. Blog pagination unbounded

`blog/controller/BlogController.java:42-45` has no `@Validated` and no `@Min`/`@Max`,
unlike `UserController` and `LeaderboardController` which both bound size to 100. So
`GET /api/blogs?size=1000000` loads every row including every `TEXT` content column, and
`page=-1` throws into the catch-all above as a 500. Default size is also inconsistent
(10 here, 20 elsewhere).

### 7. Expired sessions return 403, not 401

`SecurityConfig.java:133` permits all GETs under `/api/users/**`, so an unauthenticated
`GET /api/users/profile` passes the filter chain and reaches the controller, where
`@PreAuthorize` throws `AuthorizationDeniedException` → mapped to **403**.
`AuthEntryPointJwt`, which correctly writes 401, is never reached.

The frontend only reacts to 401 (`frontend/src/lib/axios.ts:44`), so a user with an
expired token gets a stuck error state instead of being redirected to login. This fails
playbook §9's exit gate: *"401 Unauthorized errors correctly redirect to /login."*

**Fix:** narrow the permitAll rule so `/api/users/profile` falls through to
`.anyRequest().authenticated()`.

### 8. N+1 queries on the leaderboard

`leaderboard/service/LeaderboardService.java:43-53` runs one `COUNT` per row. A default
page of 20 costs 22 round trips, on the most-visited endpoint in the app. The unrated
rank is cached; the rated path is not.

**Fix:** a single `RANK() OVER (ORDER BY rating DESC NULLS LAST)` query.

### 9. Case-sensitive handle uniqueness, then silent drop

`UserRepository.java:43` uses exact-match `existsByCodeforcesHandle`, so `Tourist` and
`tourist` both pass the uniqueness check and both persist. But the sync keys its map
case-insensitively and discards collisions:

```java
u -> u.getCodeforcesHandle().toLowerCase(),
u -> u,
(existing, replacement) -> existing   // second user silently dropped
```

`UserService.java:119` *compares* with `equalsIgnoreCase` but *queries* case-sensitively
— the intent was clearly case-insensitive; the implementation is not.

**Fix:** store handles lowercased, or add `existsByCodeforcesHandleIgnoreCase` plus a
functional unique index on `LOWER(codeforces_handle)`.

### 10. Frontend truncates lists at 20

`frontend/src/lib/services/dashboard.ts:66,77` requests no `size` and ignores
`totalPages`/`last`, so member 21 onward never appears on `/members` or `/leaderboard`.
The API is fine; the client under-requests.

---

## Minor

- **Stray files tracked at repo root:** `fix_docs.py` and `final_diff.txt` (97 KB of diff
  output). Confirmed via `git ls-files`. Neither belongs in the repo.
- **Bare `assert` keyword** in `CodeforcesSyncServiceTest.java:53` — silently becomes a
  no-op if `argLine` is ever overridden. Only occurrence in the suite.
- **Blog slug generation breaks on non-ASCII titles** (`BlogService.java:170-175`). A
  Cyrillic or CJK title strips to the empty string, producing an unreachable URL. Also
  never trims leading/trailing hyphens.
- **Slug collision is check-then-act** — concurrent creates violate the unique constraint
  → 500. Low likelihood with one admin.
- **`updateBlog` changes the title but never the slug**, so slugs drift permanently.
  Possibly intentional for SEO, but undocumented.
- **`published` is nullable in the schema** — a NULL row is excluded from listings but
  still fetchable by id.
- **Redundant indexes** — `idx_blog_slug` and `idx_codeforces_handle` duplicate the
  implicit unique indexes.
- **Column lengths unconstrained** — blueprint §4.1 specifies `varchar(100)` for handles;
  DDL emits `varchar(255)`.
- **Redundant `save()` on managed entities** inside transactions (5 sites) — dirty
  checking already flushes. The one inside the sync loop is the most wasteful.
- **Dead code** — `auth/dto/MessageResponse.java` and
  `UserRepository.findByCodeforcesHandle` have no callers.
- **Double round-trip on profile writes** — controller resolves the caller in one
  transaction, then mutates in a second. Not atomic.
- **The entire blog feature is outside Phase 1 scope** per blueprint §4.1 (*"exactly one
  table"*), and the frontend never calls `/api/blogs`. Dead surface area carrying real
  risk (issue 6). Either amend the blueprint or defer the feature.

---

## API contract deviations from blueprint §11

§11 is described as "frozen" but the implementation deviates from **every** contract in
it. In almost all cases the code is the better design — §11 was written at Stage 0 before
the `ApiResponse` envelope existed, and is now stale rather than authoritative.

| Endpoint | Documented | Actual | Change |
| --- | --- | --- | --- |
| `POST /auth/register` | Res 200 | 201 Created | **Doc** — 201 is correct |
| `POST /auth/register` | `{token, role}` | `ApiResponse` envelope, payload under `data` | **Doc** — envelope is consistent everywhere |
| `POST /auth/register` | 400 `{error: "..."}` | `{success, message, data, timestamp}` | **Doc** — no endpoint returns an `error` key |
| `POST /auth/login` | Absent from §11 | 200 + envelope | **Doc** — add it |
| `GET /users/{id}` | `Bearer` required | Public | **Decide, then align** |
| `GET /users/{id}` | `{id, name, codeforcesHandle, rating}` | Also `email`, `role`, timestamps | **Code** for `email`; doc for the rest |
| `GET /users` | Absent | Paginated, `?query`, max size 100 | **Doc** |
| `PUT /users/{id}/handle` | Absent from §11 | 200 + envelope, 403 on non-owner | **Doc** |
| `GET /leaderboard` | Absent | Paginated with `rank`/`tier` | **Doc** — core feature, no contract |
| `/api/blogs/**` | Forbidden in Phase 1 by §4.1 | Six live endpoints | **Both** |
| `POST /codeforces/sync` | Absent | Admin-only trigger | **Doc** |
| Validation failure | Unspecified | Structured per-field errors under `data` | **Doc** — this is good, freeze it |

---

## Test quality

56 tests, all green, and better than typical for a student project — MockMvc tests assert
on `$.data.*` JSON paths rather than just status, and `LeaderboardServiceTest` is
genuinely strong. But there are structural gaps that mean the suite **cannot** catch
several bugs above.

**`standaloneSetup` everywhere.** All six MockMvc test classes bypass the Spring context,
so they cannot see:

- **The security filter chain** — issue 7 (403-vs-401) is structurally invisible.
- **`@PreAuthorize`** — the `hasRole('ADMIN')` guards on blog writes, role changes, user
  deletion, and manual sync are **never exercised**. `BlogControllerTest.java:107-116`
  calls an admin-only endpoint with *no* user and gets 200. Blueprint §20's *"Admin
  endpoints reject Normal users (403)"* is checked off with no automated proof.
- **Real exception-resolver ordering** — issue 5 cannot surface.

**CF sync error paths are essentially untested.** `CodeforcesSyncServiceTest` has exactly
one test, the happy path. Zero coverage of `status: "FAILED"` (issue 1 — the critical
bug), thrown exceptions, null responses, unrated users, empty handle lists, or
case-mismatched handles. The least-tested and highest-risk service in the codebase — the
inverse of where effort should go.

**Test/production fidelity gap.** `application-test.yml:12` pins `H2Dialect`, whose
default null ordering already matches, so the emitted SQL omits `nulls last` — meaning
the exact SQL that runs in production is never executed by any test. Delete `NULLS LAST`
from the JPQL and all 56 tests still pass while the production leaderboard silently
inverts.

**Tests asserting nothing meaningful:** `UserRepositoryTest.java:29-31` tests test
isolation, not application behaviour.

---

## Recommended order of work

1. **CF sync resilience** — batch, per-handle fallback on `FAILED`, timeouts, move the
   HTTP call out of the transaction. *(issues 1, 3, 4)*
2. **Migration tooling** — add Flyway, baseline `V1__init.sql`, and set
   `SPRING_PROFILES_ACTIVE=prod` in the Dockerfile. *(issue 2)*
3. **Error statuses** — extend `ResponseEntityExceptionHandler`; fix the 403/401 path.
   *(issues 5, 7)*
4. **Bound blog pagination.** *(issue 6)*
5. **Tests** — CF sync failure modes, plus one `@SpringBootTest` MockMvc test exercising
   the real security chain. Without the latter, the admin guards stay unverified.
6. Leaderboard N+1, case-insensitive handles, frontend page size. *(issues 8, 9, 10)*
7. Delete `fix_docs.py` and `final_diff.txt`.

---

## Assessment

**Production-ready: with fixes.**

The foundations are sound, and several things that looked wrong are right — NULL ordering,
indexes, timestamps, and Lombok defaults all verified correct against generated SQL and
bytecode.

What blocks production is narrower than the issue count suggests. The Codeforces sync is
the weak point, and it is the feature Phase 1 is defined by: one member's typo silently
freezes the leaderboard for everyone, and an untimed call inside a transaction on a
one-thread scheduler can wedge the job until restart. Alongside that, the absence of
migration tooling under `validate` means the first schema change after launch fails the
deploy with no forward path and a documented remedy that drops the schema.
