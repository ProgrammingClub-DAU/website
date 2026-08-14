# Frontend Audit

**Date:** 11 August 2026
**Commit:** `64b5237`
**Scope:** `frontend/` — App Router correctness, React, state, accessibility, performance

Security (token storage, XSS, auth) and backend concerns (API contracts, pagination,
N+1) are covered in [SECURITY-AUDIT.md](SECURITY-AUDIT.md) and
[BACKEND-AUDIT.md](BACKEND-AUDIT.md) and are not repeated here.

**Build: passes.** `npm run lint` — 1 warning. `npx tsc --noEmit` — clean.
`npm run build` — exit 0, 5.9s.

---

## Verdict up front

**Not production-ready.** The static half of the site is genuinely good. The newer
data-driven layer — `/members`, `/leaderboard`, `/profile/[id]` — is **broken in
production**, and all three failures share one root cause: API responses are asserted
with `any` and hand-written types rather than validated, so three separate contract
mismatches compiled cleanly and shipped.

TypeScript certified every one of these as safe.

---

## Critical — broken for users

### C1. `/members` renders zero members

`lib/services/dashboard.ts:12-25` never sets `clubRoleCategory`, but
`members-directory.tsx:44-54` buckets members **solely** by that field. Every bucket is
empty.

Measured with 3 members loaded from the API: the page shows `SHOWING 3 OF 3 MEMBERS`,
then four section headings, then **zero member cards**.

**Fix:** derive `clubRoleCategory` in the mapper, or add an "everyone else" bucket.

### C2. `/leaderboard` blanks the entire document on a search miss

`leaderboard-dashboard.tsx:57`

```ts
e.codeforcesHandle.toLowerCase().includes(query);
```

`codeforcesHandle` is nullable on the backend, and the leaderboard returns all users
including those with no handle. An empty query short-circuits on `e.name`, which is why
the page looks fine on load — type one character that doesn't match a name and it throws.

Measured in production build: typing `zzz` produced
`Cannot read properties of null (reading 'toLowerCase')` and
`document.body.innerText.length === 0` — navbar, content and footer all gone.

**Fix:** `(e.codeforcesHandle ?? "").toLowerCase()`. Note `entry.rating` is also nullable,
so `b.rating - a.rating` at line 85 yields `NaN`.

*(`members-directory.tsx:37` guards this correctly — the same bug was already solved
one file away.)*

### C3. Every leaderboard row links to `/profile/undefined`

`lib/services/dashboard.ts:30` reads `id: user.id`, but the backend DTO exposes
**`userId`**:

```java
public record LeaderboardResponseDto(
        int rank,
        Long userId,      // <- not `id`
        String name, String codeforcesHandle, Integer rating, String tier)
```

Measured: the only `/profile*` hrefs on the page are `/profile` and `/profile/undefined`.
`key={entry.id}` also gives every row the same `undefined` key. `email`, `role` and
`createdAt` are likewise absent from that DTO and silently `undefined`.

**Fix:** `id: user.userId`.

### C4. Fabricated club figures render as fact

`FRONTEND.md:83-98` states *"Nothing on this site states a club fact that hasn't been
confirmed"* and defines four markers. These bypass it entirely:

- `members-directory.tsx:71,73` — `totalParticipants = 818`, `totalEvents = 20`,
  `totalContests = 14`. Rendered output: **"818 Participants · 3 Official Members ·
  20 Events · 14 Contests"**.
- `leaderboard-dashboard.tsx:99` — `const contests = 20; // Dynamic count of club
  contests held` — it is not dynamic, and sits inside a `useMemo` whose comment claims
  the values are "computed dynamically from entries".
- `lib/content/members.ts:429-433` — renders "N. Desai", "R. Shah", "K. Verma" as the
  site's real developers with no marker. `members.ts` contains **zero** markers, though
  `FRONTEND.md:92` says that is where `[SAMPLE]` lives — so the documented pre-launch
  grep passes while the file ships invented names about real people.

For contrast, marker discipline held everywhere Role 1 owns: 28 markers in `events.ts`,
24 in `hall-of-fame.ts`, 7 in `blog.ts`, 4 in `home.ts`.

### C5. "🔥 NaN activities" and a falsely-crowned "Most Active Member"

`leaderboard-dashboard.tsx:317` reads `solvedCount` / `yearlyActivityCount`, neither of
which the mapper produces — so `undefined * 30`. Measured on the live page:
**"🔥 NaN activities"**. The sort at lines 106-108 compares `0 - 0` for everyone, so
"Most Active Member of the Year" is simply whichever member the API returned first — a
false claim about a named person. Club Stats shows **"0 Problems Solved", "0 Active This
Year"** for the same reason.

---

## Important

**I1 — No `error.tsx` anywhere.** C2 is the proof: one null field wipes the whole
document rather than one panel. A single `app/error.tsx` would keep the shell alive and
offer a retry. `global-error.tsx` is absent too.

