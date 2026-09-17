# Phase 3 -- Senior Engineer Implementation Plan (v1)
**Branch model:** every member branches from `main` and raises PRs into `main`, merged one at a time by M6 -- unchanged from Phase 2.
**Duration:** 4 sprints -- ~8 weeks
**Team:** 6 members
**Stack:** Spring Boot 4.1 -- PostgreSQL -- Next.js 16.3 -- Cloudinary -- Apache POI -- Testcontainers -- Spring Mail -- jsoup -- react-markdown + KaTeX

> [!IMPORTANT]
> **The scope is deliberately everything.** Phase 3 carries three groups of work:
>
> 1. The stats features requested for profiles and leaderboards -- problems solved,
>    contests given and average problem rating, by week, month, year and all time,
>    per platform and overall.
> 2. Every item from the original plan (`implementation_plan.md` and
>    `project_roadmap_all_phases.md`) that was never built -- except CodeChef and
>    AtCoder sync. Those two stay **link only** (D9).
> 3. Seven additions proposed during the Phase 2 review.
>
> Section 0 traces every one of them to the stage that builds it, so nothing is
> dropped silently a second time.
>
> The cost is size. Phase 3 is roughly three times Phase 2, which was planned at
> three weeks. The work is therefore split into three waves (Section 2), ordered so
> each wave ships something members can use on its own. If the schedule slips, the
> site is better after every completed wave rather than half-built across all of
> them. M6 re-plans the remaining waves after Wave A merges, using its real pace.

---

## Amendments -- Read Before Any Phase 3 Work

This plan was written at the end of Phase 2. A run of work shipped afterwards,
between Phase 2 and Phase 3, and it invalidates parts of what follows. These
amendments override the body of the document wherever they disagree with it.

Two of them would stop a deploy or waste a fortnight, so they come first.

### A1 -- Migration numbers V8 to V12 are taken. Renumber every Phase 3 migration.

`main` already has V8 through V12:

| Applied | What it did |
|---|---|
| `V8__add_academic_year.sql` | first year / second year onwards |
| `V9__passwords_no_longer_required.sql` | password column relaxed, sign-in moved to Google |
| `V10__event_results.sql` | contest URL, podium, three visibility switches |
| `V11__event_type.sql` | Flagship / Contest / Workshop / ICPC / Talk |
| `V12__hall_of_fame.sql` | admin-maintained Hall of Fame entries, links and photos |

The plan assigns V8, V9, V10 and V12 to V15 to Phase 3 tables. Two files sharing
a version number is not a warning: Flyway refuses to start with "Found more than
one migration with version 8", so the deploy fails outright and the service does
not come up.

Renumber, keeping the plan's order:

| In the plan | Use instead |
|---|---|
| `V8__create_codeforces_problems_and_solves.sql` | `V13__...` |
| `V9__create_contest_participations.sql` | `V14__...` |
| `V10__create_platform_daily_totals.sql` | `V15__...` |
| `V12__create_scores_awards_and_practice.sql` | `V16__...` |
| `V13__create_club_contests.sql` | `V17__...` |
| `V14__blog_workflow_and_comments.sql` | `V18__...` |
| `V15__events_registrations_calendar_and_reminders.sql` | `V19__...` |

Stage 1A's "Read first" line refers to V8 to V12. Those are now Phase 2.5
migrations and are worth reading for a different reason -- they are the schema
this work extends -- but the Phase 3 tables are V13 onward.

**Check the highest applied version before adding a migration**, every time.
This table has already had to move once, because V12 was taken by the Hall of
Fame after it was written.

### A2 -- D15 is withdrawn. There is no email verification to build.

D15 specified a verification link mailed on signup, single use, 24 hours,
SHA-256 hashed at rest, plus `email_verified_at`, an allowed-domain policy in
`AuthService.register`, and a `VerifiedMemberGuard` gating Phase 3 writes.

None of it is needed, and none of it can be built as written. Sign-in is now
Google-only and restricted to the university domain. Before an account exists,
Google has already established both things D15 set out to establish:

- **The address is real and the holder controls it** -- the ID token's
  `email_verified` claim, checked server-side.
- **It belongs to the university** -- the address suffix and the `hd`
  hosted-domain claim, both checked server-side.

There is also no `AuthService.register` to modify and no registration form to
add a domain check to. Signing in for the first time is what creates an account.

**What this deletes from the plan:** the verification token table and its
columns, the mail templates, the resend endpoint and its rate limit, Spring Mail
configuration for this purpose, and every "unverified member" branch.

**What replaces `VerifiedMemberGuard`:** plain `authenticated()`. Every account
that exists reached this site through a verified university Google account, so
"signed in" and "verified member" are now the same set of people. Where the plan
says `VerifiedMemberGuard`, read `isAuthenticated()`.

Spring Mail stays on the stack list only if a later stage sends mail for another
reason -- event reminders, for instance. Check before removing the dependency.

### A3 -- Already built. Do not build these again.

Shipped between Phase 2 and Phase 3, and live on `main`:

| Area | What exists now |
|---|---|
| Sign-in | Google-only, `@dau.ac.in` enforced server-side. No passwords, no registration form. First sign-in creates the account and routes to `/welcome`, which asks for name (required), year (required), and optionally handle and phone |
| Members | `GET /api/users/team` -- office bearers, hierarchy-ordered, public. The members page is the committee in two sections: core team (Convenor, Deputy Convenor, Core, Associate Core) and batch representatives. There is no public directory of all members |
| Privacy | Phone numbers are public for office bearers and admin-only for everyone else, decided server-side in `PublicUserResponseDto` |
| Events | Public events page driven by the API, on the club timeline; public event detail page at `/events/{id}`; contest URL, podium picked from attendees, and three independent visibility switches; event type badge; reopen a completed or cancelled event |
| Attendance | Export is six columns -- name, Codeforces profile, email, student ID, year, added at. Also exports to Google Sheets from the browser, into the admin's own Drive |
| Profiles | Academic year, and a completeness badge over name, Codeforces handle, phone and year |
| Hall of Fame | Admin-maintained entries -- heading, subheading, date, details, any number of links and photos -- at `/api/hall-of-fame`, with a public page and a detail page per entry |
| Gallery | `GET /api/gallery/photos` merges event and Hall of Fame photos, newest first, each linking back to its source |

Anything in Section 0 that traces to one of these is done. Check `main` before
starting a stage rather than trusting the traceability table alone.

### A4 -- The sync must fit in 160 MB of heap. Three rules.

This is the constraint most likely to be discovered the hard way, because the
obvious implementation works perfectly on a laptop and dies in production.

The deployed backend runs with `-Xmx160m`, `-XX:MaxMetaspaceSize=160m` and
`-XX:ReservedCodeCacheSize=48m` on a 512 MB instance. Those numbers are not
arbitrary: the service died of `OutOfMemoryError: Metaspace` in production once
already, and the Metaspace cap was raised from 96 MB out of the heap's budget to
fix it. There is no slack left.

`user.status` returns a member's entire submission history in one JSON array.
For an active competitor that is megabytes of JSON, and the naive shape --
fetch everything, map it to entities, `saveAll` -- holds the raw response, the
parsed tree and the entity graph in the heap at the same time, for every member
in turn. That is the shape that fails.

**Rule 1 -- Sync incrementally.** Keep the highest submission id already stored
per member. Codeforces returns submissions newest-first, so page until you meet
that id, then stop. After the first import a routine run fetches a handful of
submissions per member rather than thousands. This is worth more than every
other optimisation combined.

**Rule 2 -- Page the backfill, never the whole history.** Use
`userStatus(handle, from, count)` with `count = 500`, insert the batch, discard
it, take the next page. Peak memory becomes one page instead of one member's
lifetime. Section 4.2 currently says "omit `from` and `count` for full history"
-- **do not**. That sentence describes the failing path.

**Rule 3 -- Store derived facts, not raw submissions.** D3 says gym and mashup
submissions are stored "because the upsolve tracker needs them". D12 defines an
upsolve far more narrowly: an accepted submission **inside the club contest,
after it ended**. That is a small, knowable set. Store distinct solves plus
those upsolve rows, and the raw submission table does not need to exist at all
-- fewer rows, less memory, and one less table to keep correct.

**And in the query layer:** every statistic in Section 1.1 is a SQL aggregate
over indexed columns. Counting or averaging in Java makes heap scale with
history, which is precisely what this instance cannot afford.

**Run the one-time historical backfill off the box.** Stage 7's backfill is the
single heaviest thing Phase 3 does. Run it from a GitHub Actions job against the
production API rather than from a Render dyno: Actions gives several GB of
memory for free, and Render then only ever performs the light incremental work.
Do not move the recurring sync there -- that would put domain logic in two
places.

### A5 -- Scheduled jobs and the free tier

`@Scheduled` is an in-process timer, and Render's free tier stops an idle
instance, which stops its timers with it. D6's daily baseline at 00:05 IST is
the case that matters: nobody is on the site at five past midnight, so on an
unattended free instance that job never runs and every LeetCode period figure is
silently wrong rather than missing.

**Current mitigation:** an external cron pings the service every 10 minutes, so
it never idles out and the timers fire. Phase 3 may rely on that.

Two consequences worth writing down:

- A service that never sleeps consumes about 730 of the ~750 free instance hours
  in a month. There is no room for a second free service on the same account.
- Sleeping used to restart the JVM nightly, which quietly reset Metaspace and
  masked any slow growth. It no longer does. If Phase 3 introduces a leak, this
  is when it will surface. `-XX:+ExitOnOutOfMemoryError` is set, so the container
  dies and restarts rather than limping -- self-healing, but with downtime and a
  cold start attached.

### A6 -- Smaller corrections

- Section 8 lists `app/(auth)/register/register-form.tsx` as a file to modify.
  It was deleted with the registration flow. The equivalent screen is
  `app/(auth)/welcome/welcome-form.tsx`.
- `lib/content/events.ts` and `components/site/events-timeline.tsx` were deleted;
  the events page reads the API through `components/site/events-list.tsx`.
- `dashboardService.getMembers` and `getDirectory` no longer exist. The members
  page uses `getTeam`.
- The frontend stack line should read Google Identity Services rather than a
  password form.

---

## Prerequisites -- Phase 2 Must Be Finished First

Phase 3 builds on Phase 2 screens that do not exist yet. Do not start Phase 3
frontend work until these are merged into `main`.

| Phase 2 item | Why Phase 3 needs it |
|---|---|
| Stage 2A -- `DataTable`, `ClubRoleBadge`, `AdminTabs` | Every Phase 3 admin screen is a new `AdminTabs` tab built on `DataTable`. |
| Stage 2C -- admin dashboard and `/admin/events/[id]` | RSVP "Mark attended" (Stage 6C) and club contest admin (Stage 4C) extend these pages. |
| Stage 2A -- Events and Gallery pages reading the API | The RSVP button and Add to Calendar live on `/events/[id]`. |
| Stage 3 -- Testcontainers | Phase 3 stats queries use PostgreSQL date and window functions that H2 cannot run. **If this is still open at kickoff, M6 lands it in Stage 0.** |
| Stage 3 -- Cloudinary preset hardening, phone backfill | Not blockers, but they should not be carried into a third phase. |

Backend Stages 0, 1A, 1B and 1C have no Phase 2 frontend dependency and can start
as soon as Phase 2 backend work is merged -- which it already is.

---

## [START] How to Get Started -- Starter Prompts for Every Member

> [!IMPORTANT]
> Every member owns **several** stages in Phase 3, not one. The prompts below are
> written to be pasted again at the start of each stage: they tell your AI coding
> assistant to work out which of your stages is next from what is already merged,
> and to stop if a dependency is missing.

---

### [M6] Member 6 -- Team Lead & DevOps
**Stages, in order:** 0 -- 1A -- 3A -- 7
**Branch:** `feature/M6-phase3-stage-<id>` (for example `feature/M6-phase3-stage-1a`)

```
I am Member 6 (Team Lead & DevOps) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 0, Stage 1A, Stage 3A, Stage 7.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1 and 2 first (scope, locked decisions, dependencies).
2. Run: git log --oneline main
   Work out which of my stages are already merged. Implement ONLY my next
   unmerged stage, and only if every stage it depends on in Section 2 is merged.
   If a dependency is missing, stop and tell me which one.
3. Read the section for that stage, and every file listed under "Read first".

Rules:
- Branch from main: feature/M6-phase3-stage-<id>
- Every new DB column must be nullable or carry a DEFAULT (backward compatible)
- @Column names must match the SQL column names exactly, or ddl-auto: validate fails
- Every @Scheduled cron takes zone = "Asia/Kolkata" (Section 1, D1)
- Every external platform call acquires that platform's own RateLimiter bean
- Run ./mvnw.cmd clean test and fix every failure before raising a PR
```

---

### [M5] Member 5 -- Backend Data & APIs
**Stages, in order:** 1B -- 3B -- 5B
**Branch:** `feature/M5-phase3-stage-<id>`

```
I am Member 5 (Backend Data & API Engineer) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 1B, Stage 3B, Stage 5B.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1 and 2 first. Section 1.1 defines every stat precisely;
   do not invent a different definition.
2. Run: git log --oneline main
   Implement ONLY my next unmerged stage, and only if its dependencies in
   Section 2 are merged. If one is missing, stop and tell me which.
3. Read the section for that stage, and every file listed under "Read first".

Rules:
- Branch from main: feature/M5-phase3-stage-<id>
- Stats queries are native PostgreSQL. Test them with Testcontainers against real
  PostgreSQL with Flyway on, never H2 (Section 1, D22)
- Period boundaries are resolved in Java, from Asia/Kolkata to UTC, before the
  query runs (D1, D31). No AT TIME ZONE in SQL.
- Invalid metric/platform/period combinations return 400, never an empty list
- Run ./mvnw.cmd clean test and fix every failure before raising a PR
```

---

### [M4] Member 4 -- Backend Security
**Stages, in order:** 1C -- 3C -- 5A
**Branch:** `feature/M4-phase3-stage-<id>`

```
I am Member 4 (Backend Security Engineer) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 1C, Stage 3C, Stage 5A.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1 and 2 first.
2. Run: git log --oneline main
   Implement ONLY my next unmerged stage, and only if its dependencies in
   Section 2 are merged. If one is missing, stop and tell me which.
3. Read the section for that stage, and every file listed under "Read first".
   Always read backend/src/main/java/com/cpclub/backend/security/config/SecurityConfig.java
   -- Spring Security evaluates matchers top to bottom, and the existing
   "/api/events/**" ADMIN wildcard will swallow any member route added below it.

Rules:
- Branch from main: feature/M4-phase3-stage-<id>
- Every new route gets an authorization test for anonymous, USER and ADMIN,
  in the style of UserEndpointAuthorizationTest
- Secrets (CF API secret, SMTP password, token hashes) never appear in logs or DTOs
- Run ./mvnw.cmd clean test and fix every failure before raising a PR
```

---

### [M1] Member 1 -- Frontend UI/UX
**Stages, in order:** 2A -- 4A -- 6A
**Branch:** `feature/M1-phase3-stage-<id>`

```
I am Member 1 (Frontend UI/UX Architect) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 2A, Stage 4A, Stage 6A.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1 and 2 first.
2. Run: git log --oneline main
   Implement ONLY my next unmerged stage, and only if its dependencies in
   Section 2 are merged. If one is missing, stop and tell me which.
3. Read the section for that stage, plus frontend/src/app/globals.css and
   frontend/src/components/site/primitives.tsx for the design system.

Rules:
- Branch from main: feature/M1-phase3-stage-<id>
- Use the type scale tokens (text-label, text-meta, text-body, text-lead), never
  text-[Npx]. tracking-caps / tracking-caps-wide only ever go with uppercase.
- Charts are plain SVG, like components/site/rating-graph.tsx. Do not add recharts
  or any chart library -- recharts was removed on purpose (about 104 kB gzipped).
- Never render invented placeholder data. Every list gets a designed empty state.
- Every page works at 400px wide and in both light and dark themes
- Run npm run build and npm run lint with zero errors before raising a PR
```

---

### [M2] Member 2 -- Frontend Auth & State
**Stages, in order:** 2B -- 4B -- 6B
**Branch:** `feature/M2-phase3-stage-<id>`

```
I am Member 2 (Frontend Auth & Logic Engineer) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 2B, Stage 4B, Stage 6B.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1, 2 and Section 11 (API contract).
2. Run: git log --oneline main
   Implement ONLY my next unmerged stage, and only if its dependencies in
   Section 2 are merged. If one is missing, stop and tell me which.
3. Read the section for that stage, plus frontend/src/types/api.ts,
   frontend/src/store/auth.ts and frontend/src/lib/axios.ts.

Rules:
- Branch from main: feature/M2-phase3-stage-<id>
- Every type mirrors the backend DTO field for field; nullable fields are "| null"
- Every service function unwraps ApiResponse and returns the data, like events.ts
- Forms validate with zod and react-hook-form, matching the existing auth forms
- Run npm run build and npm run lint with zero errors before raising a PR
```

---

### [M3] Member 3 -- Frontend Dashboards & Data
**Stages, in order:** 2C -- 4C -- 6C
**Branch:** `feature/M3-phase3-stage-<id>`

```
I am Member 3 (Frontend Dashboards Engineer) on the CP Club Website project.
I own these Phase 3 stages, in this order: Stage 2C, Stage 4C, Stage 6C.

IMPORTANT: each of my stages depends on the M1 AND M2 stages of the same wave.
Do not start until both are merged.

Before writing any code:
1. Read the full Phase 3 plan: documents/phase_3_execution_playbook.md
   -- Sections 0, 1 and 2 first.
2. Run: git log --oneline main
   Implement ONLY my next unmerged stage, and only if its dependencies in
   Section 2 are merged. If one is missing, stop and tell me which.
3. Read the section for that stage, and every file listed under "Read first".

Rules:
- Branch from main: feature/M3-phase3-stage-<id>
- Use M1's components and M2's services; do not call axios directly from pages
- Never render invented numbers. A stat with no data shows an explained empty
  state ("Tracking since 3 Nov"), never a zero that looks like a real result.
- Run npm run build and npm run lint with zero errors before raising a PR
```

