# Deployment & Operations Audit

**Date:** 14 August 2026
**Commit:** `869bc4a`
**Scope:** Containerisation, CI/CD, configuration, observability, release & rollback

Code-level findings live in [SECURITY-AUDIT.md](SECURITY-AUDIT.md),
[BACKEND-AUDIT.md](BACKEND-AUDIT.md) and [FRONTEND-AUDIT.md](FRONTEND-AUDIT.md).
This audit covers only how the application is built, configured, shipped and operated.

---

## What actually deploys, and how

| Component | Platform | Trigger | Verified? |
| --- | --- | --- | --- |
| Frontend | **Vercel** | Vercel GitHub App, auto on every push. Production on `main`, Preview on branches. | Yes — 30 deployments via `vercel[bot]` |
| Backend | **Render** (probably) | Unverified — no deployment records, no config in repo | **No** |
| Database | Managed Postgres | Manual | No |

**There is no CD in this repository.** Both workflows are CI-only. Neither builds a
container image, pushes a tag, or calls a deploy hook. Everything that deploys is
dashboard-configured platform state existing nowhere in version control.

**Deployment is not gated on CI.** From the merge of PR #35:

```
Frontend CI          created 11:13:10Z   finished 11:14:02Z
Vercel Production    created 11:13:44Z   <-- 18s BEFORE CI finished
```

