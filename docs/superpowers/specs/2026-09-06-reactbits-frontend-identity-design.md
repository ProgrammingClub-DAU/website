# React Bits frontend identity — design

**Date:** 2026-09-06
**Status:** approved, ready for planning

## Problem

The site reads as competent and generic. Its visual language is borrowed from
Linear — restrained, typographic, one accent colour — and every heading on every
page is flat single-colour text. Nothing about it says *competitive programming
club* rather than *any software project*.

The goal is a frontend with its own identity, built by integrating React Bits
components rather than hand-rolling equivalents, without turning the site into a
components showcase or making the pages members use every day slower.

## The core idea

**The club gradient is derived from the Codeforces rating ladder.**

Those colours already exist as tokens in `globals.css`, in both themes, and they
are the language members actually live in — grey, green, cyan, blue, purple,
orange, red, in order of progression.

```css
--club-gradient: linear-gradient(100deg,
  var(--cf-specialist), var(--cf-expert),
  var(--cf-candidate),  var(--cf-master));
```

One token feeds everything: gradient headings, the Aurora colour stops, the
Particles colour, the BorderGlow glow. That is what makes the result cohesive
rather than a demo reel, and it is the part no other club site has — a palette
that means progression rather than one picked for looking nice.

Defining it once also means the identity can be retuned in a single place.

## Scope of visual change

Public-facing pages (Home, About, Events, Hall of Fame, Gallery) carry the
visual character. Pages members work in (Leaderboard, Members, Profile) stay
quiet and fast: they are loaded repeatedly, and a background animation behind a
table is battery cost with no benefit.

Gradient headings are the exception — cheap enough to apply everywhere, and they
are what ties the site together.

## Component placement

| Location | Component | Technology | New dependency |
| --- | --- | --- | --- |
| Home hero background | Aurora | WebGL | `ogl` |
| Home club wordmark | ParticleText | Canvas 2D | none |
| Home stat tiles (×4) | Counter | DOM | `motion` |
| Login + Register left panel | Particles | WebGL | `ogl` (shared) |
| Hall of Fame + Events cards | BorderGlow | CSS | none |
| Gallery | DomeGallery | CSS 3D | done, shipped |
| Every page `h1` | GradientText | DOM | `motion` (shared) |

Three new dependencies total: `ogl`, `motion`, and `@use-gesture/react` (already
installed with the gallery).

**Rule: at most one WebGL context per page, and never two on the same page.**
Aurora on Home, Particles on auth. Each opens a GPU context with a render loop
that does not stop on its own; browsers also cap simultaneous contexts and
silently discard the oldest when exceeded.

Particles is placed on the auth panel specifically because that panel already
shows a "RANK TRACK" row of rank-coloured dots. Particles in the same colours
finishes an idea the page already starts.

### Dropped

Grainient, Pixel Blast, Antigravity, Infinite Menu — all WebGL, all redundant
with Aurora or without a home. Aurora's stops will be tuned toward the grainy
gradient look that motivated the Grainient request.

## Footer and navigation

The footer renders on Home only.

It currently holds the only links to the GitHub organisation and "Contact for
joining". Joining is the club's primary conversion goal, so removing that path
from every page but one is a functional regression rather than a style choice.
Both links move into the navbar and the mobile sheet before the footer is
removed elsewhere.

## Engineering guardrails

These are requirements, not preferences. Each addresses a way this class of
component commonly goes wrong.

**Reduced motion stops the animation loop.** The `prefers-reduced-motion` block
in `globals.css` disables CSS animations and transitions; it has no effect on
`requestAnimationFrame`. Aurora, Particles and ParticleText must each check the
media query and stop rendering, not merely slow down.

**Animation pauses off-screen.** An IntersectionObserver stops the loop when the
element leaves the viewport. Without it, Aurora keeps drawing while the reader is
at the bottom of the page.

**WebGL components load dynamically**, `ssr: false`. They measure their container
and read `devicePixelRatio` on mount, and the page's text is the part worth
showing first.

**Gradient text keeps a solid fallback.** `background-clip: text` with
`color: transparent` renders nothing at all where it is unsupported or where the
background fails to paint — invisible headings, not ugly ones. The fallback
colour is set first and the gradient applied over it.

**Both themes are verified.** Every colour comes from a token. No component keeps
its upstream hard-coded hex.

**Headings stay headings.** ParticleText renders a canvas with an `aria-label`,
not an `<h1>`. It is used for the wordmark only. Real heading elements are never
replaced, and the existing heading-order findings in the frontend audit are not
made worse.

## Working with vendored components

React Bits components are copied into `src/components/site/` from the shadcn
registry, not retyped. Local modifications are marked `// local:` so the file can
be re-diffed against upstream when it changes.

Every one of them needs the same three fixes seen already with DomeGallery: a
`"use client"` directive, `CSSProperties` imported rather than referenced through
the React UMD global, and hard-coded colours replaced with tokens.