---

## Section 0 -- Scope Traceability

Every item Phase 3 is committed to, where it came from, and the stages that build it.

**Source key:** *Requested* -- asked for directly. *OG P1/P2/P3* -- in the original
plan for that phase and never built. *Proposed* -- suggested during the Phase 2 review.

| ID | Item | Source | Stages |
|---|---|---|---|
| R1 | Contests given on Codeforces -- stored and rankable, not just counted in the browser on the profile page | Requested | 1A, 1B, 2C |
| R2 | Real "Total solved" on the profile, replacing the fake blurred **247** | Requested | 1B, 2C |
| R3 | Top problem solvers -- this week, month, year and all time -- CF, LeetCode and overall | Requested | 1A, 1B, 2C |
| R4 | Most contests given -- this week, month, year and all time -- CF, LeetCode and overall | Requested | 1A, 1B, 2C |
| R5 | Average problem rating solved on Codeforces | Requested | 1B, 2C |
| O1 | Blog read from the API -- the page currently renders an empty hard-coded list while `/api/blogs` sits unused | OG P1 | 5A, 6A, 6B |
| O2 | Markdown, LaTeX and code highlighting in posts | OG P1 | 6A |
| O3 | Blog comments | OG P1 | 5A, 6A, 6B |
| O4 | Member-written posts with core-team review. The plan deferred "open contribution" to a later phase, and the live blog page already promises "Drafts are reviewed by the core team" | OG decision 2 | 5A, 6A, 6C |
| O5 | Registration restricted to, and verified by, student email | OG P1 | 1C, 2B |
| O6 | CodeChef sync -- **not built.** CodeChef stays link only, as Phase 2 decided (D9) | OG P2 | -- (by decision) |
| O7 | AtCoder sync -- **not built.** AtCoder stays link only, as Phase 2 decided (D9) | OG P2 | -- (by decision) |
| O8 | Overall Aggregated Score | OG P2 | 3B, 4C |
| O9 | Weekly Winners -- Coder of the Week, Most Problems Solved This Week, Biggest Rating Jump -- as badges and homepage shoutouts | OG P2 | 3B, 4A, 4B, 4C |
| O10 | Rating progression charts for every platform. Only Codeforces is drawn today, and the frontend never calls `/api/snapshots` | OG P2 | 1A, 4C |
| O11 | Members RSVP for events themselves | OG P3 | 5B, 6A, 6B, 6C |
| O12 | Central club calendar | OG P3 | 3A, 4A |
| O13 | Codeforces club contest sync with club-only ranklists | OG P3 | 3C, 4A, 4C |
| O14 | Club Championship season leaderboard | OG P3 | 3C, 4A, 4C |
| O15 | Delete events | OG P3 | 5B, 6C -- guarded, see D14 |
| P1 | Upsolve tracker | Proposed | 3C, 4A, 4B |
| P2 | Topic (tag) strength on profiles | Proposed | 3B, 4A, 4C |
| P3 | Practice suggestions | Proposed | 3B, 4A, 4B |
| P4 | Upcoming contests calendar -- CF, LeetCode, CodeChef, AtCoder -- with Add to Google Calendar | Proposed | 3A, 4A |
| P5 | Event attendance leaderboard | Proposed | 1B, 2C |
| P6 | Compare two members | Proposed | 1B, 2A, 2C |
| P7 | Opt-in email reminders for club events and contests | Proposed | 5B, 6B |

---

## Section 1 -- Locked Decisions & Rationale

| ID | Decision | Choice | Why |
|---|---|---|---|
| D1 | Time zone and periods | **Calendar periods in `Asia/Kolkata`.** WEEK = since Monday 00:00 IST. MONTH = since the 1st, 00:00 IST. YEAR = since 1 January, 00:00 IST. ALL_TIME = everything. Every `@Scheduled` cron sets `zone = "Asia/Kolkata"`. | "This week" on a club site means the current week, not a rolling seven days. No zone is configured today, so the existing crons run in the JVM default -- UTC on Render -- and the Monday snapshot actually fires at 05:30 IST. Stage 0 fixes the existing crons as well. |
| D2 | What "solved" means | A **distinct problem with at least one accepted verdict**, dated by its **first** accepted submission. | Resubmitting a solved problem must not move it into this week. |
| D3 | Which Codeforces solves count toward stats | **Problemset problems only** (`contestId < 100000`). Gym and mashup submissions are still synced and stored, because the upsolve tracker needs them, but they are excluded from solved counts and average rating. | A mashup re-issues existing problems under a new contest ID, so counting both would count the same problem twice. The number can differ slightly from the one on a member's Codeforces profile; the UI says so in a tooltip. |
| D4 | Average problem rating | Mean `problem.rating` over **distinct counted solves that have a rating**. Unrated problems are excluded. A member needs **at least 10 rated solves** in the period to appear on that leaderboard. The UI always shows the basis ("over 42 rated problems"). | New problems have no rating yet. Averaging over two easy solves would put a beginner at the top of the board. |
| D5 | "Contests given" | A contest the member entered as a real participant, dated by the contest's start time. **CF:** rated contests from `user.rating`, plus unrated contests where `user.status` shows a `CONTESTANT` or `OUT_OF_COMPETITION` submission. Virtual contests do not count. **LeetCode:** contest history entries with `attended = true`. | Codeforces exposes no participation record for an unrated contest where the member submitted nothing. That case is not countable, and it is documented rather than guessed. |
| D6 | LeetCode solved per period | **Live total minus a baseline.** Every 6 hours the sync refreshes the member's live total on `users`. At 00:05 IST a separate job fetches a fresh total and writes it to `platform_daily_totals` as that day's baseline. Period solved = live total - the baseline from the period's first day. All-time = the live total. | LeetCode has no public, complete, dated list of solves. The submission calendar counts submissions, not distinct problems, and would overstate. **Consequence:** LeetCode period numbers only exist from the day this ships. The UI shows "Tracking since <date>" and never a misleading zero. |
| D7 | "Overall" | **Codeforces + LeetCode**, for both solved and contests. No cross-platform deduplication. CodeChef and AtCoder are link only (D9) and never counted. | The platforms have separate problem sets, so the same problem cannot appear twice. |
| D8 | Overall Aggregated Score | Formula in Section 1.2. **[DECISION NEEDED]** M6 confirms the weights at kickoff; the formula below is the default if nobody objects. | Rating scales are not comparable across platforms, so the score uses percentiles within the club rather than raw numbers. |
| D9 | CodeChef and AtCoder | **Link only -- the Phase 2 decision stands.** Members keep the existing `codechef_url` and `atcoder_url` profile links. Phase 3 fetches **no** rating, solves, contests or totals from either platform, so neither appears in stats, leaderboards, the Overall Score, weekly awards or rating charts. The calendar still lists their **public contest schedules** (D26) -- that is not member data. | CodeChef has no public API, so its stats would mean scraping profile pages that break on any markup change. Keeping both platforms link only removes that risk, and about 3 days from Stage 3A. |
| D10 | Club contest access | **Authorized Codeforces API** (`apiKey` + `apiSig`), using a key from a club group manager's account. | Group contests and private mashups are not visible to anonymous API calls. |
| D11 | Club Championship scoring | Formula in Section 1.3. **[DECISION NEEDED]** M6 confirms at kickoff; the default below applies otherwise. | Rewards finishing high among club members, independent of how many outsiders entered. |
| D12 | What counts as an upsolve | An accepted submission **inside the club contest on Codeforces**, made after the contest ended. Solving the same problem elsewhere does not count. | A mashup problem cannot be reliably mapped back to its original. Matching on the club contest ID is exact, and "upsolve from the contest page" is a one-sentence instruction to members. |
| D13 | RSVP versus attendance | **Two separate records.** An RSVP is a member's own intent, open only while the event is UPCOMING and before its start time. Attendance stays admin-only, exactly as Phase 2 decided. The admin can turn RSVPs into attendance in bulk. | RSVP answers "how many chairs". Attendance is the official record. Merging them would let members mark themselves present. |
| D14 | Deleting events | `DELETE /api/events/{id}` **succeeds only when the event has no attendees, RSVPs or photos.** Otherwise it returns 409 and tells the admin to cancel instead. | The original plan asked for delete; Phase 2 chose cancel to protect history. This allows both: an event created by mistake can be removed, and a real one cannot be erased. |
| D15 | Student email | **WITHDRAWN -- see amendment A2.** Google-only sign-in already proves the address is verified and on the university domain, so there is nothing left to build. Original text, for the record: new registrations must use an allowed domain, from `cpclub.auth.allowed-email-domains` (default `dau.ac.in`). A verification link (single use, 24 hours, SHA-256 hashed at rest) is emailed on signup. **Existing accounts are treated as verified** (`email_verified_at = created_at`). | Allowed domains live in config, so an alumni or faculty exception needs no code change. Forcing about 150 accepted members to re-verify at launch would cost goodwill for no security gain. |
| D16 | What unverified accounts can do | Sign in, and edit their own profile. **Cannot** appear in the directory or on leaderboards, RSVP, comment, or write posts. | Verification is the only thing that makes the student-email restriction mean anything. |
| D17 | Blog rendering | Markdown stays in the existing `content` column. It is rendered in the browser with `react-markdown` + `remark-gfm` + `remark-math` + `rehype-katex` + `rehype-highlight`. **Raw HTML is never enabled** (no `rehype-raw`). KaTeX CSS loads on blog routes only. | Member-written posts are untrusted input. With raw HTML off, there is no path from a post to injected script. |
| D18 | Blog workflow | `status`: DRAFT -> PENDING_REVIEW -> PUBLISHED, or REJECTED with a reviewer note. Any verified member writes drafts; only an admin publishes. Public reads stay **published-only**, as `BlogService` already enforces today. | This keeps the existing guarantee that nobody can walk post IDs to read drafts. |
| D19 | Comments | Verified members only. One level of replies. Plain text, maximum 2000 characters. Authors can edit for 15 minutes and delete at any time (soft delete, shown as "[deleted]"). Admins can delete any comment. One comment per 20 seconds per member. | Kept deliberately small. Threads and reactions belong in the Phase 4 community work. |
| D20 | Charts | **Plain SVG**, following `rating-graph.tsx`. `react-activity-calendar`, already a dependency, is the only chart-like library allowed. | recharts was removed on purpose, saving about 104 kB gzipped on the profile route. |
| D21 | Problem tags storage | A join table (`cf_problem_tags`), not a PostgreSQL array. | Portable, indexable, and simple to filter by tag. |
| D22 | Testing stats queries | **Testcontainers PostgreSQL, with Flyway on, is mandatory** for every native stats query. | The current H2 test profile runs with Flyway off and builds the schema from the entities, so a green H2 suite proves nothing about the Phase 3 migrations, their CHECK constraints and expression indexes, or the percentile window functions. |
| D23 | Rate limiting | **One `RateLimiter` bean per host.** Today LeetCode sync shares `codeforcesRateLimiter`, so every LeetCode call waits behind the Codeforces queue. | A long Codeforces backfill must not delay LeetCode, and vice versa. |
| D24 | Practice suggestions | Formula in Section 1.4. | Suggestions target the member's actual gaps rather than a random rating range. |
| D25 | Compare two members | Public data only -- never email or phone. Includes head-to-head results in rated Codeforces contests both members entered. | It is a public page, reachable by anyone with two member IDs. |
| D26 | Upcoming external contests | Codeforces `contest.list` (official). LeetCode GraphQL `upcomingContests`. AtCoder: jsoup on `atcoder.jp/contests`. CodeChef: its contest list JSON. Each is **flagged and isolated**, synced every 6 hours into `external_contests`. **Fallback:** if the AtCoder or CodeChef sources prove fragile, M6 may switch both to the clist.by API (free key) without changing any endpoint. | Four independent sources mean one breaking never empties the calendar. |
| D27 | Attendance leaderboard | Counts **admin-marked attendance** at COMPLETED events -- never RSVPs. Periods: MONTH, YEAR, ALL_TIME. | RSVPs are intent, not presence. Weekly periods would almost always be empty. |
| D28 | Removing mock data | Delete `lib/content/mock-dashboards.ts` and the mock member stats in `lib/content/members.ts`, and remove every `IS_MOCK` branch in `dashboard.ts`. The `credits` export in `members.ts` is real and moves to its own file. | While those files exist, invented numbers are one flag flip away from being shown to members again. |
| D29 | Reminder emails | **Opt-in only** (`notify_events`, `notify_contests`, both default false). Every email carries a signed one-click unsubscribe link. `reminder_log` makes every send idempotent. | A re-run cron must never email the same person twice. |
| D30 | Scaling limit | The backend stays **single-instance** for Phase 3. | `@Scheduled` has no distributed lock (already documented in `SnapshotService`), and the comment and resend rate limits are in memory. Running two instances would need ShedLock first. |
| D31 | Timestamps | **Every Phase 3 `TIMESTAMP` column holds UTC.** Epoch seconds from platform APIs are converted with `ZoneOffset.UTC`, and period boundaries are converted from IST to UTC in Java before any query runs. | Existing columns map to `LocalDateTime`, which silently takes the zone of whichever machine wrote the value -- UTC on Render, IST on a developer laptop. Pinning one zone is what keeps a Sunday 23:50 IST solve out of next week's leaderboard. |

> [!NOTE]
> **Before implementing any GraphQL, JSON or HTML parser in this plan, capture one
> real response and commit it as a test fixture.** The field names here (for
> example LeetCode `userContestRankingHistory` and `upcomingContests`, and the
> AtCoder contests page layout) are the ones in public use today, but none of
> these endpoints is versioned. The fixture is what the tests pin, and it is how a
> future break gets diagnosed.

### 1.1 Stat Definitions (Reference)

All counts are per member, for a period P from D1, with `[start, now)` computed in `Asia/Kolkata`.

| Stat | Platform | Definition |
|---|---|---|
| `solved` | CODEFORCES | `COUNT(*)` over `cf_solves` joined to `cf_problems` where the problem is a problemset problem (D3) and `first_ac_at` is in P |
| `solved` | LEETCODE | ALL_TIME: `users.leetcode_total_solved`. Otherwise: that live total minus the `platform_daily_totals` baseline with the latest `captured_on` on or before P's first IST date. Null when no such baseline exists -- shown as "Tracking since <date>" |
| `solved` | OVERALL | Codeforces + LeetCode. When LeetCode is null, the total is Codeforces alone, and the response flags it as partial |
| `contests` | each platform | `COUNT(*)` over `contest_participations` for that platform where `started_at` is in P |
| `contests` | OVERALL | Sum across platforms |
| `avgProblemRating` | CODEFORCES only | `AVG(rating)` over counted solves in P with `rating IS NOT NULL`; null below 10 rated solves (D4) |
| `lcDifficulty` | LEETCODE | Easy / Medium / Hard, from the `users.leetcode_*_solved` live columns and the matching baseline columns, using the same rule as `solved` |

### 1.2 Overall Aggregated Score (default -- D8)

Recomputed daily at 01:00 IST into `member_scores`. Every component runs 0 to 1000.

```
OverallScore = round(0.5 * RatingScore + 0.3 * SolvedScore + 0.2 * ContestScore)

RatingScore  = MAX over platforms where the member has rating > 0 of
               percentile(member rating among club members rated on that platform) * 1000
SolvedScore  = percentile(member ALL_TIME overall solved among all verified members) * 1000
ContestScore = percentile(member overall contests in the last 365 days) * 1000

percentile(x) = (number of members with a value strictly below x)
                / (number of members with any value, minus 1)
                -- defined as 1.0 when only one member has a value
```

**Why MAX for rating:** a member who is strong on one platform is not penalised for
not using the others. **Why percentiles:** a 1600 on Codeforces and a 1600 on
LeetCode mean very different things, and percentiles make them comparable without an
invented conversion table. A member with no platform data gets no score and is left
off the board -- they are not shown with a score of 0.

### 1.3 Club Championship Scoring (default -- D11)

```
points(member, contest) = round(100 * (P - rank + 1) / P)
  P    = number of club members with a result in that contest
  rank = rank among club members only (ties share a rank)
  no result = 0 points

season total = sum of that member's best K contest scores
  K = club_seasons.count_best, or every contest in the season when null
```

Ties on the season total are broken by more contests with points, then by the best
single-contest rank.

### 1.4 Practice Suggestions (default -- D24)

1. **Target band:** round the member's CF rating down to the nearest 100, then take
   +100 to +300, clamped to 800-3500. Unrated members get 800-1000.
2. **Weak tags:** from the core tag list (`dp`, `greedy`, `math`, `graphs`,
   `implementation`, `constructive algorithms`, `binary search`, `sortings`,
   `number theory`, `strings`, `trees`, `dfs and similar`, `brute force`,
   `data structures`, `two pointers`, `bitmasks`, `combinatorics`, `geometry`),
   choose the 3 tags where the member's share of solves falls furthest below the
   club-wide share. With fewer than 30 counted solves, skip this step and use every tag.
3. **Candidates:** unsolved, non-skipped problemset problems in the band carrying a
   weak tag, ordered by Codeforces `solvedCount` descending (well-trodden first).
4. **Result:** 10 suggestions, refreshed daily at 02:00 IST. A skipped problem stays
   hidden for that member permanently.

### 1.5 Weekly Awards (O9)

Computed Monday 00:30 IST for the week that has just closed (Monday 00:00 to
Monday 00:00 IST), after the daily totals run at 00:05. Stored in `weekly_awards`
and **never recomputed automatically** -- once announced, a winner does not change.
An admin can recompute a week through an audited endpoint.