**I2 — No `loading.tsx`; measured 8.2s of blank screen.** Both data routes `await` an
axios call before emitting any HTML. Against a backend taking 8s: cold navigation gives
first byte at **8.21s** with `visible_chars = 0` — a blank tab, not even the navbar.
Client-side navigation stayed on the previous page for **8.5s** with no spinner at all;
users will assume the link is dead. The deploy target is Render, whose free tier sleeps
and takes 30–60s to wake. **Highest value-per-effort fix in this audit.**

**I3 — axios has no `timeout`.** `lib/axios.ts:16-21`, default `0` = infinite. A hung
backend hangs the RSC render until the gateway kills it, then I1 applies.

**I4 — `next/image` cannot load Codeforces avatars.** `next.config.ts` is empty — no
`images.remotePatterns`. Measured: `/_next/image?url=…userpic.codeforces.org…` → **400
"url" parameter is not allowed**. Severity differs by environment: production shows a
broken image; `next dev` **hard-throws and crashes the profile page** for any user with a
linked handle.

**I5 — Unknown profile id returns 200 and tells a signed-in user to log in.** Measured at
`/profile/99999`: HTTP **200**, body reads `PLEASE LOG IN TO VIEW YOUR PROFILE.` Wrong
status, wrong message, invisible to search engines as a 404. The client fetch only
`console.error`s, leaving `profile === null`.

**I6 — Dashboard section titles are not headings.** `ui/card.tsx:36` renders `CardTitle`
as a `<div>`. "Club Stats", "Contest Rating History", "Account Details" etc. are invisible
to heading navigation — `/profile/1` exposes exactly **one** heading total.

**I7 — Heading order skips a level.** `/leaderboard`: `H1 → H3 → H4 → H3`, no `h2`
exists. `/members` emits `H3` (the CTA) before `H2`.

**I8 — Dashboard filters aren't announced; search boxes unlabelled.** All tab and filter
buttons have `aria-pressed: null` and no `aria-label`; both search inputs have zero
labels. **This is a regression against work already in the repo** —
`components/site/filter-chips.tsx` solves exactly this with `role="group"` +
`aria-pressed` + proper focus rings, and is used by three other pages. The two new
dashboards hand-rolled their own buttons instead.

**I9 — Light theme fails WCAG AA on the Codeforces colours, used as text:**

| Token | on background | on surface-2 |
| --- | --- | --- |
| `--cf-master` | 3.19 ✗ | 2.84 ✗ |
| `--cf-pupil` | 3.47 ✗ | 3.10 ✗ |
| `--cf-newbie` | 4.38 ✗ | 3.91 ✗ |
| `--cf-specialist` | 4.39 ✗ | 3.92 ✗ |
| `--primary` | 4.70 ✓ | 4.20 ✗ |

Used for member names at 16px and rank labels at 9–10px. The dark theme (the designed
default) passes throughout — the light theme and the newer coloured-text usage were never
checked.

**I10 — recharts (104 kB gzip) eagerly bundled into `/profile/[id]`**, though the chart
only renders when history exists. `react-activity-calendar` on the same page is already
dynamically imported — copy that pattern and the route drops from 288 kB to ~185 kB.

**I11 — zod costs 73 kB gzip on `/login` and `/register`** for four fields, on the two
routes a first-time visitor is most likely to land on.

**I12 — Every member renders as a grey Newbie.** `dashboard.ts:20` hardcodes
`cf: "newbie"` with a comment claiming it is "resolved dynamically by UI" — it isn't.
Same function: `batch: ""` renders a trailing `B.Tech ICT • `, `about: ""` renders an
empty paragraph, and `role` passes the raw enum through so badges read **`ROLE_USER`**.
Currently masked by C1; surfaces the moment C1 is fixed.

**I13 — Member cards link to the wrong profile.** `members-directory.tsx:371` hardcodes
`href="/profile"`, sending every visitor to their own profile — or to `/login`.

**I14 — API shapes asserted, never validated.** Three `no-explicit-any` suppressions on
the mappers, and `types/api.ts:87` declares `codeforcesHandle: string` where the backend
sends `null`. **This is the root cause of C1, C2 and C3.** Zod is already a dependency —
a parse at the service boundary would have caught all three at the point of failure.

**I15 — `<SampleBadge />` renders above live API data** (`(dashboard)/members/page.tsx:41`)
— the inverse of C4, equally misleading.

---

## Minor

- `store/auth.ts:6-9` — comment says the token is "held in memory only… away from XSS";
  lines 87-90 use `persist` + `localStorage`. The comment describes code that no longer
  exists.
- `(dashboard)/profile/[id]/page.tsx:13-19` — comment claims server-side fetching; it
  fetches nothing. The public profile *could* be server-rendered (better LCP, working
  metadata) with only the owner-edit path client-side.