## Phasing

The work splits into three parts that ship independently. Each is useful alone.

**Phase 1 — identity foundation.** The `--club-gradient` token, GradientText
vendored and applied to page headings, navbar link additions, footer restricted
to Home. Touches every route; no WebGL; the largest visible change for the least
risk.

**Phase 2 — home page.** Aurora behind the hero, ParticleText wordmark, Counter
on the stat tiles.

**Phase 3 — auth and cards.** Particles on the auth panel, BorderGlow on the Hall
of Fame and Events cards.

## Testing

Playwright, in both themes, at 1440/1920/2560 and one mobile width:

- Every route renders with no page errors and no horizontal overflow
- Gradient headings are visible — computed colour is not transparent-on-nothing
- Reduced motion: with the media query emulated, no animation frames are
  scheduled by Aurora, Particles or ParticleText
- Off-screen: scrolling the hero out of view stops Aurora's loop
- At most one `<canvas>` with a WebGL context per page
- The footer is absent everywhere except Home, and the GitHub and joining links
  are reachable from the navbar on every page
- Production build passes; the per-route bundle delta is recorded before and
  after

## Out of scope

Real photos for the gallery, real numbers behind the `[TBC]` stat tiles, and the
open findings in the frontend audit (heading order, contrast, the `any`
suppressions in `dashboard.ts`). Counter will animate whatever the tiles contain;
animating a placeholder is theatre, so the tiles keep their current values until
the data is wired.

## Risks

**Bundle size.** `ogl` and `motion` together are roughly 55 kB gzipped, against an
open audit finding about recharts at 104 kB. Both are dynamically imported and
scoped to the routes that use them; the delta is measured rather than assumed.

**Visual coherence.** The failure mode is a site that looks like a components
gallery. The single derived gradient is the mitigation, and it is the reason the
palette is derived rather than chosen.

**Vendored drift.** Copied components do not receive upstream fixes. Local edits
are marked so a future re-diff is mechanical.

## Amendment: WebGL deferred, then built

*Recorded after Phase 2 and Phase 3 were built. The design above is left as it
was approved; this is what changed and why.*

WebGL was deferred to the end of the identity work rather than ruled out — an
earlier revision of this section said otherwise and was wrong. Aurora and
Particles are now in the slots the table above gives them, and `ogl` is a
dependency. What follows describes the interim, which is still worth keeping:
it is why the guardrails exist and what the substitutes taught.

In the interim both slots used React Bits' **DotField** — Canvas 2D, no
dependency. It has since been removed; nothing references it.

The one-WebGL-context-per-page rule is now load-bearing rather than trivial, and
a test asserts it directly: one context on the home page, one on each auth page,
and zero everywhere else.

Two consequences worth recording.

**The guardrails had to be built, not inherited — by every one of them.**
Upstream Aurora, Particles and DotField have no reduced-motion check and no
off-screen pause at all. Upstream ParticleText honours reduced motion for the
particles — it snaps them and disables the drift and the repel — but
re-schedules its animation frame unconditionally, so it redraws an identical
image for the life of the page. All are fixed locally.

Upstream Particles additionally leaks its WebGL context on unmount, and neither
WebGL component survives a browser without it: the constructor throws, and an
unhandled throw in a client component takes the page it decorates to an error
boundary. Both now fail quietly.

**Colours cannot be tokens.** A canvas has no CSS cascade, so `var(--token)`
reaches `createLinearGradient()` as an unparseable string. The callers read the
tokens with `getComputedStyle` and re-read them on a `MutationObserver` when the
theme provider swaps the class on `<html>`. ParticleText additionally parses its
colours with a 6-digit-hex regex and falls back silently on anything else.

Also changed from the design above:

- The club gradient is three stops, not four. `--cf-master` was dropped: orange
  against the indigo `--primary` read as a separate decoration rather than the
  same identity. The gradient now stays in the cool half of the ladder, which
  is the half `--primary` sits in.
- BorderGlow went on the four full-width panels — the home CTA, About's "How to
  join", the featured event and the featured post — rather than the Hall of Fame
  and Events timeline cards. Those cards are still candidates.
- Buttons were not in the original placement table. Both `Button` variants now
  carry the same gradient edge, which is what ties a button to a heading.
- Counter on the stat tiles remains unbuilt, for the reason already given under
  Out of scope: the tiles read `[TBC]`.

**Aurora fails WCAG AA behind the hero copy at full strength**, which the design
above did not anticipate. Measured with the copy hidden: the heading reached
4.22:1 in the light theme and the muted paragraph 1.06:1, against a 4.5:1 floor.
`--fg-muted` is 4.99:1 on plain white to begin with, so any tint spends its whole
margin. A mask that clears the copy plus a per-theme strength token brings all
four measurements above the floor while leaving the wash clearly visible.

Anything placed behind text from here needs the same measurement. It is not
visible by eye in the dark theme, which is where this kind of background is
usually judged.
