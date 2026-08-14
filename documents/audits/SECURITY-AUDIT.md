# Security Audit

**Date:** 11 August 2026
**Commit:** `64b5237`
**Scope:** Whole application — Spring Boot backend and Next.js frontend

Findings below were each verified against the source. Anything under 80% confidence
of real exploitability was dropped rather than listed, so this is not an exhaustive
list of imperfections — it is the set of things worth acting on.

---

## Summary

| # | Severity | Issue | Status |
| --- | --- | --- | --- |
| 1 | **High** | JWT signing secret falls back to a publicly-known constant | Open |
| 2 | **High** | Every student's email address is readable without authentication | Open |
| 3 | Medium | Unpublished blog drafts are readable by anyone | Open |

Findings 1 and 2 compound: #2 tells an attacker which account holds `ROLE_ADMIN`,
and #1 lets them forge that account's token. Treat them as one incident.

**Verdict: not safe to run in production with real student data until 1 and 2 are
fixed.** Both are configuration gaps rather than design flaws — the surrounding
architecture is sound (see "Verified as not vulnerable").

---

## 1. JWT secret falls back to a publicly-known constant — HIGH

**Where:** `backend/src/main/resources/application.yml:19`

```yaml
secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
```

**Why it is live rather than theoretical:**

- `application-prod.yml` contains **no `app.jwt.secret` override**, so the production
  profile inherits this fallback whenever `JWT_SECRET` is unset.
- The startup guard in `JwtUtils` rejects only blank or sub-32-character secrets.
  This one is 64 characters, so it passes validation and the application boots
  normally with a known key. Nothing warns anybody.
- The value is not a random placeholder. It is the canonical secret from a
  widely-copied Spring Boot JWT tutorial and appears verbatim in thousands of
  public repositories — and it is committed here regardless.

**Attack path:** read the secret from the repository → mint an HS256 token with `sub`
set to an admin's email address → `AuthTokenFilter` verifies the signature and loads
authorities from the database row, so the forged token carries genuine `ROLE_ADMIN`
regardless of any claim in the token itself. That grants `DELETE /api/users/{id}`,
`PUT /api/users/{id}/role`, and full blog write access.

**Fix:**

```yaml
secret: ${JWT_SECRET}   # no default — fail fast if unset
```

Then rotate `JWT_SECRET` in production to a fresh 256-bit random value. Rotation
invalidates every existing token, which is the desired outcome here.
`application-test.yml` may keep a literal, since it is test-only.

---

## 2. Unauthenticated exposure of student PII — HIGH

**Where:** `backend/.../security/config/SecurityConfig.java:133` and
`backend/.../user/dto/UserResponseDto.java:23`

```java
.requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
```

```java
public record UserResponseDto(
        Long id, String name, String email, String codeforcesHandle,
        Integer rating, Role role, LocalDateTime createdAt, LocalDateTime updatedAt
) {
```

Every GET under `/api/users` is public, and the DTO those endpoints return carries
`email` and `role`. No redaction happens at any layer — controller, service, or DTO.

**Attack path:** with no token and no account,
`curl https://<backend>/api/users?page=0&size=100` returns page one of the full
member roster: name, **email**, Codeforces handle, rating, **role**, join date.
Increment `page` until `last: true` for the complete list. `GET /api/users/{id}`
allows sequential enumeration as an alternative.

That yields a harvested list of real student email addresses suitable for phishing,
plus a labelled list of which accounts hold `ROLE_ADMIN` — exactly the reconnaissance
needed to turn finding 1 into an account takeover.

**The strongest evidence this is unintended is in the same controller.**
`/api/users/all` already carries `@PreAuthorize("isAuthenticated()")` with the
comment *"to prevent anonymous enumeration of the full user list and email
addresses."* That control is real and enforced — but the paginated sibling endpoint
returns identical objects with no authentication at all, so it is trivially bypassed.

**Fix — apply both, they are independent:**

1. Remove `email` from the public `UserResponseDto` path. Add a separate
   authenticated-only projection for `/api/users/profile` and `/api/auth/me`, which
   are the only callers that legitimately need it.
2. Narrow the matcher at `SecurityConfig.java:133` so the directory requires
   authentication, keeping only genuinely public reads (the leaderboard) open.

Fix 1 matters more: it protects the data even if a route matcher is loosened again
later.

---

## 3. Unpublished blog drafts readable by anyone — MEDIUM

**Where:** `backend/.../blog/service/BlogService.java:68` and `:82`

The list endpoint correctly filters on publication state
(`blogRepository.findByPublishedTrue(pageable)`), but both single-item lookups
ignore it:

```java
BlogPost post = blogRepository.findById(id)          // :68
BlogPost post = blogRepository.findBySlug(slug)      // :82
```

Both are reachable anonymously via `GET /api/blogs/**` `permitAll`, neither
controller method carries a `@PreAuthorize`, and `BlogResponseDto` returns the full
`content` field. Drafts are a supported state — admins can set `published: false` at
create or update time — they are simply not hidden.