A red build ships to production regardless. Six PRs (#25, #28, #32–#35) merged with
`reviews: []` — zero approvals, by their own author — three of them security fixes.
This contradicts `README.md:48` ("no self-merging") and playbook §13.

---

## Environment variable inventory

Verified by grepping what the code actually reads.

| Variable | Read at | Documented? | Default | If missing |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring implicit | **No** | none → `default` profile | **Silent wrong config** — see C1 |
| `SPRING_DATASOURCE_URL` | `application-prod.yml:3` | **Wrong name in docs** | none | Under `default`: silently uses `localhost:5432` |
| `SPRING_DATASOURCE_USERNAME` | `application-prod.yml:4` | No | none | falls back to committed `cpclub_user` |
| `SPRING_DATASOURCE_PASSWORD` | `application-prod.yml:5` | No | none | falls back to committed `cpclub_password` |
| `JWT_SECRET` | `application.yml:22` | playbook §18 | **none (fixed)** | App fails to start — correct, loud |
| `CORS_ALLOWED_ORIGINS` | `application.yml`, `SecurityConfig.java:48` | inline comment only | `http://localhost:3000` | **Half-broken site** — see I3 |
| `NEXT_PUBLIC_API_URL` | `lib/axios.ts:17` | `frontend/.env.example` — **says "Railway"** | `http://localhost:8080` | Production build calls localhost |
| `NEXT_PUBLIC_SITE_URL` | `app/layout.tsx:25` | `FRONTEND.md` only | `http://localhost:3000` | OG/canonical URLs point at localhost |

**There is no `backend/.env.example`.** Six backend variables, zero examples.

---

## Critical

### C1. `SPRING_PROFILES_ACTIVE=prod` is set nowhere, and cannot be verified from outside

`backend/Dockerfile:15` sets JVM flags but no profile:

```dockerfile
ENTRYPOINT ["java", "-Xmx256m", "-Xss512k", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
```

Because `SPRING_DATASOURCE_*` bind through relaxed binding regardless of active profile,
**setting only the datasource variables produces a fully working application running the
`default` profile against production Postgres.** It starts, serves traffic, looks healthy
— while running `ddl-auto: update` instead of `validate`, logging every SQL statement,
and exposing Swagger UI. Nothing distinguishes it from a correct deployment.

**One-command diagnostic, run this first:**

```bash
curl -i https://<backend-host>/v3/api-docs
```

HTTP 200 ⇒ the prod profile is **not** active. 404 confirms `prod`.

**Fix:** `ENV SPRING_PROFILES_ACTIVE=prod` in the Dockerfile so it cannot drift.

### C2. The deployment docs name a variable the code does not read

`documents/phase_1_execution_playbook.md:269` — *"Provide `DATABASE_URL` and `JWT_SECRET`."*
The code reads `SPRING_DATASOURCE_URL`. `DATABASE_URL` is read by nothing.

This is worse than a missing doc: **Render's managed Postgres auto-injects a variable
called `DATABASE_URL`.** An operator following the playbook sees it already populated,
concludes the step is done, and deploys straight into C1's silent-fallback path.

Two further traps, documented nowhere: Render's value is a `postgres://user:pass@host/db`
URI which Spring's JDBC driver **cannot parse** (it needs `jdbc:postgresql://host:5432/db`),
and the credentials must be split into two separate variables.

### C3. Zero repository record of backend deployment config — bus factor 1

No `render.yaml`, `Procfile`, deploy workflow, or runbook (verified: none exist). The
backend's build command, start command, region, plan, health-check path and entire
environment-variable set exist only as mutable dashboard state in one person's account.

**The backend cannot be reconstructed from this repository.** Nobody can answer "what env
vars does production have?" — which is exactly why C1 is unverifiable.

Meanwhile `phase_1_execution_blueprint.md:269` marks P1-010 **Done** while `:277`
("Live deployment URLs are accessible") is still unchecked.

### C4. The documented recovery procedure destroys data and depends on a backup that doesn't exist

`phase_1_execution_playbook.md:273` — *"If migrations corrupt data, drop schema and
restore from local backup SQL dump."*

Three failures in one sentence:

- **No backup exists.** No `pg_dump` script, no scheduled job, no documented manual step.
  Render's free Postgres tier has no PITR and **expires the database after 30 days**.
- **No restore procedure.** No connection instructions, no `psql` invocation, no
  verification step.
- **`drop schema` is the data-loss event, not the recovery.** Under `ddl-auto: validate`
  with no migration tool, dropping the schema leaves nothing to recreate it — the next
  boot fails validation and the service never starts. The "recovery" bricks it permanently.

Every member account, linked handle, and blog post is one incident from unrecoverable
loss. **For a project holding real student accounts this outranks everything else here.**

---

## Important

**I1. No `backend/.env.example`.** Six variables, no examples anywhere. A contributor has
no way to discover `CORS_ALLOWED_ORIGINS` short of grepping YAML.

**I2. `frontend/.env.example:3` names the wrong platform** — "Railway" where every other
source says Render. It is the first file a new contributor opens. It also omits
`NEXT_PUBLIC_SITE_URL`, so it documents one of the two variables the frontend reads.

**I3. CORS default breaks writes silently in production.** If `CORS_ALLOWED_ORIGINS`
doesn't include the Vercel domain, SSR pages keep working (server-to-server, CORS-exempt)
while every browser-side call — login, register, edit handle — fails preflight. **Reads
work, writes don't**, which is the hardest failure mode to diagnose.

**I4. No graceful shutdown.** No `server.shutdown: graceful` in either YAML (verified);
Spring's default is `immediate`. Every redeploy — which happens on every push to `main` —
severs in-flight requests.

**I5. No connection-pool configuration.** No `spring.datasource.hikari.*` anywhere.
Default `maximum-pool-size: 10` with no `max-lifetime` below the provider's idle reaper,
so the pool hands out sockets the server already closed — intermittent
`Connection is closed` errors that look random.

**I6. The scheduled sync won't fire on a sleeping instance.** `@Scheduled` is an
**in-process** timer. On Render's free tier the service spins down after 15 minutes idle,
so the job only runs if the instance happens to be awake at 00/06/12/18 UTC. The
leaderboard can silently go stale for days. The admin `POST /api/codeforces/sync` is the
de-facto mechanism, but nothing documents that.

**I7. Health endpoint can't detect the failure that matters.** `HealthController` returns
a hardcoded `{"status":"UP"}` and never touches the database — deliberate, and fine for
liveness. But there is **no readiness counterpart**: if Postgres is unreachable,
`/api/health` still returns 200 while every real endpoint 500s. `spring-boot-starter-actuator`
is absent (verified: 0 matches in `pom.xml`), so `/actuator/health/readiness` doesn't
exist either.

**I8. CI never builds the container image.** Neither workflow runs `docker build`. The
Dockerfile is executed for the first time on Render, after the merge is on `main`. Adding
it costs ~60s and turns a production failure into a PR failure.

**I9. `-Xmx256m` is brittle.** The recent Dockerfile change is a good instinct for a small
instance, but a fixed heap won't adapt if the instance is resized.
`-XX:MaxRAMPercentage=75.0` scales automatically.

---

## Minor

- **Base image on EOL OS** — `Dockerfile:2,10` use `-focal` (Ubuntu 20.04), past standard
  support. Move to `-jammy` or `-noble`.
- **`docker-compose.yml:1`** declares obsolete `version: '3.8'` — warns on every run.
- **No `EXPOSE`, no image healthcheck** in the Dockerfile. `EXPOSE 8080` is how most
  platforms discover the listen port.
- **Maven version mismatch** — `Dockerfile:2` pins `maven:3.9.6`; the wrapper (used by CI
  and every developer) pins 3.9.16. The production artifact is built with a version
  nobody else uses.
- **No `.dockerignore`** — local builds ship `backend/target/` into the context.
- **No versioning, tagging or changelog.** `git tag -l` and `gh release list` are both
  empty. `pom.xml` says `0.0.1-SNAPSHOT`, `package.json` says `0.1.0`, `HealthController`
  reports `"1.0.0"`. **No deployed artifact can be mapped back to a commit.**
- **Rollback is uneven** — instant and atomic for Vercel; a multi-minute rebuild for
  Render; **impossible for the database**. With no migration tool, rolling code back past
  a schema change leaves the app validating against a schema it no longer matches.
- **Environment parity gap** — tests use H2 with `ddl-auto: create-drop`, so the suite
  generates its own schema and **can never detect the `validate` mismatch that would break
  production**. All 56 tests pass against a schema Hibernate just built to match the
  entities.
- **Repo hygiene is regressing.** Tracked at root: `fix_docs.py`, `final_diff.txt`,
  `pr_diff.txt` (317 KB), `refactor.py`, `tick_member3.py`, `delete_old.py`. Four were
  added by the audit-fix PRs themselves. Root `.gitignore` has no rule to prevent this.

---

## Strengths

Genuinely well done:

- **Multi-stage Docker build with correct layer caching** — `COPY pom.xml` →
  `dependency:go-offline` → `COPY src`. Dependency layer survives source changes.
- **Non-root runtime user** (`Dockerfile:12-13`) and a JRE-only final stage.
- **No PID-1 signal problem** — exec-form `ENTRYPOINT` means the JVM is PID 1 and gets
  `SIGTERM` directly, where Spring's shutdown hook handles it.
- **CI mirrors local commands exactly** and caches dependencies. Runs finish in 25–55s —
  fast enough that nobody skips it. 24 of the last 25 runs green.
- **Path filters were correctly removed.** With them, a required check on an untouched
  path stays permanently pending and deadlocks the merge. Right trade.
- **Prod profile disables Swagger and SQL logging**, with a comment explaining why.
- **CORS is configurable** with multi-origin support, not hardcoded.
- **`force-dynamic` on the data routes** prevents CI baking a `localhost:8080` fetch into
  a static page.
- **Vercel's half of the pipeline is genuinely production-grade** — preview deploys,
  atomic production deploys, instant rollback, for free.

---

## Could a new team member deploy this from the repo alone?

**No.** `README.md` covers local development well and CI correctly, but contains zero
deployment instructions, and its status table still says *"deployment | Not started"* for
work that is live.

What the repository cannot tell an operator:

1. The backend's hostname — **no backend URL appears anywhere in the repo**
2. Whether Render or Railway is real (sources contradict)
3. The correct datasource variable name (docs say `DATABASE_URL`; code reads `SPRING_DATASOURCE_URL`)
4. That `SPRING_PROFILES_ACTIVE=prod` must be set at all
5. That Render's Postgres URI must be converted to JDBC form and split into three variables
6. That `CORS_ALLOWED_ORIGINS` must include the Vercel domain or all writes break
7. Render's build command, start command, plan, or health-check path
8. How to take or restore a database backup
9. How to verify a deploy succeeded

---

## Recommended order of work

1. **`curl /v3/api-docs`** against production to determine the active profile *(minutes)*
2. **`ENV SPRING_PROFILES_ACTIVE=prod`** in the Dockerfile so it can never drift
3. **Take a `pg_dump` today** and write down how to restore it
4. **`documents/DEPLOYMENT.md` + `backend/.env.example` + `render.yaml`**; correct
   playbook §18 and `frontend/.env.example`
5. `server.shutdown: graceful`, Hikari limits, `-XX:MaxRAMPercentage=75.0`
6. `docker build` in CI; delete the six stray root files and add a `.gitignore` rule

---

## Assessment

**Production-grade: with fixes.**

The parts that are built are built properly — the Dockerfile gets multi-stage layering,
non-root, and exec-form `ENTRYPOINT` right; CI is fast, cached, and runs exactly what
developers run; the prod profile makes real hardening decisions. Vercel's half is fully
production-grade.

What disqualifies it is not sophistication but **verifiability and recoverability**.
Nobody — including the person who built it — can prove from the repository whether
production runs the prod profile, and the documentation actively misdirects operators
toward a variable name Render pre-populates and Spring ignores. Underneath sits a database
with no backup whose written recovery procedure would destroy it and leave a service that
cannot boot.

Items 1–4 above are an afternoon's work and move this from *"works, and one person knows
why"* to *"works, and the repo can prove it."*
