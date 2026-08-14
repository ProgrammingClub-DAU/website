# Audit Remediation Status

Every issue raised across the four audits, checked against the code as it stands
rather than against what the fix PRs claimed. Verified 14 August 2026.

Legend: **Fixed** — verified in code. **Partial** — the dangerous part is closed,
something specific remains. **Open** — untouched.

| Audit | Issues | Fixed | Partial | Open |
| --- | --- | --- | --- | --- |
| Security | 3 | 3 | 0 | 0 |
| Backend | 10 | 6 | 1 | 3 |
| Frontend | 20 | 8 | 0 | 12 |
| Deployment | 13 + minors | 6 | 1 | 6 + minors |

**Nothing rated Critical is still open.** What remains is one correctness issue,
several accessibility and bundle-size issues, and production-readiness settings
that have no default worth relying on.

---

## Security

| # | Issue | Status | Evidence |
| --- | --- | --- | --- |
| 1 | JWT secret falls back to a public constant | **Fixed** | Fallback removed in #36; PR #38 adds a startup guard that rejects the leaked value by identity. Rotation in the dashboard is still a human action. |
| 2 | Every student's email readable unauthenticated | **Fixed** | `PublicUserResponseDto` (`598ea57`) omits `email` and `role`; the public endpoints return that type. Now asserted by `UserEndpointAuthorizationTest`. |
| 3 | Unpublished blog drafts readable by anyone | **Fixed** | Both single-item lookups use publication-aware finders; two regression tests. |

### A claim worth recording as *not* a finding

A later review reported issue 2 as still open, citing `SecurityConfig` line 133
(`permitAll` on `GET /api/users/**`) and the `email` field on `UserResponseDto`.
Both facts are true; the conclusion does not follow. The public endpoints return
`PublicUserResponseDto`, which has no such field, and the two endpoints that do
return `UserResponseDto` carry `@PreAuthorize` with `@EnableMethodSecurity`
active.

The check that would have made the finding correct is whether method security is
enabled — if it were not, the annotations would be silently ignored. It is.

The underlying concern was still worth acting on, and was: relying on the
annotations alone left no filter-chain backstop, so the public paths are now
listed explicitly and everything else under `/api/users` is deny-by-default.

---

## Backend