| Award | Metric for the closed week |
|---|---|
| `MOST_SOLVED` | Overall `solved` (Section 1.1) |
| `BIGGEST_RATING_JUMP` | Largest net rating change on a single platform across that week's rated contests, from `contest_participations.new_rating - old_rating`. The platform is stored with the award. |
| `CODER_OF_THE_WEEK` | Weekly points: for each counted CF solve, `max(8, rating / 100)` (unrated counts as 8); LeetCode Easy 5, Medium 12, Hard 25; 20 per contest given; plus `max(0, net rating gain) / 5`. **[DECISION NEEDED]** M6 confirms the weights at kickoff. |

**Rules:** nobody wins with a metric of 0. Members tied for the top value all
receive the award. Only verified members are eligible.

---

## Section 2 -- Implementation Stages & Dependencies

```
STAGE 0 --- Migrations V8-V15, entities, Testcontainers, per-host rate limiters
              Owner: M6 -- must merge before ANY other Phase 3 branch starts
                    |
  ========================= WAVE A -- STATS + STUDENT EMAIL =========================
                    |
        +-----------+-----------------------+-----------------------+
        v                                   v                       v
STAGE 1A - Sync pipeline            STAGE 1B - Stats queries   STAGE 1C - Email + security
  Owner: M6                           Owner: M5                  Owner: M4
  CF submissions, problemset,         leaderboards, profile      domain rule, verification,
  contest history (CF, LC),           stats, compare,            mail service, Phase 3
  LC daily totals                     attendance board           SecurityConfig rules
        |                                   |                       |
        |                     1B builds against fixture rows in Testcontainers,
        |                     so it does not wait for 1A to merge.
        +-----------+-----------------------+-----------------------+
                    v
        +-----------+-----------+
        v           v           v
    STAGE 2A    STAGE 2B    STAGE 2C
    UI (M1)     State (M2)  Dashboards (M3) -- after 2A AND 2B
        |
  ===================== WAVE B -- SCORES, CONTESTS, CALENDAR =====================
        |      (3A/3B/3C start as soon as Stage 1 merges -- in parallel with Stage 2)
        +-----------+-----------------------+-----------------------+
        v                                   v                       v
STAGE 3A - Calendar + sync health   STAGE 3B - Scores + practice  STAGE 3C - Club contests
  Owner: M6                           Owner: M5                    Owner: M4
  external contests, calendar,        overall score, weekly        authorized CF client,
  ICS, sync health                    awards, topics, practice     standings, championship,
                                                                   upsolve
        +-----------+-----------------------+-----------------------+
                    v
    STAGE 4A (M1)   STAGE 4B (M2)   STAGE 4C (M3) -- after 4A AND 4B
        |
  ====================== WAVE C -- BLOG, RSVP, REMINDERS ======================
        |      (5A/5B start as soon as their owners finish Wave B backend)
        +-----------------------+-----------------------+
        v                                               v
STAGE 5A - Blog workflow + comments            STAGE 5B - RSVP, delete, reminders
  Owner: M4                                      Owner: M5
        +-----------------------+-----------------------+
                    v
    STAGE 6A (M1)   STAGE 6B (M2)   STAGE 6C (M3) -- after 6A AND 6B
                    |
                    v
STAGE 7 --- Integration, rollout, backfill runbook, flags on
              Owner: M6
```

### 2.1 Dependency Table

| Stage | Owner | Depends on (merged) | Est. days |
|---|---|---|---|
| 0 | M6 | Phase 2 backend (already merged) | 4 |
| 1A | M6 | 0 | 6 |
| 1B | M5 | 0 | 6 |
| 1C | M4 | 0 | 5 |
| 2A | M1 | 1B, 1C, Phase 2 Stage 2A | 5 |
| 2B | M2 | 1B, 1C | 4 |
| 2C | M3 | 1A, 2A, 2B, Phase 2 Stage 2C | 5 |
| 3A | M6 | 1A | 4 |
| 3B | M5 | 1A, 1B | 6 |
| 3C | M4 | 1A, 1C | 7 |
| 4A | M1 | 2A, 3A, 3B, 3C | 6 |
| 4B | M2 | 2B, 3A, 3B, 3C | 4 |
| 4C | M3 | 2C, 4A, 4B | 6 |
| 5A | M4 | 1C, 3C | 5 |
| 5B | M5 | 1C, 3A, 3B | 5 |
| 6A | M1 | 4A, 5A, 5B | 6 |
| 6B | M2 | 4B, 5A, 5B | 4 |
| 6C | M3 | 4C, 6A, 6B | 4 |
| 7 | M6 | every stage above | 4 |

### 2.2 Timeline (working days, each member one stage at a time)

```
Day:     1    5    10   15   20   25   30   35
M6  |S0--|1A----|3A--|....reviews + merges.....|7---|
M5       |1B----|3B----|5B---|
M4       |1C---|3C-----|5A---|
M1              |2A---|      |4A----|6A----|
M2              |2B--|       |4B--|  |6B--|
M3                   |2C---|        |4C----|6C--|
         |<--- Wave A --->|<- Wave B ->|<- Wave C ->|
```

About 37 working days. Wave A is usable on its own at the end of week 4 (real stats
on every profile and leaderboard), Wave B at week 6, Wave C at week 7, and Stage 7
closes the phase in week 8.

> [!WARNING]
> M6 also reviews and merges every PR. That is the real bottleneck, not any single
> stage. **Rule:** a PR that has waited two working days for review may be approved
> by any other member who did not write it, and M6 merges it without a second pass.

---

## Section 3 -- Stage 0: Migrations, Entities & Test Infrastructure
**Owner: Member 6 (Team Lead)**
**PR: `phase3/stage-0-foundation`**
**Estimated time: 4 days**
**Blocks: All other members**

**Read first:** every file in `backend/src/main/resources/db/migration/`,
`user/entity/User.java`, `common/config/AppConfig.java`,
`snapshot/service/SnapshotService.java`, `codeforces/service/CodeforcesSyncService.java`,
`leetcode/service/LeetCodeSyncService.java`, `resources/application.yml`,
`src/test/resources/application-test.yml`, `pom.xml`.

---

### 3.1 Testcontainers (skip if Phase 2 Stage 3 already landed it)

- Add `org.springframework.boot:spring-boot-testcontainers` and
  `org.testcontainers:postgresql` (test scope; versions come from the Spring Boot BOM).
- Create `src/test/java/com/cpclub/backend/support/PostgresIntegrationTest.java`:
  an abstract `@SpringBootTest` base with a static
  `@Container @ServiceConnection PostgreSQLContainer<?>` on `postgres:15-alpine`,
  and **Flyway enabled**, so every migration runs exactly as it will in production.
- Keep the existing H2 profile for the current fast unit tests. Only classes that
  extend `PostgresIntegrationTest` pay the container start-up cost.
- CI needs no change: `ubuntu-latest` runners have Docker.

### 3.2 Migrations

All eight files go in `backend/src/main/resources/db/migration/`. Every Phase 3
`TIMESTAMP` column holds UTC (D31).

#### `V8__create_codeforces_problems_and_solves.sql`

```sql
-- Phase 3, Stage 0: Codeforces problem metadata, and every problem a member has
-- solved. Stats (Section 1.1), topic strength and practice suggestions all read these.

CREATE TABLE IF NOT EXISTS cf_problems (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY,
    -- "1843/C", or "acmsguru/100" for problems with no contestId. A plain UNIQUE on
    -- (contest_id, problem_index) would not work: PostgreSQL treats NULLs as distinct.
    problem_key     VARCHAR(64)  NOT NULL,
    contest_id      INTEGER,
    problemset_name VARCHAR(50),
    problem_index   VARCHAR(10)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    rating          INTEGER,
    solved_count    INTEGER,     -- from problemset.problems; orders practice suggestions
    PRIMARY KEY (id),
    CONSTRAINT uk_cf_problems_key UNIQUE (problem_key)
);
CREATE INDEX IF NOT EXISTS idx_cf_problems_rating ON cf_problems (rating);

CREATE TABLE IF NOT EXISTS cf_problem_tags (
    problem_id BIGINT      NOT NULL,
    tag        VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag),
    CONSTRAINT fk_cf_problem_tags_problem FOREIGN KEY (problem_id) REFERENCES cf_problems (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cf_problem_tags_tag ON cf_problem_tags (tag);

CREATE TABLE IF NOT EXISTS cf_solves (
    id                     BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id                BIGINT       NOT NULL,
    problem_id             BIGINT       NOT NULL,
    first_ac_at            TIMESTAMP(6) NOT NULL,  -- first accepted submission (D2)
    first_ac_submission_id BIGINT       NOT NULL,
    participant_type       VARCHAR(30)  NOT NULL,  -- CONTESTANT, PRACTICE, VIRTUAL, OUT_OF_COMPETITION, MANAGER
    PRIMARY KEY (id),
    CONSTRAINT uk_cf_solves_user_problem UNIQUE (user_id, problem_id),
    CONSTRAINT fk_cf_solves_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cf_solves_problem FOREIGN KEY (problem_id) REFERENCES cf_problems (id)
);
CREATE INDEX IF NOT EXISTS idx_cf_solves_user_time ON cf_solves (user_id, first_ac_at);
CREATE INDEX IF NOT EXISTS idx_cf_solves_time ON cf_solves (first_ac_at);

-- One row per scheduled or admin-triggered sync run. Backs the Sync Health admin tab
-- (Stage 3A) and is the first place to look when a platform starts failing.
CREATE TABLE IF NOT EXISTS sync_runs (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY,
    job            VARCHAR(40)   NOT NULL,  -- CF_SUBMISSIONS, LC_TOTALS, EXTERNAL_CONTESTS, ...
    started_at     TIMESTAMP(6)  NOT NULL,
    finished_at    TIMESTAMP(6),
    members_ok     INTEGER       NOT NULL DEFAULT 0,
    members_failed INTEGER       NOT NULL DEFAULT 0,
    error_sample   VARCHAR(1000),           -- first failure message; never a full stack trace
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sync_runs_job_started ON sync_runs (job, started_at);
```

#### `V9__create_contest_participations.sql`

```sql
-- Phase 3, Stage 0: every Codeforces and LeetCode contest a member entered (D5).
-- Also the source for the multi-platform rating charts (O10): new_rating per contest
-- is exact, where weekly_snapshots only samples once a week.

CREATE TABLE IF NOT EXISTS contest_participations (
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id             BIGINT       NOT NULL,
    platform            VARCHAR(20)  NOT NULL,
    external_contest_id VARCHAR(64)  NOT NULL,
    contest_name        VARCHAR(255) NOT NULL,
    started_at          TIMESTAMP(6) NOT NULL,
    rated               BOOLEAN      NOT NULL,
    contest_rank        INTEGER,     -- not "rank": that is a PostgreSQL window function name
    old_rating          INTEGER,
    new_rating          INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_contest_participations UNIQUE (user_id, platform, external_contest_id),
    CONSTRAINT fk_contest_participations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT contest_participations_platform_check
        CHECK (platform IN ('CODEFORCES', 'LEETCODE'))  -- CodeChef and AtCoder are link only (D9)
);
CREATE INDEX IF NOT EXISTS idx_contest_participations_platform_time ON contest_participations (platform, started_at);
CREATE INDEX IF NOT EXISTS idx_contest_participations_user_time ON contest_participations (user_id, platform, started_at);
```

#### `V10__create_platform_daily_totals.sql`

```sql
-- Phase 3, Stage 0: daily LeetCode solved-total baselines. LeetCode has no dated
-- solve history, so period counts subtract a baseline from the live total (D6).

CREATE TABLE IF NOT EXISTS platform_daily_totals (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id       BIGINT      NOT NULL,
    platform      VARCHAR(20) NOT NULL,
    captured_on   DATE        NOT NULL,  -- the IST calendar date this total belongs to
    total_solved  INTEGER     NOT NULL,
    easy_solved   INTEGER,
    medium_solved INTEGER,
    hard_solved   INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_platform_daily_totals UNIQUE (user_id, platform, captured_on),
    CONSTRAINT fk_platform_daily_totals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT platform_daily_totals_platform_check CHECK (platform IN ('LEETCODE'))
);
```

#### `V11__extend_users_for_phase3.sql`

```sql
-- Phase 3, Stage 0: LeetCode live totals, sync cursors, email verification and
-- notification preferences. Every new column is nullable or defaulted.

-- Live LeetCode solved totals, refreshed by every sync (D6). Period math subtracts a baseline.
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_total_solved  INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_easy_solved   INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_medium_solved INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_hard_solved   INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS cf_last_submission_id  BIGINT;  -- incremental sync cursor
ALTER TABLE users ADD COLUMN IF NOT EXISTS cf_synced_at           TIMESTAMP(6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_synced_at     TIMESTAMP(6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at      TIMESTAMP(6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_events          BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_contests        BOOLEAN NOT NULL DEFAULT FALSE;

-- D15: every account that exists before this migration was accepted under the
-- Phase 1 rules, and is treated as verified.
UPDATE users SET email_verified_at = created_at WHERE email_verified_at IS NULL;

-- The Phase 2 codechef_url and atcoder_url columns are left untouched: those two
-- platforms stay link only (D9).

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id    BIGINT       NOT NULL,
    token_hash CHAR(64)     NOT NULL,  -- SHA-256 hex. The raw token exists only in the email.
    expires_at TIMESTAMP(6) NOT NULL,
    used_at    TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
```

#### `V12__create_scores_awards_and_practice.sql`

```sql
-- Phase 3, Stage 0: Overall Aggregated Score (D8), weekly awards (Section 1.5),
-- and practice suggestion skips (Section 1.4).

CREATE TABLE IF NOT EXISTS member_scores (
    user_id       BIGINT       NOT NULL,
    overall_score INTEGER      NOT NULL,
    rating_score  INTEGER      NOT NULL,
    solved_score  INTEGER      NOT NULL,
    contest_score INTEGER      NOT NULL,
    computed_at   TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_member_scores_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_member_scores_overall ON member_scores (overall_score DESC);

CREATE TABLE IF NOT EXISTS weekly_awards (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY,
    week_start   DATE         NOT NULL,  -- the Monday (IST) the awarded week began
    award_type   VARCHAR(40)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    metric_value INTEGER      NOT NULL,
    platform     VARCHAR(20),            -- BIGGEST_RATING_JUMP only
    computed_at  TIMESTAMP(6) NOT NULL,
    computed_by  BIGINT,                 -- null = the scheduler; set = an admin recompute (audit)
    PRIMARY KEY (id),
    CONSTRAINT uk_weekly_awards UNIQUE (week_start, award_type, user_id),
    CONSTRAINT fk_weekly_awards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_awards_computed_by FOREIGN KEY (computed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT weekly_awards_type_check
        CHECK (award_type IN ('CODER_OF_THE_WEEK', 'MOST_SOLVED', 'BIGGEST_RATING_JUMP'))
);
CREATE INDEX IF NOT EXISTS idx_weekly_awards_week ON weekly_awards (week_start);

CREATE TABLE IF NOT EXISTS practice_suggestion_skips (
    user_id    BIGINT       NOT NULL,
    problem_id BIGINT       NOT NULL,
    skipped_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, problem_id),
    CONSTRAINT fk_practice_skips_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_practice_skips_problem FOREIGN KEY (problem_id) REFERENCES cf_problems (id) ON DELETE CASCADE
);
```

#### `V13__create_club_contests.sql`

```sql
-- Phase 3, Stage 0: club-hosted Codeforces contests, seasons, results and per-problem
-- outcomes. Powers club ranklists (O13), the Championship (O14) and upsolve (P1).

CREATE TABLE IF NOT EXISTS club_seasons (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
    name       VARCHAR(100) NOT NULL,
    starts_on  DATE         NOT NULL,
    ends_on    DATE         NOT NULL,
    count_best INTEGER,                 -- null = every contest counts (Section 1.3)
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT club_seasons_dates_check CHECK (ends_on >= starts_on),
    CONSTRAINT club_seasons_count_best_check CHECK (count_best IS NULL OR count_best > 0)
);

CREATE TABLE IF NOT EXISTS club_contests (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY,
    cf_contest_id    INTEGER      NOT NULL,
    season_id        BIGINT,
    kind             VARCHAR(20)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    starts_at        TIMESTAMP(6) NOT NULL,
    duration_seconds INTEGER      NOT NULL,
    last_synced_at   TIMESTAMP(6),
    created_by       BIGINT       NOT NULL,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uk_club_contests_cf_id UNIQUE (cf_contest_id),
    CONSTRAINT fk_club_contests_season FOREIGN KEY (season_id) REFERENCES club_seasons (id) ON DELETE SET NULL,
    CONSTRAINT fk_club_contests_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT club_contests_kind_check CHECK (kind IN ('GYM', 'MASHUP', 'GROUP', 'OFFICIAL'))
);

CREATE TABLE IF NOT EXISTS club_contest_problems (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY,
    club_contest_id BIGINT       NOT NULL,
    problem_index   VARCHAR(10)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    rating          INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_contest_problems UNIQUE (club_contest_id, problem_index),
    CONSTRAINT fk_club_contest_problems_contest FOREIGN KEY (club_contest_id) REFERENCES club_contests (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS club_contest_results (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY,
    club_contest_id BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    contest_rank    INTEGER        NOT NULL,  -- rank among club members only
    cf_points       NUMERIC(10, 2) NOT NULL,  -- the Codeforces standings score, not championship points
    penalty         INTEGER        NOT NULL,
    solved_count    INTEGER        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_club_contest_results UNIQUE (club_contest_id, user_id),
    CONSTRAINT fk_club_contest_results_contest FOREIGN KEY (club_contest_id) REFERENCES club_contests (id) ON DELETE CASCADE,
    CONSTRAINT fk_club_contest_results_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS club_contest_problem_results (
    result_id         BIGINT  NOT NULL,
    problem_id        BIGINT  NOT NULL,
    solved_in_contest BOOLEAN NOT NULL,
    PRIMARY KEY (result_id, problem_id),
    CONSTRAINT fk_ccpr_result FOREIGN KEY (result_id) REFERENCES club_contest_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_ccpr_problem FOREIGN KEY (problem_id) REFERENCES club_contest_problems (id) ON DELETE CASCADE
);
```

