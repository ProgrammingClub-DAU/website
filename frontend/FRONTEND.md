# Frontend

Marketing and informational site for the Programming Club @ DAU — the Role 1
(Frontend UI/UX Architect) slice of Phase 1. Covers the design system, the app
shell, and every static page.

Not in here, by design: authentication, profiles, and the live leaderboard.
Those belong to Roles 2 and 3 and depend on backend APIs that do not exist yet.

> **Open question for the Team Leader.** This branch ships a static
> `/members` directory, but `team_roles.md` assigns the members directory to
> Role 3 and `CLAUDE.md` maps it to `src/app/(dashboard)/`. Route groups do
> not change the URL, so a later `(dashboard)/members/page.tsx` collides with
> `app/members/page.tsx` and Next.js fails the build. Confirm who owns the
> route before Role 3 starts, and update the ownership table either way.

## Running it

```bash
npm install
npm run dev      # localhost:3000
npm run build    # must pass before opening a PR
npm run lint
```

No backend or database is needed. Every page renders from local content files,
so `docker-compose` and the Spring Boot app can stay switched off while working
on the frontend.

## Stack

Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS v4 · shadcn/ui
(`radix-nova` style, `neutral` base — see `components.json`) · `next-themes` ·
`lucide-react` · Geist + Geist Mono.

Tailwind v4 has no `tailwind.config.ts` — the theme lives in CSS, in the
`@theme` block of `src/app/globals.css`.

## Design direction

Modelled on [linear.app](https://linear.app): near-black surfaces, hairline
borders, restrained type, one accent colour. Values are Linear's own measured
tokens rather than approximations — `#08090A` page, `#5E6AD2` accent, 8px
control / 12px panel radii, hairline borders.

The accent appears **only** on primary buttons, links, and focus rings.
Everything else is greyscale, with one exception below.

**Codeforces rank colours are the signature element.** Newbie grey through
Grandmaster red appear as small dots on rank badges and the hero rating card.
It is the one place colour beyond the accent is allowed. Anyone who has done
competitive programming reads it instantly; anyone who hasn't sees a tasteful
dot. It is what stops the site from looking like a generic Linear clone.

Both themes ship. Dark is the default; the toggle sits in the navbar and
persists via `next-themes`.

## Layout

```
src/
  app/
    layout.tsx          root shell — fonts, theme provider, navbar, footer
    page.tsx            home
    about|events|hall-of-fame|blog|members/page.tsx
    not-found.tsx
    globals.css         design tokens, theme, component-layer CSS
  components/
    ui/                 shadcn primitives (button, card, badge, input, sheet, separator)
    site/               composed pieces owned by this project
  lib/
    content/            all page copy and sample data
    site.ts             club name, nav items, external links
    cf-ranks.ts         Codeforces rank colours
    utils.ts            `cn` helper
```

`lib/content/` exists so copy is never buried in JSX. When the backend lands,
a page swaps its content import for a fetch, and nothing else moves.

## Placeholder content

Nothing on this site states a club fact that hasn't been confirmed. This is
deliberate: it is a real club's public site, so invented ICPC results, member
counts, or alumni names would be false claims about real people.

Four markers are in use — grep for all four before launch:

| Marker | Means | Where |
| --- | --- | --- |
| `[PLACEHOLDER]` | Copy to be written or confirmed | most content files |
| `[SAMPLE]` | Illustrative row, not a real person | `members.ts`, `blog.ts` |
| `[TBC]` | Figure to confirm against club records | `home.ts` stats, `events.ts` |
| `[DATE]` | Real date to fill in | `blog.ts` |

```bash
grep -rnE "\[(PLACEHOLDER|SAMPLE|TBC|DATE)\]" src/lib/content src/lib/site.ts
```

Two links are deliberately unset rather than guessed: the Codeforces group URL
(`site.ts` — a group lives at `/group/<id>`, and the old handle predates the
DAU rename) and the joining contact. Both render as placeholder text in the
footer until confirmed.

`about.ts` also contains operational statements — meeting cadence, "C++ is the
most common in the club", "no fee and no selection". They read as facts, so
have the core team confirm them even though they carry no marker.

## Notes worth keeping

**Timeline connectors** (Events, Hall of Fame). Branch curves are inline SVG
using `preserveAspectRatio="none"`, so a single path stretches to any rail
width, with `vector-effect="non-scaling-stroke"` keeping the line crisp.
Nothing round goes inside that SVG — non-uniform scaling would turn a circle
into an ellipse.

The hover pulse rides a single `offset-path` built in real pixel coordinates on
hover, covering the whole route: year head → down the spine → out along the
curve. Two things forced that design:

- Per-segment animations make dots appear mid-line instead of flowing from the
  head, and cannot read as one continuous stream.
- Driving a dot with `stroke-dasharray` maths on a `preserveAspectRatio="none"`
  path is unreliable — the same combination GSAP's DrawSVG and anime.js both
  warn about, because path length cannot be measured under non-uniform scale.

Pulse duration scales with the square root of the run length. A fixed duration
made distant cards' dots travel proportionally faster; the square root lets
speed rise gently instead.

**CSS layers.** Tailwind utilities outrank the `components` layer, so a
`bg-*` utility silently beats a hover rule written in `@layer components`.
Spine colours are therefore set in the component rule, not via a utility class.

**Hover is decoration.** All of it is `:has()`-driven CSS with no React state,
so hovering never re-renders. Touch devices get the static layout, and
`prefers-reduced-motion` disables the animation entirely.

## Accessibility and responsiveness

Verified down to 375px on every page — no horizontal overflow at 375 / 768 /
1280. Navbar collapses into a shadcn `Sheet`. Focus rings are visible on every
interactive element, decorative SVG is `aria-hidden`, and reduced motion is
respected.

## Handing over to Roles 2 and 3

- The navbar's auth area is a single slot — drop real state in without touching
  layout.
- **`/login` is linked but does not exist** (navbar, footer, every join CTA).
  Building it clears every dead link on the site.
- `/leaderboard` is not linked anywhere on purpose — a footer link to a 404 is
  worse than no link. Add it back once the route exists.
- Blog posts render as `<article>`, not links, because per-post routes don't
  exist. Give `Post` an `href` and restore the `Link` wrapper in
  `blog-list.tsx` when they do.
- Leaderboard, profile, and members data should replace the matching
  `lib/content/` module, keeping the shape the components already consume.
- `ui/badge.tsx`, `ui/card.tsx`, and `ui/separator.tsx` are installed but
  unused — kept deliberately for these tasks.
- Set `NEXT_PUBLIC_SITE_URL` in the Vercel environment so `metadataBase`
  resolves absolute share-card URLs.

## Known gaps

- No OG share image (`opengraph-image.tsx`) — metadata is wired, the asset is
  not.
- Three of the four empty states (blog tags, Hall of Fame years, event types)
  cannot trigger today, because each filter list is derived from the data it
  filters. They exist for real data; treat them as untested.
- `enableSystem={false}`, so a first-time visitor gets dark regardless of
  `prefers-color-scheme`. Deliberate — flip it in `layout.tsx` if the club
  would rather follow the OS.