| # | Issue | Status | Evidence |
| --- | --- | --- | --- |
| 1 | One bad handle freezes rating sync for the whole club | **Fixed** | Batched with per-handle fallback; 6 regression tests. |
| 2 | No migration tooling under `ddl-auto: validate` | **Fixed** | Flyway + `V1__init.sql` generated from the entities (PR #38). Not yet executed against real PostgreSQL. |
| 3 | Untimed network call inside a transaction | **Partial** | Connect/read timeouts added, so it can no longer hang forever. The call is still inside `@Transactional`, so a slow Codeforces response still holds a database connection for up to 15s. |
| 4 | No batching in CF sync | **Fixed** | `BATCH_SIZE = 100`. |
| 5 | Catch-all handler turns client errors into 500s | **Open** | `GlobalExceptionHandler` has no handler for `HttpRequestMethodNotSupportedException` or `HttpMessageNotReadableException`, so a wrong verb or malformed JSON still reports a server fault for a client mistake. |
| 6 | Blog pagination unbounded and unvalidated | **Open** | `BlogController` still takes bare `int page, int size` with no `@Min`/`@Max`. `?size=1000000` is accepted. `UserController` and `LeaderboardController` both bound it to 100. |
| 7 | Expired sessions return 403 not 401 | **Fixed** | `AuthEntryPointJwt` returns 401 — and now actually serializes its body; see below. |
| 8 | N+1 queries on the leaderboard | **Fixed** | Replaced with `RANK()`; a test asserts a page costs 2 queries regardless of page size. |
| 9 | Case-sensitive handle uniqueness, then silent drop | **Open** | `existsByCodeforcesHandle` is case-sensitive, so `Tourist` and `tourist` can both register. The sync keys its lookup map on the lowercased handle and resolves the collision with `(existing, replacement) -> existing`, so one of those members never receives a rating and nothing is logged. |
| 10 | Frontend truncates lists at 20 | **Open** | `dashboard.ts` calls `/api/users` and `/api/leaderboard` with no `size`, taking the backend default of 20. Member 21 onward is invisible on both pages. |

### Found while fixing, not in any audit

`AuthEntryPointJwt` constructed its own Jackson 2 `ObjectMapper`. Spring Boot 4
serializes with Jackson 3, and Jackson 2 is only on the classpath transitively,
so that mapper had no JSR-310 module and threw on the entry point's own
`LocalDateTime` timestamp. **Every 401 failed while writing its body.**

Issue 7 above was therefore fixed in name only: the status code was right, but
the response never serialized, so the frontend still never received the 401 it
keys its login redirect on. Fixed by injecting Spring's configured mapper.

It survived four audits because no test had ever sent a request through the real
security filter chain — every MockMvc test uses `standaloneSetup`, which contains
no security at all. The first test that did found it immediately.

---

## Frontend

All five criticals are fixed: `/members` rendering zero members, `/leaderboard`
blanking the document on a search miss, `/profile/undefined` links, fabricated
club figures, and `🔥 NaN activities`.

| # | Issue | Status |
| --- | --- | --- |
| C1–C5 | All five criticals | **Fixed** |
| I1 | No `error.tsx` | **Fixed** |
| I2 | No `loading.tsx` | **Fixed** |
| I3 | axios has no timeout | **Fixed** |
| I12 | Every member renders as a grey Newbie | **Fixed** |
| I15 | `<SampleBadge />` above live API data | **Fixed** |
| I4 | `next/image` cannot load Codeforces avatars — `next.config.ts` has no `remotePatterns` | **Open** |
| I5 | Unknown profile id returns 200 — `profile/[id]/page.tsx` never fetches server-side and never calls `notFound()` | **Open** |
| I6 | Dashboard section titles are not headings | **Open** |
| I7 | Heading order skips a level (`H1 → H3 → H4`) | **Open** |
| I8 | Filters not announced; search boxes unlabelled | **Open** |
| I9 | Light theme fails WCAG AA on Codeforces colours used as text | **Open** |
| I10 | recharts (104 kB gzip) eagerly bundled into `/profile/[id]` | **Open** |
| I11 | zod costs 73 kB gzip on `/login` and `/register` | **Open** |
| I13 | Member cards link to `/profile`, not the member's profile (`members-directory.tsx:394`) | **Open** |
| I14 | API shapes asserted, never validated — three `no-explicit-any` suppressions | **Open** |

I13 is the one to fix first: it is a plain bug, not a refinement. Every card in
the directory sends you to your own profile.

I6–I9 are accessibility issues on a public university club site, which is the
context in which they matter most.

---

## Deployment

| # | Issue | Status | Evidence |
| --- | --- | --- | --- |
| C1 | `SPRING_PROFILES_ACTIVE=prod` set nowhere | **Fixed** | Baked into the Dockerfile; `curl /v3/api-docs` documented as the check. |
| C2 | Docs name `DATABASE_URL`, which the code does not read | **Fixed** | Playbook corrected; `backend/.env.example` documents the JDBC rewrite. |
| C3 | Zero repo record of backend deploy config — bus factor 1 | **Partial** | `.env.example` documents the variables; there is still no `render.yaml` or equivalent, so the deployment cannot be recreated from the repo alone. |
| C4 | Documented recovery destroys data, depends on a backup that doesn't exist | **Fixed** | `DATABASE-BACKUP.md` plus backup/restore scripts. **The first backup still has to be taken.** |
| I1 | No `backend/.env.example` | **Fixed** | |
| I2 | `frontend/.env.example` names the wrong platform | **Fixed** | |
| I9 | `-Xmx256m` brittle | **Fixed** | `MaxRAMPercentage=75.0`. |
| I3 | CORS default silently breaks writes in production | **Open** | Still defaults to `http://localhost:3000`. If `CORS_ALLOWED_ORIGINS` is unset, reads work and every browser write fails preflight — the hardest class of bug to diagnose from a bug report. |
| I4 | No graceful shutdown | **Open** | No `server.shutdown: graceful` in either YAML; in-flight requests are cut on deploy. |
| I5 | No connection-pool configuration | **Open** | No `spring.datasource.hikari.*`. The default pool of 10 against a free-tier Postgres connection cap is a plausible outage. |
| I6 | Scheduled sync won't fire on a sleeping instance | **Open** | `@Scheduled` is in-process; a free-tier instance that spins down never runs it, so ratings quietly stop updating. |
| I7 | Health endpoint cannot detect the failure that matters | **Open** | Returns a static `UP` map; reports healthy with the database unreachable. |
| I8 | CI never builds the container image | **Open** | A Dockerfile change that breaks the build is only discovered at deploy. |

Minor and still open: EOL `-focal` base images, obsolete `version:` key in
`docker-compose.yml`, Maven version mismatch between Dockerfile and wrapper, no
`.dockerignore`, no tags or releases, and the test-environment parity gap (H2
with `create-drop` cannot execute the PostgreSQL migrations).

---

## Suggested order

1. **Frontend I13** — member cards link to the wrong page. A visible bug.
2. **Backend 10** — pass an explicit page size; members past the 20th are
   invisible on both the directory and the leaderboard.
3. **Deployment I3 and I5** — CORS and connection pool. Both are one-line
   settings whose defaults are wrong for the deployed environment.
4. **Backend 9** — case-insensitive handle uniqueness, so a member cannot end up
   silently unrated forever.
5. **Backend 5 and 6** — client errors reported as client errors; bound the blog
   page size.
6. **Frontend I6–I9** — accessibility.
7. **Deployment I4, I6, I7, I8** — graceful shutdown, external sync trigger, a
   health check that touches the database, and a container build in CI.

Two things remain that no code change can do: **rotate `JWT_SECRET`** in the
hosting dashboard, and **take the first database backup.**