#### `V14__blog_workflow_and_comments.sql`

```sql
-- Phase 3, Stage 0: member-written posts with review (D18), and comments (D19).

ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS author_id    BIGINT;
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS reviewer_id  BIGINT;
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS review_note  VARCHAR(500);
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP(6);
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS published_at TIMESTAMP(6);

-- Map the legacy boolean. V1 allows published to be NULL, and the public endpoints
-- only ever served "published = true" (findByPublishedTrue). A NULL row is therefore
-- not public today, and must NOT become PUBLISHED here.
UPDATE blog_posts SET status = CASE WHEN published IS TRUE THEN 'PUBLISHED' ELSE 'DRAFT' END;
UPDATE blog_posts SET published_at = created_at WHERE status = 'PUBLISHED' AND published_at IS NULL;

ALTER TABLE blog_posts ADD CONSTRAINT blog_posts_status_check
    CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED'));
ALTER TABLE blog_posts ADD CONSTRAINT fk_blog_posts_author
    FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE blog_posts ADD CONSTRAINT fk_blog_posts_reviewer
    FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_blog_posts_status_published ON blog_posts (status, published_at);

-- The published column is kept, and BlogService keeps it in step with status, so a
-- backend still running the previous release reads correct values during rollout.
-- Drop it in a Phase 4 cleanup migration.

CREATE TABLE IF NOT EXISTS blog_comments (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
    post_id    BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    parent_id  BIGINT,                  -- one level of replies; enforced in the service
    body       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6)  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6),
    deleted_at TIMESTAMP(6),            -- soft delete, rendered as "[deleted]"
    PRIMARY KEY (id),
    CONSTRAINT fk_blog_comments_post FOREIGN KEY (post_id) REFERENCES blog_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_comments_parent FOREIGN KEY (parent_id) REFERENCES blog_comments (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_blog_comments_post_time ON blog_comments (post_id, created_at);
```

#### `V15__events_registrations_calendar_and_reminders.sql`

```sql
-- Phase 3, Stage 0: member RSVPs (D13), external contests for the calendar (D26),
-- and the idempotency log for reminder emails (D29).

ALTER TABLE events ADD COLUMN IF NOT EXISTS capacity INTEGER;
ALTER TABLE events ADD CONSTRAINT events_capacity_check CHECK (capacity IS NULL OR capacity > 0);

CREATE TABLE IF NOT EXISTS event_registrations (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY,
    event_id      BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    registered_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uk_event_registrations UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_event_registrations_event ON event_registrations (event_id);

CREATE TABLE IF NOT EXISTS external_contests (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY,
    platform         VARCHAR(20)  NOT NULL,
    external_id      VARCHAR(100) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    url              VARCHAR(512) NOT NULL,
    starts_at        TIMESTAMP(6) NOT NULL,
    duration_seconds INTEGER      NOT NULL,
    synced_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_external_contests UNIQUE (platform, external_id),
    CONSTRAINT external_contests_platform_check
        CHECK (platform IN ('CODEFORCES', 'LEETCODE', 'CODECHEF', 'ATCODER'))
);
CREATE INDEX IF NOT EXISTS idx_external_contests_start ON external_contests (starts_at);

CREATE TABLE IF NOT EXISTS reminder_log (
    id      BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT       NOT NULL,
    kind    VARCHAR(30)  NOT NULL,
    ref_key VARCHAR(120) NOT NULL,  -- "event:42", or "digest:2026-11-03"
    sent_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uk_reminder_log UNIQUE (user_id, kind, ref_key),
    CONSTRAINT fk_reminder_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT reminder_log_kind_check CHECK (kind IN ('EVENT_24H', 'CONTEST_DIGEST'))
);
```

### 3.3 Enums & Entities

**Enums** (each in the package that owns it):

| Enum | Values | Package |
|---|---|---|
| `Platform` | CODEFORCES, LEETCODE -- the platforms whose member stats are synced | `common/model` |
| `ContestPlatform` | CODEFORCES, LEETCODE, CODECHEF, ATCODER -- calendar contest schedules only (D26) | `calendar/entity` |
| `StatsPlatform` | CODEFORCES, LEETCODE, OVERALL | `stats/dto` |
| `StatsPeriod` | WEEK, MONTH, YEAR, ALL_TIME | `stats/dto` |
| `StatsMetric` | SOLVED, CONTESTS, AVG_PROBLEM_RATING, OVERALL_SCORE, ATTENDANCE | `stats/dto` |
| `AwardType` | CODER_OF_THE_WEEK, MOST_SOLVED, BIGGEST_RATING_JUMP | `score/entity` |
| `ClubContestKind` | GYM, MASHUP, GROUP, OFFICIAL | `contest/entity` |
| `BlogStatus` | DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED | `blog/entity` |
| `ReminderKind` | EVENT_24H, CONTEST_DIGEST | `mail/entity` |
| `SyncJob` | CF_SUBMISSIONS, CF_PROBLEMSET, CF_CONTESTS, LC_CONTESTS, LC_TOTALS, EXTERNAL_CONTESTS, CLUB_CONTEST | `sync/entity` |

**Entities** -- one per table in V8 to V15, with `@Column` names copied exactly from
the SQL above. Enum columns use `@Enumerated(EnumType.STRING)`. Composite keys
(`cf_problem_tags`, `club_contest_problem_results`, `practice_suggestion_skips`) use
an `@Embeddable` id class.

**`User.java` [MODIFY]** -- add all 10 V11 columns. `emailVerifiedAt` gets a helper
`isEmailVerified()` that returns `emailVerifiedAt != null`. `notifyEvents` and
`notifyContests` default to `false` in the builder.

**`BlogPost.java` [MODIFY]** -- add the V14 columns. Keep the `published` field, and
set it from `status` in a `@PrePersist` / `@PreUpdate` hook so the two never disagree.

**`Event.java` [MODIFY]** -- add `capacity`.

### 3.4 Configuration

**`application.yml` [MODIFY]** -- add under the existing `cpclub:` block. Every
platform flag defaults to **off**, so deploying Stage 0 changes no behaviour; Stage 7
turns them on.

```yaml
cpclub:
  scheduling:
    zone: Asia/Kolkata
  codeforces:
    api-key: ${CF_API_KEY:}
    api-secret: ${CF_API_SECRET:}
    submissions-enabled: ${CPCLUB_CF_SUBMISSIONS_ENABLED:false}
  leetcode:
    totals-cron: "0 5 0 * * *"
  external-contests:
    enabled: ${CPCLUB_EXTERNAL_CONTESTS_ENABLED:false}
    clist-api-key: ${CLIST_API_KEY:}
  auth:
    allowed-email-domains: ${CPCLUB_AUTH_ALLOWED_EMAIL_DOMAINS:dau.ac.in}
    require-email-verification: ${CPCLUB_AUTH_REQUIRE_EMAIL_VERIFICATION:false}
  mail:
    enabled: ${CPCLUB_MAIL_ENABLED:false}
    from: ${CPCLUB_MAIL_FROM:no-reply@localhost}
    unsubscribe-secret: ${CPCLUB_UNSUBSCRIBE_SECRET:}
  public-site-url: ${CPCLUB_PUBLIC_SITE_URL:http://localhost:3000}
```

**`AppConfig.java` [MODIFY]** -- one `RateLimiter` bean per host (D23), each
`@Qualifier`-named:

| Bean | Rate | Why |
|---|---|---|
| `codeforcesRateLimiter` | 0.5/s (existing) | Codeforces allows one call every 2 seconds |
| `leetcodeRateLimiter` | 0.5/s | Unofficial endpoint; stay polite |

**`LeetCodeSyncService.java` [MODIFY]** -- inject `leetcodeRateLimiter` in place of
`codeforcesRateLimiter`.

**Existing crons [MODIFY]** -- add `zone = "${cpclub.scheduling.zone:Asia/Kolkata}"` to
the `@Scheduled` annotations in `CodeforcesSyncService` and `SnapshotService` (D1).
The Monday snapshot then fires at 00:00 IST instead of 05:30 IST.

**`backend/.env.example` and `frontend/.env.example` [MODIFY]** -- document every
variable in Section 12.

---

## Section 4 -- Wave A Backend: Stages 1A, 1B, 1C

### Stage 1A -- Sync Pipeline
**Owner: Member 6**
**PR: `phase3/stage-1a-sync`**
**Estimated time: 6 days**
**Depends on: Stage 0**