**Attack path:** walk `GET /api/blogs/1`, `/2`, `/3`… Any ID absent from the
paginated public listing is a draft, and the request returns its full title, body,
author and tags. Embargoed contest problems, event details, or results are readable
before the club intends to publish them.

**Fix:** switch both lookups to publication-aware finders
(`findByIdAndPublishedTrue`, `findBySlugAndPublishedTrue`) and return 404 otherwise.
If admins need draft preview, add an explicit `hasRole('ADMIN')` branch that falls
back to the unfiltered finder.

---

## Verified as NOT vulnerable

Recorded so these are not re-investigated. Each was traced end to end, not assumed.

- **Ownership checks / IDOR.** `PUT /api/users/{id}/handle` resolves the caller from
  the JWT principal and compares against the path `id`, returning 403 unless they
  match or the caller is an admin. `PUT /api/users/profile` ignores any client-supplied
  ID entirely. The path `id` is never trusted on its own.
  *(Note: an earlier review reported an IDOR here in error, having grepped only the
  controller and missed the check. The guard is real.)*
- **DTO boundary.** Every controller return path was traced. All return records, never
  a JPA entity. `User.password` has no route into a response body, and
  `UserDetailsImpl.password` is additionally `@JsonIgnore`.
- **Privilege escalation via registration.** `RegisterRequest` has no `role` field and
  `AuthService.registerUser` hardcodes `ROLE_USER`.
- **Admin endpoints.** Role changes, user deletion, blog writes and manual CF sync all
  carry `@PreAuthorize("hasRole('ADMIN')")`, and `@EnableMethodSecurity` is active, so
  method security correctly overrides the broad `permitAll` matcher.
- **Password hashing.** BCrypt with per-hash salt, wired through
  `DaoAuthenticationProvider`. Plaintext is never persisted or logged.
- **SQL / JPQL injection.** No string-concatenated queries. The one search query binds
  a named parameter; everything else is a derived query method. Sort keys are hardcoded
  literals, so there is no sort-injection either.
- **SSRF in the Codeforces sync.** The handle lands in the query string of a constant
  `https://codeforces.com` base. Host and protocol are fixed and unreachable from the
  injected value. The Next.js proxies also use a fixed host with `encodeURIComponent`.
- **Frontend XSS.** No `dangerouslySetInnerHTML`, `eval`, `new Function`, `innerHTML`,
  `document.write`, or `javascript:` anywhere in `frontend/src`. All rendering goes
  through React's escaping.
- **Cross-request token leakage in Server Components.** `lib/axios.ts` is imported at
  module scope by `lib/services/dashboard.ts`, so the Zustand store does instantiate
  per Node process. But the request interceptor only *reads* the token (never set
  server-side), the 401 handler's `logout()` is gated behind `typeof window`, and
  `localStorage` throws on the server — zustand catches it and `persist` short-circuits
  every read and write. No session state can follow the next request.
- **User enumeration on login.** `DaoAuthenticationProvider` hides the distinction and
  the handler returns a fixed *"Invalid email or password!"*. No oracle.
- **CORS.** Explicit origin allowlist from `CORS_ALLOWED_ORIGINS`; no wildcard is ever
  paired with `allowCredentials(true)`.
- **Committed credentials.** The only committed database credentials are the local dev
  Postgres pair, fully overridden by env vars in `application-prod.yml`. CI workflows
  contain no secrets.
- **Swagger in production.** Disabled in `application-prod.yml`, so the `permitAll` on
  the Swagger routes has no production effect.

---

## Noted but not raised as vulnerabilities

**Token in `localStorage`.** `frontend/src/store/auth.ts` now uses zustand `persist`
with `localStorage`. This is only exploitable given an XSS sink, and none exists in
the codebase, so it does not meet the bar for a finding. httpOnly cookies would be
strictly better and it is worth revisiting on its own merits.

The urgent part is the docblock directly above that config, which still reads
*"The token is held in memory only… That keeps it out of localStorage and away from
XSS."* That is now the opposite of what the code does and will mislead the next
reviewer. Correct it regardless of which storage strategy you settle on.

**`GlobalExceptionHandler` echoes `ex.getMessage()`** into 500 response bodies. A
genuine CWE-209 information-disclosure weakness, but the reachable messages disclose
little of value, so it is listed as cheap hardening rather than a vulnerability:
return a static message and log the detail server-side.

---

## Recommended order of work

1. Remove the JWT secret fallback and rotate the production value. *(minutes)*
2. Strip `email` from the public user DTO path. *(small)*
3. Narrow the `GET /api/users/**` matcher. *(one line)*
4. Make the blog single-item lookups publication-aware. *(small)*
5. Fix the misleading `store/auth.ts` docblock. *(one comment)*
6. Return a static message from the global exception handler. *(one line)*