- `dashboard.ts:7` — `IS_MOCK = false` leaves eight unreachable branches while
  `mock-dashboards.ts` and a 425-line array stay imported at module scope.
  `generateMockActivityData` uses `Math.random()` — a hydration mismatch waiting for
  whoever flips the flag.
- `profile-dashboard.tsx` — 8 lint suppressions keeping ~120 lines of Phase-2 scaffolding
  compiling; two `useMemo`s compute values nothing renders.
- `components/ui/badge.tsx` unused — `FRONTEND.md:159` kept it for Roles 2/3, both of
  which have now landed without it.
- `package.json:24` — `shadcn` is a runtime dependency but only used at build time.
- `footer.tsx:20` — comment says the leaderboard link is "intentionally absent until Role
  3 builds `/leaderboard`". The route exists and is in `navItems`; the link is still
  missing.
- `footer.tsx:77` — `new Date().getFullYear()` is baked at build time on 9 static routes
  but per-request on the 2 dynamic ones, so the copyright year can differ across the site.
- Auth pages have **no `h1` at mobile widths** — the only `h1` is in a `hidden lg:flex`
  column.
- `members-directory.tsx:154` — `key={m.name}`; two members sharing a name collide.
- Saving an invalid CF handle succeeds silently; errors are only `console.error`ed.
- No `opengraph-image`, `robots.ts` or `sitemap.ts`.

---

## Performance

Next 16 + Turbopack prints no First Load JS table; recovered from
`.next/diagnostics/route-bundle-stats.json`:

| Route | gzip |
| --- | --- |
| `/profile/[id]` | **288.5 kB** |
| `/register` | 253.7 kB |
| `/login` | 253.6 kB |
| `/leaderboard` | 170.3 kB |
| `/members` | 170.5 kB |
| shared baseline | **161.0 kB** |

161 kB ships on every route including `/about`, which renders text that never changes —
that is the framework floor here, not a defect. The outliers are recharts on
`/profile/[id]` (I10) and zod on the auth routes (I11).

Fonts are optimal (self-hosted, preloaded, `swap`, variable-based). Images set explicit
dimensions everywhere, so no CLS. Caching decisions are correct throughout.

---

## What's genuinely good

Worth not regressing:

- **`lib/axios.ts`** — the SSR-singleton hazard is identified *and* handled correctly:
  `getState()` reads per-request, the 401 logout is gated on `typeof window`. The most
  careful code in the repo.
- **`force-dynamic` on both data routes** — the "live leaderboard baked at build time"
  trap was avoided, confirmed by the build output.
- **`api/cf/*` handlers** use `fetch` with `revalidate: 3600` (so Next's Data Cache
  applies, which it wouldn't with axios), validate params, and return 400/502 rather than
  throwing.
- **`prefers-reduced-motion` measured, not assumed** — every animated element collapses
  to `1e-05s`; nothing escapes.
- **Zero horizontal overflow at 375px** on all 10 routes.
- **Dark theme passes AA throughout** — `fg-muted` 6.13:1, `fg-subtle` 4.95:1, every rank
  colour 5.39–9.77:1.
- **`navbar.tsx:46-55`** — the `isMounted` guard genuinely prevents the persisted-store
  hydration mismatch. Zero hydration warnings across every page.
- **Auth forms** — full labelling, `aria-invalid`, `aria-describedby`, `role="alert"`,
  correct `autoComplete`. Best accessibility in the codebase.
- **`filter-chips.tsx`** — a correct, reusable pattern that already exists (see I8).
- **`timeline.tsx`** — `:has()`-driven with no React state, symmetric listener cleanup,
  `(hover: hover)` guard keeping touch and pointer drivers exclusive.

---

## Recommended order of work

1. **C1, C2, C3** — three small mapper/guard fixes. Restores all three dynamic routes.
2. **I2 + I1** — add `loading.tsx` and `error.tsx`. Two new files that would have
   contained the blast radius of every critical above.
3. **C4, C5** — source the club figures or mark them `[TBC]`; remove the fabricated
   developer credits. This is a real organisation's public site.
4. **I14** — Zod-parse the three mappers at the service boundary. Stops this whole class
   of bug recurring.
5. **I3, I4** — axios timeout, `images.remotePatterns`.
6. **I6, I7, I8** — headings and filter accessibility; reuse `FilterChips`.
7. **I9** — darken light-mode CF tokens, or reserve them for dots.
8. **I10, I11** — dynamic-import recharts, trim zod.

---

## Assessment

**Not production-ready**, but the gap is narrow and concentrated.

The static, Role-1 half is careful work: verified accessibility, measured responsiveness,
honest placeholder discipline, correct rendering decisions. The failures sit almost
entirely in the newer data layer and share a single root cause — unvalidated API
assertions — which is why three separate contract mismatches shipped without a compiler
complaint.

C1–C5 are each localised, probably an afternoon in total. `loading.tsx` and `error.tsx`
are two files that would have contained every one of them. Validating the mappers is the
change that stops it happening again.