**Read first:** `codeforces/service/CodeforcesSyncService.java`,
`leetcode/service/LeetCodeSyncService.java`, `common/config/AppConfig.java`,
`user/service/UserService.java`, migrations V8 to V12 (these are Phase 2.5 -- the
schema this stage extends; Phase 3's own migrations start at V13, amendment A1),
Section 1.1, and **amendment A4 before writing any sync code**.

#### 4.1 `sync/` package [NEW]

- `SyncRunRecorder.java` -- `start(SyncJob)` returns a run handle with `ok()`,
  `failed(String message)` and `finish()`. Writes one `sync_runs` row per run.
  Truncates `error_sample` to 1000 characters, and **never throws** -- a broken
  recorder must not break a sync.
- `SyncAdminController.java` -- under the existing `/api/admin/**` ADMIN rule:
  - `POST /api/admin/sync/{job}` -- starts that job asynchronously and returns
    `202` with the `sync_runs` id. This is how the Stage 7 backfill is run.
  - `GET /api/admin/sync/runs?job=&limit=20` -- recent runs, newest first.

#### 4.2 `codeforces/client/CodeforcesApiClient.java` [NEW]

A typed client for the four public methods Phase 3 needs. Every call acquires
`codeforcesRateLimiter` first.

| Method | Codeforces API |
|---|---|
| `userStatus(handle, from, count)` | `user.status`. **Always page** -- `count = 500`. Omitting `from` and `count` pulls a member's entire history into a 160 MB heap; see amendment A4 |
| `userRating(handle)` | `user.rating` |
| `contestList(boolean gym)` | `contest.list` |
| `problemsetProblems()` | `problemset.problems` |

- Unwraps the `{ status, comment, result }` envelope. A `FAILED` status whose
  comment says the handle was not found throws `HandleNotFoundException`: the
  member is skipped and counted as failed, and the run carries on.
- HTTP 5xx or a timeout retries **once**, after a fresh limiter permit, then fails
  that member.
- The existing batched `user.info` code in `CodeforcesSyncService` (with its bisect
  fallback) is **not** rewritten. Only the new services use this client.

#### 4.3 `codeforces/service/CodeforcesProblemsetSyncService.java` [NEW]

- `@Scheduled(cron = "0 0 3 * * *", zone = ...)` -- daily at 03:00 IST, one API call.
- Upserts every `cf_problems` row (`problem_key`, `rating`, `solved_count` from
  `problemStatistics`), and replaces that problem's `cf_problem_tags`.

#### 4.4 `codeforces/service/CodeforcesSubmissionSyncService.java` [NEW]

`syncMember(User user)`:

1. **Backfill** when `cf_last_submission_id` is null: one `user.status` call with no
   `from`/`count`, which returns the member's full history in a single response.
2. **Incremental** otherwise: `user.status?from=1&count=100`. Submissions come newest
   first. Process until reaching an id at or below the cursor. If all 100 are newer,
   fetch `from=101` and continue, up to 50 pages.
3. For every submission with `verdict = OK`:
   - Upsert the `cf_problems` row if it is missing, from `submission.problem`.
     Gym and mashup problems (`contestId >= 100000`) are stored too (D3, D12).
   - Upsert `cf_solves` with PostgreSQL
     `ON CONFLICT (user_id, problem_id) DO UPDATE` keeping the **earliest**
     `first_ac_at` and its submission id. Backfill processes newest first, so the
     earliest accepted submission arrives last and must win.
4. Collect the contest ids of `CONTESTANT` and `OUT_OF_COMPETITION` submissions in
   problemset contests, and hand them to `ContestHistorySyncService` (D5).
5. Set `cf_last_submission_id` to the highest id seen, and `cf_synced_at` to now.

`syncAll()` runs after the existing `syncCodeforcesRatings()`, chained the same way
LeetCode already is, and only when `cpclub.codeforces.submissions-enabled` is true.

**Handle change [MODIFY `UserService.updateProfile`]:** when `codeforcesHandle`
changes, clear `cf_last_submission_id` and delete that member's `cf_solves` and
Codeforces `contest_participations`. The next run backfills the new handle from
scratch instead of mixing two accounts.

#### 4.5 `stats/service/ContestHistorySyncService.java` [NEW]

- **Contest cache:** `CodeforcesContestCache` holds `contest.list?gym=false`
  (id -> name, start time, duration), refreshed daily.
- **Codeforces rated:** `user.rating` -> upsert `contest_participations` with
  `rated = true`, `contest_rank`, `old_rating`, `new_rating`. `started_at` comes from
  the contest cache, falling back to `ratingUpdateTimeSeconds` if the contest is missing.
- **Codeforces unrated:** for each contest id handed over by 4.4 with no row yet,
  insert `rated = false` with no rank or rating.
- **LeetCode:** GraphQL `userContestRankingHistory(username)` -> entries with
  `attended = true` -> upsert with `external_contest_id` = the contest title slug,
  `new_rating` = `rating`, `contest_rank` = `ranking`, and `old_rating` = the previous
  attended entry's rating.
- Runs in the same 6-hourly chain, after the CF and LeetCode rating syncs.
- Only problemset contests (`contestId < 100000`) count as "contests given". Club gym
  and mashup contests are tracked by Stage 3C instead (D3).

#### 4.6 LeetCode solved totals [NEW + MODIFY]

- **`LeetCodeSyncService` [MODIFY]** -- add `matchedUser(username) { submitStats {
  acSubmissionNum { difficulty count } } }` to the **same** GraphQL request as the
  existing `userContestRanking` query, so there is still one call per member. Store
  `All`, `Easy`, `Medium` and `Hard` in the `users.leetcode_*_solved` live columns.
- **`LeetCodeTotalsSnapshotService` [NEW]** -- cron `${cpclub.leetcode.totals-cron}`
  (00:05 IST). For each linked member, fetch a fresh total, update the live columns,
  and `INSERT ... ON CONFLICT DO NOTHING` a `platform_daily_totals` row for today's IST
  date. A second run on the same day keeps the first baseline.

#### 4.7 Stage 1A Tests

Fixtures live in `src/test/resources/fixtures/` -- one real, captured response per
endpoint (see the note in Section 1).

| Test class | Scenario | Expected |
|---|---|---|
| `CodeforcesApiClientTest` | `FAILED` envelope, handle not found | `HandleNotFoundException` |
| | HTTP 503, then 200 | One retry, result returned |
| `CodeforcesSubmissionSyncServiceTest` (Testcontainers) | Backfill fixture with two AC submissions on one problem | One `cf_solves` row, dated by the earlier submission |
| | Incremental run with the cursor set | Stops at the cursor; no duplicate rows |
| | Gym submission accepted | Stored, with `contest_id >= 100000` |
| | Handle changed | Old solves and CF participations deleted, cursor cleared |
| `ContestHistorySyncServiceTest` | Rated contest plus unrated `CONTESTANT` submission plus `VIRTUAL` submission | Two participations; virtual ignored |
| | LeetCode history with `attended = false` entries | Only attended entries stored; `old_rating` chained |
| `LeetCodeTotalsSnapshotServiceTest` | Run twice on the same IST date | One baseline row, from the first run |
| | Clock at Sunday 18:35 UTC (Monday 00:05 IST) | `captured_on` is the Monday |

---

### Stage 1B -- Stats Query Layer
**Owner: Member 5**
**PR: `phase3/stage-1b-stats`**
**Estimated time: 6 days**
**Depends on: Stage 0** -- builds against fixture rows inserted in Testcontainers tests, so it does not wait for 1A

**Read first:** `leaderboard/controller/LeaderboardController.java`,
`leaderboard/service/LeaderboardService.java`, `leaderboard/dto/*`,
`user/repository/UserRepository.java` (`findLeaderboardPage`), migrations V4 and
V8 to V12, Sections 1.1 and D1-D7.

#### 4.8 Files

| File | Action | What it does |
|---|---|---|
| `stats/service/StatsPeriodResolver.java` | NEW | `resolve(StatsPeriod)` returns `{ startUtc, endUtc, startDateIst }`, from D1 and D31. Uses an injected `Clock` bean, so tests pin the time. |
| `common/config/AppConfig.java` | MODIFY | Add `Clock clock()` returning `Clock.system(ZoneId.of(zone))`. |
| `stats/repository/StatsQueryRepository.java` | NEW | Native queries through `NamedParameterJdbcTemplate`. One method per stat in Section 1.1, plus attendance. Leaderboards rank with `RANK() OVER (ORDER BY value DESC)`, so ties share a rank. |
| `stats/service/StatsService.java` | NEW | Validation, assembly, and the business rules below. |
| `stats/controller/StatsController.java` | NEW | `/api/stats/**` endpoints (Section 11). |
| `leaderboard/controller/LeaderboardController.java` | MODIFY | Add `GET /api/leaderboard/stats`, delegating to `StatsService`. The existing `GET /api/leaderboard` is untouched. |
| `stats/dto/*` | NEW | `StatsLeaderboardEntryDto`, `StatsLeaderboardResponseDto`, `MemberStatsDto`, `PlatformStatsDto`, `RatingPointDto`, `CompareDto`, `HeadToHeadDto` |

`NamedParameterJdbcTemplate` rather than a JPA projection: the metric, platform and
period combinations make the SQL dynamic, and the existing
`LeaderboardEntryProjection` already documents how fragile native alias binding is.

#### 4.9 Business Rules

**Leaderboard validation** -- anything else returns `400` with a message naming the
allowed values:

| Metric | Platforms | Periods |
|---|---|---|
| `SOLVED` | `CODEFORCES`, `LEETCODE`, `OVERALL` | all four |
| `CONTESTS` | `CODEFORCES`, `LEETCODE`, `OVERALL` | all four |
| `AVG_PROBLEM_RATING` | `CODEFORCES` only | all four |
| `ATTENDANCE` | `OVERALL` (or omitted) | `MONTH`, `YEAR`, `ALL_TIME` |
| `OVERALL_SCORE` | wired in Stage 3B -- rejected with 400 until then | |

**Who appears on a board:**
- A member with **no value** for the metric is omitted, never listed with 0.
- LeetCode with no baseline: the member is omitted, and the response
  carries `trackingSince` (the earliest `captured_on`) so the UI can explain why.
- When `cpclub.auth.require-email-verification` is on, unverified members are
  excluded (D16). Every pre-Phase-3 account is already verified by V11.
- The existing `filter` (ALL / CORE / BATCH_REP / STUDENTS) applies unchanged.

**Member stats** -- `getMemberStats(userId, period)` returns one `PlatformStatsDto`
per linked platform (`solved`, `contests`, `avgProblemRating` for CF,
`lcDifficulty` for LeetCode, `trackingSince`, `lastSyncedAt`), plus an overall
block with `solved`, `solvedPartial` and `contests`. An unverified member returns
404 unless the caller is that member or an admin.

**Rating history (O10)** -- `getRatingHistory(userId, platform)`: rated
`contest_participations` with a non-null `new_rating`, oldest first. This replaces
the browser-side `user.rating` call on the profile page.

**Compare (P6, D25)** -- `compare(aId, bId)`: both members' ALL_TIME and YEAR stats,
rating history per platform, and a head-to-head over rated Codeforces contests both
entered: `aAhead`, `bAhead`, and the 10 most recent shared contests with both ranks.
No email or phone.

**Attendance (P5, D27)** -- `COUNT(*)` of `event_attendees` joined to `events`
where `status = 'COMPLETED'` and `event_date` is in the period.

#### 4.10 Stage 1B Tests (Testcontainers)

| Test | Scenario | Expected |
|---|---|---|
| `weekBoundary_excludesSundayNightIst` | Solve at Sunday 23:59:59 IST, clock on Tuesday | Not in WEEK |
| `weekBoundary_includesMondayMidnightIst` | Solve at Monday 00:00:00 IST | In WEEK |
| `cfSolved_excludesGymProblems` | One problemset and one gym solve | `solved = 1` |
| `avgRating_nullBelowTenRatedSolves` | 9 rated solves plus 5 unrated | Null; member absent from the AVG board |
| `lcSolved_liveMinusBaseline` | Baseline 100 on Monday, live total 112 | WEEK `solved = 12` |
| `lcSolved_noBaseline` | No row on or before the period start | Null; response carries `trackingSince` |
| `leaderboard_tiesShareRank` | Two members on 7 solves | Both rank 1; the next member is rank 3 |
| `leaderboard_omitsMembersWithoutData` | Member with no CF handle | Not in the list |
| `leaderboard_clubFilterApplies` | `filter=CORE` | Only CORE and ASSOCIATE_CORE members |
| `leaderboard_rejectsInvalidCombination` | `AVG_PROBLEM_RATING` with `LEETCODE` | 400 |
| `attendance_countsCompletedEventsOnly` | Attendee rows on a CANCELLED and a COMPLETED event | Count = 1 |
| `compare_headToHead` | Three shared rated contests, A ahead in two | `aAhead = 2`, `bAhead = 1` |

---

### Stage 1C -- Student Email, Verification & Phase 3 Security Rules
**Owner: Member 4**
**PR: `phase3/stage-1c-email-security`**
**Estimated time: 5 days**
**Depends on: Stage 0**

**Read first:** `security/config/SecurityConfig.java`, `auth/controller/AuthController.java`,
`auth/service/AuthService.java`, `auth/dto/RegisterRequest.java`,
`security/UserEndpointAuthorizationTest.java`, Section 1 D15, D16 and D30.

#### 4.11 Files

| File | Action | What it does |
|---|---|---|
| `pom.xml` | MODIFY | Add `spring-boot-starter-mail`. |
| `mail/service/MailService.java` | NEW | `send(to, subject, text, html)`, run `@Async` on a small executor. When `cpclub.mail.enabled` is false it logs the subject only -- **never the body**, which can hold a token -- and returns. Send failures are logged and never reach the request. |
| `mail/service/MailTemplates.java` | NEW | Java text blocks for the verification, event-reminder and contest-digest emails. No template engine dependency. |
| `auth/service/EmailDomainPolicy.java` | NEW | Trims and lowercases the address, then requires an exact domain match against `cpclub.auth.allowed-email-domains`. A subdomain such as `student.dau.ac.in` must be listed on its own. |
| `auth/service/EmailVerificationService.java` | NEW | `issue(User)` makes 32 `SecureRandom` bytes, base64url-encodes them into the link, and stores only the SHA-256 hex. Earlier unused tokens for that member are marked used. `verify(token)` checks hash, expiry and single use, then sets `email_verified_at`. **Every failure returns the same message** ("This link is invalid or has expired"), so the endpoint reveals nothing to someone probing tokens. |
| `auth/service/VerifiedMemberGuard.java` | NEW | `require(User)` throws 403 with code `EMAIL_NOT_VERIFIED` when verification is required and missing. Used by Stages 5A and 5B. |
| `auth/service/AuthService.java` | MODIFY | `register`: apply the domain policy (400 with the allowed domains in the message), save with `email_verified_at = null`, then issue a token. |
| `auth/controller/AuthController.java` | MODIFY | `POST /api/auth/verify-email` (public). `POST /api/auth/resend-verification` with `@PreAuthorize("isAuthenticated()")`, because `/api/auth/**` is permitAll at URL level. Resend is limited to 1 per 60 seconds and 5 per day per member (in memory, D30), returning 429 with `Retry-After`. |
| `auth/dto/AuthResponse.java`, `user/dto/UserResponseDto.java` | MODIFY | Add `emailVerified`. |
| `security/config/SecurityConfig.java` | MODIFY | Every Phase 3 URL rule, in one reviewed change -- see 4.12. |

`cpclub.auth.require-email-verification` defaults to **false**, so this stage can
merge before an SMTP provider exists. Stage 7 turns it on.

#### 4.12 SecurityConfig -- Phase 3 Rules

Rules for routes that later stages build are added **now**, in one place. A rule for
a route that does not exist yet only yields a 404, and it keeps the ordering reviewed
once instead of six times.

**Order matters. Insert these blocks exactly where marked:**

```java
// ---- BEFORE the existing GET "/api/blogs/**" permitAll ----
// That permitAll would otherwise make these two readable by anyone.
.requestMatchers(HttpMethod.GET, "/api/blogs/mine").authenticated()
.requestMatchers(HttpMethod.GET, "/api/blogs/review-queue").hasRole("ADMIN")

// ---- BEFORE the existing "/api/events", "/api/events/**" ADMIN wildcard ----
// Without these, the wildcard swallows member RSVPs and the public calendar file.
.requestMatchers(HttpMethod.GET, "/api/events/{id:[0-9]+}/ics").permitAll()
.requestMatchers("/api/events/{id:[0-9]+}/registration").authenticated()

// ---- Phase 3 public reads (anywhere before anyRequest) ----
.requestMatchers(HttpMethod.GET,
        "/api/stats/**", "/api/awards/**",
        "/api/club-contests", "/api/club-contests/**",
        "/api/seasons", "/api/seasons/**",
        "/api/calendar", "/api/calendar/**",
        "/api/notifications/unsubscribe").permitAll()
// One-click unsubscribe from mail clients arrives as a POST (RFC 8058).
.requestMatchers(HttpMethod.POST, "/api/notifications/unsubscribe").permitAll()

// ---- Phase 3 member routes ----
.requestMatchers("/api/practice/**").authenticated()
.requestMatchers(HttpMethod.POST, "/api/blogs", "/api/blogs/{id:[0-9]+}/submit",
        "/api/blogs/{id:[0-9]+}/comments").authenticated()
.requestMatchers(HttpMethod.PUT, "/api/blogs/{id:[0-9]+}").authenticated()
.requestMatchers("/api/comments/**").authenticated()

// ---- Phase 3 admin writes ----
.requestMatchers("/api/club-contests/**", "/api/seasons/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.POST, "/api/blogs/{id:[0-9]+}/publish",
        "/api/blogs/{id:[0-9]+}/reject").hasRole("ADMIN")
```

`PUT /api/blogs/{id}` and `DELETE /api/blogs/{id}` stay method-secured: the author
may edit their own DRAFT or REJECTED post, and an admin may edit any post. The URL
layer cannot know who wrote a post, so the check lives in `BlogService` (Stage 5A).

#### 4.13 Stage 1C Tests

| Test | Scenario | Expected |
|---|---|---|
| `EmailDomainPolicyTest` | `" Name@DAU.ac.in "` | Accepted |
| | `name@student.dau.ac.in` with only `dau.ac.in` configured | Rejected |
| | Two domains configured | Both accepted |
| `EmailVerificationServiceTest` | Issue a token | DB holds the SHA-256 hex, never the raw token |
| | Expired, reused, or unknown token | Same 400 message for all three |
| | Issue twice | The first token no longer verifies |
| `AuthServiceTest` | Register with `gmail.com` | 400; no user saved |
| `ResendVerificationTest` | Two resends within 60 seconds | Second returns 429 with `Retry-After` |
| `MailServiceTest` | Mail disabled, verification email | Log line holds the subject, and does not contain the token |
| `Phase3SecurityOrderingTest` | USER `POST /api/events/1/registration` | **Not 403** (404 until Stage 5B) -- proves the ADMIN wildcard did not swallow it |
| | Anonymous `GET /api/events/1/ics` | **Not 401** |
| | Anonymous `GET /api/blogs/mine` | 401 |
| | USER `GET /api/blogs/review-queue` | 403 |
| | Anonymous `GET /api/stats/users/1` | **Not 401** |

---

## Section 5 -- Wave A Frontend: Stages 2A, 2B, 2C

### Stage 2A -- Stats UI Components
**Owner: Member 1**
**PR: `phase3/stage-2a-stats-ui`**
**Estimated time: 5 days**
**Depends on: 1B, 1C, Phase 2 Stage 2A**

**Read first:** `app/globals.css`, `components/site/primitives.tsx`,
`components/site/rating-graph.tsx`, `components/site/filter-chips.tsx`,
`components/site/platform-mark.tsx`, `components/site/leaderboard-dashboard.tsx`,
Phase 2's `components/ui/data-table.tsx` and `components/ui/club-role-badge.tsx`.

| File | Action | What it does |
|---|---|---|
| `components/site/segmented-toggle.tsx` | NEW | Generic single-select pill group with real radio semantics (`role="radiogroup"`, arrow-key navigation, one tab stop). **Reuse `filter-chips.tsx` instead if it already provides this.** |
| `components/site/period-toggle.tsx` | NEW | This week / This month / This year / All time. |
| `components/site/platform-toggle.tsx` | NEW | Codeforces / LeetCode / Overall, using `PlatformMark`. CodeChef and AtCoder are link only (D9), so they never appear here. An `enabled` prop disables options with an explanation (for example, average rating is Codeforces only). |
| `components/site/stat-tile.tsx` | NEW | Value, label and basis line ("over 42 rated problems"). States: loading skeleton; **null** -> an explained empty state ("Tracking since 3 Nov", "Handle not linked"); partial -> a small "partial" note. Never shows 0 for missing data. |
| `components/site/stats-leaderboard-table.tsx` | NEW | Built on `DataTable`. Columns: rank (ties shown as "=3"), member (avatar, `RankDot`, name), club role, value, basis. Highlights the signed-in member's row. The empty state is specific to the metric and period. |
| `components/site/rating-graph.tsx` | MODIFY | Accept several series (`{ platform, points }[]`). Codeforces keeps its rank-coloured dots; other platforms draw one line in their platform colour; add a legend. Still plain SVG (D20). |
| `components/site/compare-view.tsx` | NEW | Two columns of stat rows. The leader in each row is marked with text and an arrow, not colour alone. Overlaid rating graph and a head-to-head summary. |
| `app/compare/page.tsx` | NEW | Page shell with two member pickers. M3 wires the data in 2C. |

**Accessibility, for every chart:** `role="img"` with an `aria-label` summarising the
data, plus a visually hidden table holding the same numbers.

---

### Stage 2B -- Stats Services, Verification Flow & Mock Removal
**Owner: Member 2**
**PR: `phase3/stage-2b-stats-state`**
**Estimated time: 4 days**
**Depends on: 1B, 1C**

**Read first:** `types/api.ts`, `store/auth.ts`, `lib/axios.ts`,
`lib/services/leaderboard.ts`, `lib/services/dashboard.ts`,
`app/(auth)/register/register-form.tsx`, `app/(auth)/login/login-form.tsx`.

| File | Action | What it changes |
|---|---|---|
| `types/api.ts` | MODIFY | Add the `StatsPlatform`, `StatsPeriod` and `StatsMetric` unions, plus `StatsLeaderboardEntry`, `StatsLeaderboardResponse`, `MemberStats`, `PlatformStats`, `RatingHistoryPoint`, `CompareResult`, `HeadToHead`. Add `emailVerified` to `User`. |
| `lib/services/stats.ts` | NEW | `getLeaderboard({ metric, platform, period, filter, page, size })`, `getMemberStats(userId, period)`, `getRatingHistory(userId, platform)`, `compare(a, b)`. |
| `store/auth.ts` | MODIFY | Add `emailVerified`, and a `refreshUser()` that re-fetches the profile after verification. |
| `app/(auth)/register/register-form.tsx` | MODIFY | zod check on the email domain, from `NEXT_PUBLIC_ALLOWED_EMAIL_DOMAINS` (default `dau.ac.in`). The server stays the source of truth: show its 400 message verbatim. After a successful signup, show a "Check your inbox" state instead of redirecting. |
| `app/(auth)/verify-email/page.tsx` | NEW | Reads `token` from the URL and calls verify. Success: refresh the user and link onward. Failure: the generic message, plus a Resend button when signed in. |
| `components/site/verify-email-banner.tsx` | NEW | For signed-in, unverified members, only when `NEXT_PUBLIC_REQUIRE_EMAIL_VERIFICATION=true`. The Resend button honours `Retry-After` from a 429. |
| `lib/services/dashboard.ts` | MODIFY | **D28:** remove every `IS_MOCK` branch and the mock imports. |
| `lib/content/mock-dashboards.ts` | DELETE | D28. |
| `lib/content/members.ts` | MODIFY -> DELETE | Move the real `credits` export to `lib/content/credits.ts`, update its import in `app/(dashboard)/members/page.tsx`, then delete the file with its invented member stats. |

---

### Stage 2C -- Profile Stats, Stats Leaderboards & Compare
**Owner: Member 3**
**PR: `phase3/stage-2c-stats-dashboards`**
**Estimated time: 5 days**
**Depends on: 1A, 2A, 2B, Phase 2 Stage 2C**

**Read first:** `components/site/profile-dashboard.tsx`,
`components/site/leaderboard-dashboard.tsx`, `app/(dashboard)/leaderboard/page.tsx`,
`app/compare/page.tsx` (from 2A).

#### Profile -- `components/site/profile-dashboard.tsx` [MODIFY]

- **Delete the fake "247 Total Solved" card** and its lock overlay (R2).
- Add a stats section with `PeriodToggle`: one `StatTile` row per linked platform
  (Solved, Contests), the Codeforces average problem rating with its basis (R5), the
  LeetCode Easy / Medium / Hard split, and an Overall row with its partial note.
- Replace the browser-side `cfHistory.length` contest count with the backend value (R1).
- Feed `RatingGraph` from `GET /api/stats/users/{id}/rating-history` for every linked
  platform (O10). Once nothing imports `app/next-api/cf/user-rating/route.ts`, delete it.
- On another member's profile, a signed-in member sees "Compare with me" linking to
  `/compare?a={me}&b={them}`.

#### Leaderboard -- `leaderboard-dashboard.tsx` and `leaderboard/page.tsx` [MODIFY]

- Metric tabs: **Rating** (existing), **Problems solved**, **Contests given**,
  **Avg problem rating**, **Attendance**.
- `PlatformToggle` and `PeriodToggle` drive `GET /api/leaderboard/stats`. Controls
  that do not apply to the selected metric are disabled with an explanation, not hidden.
- Mirror the state in the URL (`?metric=SOLVED&platform=OVERALL&period=WEEK`) so a
  board can be shared. The requested views are all combinations of these controls:
  "top solvers this month on LeetCode" is `metric=SOLVED&platform=LEETCODE&period=MONTH`.
- Pass the club filter to the server if Phase 2 still filters it in the browser.
- A "Compare" row action for signed-in members.

#### Compare -- `app/compare/page.tsx` [MODIFY]

Wire both pickers to member search and `statsService.compare`. Keep the selected
members in the URL.

**Empty states, never zeros:** "No solves recorded this week yet". "LeetCode tracking
started 3 Nov". "Needs 10 rated solves".

---

## Section 6 -- Wave B Backend: Stages 3A, 3B, 3C

### Stage 3A -- External Contests, Calendar & Sync Health
**Owner: Member 6**
**PR: `phase3/stage-3a-calendar`**
**Estimated time: 4 days**
**Depends on: 1A**

**Read first:** the `sync/` package and `CodeforcesApiClient` from 1A, migration V15,
Section 1 D9 and D26.

#### 6.1 CodeChef and AtCoder -- Link Only [NO CHANGE]

No sync, no handles, no stats (D9). The Phase 2 `codechef_url` and `atcoder_url`
profile links stay exactly as they are. The only place either platform appears in
Phase 3 is the public contest schedule on the calendar, below.

#### 6.2 `calendar/` package [NEW]

- **`pom.xml`** -- add `org.jsoup:jsoup`, pinned to a current 1.x release (it is not
  in the Spring Boot BOM). Only `AtCoderContestSource` uses it.

**Sources** -- `ExternalContestSource { ContestPlatform platform(); List<ExternalContestDto> fetchUpcoming(); }`

| Implementation | Source |
|---|---|
| `CodeforcesContestSource` | `contest.list?gym=false`, `phase = BEFORE`, through `CodeforcesApiClient` |
| `LeetCodeContestSource` | GraphQL `upcomingContests { title titleSlug startTime duration }` |
| `AtCoderContestSource` | jsoup on the upcoming-contests table at `https://atcoder.jp/contests/` |
| `CodeChefContestSource` | The contest-list JSON the CodeChef site itself loads -- confirm the exact URL while capturing the fixture |
| `ClistContestSource` | **Fallback only** (D26). When `cpclub.external-contests.clist-api-key` is set, it replaces the AtCoder and CodeChef sources. |

**`ExternalContestSyncService.java`** -- every 6 hours, only when
`cpclub.external-contests.enabled` is true. Each source runs in its own try/catch
with its own `sync_runs` row, then:

- Upserts on `(platform, external_id)`.
- Deletes a future contest only when a **successful** fetch from its own source no
  longer lists it (cancelled or rescheduled). **A failing source never removes
  anything** -- one broken scraper must not empty the calendar.
- Deletes rows whose start was more than 7 days ago.

**`CalendarService.java`** -- `getRange(from, to, platforms)` merges club events
(UPCOMING and COMPLETED, never CANCELLED) with external contests. A range longer
than 62 days returns 400.

**`IcsWriter.java`** -- RFC 5545 output:
- CRLF line endings, with lines folded at 75 octets
- Commas, semicolons, backslashes and newlines escaped in TEXT values
- `DTSTART` and `DTEND` in UTC `Z` form
- A stable `UID` per item (`event-42@<site host>`, `codeforces-1843@<site host>`)
- `PRODID:-//Programming Club DAU//Calendar//EN`

**`CalendarController.java`**

| Method | Path | Returns |
|---|---|---|
| GET | `/api/calendar?from=&to=&platforms=` | JSON items for the calendar page |
| GET | `/api/calendar/club.ics?include=events,contests&platforms=` | `text/calendar` subscription feed, `Cache-Control: public, max-age=900` |
| GET | `/api/events/{id}/ics` | A single `VEVENT` download for one club event |

#### 6.3 Sync Health [NEW]

`GET /api/admin/sync/health` -- for every `SyncJob`: the last run's start and finish,
members ok and failed, the last **successful** run, and `stale = true` when no run
has succeeded within twice the job's schedule interval. Stage 4C renders it.

#### 6.4 Stage 3A Tests

| Test | Scenario | Expected |
|---|---|---|
| `ExternalContestSyncServiceTest` | AtCoder source throws | Existing AtCoder rows kept |
| | Successful Codeforces fetch no longer lists a future contest | That row deleted |
| `IcsWriterTest` | Summary of 120 characters containing a comma and a newline | Folded at 75 octets, escaped, CRLF line endings |
| `CalendarControllerTest` | A 90-day range | 400 |
| | A CANCELLED club event in range | Not returned |
| `SyncHealthServiceTest` | Last success three intervals ago | `stale = true` |

---

### Stage 3B -- Overall Score, Weekly Awards, Topic Strength & Practice
**Owner: Member 5**
**PR: `phase3/stage-3b-scores-practice`**
**Estimated time: 6 days**
**Depends on: 1A, 1B**

**Read first:** the `stats/` package from 1B, migrations V8 and V12,
Sections 1.2, 1.4 and 1.5.

#### 6.5 Files

| File | Action | What it does |
|---|---|---|
| `score/service/MemberScoreService.java` | NEW | Cron 01:00 IST. Computes Section 1.2 in SQL with `PERCENT_RANK()`, which equals the plan's percentile definition exactly, plus a `CASE` returning 1.0 when only one member has a value. Refreshes `member_scores` by delete-and-insert inside **one transaction**, so the board never shows a half-computed mix. |
| `stats/service/StatsService.java` | MODIFY | Wire `OVERALL_SCORE` (platform `OVERALL`, period `ALL_TIME`), with `computedAt` in the response. Add an `overallScore` block to `MemberStatsDto` carrying the score and its three components. |
| `score/service/WeeklyAwardService.java` | NEW | Cron Monday 00:30 IST. `computeWeek(LocalDate weekStart, Long adminId)` -- see 6.6. |
| `score/controller/AwardController.java` | NEW | Award endpoints (Section 11). |
| `practice/service/TopicStrengthService.java` | NEW | Per core tag (Section 1.4) plus an "other" bucket: the member's solves, their share, the club-wide share, and the gap. Counts follow D3. A problem with several tags counts toward each one -- stated in the response. |
| `practice/service/PracticeSuggestionService.java` | NEW | Section 1.4. Computed on request, then cached per member per IST date in memory (D30), so the same 10 suggestions show all day. Ordered by `solved_count` descending, then `problem_key`. A skip clears that member's cache entry. |
| `practice/controller/PracticeController.java` | NEW | `GET /api/practice/suggestions` and `POST /api/practice/suggestions/{problemId}/skip` (204). |
| `stats/controller/StatsController.java` | MODIFY | `GET /api/stats/users/{id}/topics`. |

A member with no Codeforces handle gets `200` with `reason: "NO_CODEFORCES_HANDLE"` and
an empty list, so the page can say why -- not a 404.

#### 6.6 Weekly Award Computation

- **The closed week** is Monday 00:00 IST, seven days back, to this Monday 00:00 IST,
  converted to UTC (D31).
- **LeetCode for a closed week** = this Monday's baseline - last
  Monday's baseline, for both totals and the difficulty columns. When either baseline
  is missing, that platform is left out for that member, and the award row is still
  written from the platforms that do have data.
- **Codeforces** uses its dated solves directly.
- **`BIGGEST_RATING_JUMP`** = for each member and platform, `SUM(new_rating - old_rating)`
  over rated participations in the week with a non-null `old_rating`. The member's best
  platform is compared across members.
- **Idempotent:** when rows already exist for `(week_start, award_type)`, the scheduled
  run skips that award. An admin recompute deletes and re-inserts it inside one
  transaction and sets `computed_by`.
- **`GET /api/awards/weekly/latest`** returns `{ weekStart, computed, awards[] }`.
  `computed = false` means the job has not run yet. `computed = true` with an empty
  list means there were no winners. The homepage words those two cases differently.
- **Recompute** rejects the current or a future week with 400.

#### 6.7 Stage 3B Tests (Testcontainers)

| Test | Scenario | Expected |
|---|---|---|
| `MemberScoreServiceTest` | Three members with equal solved totals | Equal `solved_score` |
| | Only one member has a LeetCode rating | That component = 1000 |
| | Strong on CF only, versus average on two platforms | Rating score uses MAX, not mean |
| | Member with no platform data | No `member_scores` row |
| `WeeklyAwardServiceTest` | Solve at Monday 00:00:00 IST | Belongs to the new week, not the closed one |
| | Every metric is 0 | No award rows |
| | Two members tied on most solved | Both awarded |
| | Scheduled run executes twice | No duplicate rows |
| | Admin recompute | Rows replaced; `computed_by` set |
| `AwardControllerTest` | Recompute the current week | 400 |
| | USER calls recompute | 403 |
| `TopicStrengthServiceTest` | A problem tagged `dp` and `greedy` | Counts toward both |
| | Gym solve tagged `dp` | Not counted (D3) |
| `PracticeSuggestionServiceTest` | Unrated member | Band 800-1000 |
| | Member with 12 counted solves | Every tag used, no weak-tag filter |
| | A solved problem and a skipped problem in the band | Neither suggested |
| | Called twice on one IST date | Identical list |
| | No Codeforces handle | `reason = NO_CODEFORCES_HANDLE` |

---

### Stage 3C -- Club Contests, Championship & Upsolve
**Owner: Member 4**
**PR: `phase3/stage-3c-club-contests`**
**Estimated time: 7 days**
**Depends on: 1A, 1C**

**Read first:** `CodeforcesApiClient` from 1A, migrations V8 and V13,
Section 1 D10-D12 and Section 1.3, and the SecurityConfig block from 1C.

#### 6.8 `contest/client/CodeforcesAuthorizedClient.java` [NEW]

Signs Codeforces API requests (D10):

```
params  += apiKey=<CF_API_KEY>, time=<unix seconds>
rand     = 6 random alphanumeric characters
sigInput = rand + "/" + methodName + "?" + sortedParams + "#" + <CF_API_SECRET>
             sortedParams = every "name=value" pair, sorted by name then value,
                            joined with "&", values NOT url-encoded here
apiSig   = rand + sha512Hex(sigInput)
```

- Exposes `contestStandings(contestId, handles, showUnofficial)`, and shares
  `codeforcesRateLimiter`, since it is the same host.
- **The secret, the key and `apiSig` never reach a log.** Any logged request URL has
  those parameters stripped.
- When `CF_API_KEY` or `CF_API_SECRET` is unset, only `OFFICIAL` contests can be
  added. Creating `GROUP`, `MASHUP` or `GYM` returns 400 ("Codeforces API key not
  configured").

#### 6.9 Services

| File | What it does |
|---|---|
| `contest/service/ClubContestService.java` | `create(cfContestId, seasonId, kind, adminEmail)` fetches the standings once, to validate the contest and read its name, start, duration and problem list, then stores the contest and its problems. Also `delete`, `list` and `getRanklist`. |
| `contest/service/ClubContestSyncService.java` | `sync(clubContestId)` requests standings filtered to every club member's Codeforces handle (the `handles` parameter). It keeps `CONTESTANT` and `OUT_OF_COMPETITION` rows, matching handles case-insensitively, then recomputes rank among club members only (ties share). Results and per-problem outcomes are replaced inside one transaction, so a resync is idempotent. |
| `contest/service/ChampionshipService.java` | `getStandings(seasonId)` computes Section 1.3 on read. Rows: rank, member, total, contests with points, best rank, and a per-contest breakdown. |
| `contest/service/UpsolveService.java` | See 6.10. |
| `contest/controller/ClubContestController.java` | Club contest endpoints (Section 11). |
| `contest/controller/SeasonController.java` | Season endpoints (Section 11). Deleting a season leaves its contests in place with `season_id = NULL`. |

**Sync schedule** -- one cron every 15 minutes picks the contests that need it:
- every run while `starts_at <= now <= end + 30 minutes`
- once at end + 2 hours
- then once a day for 3 days, to catch rejudges

An admin can also force a sync with `POST /api/club-contests/{id}/sync`.

#### 6.10 Upsolve (P1, D12)

- **`getContestUpsolve(clubContestId)`** returns a matrix of participating members
  against problems. Each cell is one of:
  - `SOLVED_IN_CONTEST` -- from `club_contest_problem_results`
  - `UPSOLVED` -- a `cf_solves` row whose problem has `contest_id` = the club
    contest's `cf_contest_id` and the same `problem_index`, with `first_ac_at` at or
    after the contest's end
  - `UNSOLVED`

  Plus a club-wide upsolve rate per problem.
- **`getMyUpsolves(userId)`** returns, across every club contest the member entered,
  their pending problems, with links: `https://codeforces.com/contest/{id}/problem/{index}`
  for a contest, or `https://codeforces.com/gym/{id}/problem/{index}` for a gym.
- Upsolve status follows the 6-hourly submission sync, so it can lag by up to 6 hours.
  The response carries the member's `cf_synced_at`.

**Routing detail:** map the detail route as `@GetMapping("/{id:[0-9]+}")` so it can
never capture `/upsolve/me`. The 1C URL rules make every GET under
`/api/club-contests/**` public, so **`GET /api/club-contests/upsolve/me` must carry
`@PreAuthorize("isAuthenticated()")`**.

#### 6.11 Stage 3C Tests

| Test | Scenario | Expected |
|---|---|---|
| `CodeforcesAuthorizedClientTest` | Fixed `rand`, `time`, key, secret and params | `apiSig` equals a value computed independently in the test |
| | Params given out of order | Signature input sorted by name, then value |
| | A request is logged | No key, secret or `apiSig` in the log output |
| `ClubContestServiceTest` | Create a `MASHUP` with no API key configured | 400 |
| `ClubContestSyncServiceTest` (Testcontainers) | Standings fixture with PRACTICE rows | PRACTICE ignored |
| | Two club members on equal points and penalty | Both on the same club rank |
| | Sync run twice | Identical rows |
| | Handle case differs from the stored handle | Still matched |
| `ChampionshipServiceTest` | Default formula, five contests, `count_best = 3` | Best three summed; ties broken as in Section 1.3 |
| `UpsolveServiceTest` | AC inside the contest, before it ends | Not an upsolve |
| | AC inside the contest, after it ends | `UPSOLVED` |
| | AC on the original problem in another contest | `UNSOLVED` (D12) |
| | Gym contest | Link uses `/gym/` |
| `ClubContestAuthorizationTest` | USER `POST /api/club-contests` | 403 |
| | Anonymous `GET /api/club-contests/1` | Not 401 |
| | Anonymous `GET /api/club-contests/upsolve/me` | 401 |

---

## Section 7 -- Wave B Frontend: Stages 4A, 4B, 4C

### Stage 4A -- Contests, Championship, Upsolve, Practice, Awards & Calendar UI
**Owner: Member 1**
**PR: `phase3/stage-4a-contests-calendar-ui`**
**Estimated time: 6 days**
**Depends on: 2A, 3A, 3B, 3C**

**Read first:** the components from 2A, Phase 2's `DataTable` and the events pages,
`components/site/primitives.tsx`, `lib/site.ts`.

| File | Action | What it does |
|---|---|---|
| `components/site/ranklist-table.tsx` | NEW | Club ranklist on `DataTable`: club rank, member, Codeforces points and penalty, and one cell per problem. Solved and unsolved cells carry text for screen readers, not colour alone. |
| `components/site/upsolve-matrix.tsx` | NEW | Members against problems. The three states differ by icon **and** label. Per-problem upsolve rate as a footer row. Sticky first column inside a horizontal scroll container, so it works at 400px. |
| `components/site/championship-table.tsx` | NEW | Season standings, with an expandable per-contest breakdown. |
| `components/site/topic-strength-chart.tsx` | NEW | Horizontal SVG bars: the member's share against the club share per tag, sorted by gap. **Bars, not a radar chart** -- eighteen radar axes are unreadable, and a radar hides exactly the comparison this chart exists to show. |
| `components/site/award-badge.tsx` | NEW | One badge per award type, with the week label and accessible text. |
| `components/site/weekly-highlights.tsx` | NEW | Homepage section with three award cards. Three states: not computed yet ("First highlights on Monday 10 Nov"), computed with no winners ("No awards last week"), and winners -- tied winners all shown. |
| `components/site/add-to-calendar.tsx` | NEW | "Add to Google Calendar" through the template URL (`https://calendar.google.com/calendar/render?action=TEMPLATE&text=...&dates=<start>/<end>&details=...&location=...`, UTC `YYYYMMDDTHHMMSSZ`), plus "Download .ics" linking to the backend. |
| `app/contests/page.tsx` | NEW | Club contests grouped by season, with a season switcher and an empty state. |
| `app/contests/[id]/page.tsx` | NEW | Ranklist and Upsolve tabs, "Open on Codeforces", last synced time. |
| `app/contests/seasons/[id]/page.tsx` | NEW | Championship standings. |
| `app/practice/page.tsx` | NEW | Signed-in only. "Suggested for you" -- 10 cards with name, rating, tags, a Codeforces link and Skip -- with a no-handle empty state linking to profile edit. "Your pending upsolves", grouped by contest. |
| `app/calendar/page.tsx` | NEW | Month grid on wide screens, an agenda list below 640px. Club events and external contests look distinct, with a legend. Platform filter. "Subscribe" copies the ICS URL and offers a `webcal://` link. Every item has `AddToCalendar`. |
| `lib/site.ts` | MODIFY | Add **Contests** to the main nav only -- it already holds 8 links. Calendar is reached from the Events page header and the footer. Practice is reached from the profile and Contests pages. Compare is reached from profiles and the leaderboard. |

---

### Stage 4B -- Wave B Services & State
**Owner: Member 2**
**PR: `phase3/stage-4b-contests-state`**
**Estimated time: 4 days**
**Depends on: 2B, 3A, 3B, 3C**

**Read first:** `types/api.ts`, the services from 2B, Section 11.

| File | Action | What it changes |
|---|---|---|
| `types/api.ts` | MODIFY | Add `ClubContest`, `ClubContestDetail`, `RanklistRow`, `UpsolveMatrix`, `MyUpsolves`, `Season`, `ChampionshipRow`, `PracticeSuggestions`, `TopicStrength`, `WeeklyAwards`, `UserAward`, `OverallScore`, `ExternalContest`, `SyncHealth`, and `CalendarItem` as a discriminated union on `kind: "EVENT" \| "CONTEST"`. |
| `lib/services/contests.ts` | NEW | `list`, `get`, `create`, `sync`, `remove`, `getUpsolve`, `getMyUpsolves`. |
| `lib/services/seasons.ts` | NEW | `list`, `create`, `update`, `remove`, `getChampionship`. |
| `lib/services/practice.ts` | NEW | `getSuggestions`, and `skip` -- optimistic removal, rolled back on error. |
| `lib/services/awards.ts` | NEW | `getLatest`, `getWeek`, `getForUser`, `recompute`. |
| `lib/services/calendar.ts` | NEW | `getRange(from, to, platforms)`, plus `clubIcsUrl(params)` and `eventIcsUrl(id)`. The URL builders use the axios `baseURL`, so the links work on Vercel. |
| `lib/services/sync.ts` | NEW | Admin: `getHealth`, `getRuns`, `trigger(job)`. |
| `lib/services/stats.ts` | MODIFY | `getTopics(userId)`, and the `OVERALL_SCORE` metric. |

---

### Stage 4C -- Scores, Awards, Topics & Admin Tabs
**Owner: Member 3**
**PR: `phase3/stage-4c-scores-admin`**
**Estimated time: 6 days**
**Depends on: 2C, 4A, 4B**

**Read first:** `components/site/profile-dashboard.tsx`,
`components/site/leaderboard-dashboard.tsx`, `app/page.tsx`, Phase 2's admin page
and `AdminTabs`.

#### Home -- `app/page.tsx` [MODIFY]

Add `WeeklyHighlights` (O9) after the hero. It follows the same honest-empty-state
rule as the rest of the home page.

#### Profile -- `profile-dashboard.tsx` [MODIFY]

- Award badges, most recent first, with repeat awards grouped ("x3").
- An Overall Score tile (O8), with its three components explained in a tooltip.
- `TopicStrengthChart` (P2) for members with a Codeforces handle.

#### Leaderboard [MODIFY]

- An **Overall score** metric tab (O8), showing when it was computed.

#### Admin -- `app/(dashboard)/admin/page.tsx` [MODIFY]

Three new `AdminTabs` tabs:

- **Contests & Seasons** -- a create-season form (name, dates, count best); an
  add-contest form (Codeforces contest id, kind, season) that shows the server's
  validation message; a contests `DataTable` with Sync now, last synced, and Delete
  behind a confirmation.
- **Awards** -- the last 8 weeks. Recompute sits behind a confirmation that says
  announced winners may change, and is offered for closed weeks only.
- **Sync Health** -- per job: last success, failures, a stale badge, and Run now
  (behind a confirmation for full backfill jobs). Recent runs with their error sample.

#### Events -- `app/events/page.tsx` [MODIFY]

Link to the Calendar from the page header.

---

## Section 8 -- Wave C Backend: Stages 5A, 5B

### Stage 5A -- Blog Workflow & Comments
**Owner: Member 4**
**PR: `phase3/stage-5a-blog`**
**Estimated time: 5 days**
**Depends on: 1C, 3C**

**Read first:** the whole `blog/` package (`BlogController`, `BlogService`, `BlogPost`,
`BlogRepository`, DTOs), migration V14, the SecurityConfig block from 1C,
`VerifiedMemberGuard`, Section 1 D17-D19.

#### 8.1 Post Workflow -- `BlogService.java` [MODIFY]

**Public reads keep the published-only guarantee.** `getPublishedBlogs`,
`getBlogById` and `getBlogBySlug` switch from `findByPublishedTrue*` to repository
methods filtered on `status = PUBLISHED`, ordered by `published_at DESC`. Keep the
existing comment explaining why an unfiltered lookup would let anyone walk IDs to
read drafts.

| Operation | Who | Allowed from | Result |
|---|---|---|---|
| `create` | Verified member | -- | DRAFT, `author_id` = caller, `author_name` = caller's name. An admin may pass `publishNow = true` for PUBLISHED. The slug comes from the title, with `-2`, `-3` appended on a clash. |
| `update` | Author | DRAFT, REJECTED | Content saved. The status is unchanged. |
| `update` | Admin | any | Content saved. A PUBLISHED post stays published. |
| `submit` | Author | DRAFT, REJECTED | PENDING_REVIEW, sets `submitted_at`, clears `review_note` |
| `publish` | Admin | PENDING_REVIEW, or an admin's own DRAFT | PUBLISHED, sets `reviewer_id`, and `published_at` if not already set |
| `reject` | Admin | PENDING_REVIEW | REJECTED with `review_note` (required, 1-500 characters) |
| `delete` | Author | own DRAFT or REJECTED | Deleted |
| `delete` | Admin | any | Deleted |
| `getForEditing` | Author or admin | any | Full post, including `review_note` |
| `mine` | Signed-in member | -- | The caller's posts, every status |
| `reviewQueue` | Admin | -- | PENDING_REVIEW, oldest submission first |

- **Any other transition returns 409**, naming the current status.
- `getForEditing` returns **404, not 403**, to anyone who is neither the author nor an
  admin, so it never confirms that a draft exists.
- **`GET /api/blogs/{id}/edit` and `GET /api/blogs/mine` need
  `@PreAuthorize("isAuthenticated()")`.** The existing `GET /api/blogs/**` permitAll
  admits them at URL level, and 1C only overrides `mine` and `review-queue`.
- The `@PreAuthorize("hasRole('ADMIN')")` on today's `PUT` and `DELETE` handlers is
  replaced by the author-or-admin check in the service, because the URL layer cannot
  know who wrote a post.

#### 8.2 DTOs [MODIFY]

- `BlogResponseDto` -- add `status`, `authorId`, `publishedAt`, `commentCount`, and
  `readMinutes` = `max(1, round(words / 200))`, computed on the server.
- `BlogEditDto` [NEW] -- `BlogResponseDto` plus `reviewNote` and `submittedAt`.
  Returned only by `getForEditing` and `mine`.
- `BlogRequest` -- `published` is ignored unless the caller is an admin.

#### 8.3 Comments [NEW]

`blog/entity/BlogComment`, `BlogCommentRepository`, `BlogCommentService`,
`BlogCommentController`.

- **List** (`GET /api/blogs/{id}/comments`) -- PUBLISHED posts only, otherwise 404.
  Top-level comments oldest first, replies nested. A deleted comment that has replies
  keeps its place with `body = null` and `deleted = true`. A deleted comment with no
  replies is left out.
- **Create** -- `VerifiedMemberGuard`. The post must be PUBLISHED. A `parentId` must
  belong to the same post and be top-level, otherwise 400. The body is trimmed, 1 to
  2000 characters. One comment per 20 seconds per member, then 429 with `Retry-After`.
- **Edit** -- the owner, within 15 minutes of `created_at`, on a comment that is not
  deleted. Otherwise 403.
- **Delete** -- the owner or an admin. A soft delete that sets `deleted_at`.
- **Response** -- `id`, `parentId`, `author { id, name, avatarUrl, codeforcesHandle }`,
  `body`, `createdAt`, `editedAt`, `deleted`, plus `canEdit` and `canDelete`
  calculated for the caller.
- **Plain text only (D19)** -- stored exactly as submitted and rendered as text by React.

#### 8.4 Stage 5A Tests

| Test | Scenario | Expected |
|---|---|---|
| `BlogServiceTest` | Anonymous `GET /api/blogs/{id}` of a DRAFT | 404 |
| | Author submits a DRAFT | PENDING_REVIEW, `submitted_at` set |
| | Author edits a PENDING_REVIEW post | 409 |
| | Admin rejects without a note | 400 |
| | Author resubmits a REJECTED post | PENDING_REVIEW, note cleared |
| | Another member calls `getForEditing` | 404 |
| | Publish | `published` column is also true (entity hook) |
| | Unverified member creates, with verification required | 403 `EMAIL_NOT_VERIFIED` |
| `BlogCommentServiceTest` | Reply to a reply | 400 |
| | Comment on a DRAFT post | 404 |
| | Two comments within 20 seconds | Second returns 429 |
| | Edit after 16 minutes | 403 |
| | Delete a comment that has replies | Kept in the list as deleted, body null |
| `BlogAuthorizationTest` | Anonymous `POST /api/blogs` | 401 |
| | USER `POST /api/blogs/1/publish` | 403 |
| | Anonymous `GET /api/blogs/1/edit` | 401 |

---

### Stage 5B -- RSVP, Event Delete, Notification Preferences & Reminders
**Owner: Member 5**
**PR: `phase3/stage-5b-rsvp-reminders`**
**Estimated time: 5 days**
**Depends on: 1C, 3A, 3B**

**Read first:** the `event/` package (`EventService`, `EventController`, DTOs),
migration V15, `mail/service/*` and `VerifiedMemberGuard` from 1C, Section 1 D13,
D14 and D29.

#### 8.5 RSVP & Delete -- `EventService.java` [MODIFY]

| Operation | Rule |
|---|---|
| `register(eventId, caller)` | `VerifiedMemberGuard`. The event must be UPCOMING with `event_date` in the future, otherwise 409 "Registration closed". Registering twice returns 200, idempotent. **Capacity:** lock the event row with `SELECT ... FOR UPDATE` inside the transaction before counting, so two requests for the last seat cannot both succeed. A full event returns 409 "Event is full". |
| `unregister(eventId, caller)` | Allowed until `event_date`. Idempotent, 204. |
| `registrationStatus(eventId, caller)` | `{ registered, count, capacity, open }` |
| `getRegistrations(eventId)` | Admin. Name, email, phone, club role, registered at, and `alreadyAttending`. |
| `markAttended(eventId, userIds, admin)` | Admin. Creates `event_attendees` rows and skips members already present. It applies the **Phase 2 `addAttendee` rules unchanged** -- including "the event must be UPCOMING" -- so the admin UI tells admins to mark attendance before marking the event completed. |
| `deleteEvent(eventId)` | D14. When any attendee, registration or photo exists: 409 "This event has attendance, RSVPs or photos. Cancel it instead." Otherwise delete. |

- `EventCreateRequest` and `EventUpdateRequest` -- add an optional `capacity` (> 0).
- `EventDetailDto` -- add `capacity` and `registrationCount`.

#### 8.6 Notification Preferences [MODIFY]

- `UserProfileUpdateRequest` -- add `notifyEvents` and `notifyContests`.
- `UserResponseDto` (own profile only) -- add both. They are **never** in the public
  profile DTO.

#### 8.7 `mail/` package [NEW + MODIFY]

- **`UnsubscribeTokenService.java`** -- token = base64url(`userId:kind:HMAC-SHA256`),
  keyed by `cpclub.mail.unsubscribe-secret`. **No expiry** -- an unsubscribe link in
  an old email must keep working. When mail is enabled and the secret is missing, the
  application refuses to start, in the same way `JwtUtils` guards the JWT secret.
- **`NotificationController.java`** -- `GET` and `POST /api/notifications/unsubscribe?token=`
  sets that preference to false and returns `{ kind }`. POST exists for mail clients'
  one-click unsubscribe (RFC 8058).
- **`ReminderService.java`**
  - **Event reminders** -- hourly, on the hour, IST. For each UPCOMING event with
    `event_date` in `(now + 23h, now + 24h]`, email every verified member with
    `notify_events = true`. The email adds "You're registered" for members who RSVP'd.
  - **Contest digest** -- daily at 09:00 IST. External contests and club contests
    starting in the next 24 hours, sent to verified members with
    `notify_contests = true`. **No email on a day with no contests.**
  - **Idempotency (D29)** -- insert the `reminder_log` row with
    `ON CONFLICT DO NOTHING` **before** sending, and send only when the insert
    succeeded. Consequence: a send that fails after the insert is not retried. That
    is chosen deliberately, because a duplicate email is worse than a missed reminder.
  - **Pacing** -- a `mailRateLimiter` bean (2 per second) in `AppConfig`, to stay
    inside SMTP provider limits.
  - **Headers** -- `List-Unsubscribe` and `List-Unsubscribe-Post: List-Unsubscribe=One-Click`.
    Every link in every email is built from `cpclub.public-site-url`.

#### 8.8 Stage 5B Tests

| Test | Scenario | Expected |
|---|---|---|
| `EventRegistrationServiceTest` (Testcontainers) | Capacity 1, two concurrent registrations | Exactly one succeeds, one gets 409 |
| | Register for a COMPLETED event | 409 "Registration closed" |
| | Register twice | One row, 200 both times |
| | Unverified member, verification required | 403 |
| `EventDeleteTest` | Event with one RSVP | 409 with the cancel message |
| | Event with nothing attached | Deleted |
| `MarkAttendedTest` | Three user ids, one already attending | Two rows created |
| `UnsubscribeTokenServiceTest` | Token with a tampered user id | Rejected |
| | Mail enabled, secret unset | Context fails to start |
| `ReminderServiceTest` | Hourly job runs twice in the same window | One email per member |
| | Digest day with no contests | No emails |
| | Member with `notify_contests = false` | Not emailed |
| `NotificationAuthorizationTest` | Anonymous POST unsubscribe with a valid token | 200 |

---

## Section 9 -- Wave C Frontend: Stages 6A, 6B, 6C

### Stage 6A -- Blog Pages, Editor, Comments, RSVP & Unsubscribe UI
**Owner: Member 1**
**PR: `phase3/stage-6a-blog-rsvp-ui`**
**Estimated time: 6 days**
**Depends on: 4A, 5A, 5B**

**Read first:** `app/blog/page.tsx`, `components/site/blog-list.tsx`,
`lib/content/blog.ts`, Phase 2's `app/events/[id]/page.tsx`,
`components/site/add-to-calendar.tsx` from 4A.

| File | Action | What it does |
|---|---|---|
| `package.json` | MODIFY | Add `react-markdown`, `remark-gfm`, `remark-math`, `rehype-katex`, `rehype-highlight` and `katex`, pinned to exact versions. **Never add `rehype-raw`** (D17). |
| `components/site/markdown-renderer.tsx` | NEW | `react-markdown` with those plugins. External links open with `rel="noopener noreferrer nofollow"`. Images: https only, lazy-loaded, never wider than the column. Headings get anchor ids. The code highlight theme maps onto design tokens, so it works in both themes. |
| `app/blog/layout.tsx` | NEW | Imports the KaTeX CSS -- blog routes only. |
| `app/blog/page.tsx` | MODIFY | Fetch published posts on the server. The featured slot shows the newest post tagged `featured`, and renders nothing otherwise -- the conditional from #72 stays. The "Start a draft" call to action links to `/blog/write`, via sign-in when signed out. |
| `components/site/blog-list.tsx` | MODIFY | Render API posts (title linking to the slug, excerpt, read minutes, tags, author). Derive the tag chips from the posts it is given. Keep both empty states from #72. |
| `app/blog/[slug]/page.tsx` | NEW | Server component. `generateMetadata` supplies the title, a description (the first 160 characters of plain text) and Open Graph data. Author linked to their profile, published date, read time, tags, `MarkdownRenderer`, `CommentThread`. Missing post -> `notFound()`. |
| `app/blog/write/page.tsx`, `app/blog/edit/[id]/page.tsx` | NEW | Signed-in only. Title, tag chips, and a Markdown textarea with a live preview -- side by side on wide screens, tabs on narrow ones. A local draft is autosaved to `localStorage` every 5 seconds as a convenience only; the server stays the source of truth. Save draft and Submit for review. Shows a status badge, and the reviewer's note as a banner when REJECTED. Admins also see Publish. |
| `app/blog/mine/page.tsx` | NEW | The member's own posts, grouped by status. |
| `components/site/comment-thread.tsx` | NEW | Comments with one level of replies. Composer with a 2000-character counter. Edit available for 15 minutes, with a countdown. Delete behind a confirmation. "[deleted]" placeholder. Separate prompts for signed-out and unverified members. Plain text with preserved line breaks (`whitespace-pre-wrap`) -- no Markdown. |
| `components/site/registration-button.tsx` | NEW | States: signed out ("Sign in to RSVP"), unverified (verify prompt), open ("RSVP" / "Cancel RSVP"), full ("Event full", disabled), closed ("Registration closed"). Shows "23 going" and the capacity. |
| `app/events/[id]/page.tsx` | MODIFY | Add `RegistrationButton` and `AddToCalendar`. |
| `app/unsubscribe/page.tsx` | NEW | Reads the token, calls the API, confirms which emails stopped, and links to the notification settings. |

---

### Stage 6B -- Blog, Comments, RSVP & Notification State
**Owner: Member 2**
**PR: `phase3/stage-6b-blog-rsvp-state`**
**Estimated time: 4 days**
**Depends on: 4B, 5A, 5B**

**Read first:** `types/api.ts`, `lib/services/events.ts`, `lib/content/blog.ts`,
Section 11.

| File | Action | What it changes |
|---|---|---|
| `types/api.ts` | MODIFY | Add `BlogStatus`, `BlogPost`, `BlogPostEditable`, `BlogComment`, `RegistrationStatus`, `EventRegistrationRow`. Add `capacity` and `registrationCount` to `Event`. Add `notifyEvents` and `notifyContests` to `User`. |
| `lib/services/blog.ts` | NEW | `listPublished`, `getBySlug` (safe to call from server components), `getForEditing`, `create`, `update`, `submit`, `publish`, `reject`, `remove`, `mine`, `reviewQueue`. |
| `lib/services/comments.ts` | NEW | `list`, `create`, `edit`, `remove`. `create` turns a 429 into an error that carries the wait from `Retry-After`. |
| `lib/services/events.ts` | MODIFY | `register`, `unregister`, `getRegistrationStatus`, `getRegistrations`, `markAttended`, `deleteEvent`. |
| `lib/services/notifications.ts` | NEW | `unsubscribe(token)`. |
| Editor form schema | NEW | zod: title 3-200 characters, at most 5 tags, and content of at least 50 characters **on submit** (saving a draft allows less). |
| Profile edit form | MODIFY | An "Email me about" section with the two toggles. Disabled, with an explanation, while the member is unverified. |
| `lib/content/blog.ts` | DELETE | The empty hard-coded list and featured slot. The blog now reads the API. |

---

### Stage 6C -- Admin Blog Review, RSVP Attendance & Event Delete
**Owner: Member 3**
**PR: `phase3/stage-6c-admin-review-rsvp`**
**Estimated time: 4 days**
**Depends on: 4C, 6A, 6B**

**Read first:** Phase 2's admin page and `app/(dashboard)/admin/events/[id]/page.tsx`,
`components/site/markdown-renderer.tsx` from 6A.

- **Admin -- new "Blog Review" tab.** A `DataTable` of PENDING_REVIEW posts (title,
  author, submitted), with a count badge on the tab. A row opens a preview drawer
  rendered by `MarkdownRenderer`, with **Publish** and **Reject** -- Reject requires
  a note, up to 500 characters. A second sub-tab lists published posts, with Delete
  behind a confirmation.
- **Admin Events tab [MODIFY].** Capacity field on the create and edit forms. A Delete
  action per event: on 409, show the server's message and offer Cancel instead.
- **`admin/events/[id]` [MODIFY].** A new **RSVPs** panel: a `DataTable` of
  registrations with row selection and "Mark selected as attended", marking members
  already attending, with the count against capacity. A note above it: "Mark
  attendance before marking the event completed."

---

## Section 10 -- Stage 7: Integration, Rollout & Backfill
**Owner: Member 6**
**PR: `phase3/stage-7-integration`**
**Estimated time: 4 days**
**Depends on: every stage above**

### 10.1 Pre-flight

- Every Phase 3 PR is merged. `./mvnw clean test` is green in CI, including the
  Testcontainers suite. `npm run build` and `npm run lint` are green.
- **Back up the production database** (`documents/DATABASE-BACKUP.md`) before V8-V15 run.
- Every variable in Section 12 is set. The sending domain has SPF and DKIM, and a test
  email has reached a team inbox.
- The carried-over Cloudinary preset hardening is done.

### 10.2 Rollout Order -- verify each step before the next

1. **Deploy the backend with every Phase 3 flag off.** Confirm `flyway_schema_history`
   shows V8-V15 applied, the app is healthy, and every existing page is unchanged.
2. `POST /api/admin/sync/CF_PROBLEMSET`. Expect roughly ten thousand `cf_problems` rows.
3. Set `CPCLUB_CF_SUBMISSIONS_ENABLED=true`, then `POST /api/admin/sync/CF_SUBMISSIONS`.
   That is one call per member at one call every 2 seconds -- about 5 minutes for 150
   members. **Check three members you know:** their solved count against their
   Codeforces profile (small differences are expected, D3) and their most recent solve.
4. `POST /api/admin/sync/CF_CONTESTS`, then `POST /api/admin/sync/LC_CONTESTS`.
5. The first 00:05 IST LeetCode baseline runs that night. **Record the date** -- it is
   "Tracking since" for LeetCode period stats. Tell members weekly LeetCode numbers
   start from then.
6. Enable external contests. The calendar should show the next seven days for each
   platform. If the AtCoder or CodeChef source fails, the others still appear --
   **it does not block the rollout.**
7. Add the `NEXT_PUBLIC_*` variables on Vercel **first** (they are build-time), then
   deploy the frontend.
8. After the first Monday: confirm weekly awards were computed (Sync Health and the
   home page), and check the winners by hand against Section 1.5.
9. Set `CPCLUB_MAIL_ENABLED=true`. Register a test account on an allowed domain and
   complete verification. Then set `CPCLUB_AUTH_REQUIRE_EMAIL_VERIFICATION=true` and
   `NEXT_PUBLIC_REQUIRE_EMAIL_VERIFICATION=true`, and redeploy the frontend.
10. With `CF_API_KEY` and `CF_API_SECRET` set, add the next club contest, and check its
    ranklist and upsolve tab after it ends.
11. Announce.

### 10.3 Rollback

- Every Phase 3 feature sits behind a flag -- turn it off and restart.
- Every migration is additive. `blog_posts.published` is kept (V14), so the previous
  backend release still runs against the new schema.
- Frontend: promote the previous Vercel deployment.

### 10.4 Manual Verification

- Two browser sessions register for the last seat of an event at the same moment.
- A comment inside 20 seconds of another is rejected with a readable wait time.
- A draft post cannot be read anonymously by ID or by slug.
- `club.ics` imports into Google Calendar and Apple Calendar with the correct IST times.
- An upsolve submitted on Codeforces appears within one sync cycle (6 hours).
- Every new page at 400px wide, in light and dark themes.
- The unsubscribe link from a real email works, and so does one-click unsubscribe in Gmail.

### 10.5 Documentation

- `README.md` -- update the feature list.
- `documents/project_roadmap_all_phases.md` -- mark Phase 3 complete, and record that
  CodeChef and AtCoder sync from the original Phase 2 roadmap was dropped by decision
  (D9) -- both stay link only.
- Open the **Phase 4 cleanup** list: drop `blog_posts.published`; delete the
  `app/next-api/cf/*` routes if nothing uses them; add ShedLock before ever running a
  second backend instance.

---

## Section 11 -- API Contract Reference (Phase 3 Additions)

Every Phase 2 endpoint in `phase_2_execution_playbook.md` Section 11 is unchanged
unless marked **[MODIFY]** below.

### Stats & Leaderboards

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| GET | `/api/leaderboard/stats?metric=&platform=&period=&filter=&page=&size=` | Public | 1B (`OVERALL_SCORE`: 3B) | Stats leaderboards (validation in 4.9) |
| GET | `/api/stats/users/{id}?period=` | Public* | 1B | Member stats. *Unverified members: the member themselves or an admin only |
| GET | `/api/stats/users/{id}/rating-history?platform=` | Public | 1B | Rating progression per contest |
| GET | `/api/stats/users/{id}/topics` | Public | 3B | Topic strength |
| GET | `/api/stats/compare?a={id}&b={id}` | Public | 1B | Compare two members, with head-to-head |

### Auth, Email & Notifications

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | 1C | **[MODIFY]** Allowed email domains only; sends verification |
| POST | `/api/auth/verify-email` | Public | 1C | `{ token }` |
| POST | `/api/auth/resend-verification` | Auth | 1C | Rate-limited, 429 with `Retry-After` |
| PUT | `/api/users/profile` | Auth | 5B | **[MODIFY]** Adds `notifyEvents`, `notifyContests` |
| GET, POST | `/api/notifications/unsubscribe?token=` | Public | 5B | Turns off one email preference |

### Sync (Admin)

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| POST | `/api/admin/sync/{job}` | **Admin** | 1A | Start a sync job; returns 202 with the run id |
| GET | `/api/admin/sync/runs?job=&limit=` | **Admin** | 1A | Recent runs |
| GET | `/api/admin/sync/health` | **Admin** | 3A | Last success and stale flag per job |

### Calendar

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| GET | `/api/calendar?from=&to=&platforms=` | Public | 3A | Club events and external contests (62 days maximum) |
| GET | `/api/calendar/club.ics?include=&platforms=` | Public | 3A | Subscribable calendar feed |
| GET | `/api/events/{id}/ics` | Public | 3A | One club event as `.ics` |

### Scores, Awards & Practice

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| GET | `/api/awards/weekly/latest` | Public | 3B | `{ weekStart, computed, awards[] }` |
| GET | `/api/awards/weekly/{weekStart}` | Public | 3B | Awards for one week |
| GET | `/api/awards/users/{id}` | Public | 3B | A member's award history |
| POST | `/api/admin/awards/weekly/{weekStart}/recompute` | **Admin** | 3B | Recompute a closed week (audited) |
| GET | `/api/practice/suggestions` | Auth | 3B | 10 suggestions for the caller |
| POST | `/api/practice/suggestions/{problemId}/skip` | Auth | 3B | Hide one suggestion permanently |

### Club Contests & Seasons

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| GET | `/api/club-contests` | Public | 3C | All club contests |
| POST | `/api/club-contests` | **Admin** | 3C | `{ cfContestId, kind, seasonId }` |
| GET | `/api/club-contests/{id}` | Public | 3C | Club-only ranklist |
| POST | `/api/club-contests/{id}/sync` | **Admin** | 3C | Force a standings sync |
| DELETE | `/api/club-contests/{id}` | **Admin** | 3C | Remove a club contest |
| GET | `/api/club-contests/{id}/upsolve` | Public | 3C | Upsolve matrix |
| GET | `/api/club-contests/upsolve/me` | Auth | 3C | The caller's pending upsolves |
| GET | `/api/seasons` | Public | 3C | All seasons |
| POST | `/api/seasons` | **Admin** | 3C | Create a season |
| PUT | `/api/seasons/{id}` | **Admin** | 3C | Update a season |
| DELETE | `/api/seasons/{id}` | **Admin** | 3C | Delete a season (contests are kept) |
| GET | `/api/seasons/{id}/championship` | Public | 3C | Championship standings |

### Blog & Comments

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| GET | `/api/blogs` | Public | 5A | **[MODIFY]** PUBLISHED only, newest first |
| GET | `/api/blogs/{id}`, `/api/blogs/slug/{slug}` | Public | 5A | **[MODIFY]** PUBLISHED only |
| GET | `/api/blogs/{id}/edit` | Author or **Admin** | 5A | Full post, including the review note |
| POST | `/api/blogs` | Auth (verified) | 5A | **[MODIFY]** Was Admin only. Creates a DRAFT |
| PUT | `/api/blogs/{id}` | Author (DRAFT, REJECTED) or **Admin** | 5A | **[MODIFY]** Was Admin only |
| DELETE | `/api/blogs/{id}` | Author (DRAFT, REJECTED) or **Admin** | 5A | **[MODIFY]** Was Admin only |
| POST | `/api/blogs/{id}/submit` | Author | 5A | Submit for review |
| POST | `/api/blogs/{id}/publish` | **Admin** | 5A | Publish |
| POST | `/api/blogs/{id}/reject` | **Admin** | 5A | `{ note }` |
| GET | `/api/blogs/mine` | Auth | 5A | The caller's posts |
| GET | `/api/blogs/review-queue` | **Admin** | 5A | Posts waiting for review |
| GET | `/api/blogs/{id}/comments` | Public | 5A | Comments on a published post |
| POST | `/api/blogs/{id}/comments` | Auth (verified) | 5A | `{ body, parentId }` |
| PUT | `/api/comments/{id}` | Owner, within 15 minutes | 5A | Edit a comment |
| DELETE | `/api/comments/{id}` | Owner or **Admin** | 5A | Soft delete |

### Events

| Method | Path | Auth | Stage | Description |
|---|---|---|---|---|
| POST | `/api/events`, PUT `/api/events/{id}` | **Admin** | 5B | **[MODIFY]** Optional `capacity` |
| GET | `/api/events/{id}` | Public | 5B | **[MODIFY]** Adds `capacity`, `registrationCount` |
| DELETE | `/api/events/{id}` | **Admin** | 5B | Only when nothing is attached (D14) |
| GET | `/api/events/{id}/registration` | Auth | 5B | `{ registered, count, capacity, open }` |
| POST | `/api/events/{id}/registration` | Auth (verified) | 5B | RSVP |
| DELETE | `/api/events/{id}/registration` | Auth | 5B | Cancel RSVP |
| GET | `/api/events/{id}/registrations` | **Admin** | 5B | Everyone who RSVP'd |
| POST | `/api/events/{id}/registrations/mark-attended` | **Admin** | 5B | `{ userIds }` -> attendance rows |

---

## Section 12 -- Environment Variables

### Backend (Render)

| Variable | Stage | Default | Purpose |
|---|---|---|---|
| `CPCLUB_CF_SUBMISSIONS_ENABLED` | 1A | `false` | Codeforces submissions sync |
| `CPCLUB_AUTH_ALLOWED_EMAIL_DOMAINS` | 1C | `dau.ac.in` | Comma-separated list of allowed email domains |
| `CPCLUB_AUTH_REQUIRE_EMAIL_VERIFICATION` | 1C | `false` | Enforce D16 |
| `CPCLUB_MAIL_ENABLED` | 1C | `false` | Actually send email |
| `CPCLUB_MAIL_FROM` | 1C | `no-reply@localhost` | Sender address |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | 1C | -- | SMTP provider (standard Spring Boot properties). Also set `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`. |
| `CPCLUB_PUBLIC_SITE_URL` | 1C | `http://localhost:3000` | Links in emails, and ICS UIDs |
| `CPCLUB_EXTERNAL_CONTESTS_ENABLED` | 3A | `false` | Upcoming contests calendar sync |
| `CLIST_API_KEY` | 3A | empty | Optional fallback source (D26) |
| `CF_API_KEY`, `CF_API_SECRET` | 3C | empty | Authorized Codeforces API for club contests. **Secret.** |
| `CPCLUB_UNSUBSCRIBE_SECRET` | 5B | -- | HMAC key for unsubscribe links. **Required when mail is enabled; the app refuses to start without it.** |

### Frontend (Vercel)

`NEXT_PUBLIC_*` values are baked in at build time: changing one does nothing until
the site is redeployed.

| Variable | Stage | Default | Purpose |
|---|---|---|---|
| `NEXT_PUBLIC_ALLOWED_EMAIL_DOMAINS` | 2B | `dau.ac.in` | Registration form hint and validation. **Must match the backend list.** |
| `NEXT_PUBLIC_REQUIRE_EMAIL_VERIFICATION` | 2B | `false` | Shows the verification banner. **Must match the backend flag.** |

---

## Section 13 -- Testing Strategy

| Layer | Tooling | Covers |
|---|---|---|
| Unit | JUnit + Mockito, existing H2 profile | Services with no native SQL: parsers, token and signature code, validation, state transitions |
| Integration | Testcontainers PostgreSQL, Flyway on (D22) | Everything in `stats/` and `score/`, club contest sync and upsolve, RSVP capacity locking, every native query |
| Migration smoke | Testcontainers | `MigrationSmokeTest` runs V1-V15 twice: on an empty database, and on one seeded with Phase 2 shaped data -- existing users, and `blog_posts.published` set to NULL, TRUE and FALSE -- asserting the V11 email-verified backfill and the V14 status mapping |
| Authorization | MockMvc, in the style of `UserEndpointAuthorizationTest` | Anonymous, USER and ADMIN on every new route, plus 1C's `Phase3SecurityOrderingTest` |
| Fixtures | `src/test/resources/fixtures/{codeforces,leetcode,atcoder,codechef,clist}/` | One captured real response per external endpoint, with a README recording the capture date and command |
| Frontend | `npm run build`, `npm run lint`, and the manual checklist in 10.4 | Unchanged from Phase 2 |

**Time rule:** every scheduled job and every period calculation in Phase 3 takes the
injected `Clock`. A bare `LocalDateTime.now()` in Phase 3 code fails review.

---

## Section 14 -- Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AtCoder or CodeChef contest-schedule page changes | Medium | That platform's contests go missing from the calendar | Each source isolated; a failing source never deletes rows; stale badge in Sync Health; fixture test; clist.by fallback (D26) |
| Phase 3 overruns (about 3x Phase 2) | High | Waves slip | Each wave ships on its own; M6 re-plans after Wave A; the two-day review rule in Section 2 |
| Codeforces API slow or returning 503 during rounds | High | Sync delays | 6-hourly cadence; one retry; one member's failure never fails the run |
| LeetCode GraphQL changes or blocks server requests | Medium | LeetCode stats go stale | Fixture tests; stale badge; the last good values are kept; the endpoint is already in production use from Phase 2 |
| Verification emails land in spam | Medium | New members cannot verify | SPF and DKIM; verification stays optional until step 9 of the rollout proves delivery; resend |
| A platform objects to automated requests | Medium | That source must stop | Low rates, identifying `User-Agent`, caching, clist.by fallback (D26), and a flag to stop immediately |
| Members upsolve outside the club contest | Medium | "My upsolve didn't count" | D12 stated on the contest page and the practice page |
| Public stats or comparison feel intrusive | Low | Member complaints | Only public platform data; no email or phone (D25); unverified members hidden |
| Someone scales the backend to two instances | Low | Duplicate jobs and emails | D30; ShedLock is on the Phase 4 cleanup list |

---

## Section 15 -- 6-Member Summary

| Member | Role | Stages | Owns |
|---|---|---|---|
| **M6 (Lead)** | Foundation, Sync & DevOps | 0, 1A, 3A, 7 | Migrations V8-V15, entities, Testcontainers, per-host rate limiters, IST crons. Codeforces submissions, problemset and contest history; LeetCode contest history and daily totals; `sync_runs` and the admin sync endpoints. External contests, calendar and ICS, Sync Health. Rollout and backfill. Every PR review and merge. |
| **M5** | Backend Data & APIs | 1B, 3B, 5B | The stats query layer: stats leaderboards, member stats, rating history, compare, attendance board. Overall Aggregated Score, weekly awards, topic strength, practice suggestions. RSVP, guarded event delete, notification preferences, unsubscribe, reminder emails. |
| **M4** | Backend Security | 1C, 3C, 5A | Email domain rule, verification, mail service, and every Phase 3 SecurityConfig rule. Authorized Codeforces client, club contest sync, seasons and Championship, upsolve. Blog workflow and comments. |
| **M1** | Frontend UI/UX | 2A, 4A, 6A | Stats components, multi-platform rating graph, compare view. Ranklist, upsolve matrix, Championship table, topic chart, award badges, weekly highlights, practice page, calendar, Add to Calendar, nav. Markdown renderer, blog post, editor and "my posts" pages, comment thread, RSVP button, unsubscribe page. |
| **M2** | Frontend Auth & State | 2B, 4B, 6B | Stats types and services, verify-email flow and banner, registration domain check, mock data removal. Contest, season, practice, award, calendar and sync services. Blog, comment, RSVP and notification services; notification toggles; removal of the hard-coded blog content. |
| **M3** | Frontend Dashboards | 2C, 4C, 6C | Real profile stats (the fake 247 removed), stats leaderboards, compare page. Home weekly highlights; award badges, Overall Score and topics on profile and leaderboard; admin Contests & Seasons, Awards and Sync Health tabs. Admin Blog Review tab, RSVP attendance panel, guarded event delete. |
