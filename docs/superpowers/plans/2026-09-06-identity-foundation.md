# Identity Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the site a single club gradient derived from the Codeforces rating ladder, apply it to every page heading, move the joining links into the navbar, and restrict the footer to the home page.

**Architecture:** One CSS custom property, `--club-gradient-stops`, defined per theme in `globals.css` from the existing `--cf-*` rank tokens. A vendored, locally-adapted React Bits `GradientText` reads those stops. A new `PageTitle` primitive wraps the repeated `<h1>` markup so the treatment is applied once rather than in thirteen places.

**Prerequisite:** the dev server must be running (`cd frontend && npm run dev`) — every test drives `http://localhost:3000`.

**Tech Stack:** Next.js 16 App Router, React 19, Tailwind CSS v4 (CSS-first `@theme`), TypeScript, `motion` (new), Playwright for verification.

Phase 1 of the design in `docs/superpowers/specs/2026-09-06-reactbits-frontend-identity-design.md`. Phases 2 (Aurora, ParticleText, Counter) and 3 (Particles, BorderGlow) get their own plans once this lands.

## Global Constraints

- Vendored React Bits components live in `frontend/src/components/site/`, are copied from the shadcn registry rather than retyped, and every local modification is marked with a `// local:` comment.
- Every vendored component needs a `"use client"` directive and must import `CSSProperties` from `react` rather than referencing `React.CSSProperties`, which does not compile in a TS module.
- No component keeps an upstream hard-coded colour. All colours resolve from CSS tokens so both themes work.
- Gradient text must keep a solid fallback colour. `background-clip: text` with `color: transparent` renders nothing where the background fails to paint.
- Any animation loop must stop under `prefers-reduced-motion: reduce`. The block in `globals.css` disables CSS animation only and has no effect on `requestAnimationFrame`.
- Heading elements stay heading elements. Nothing replaces an `<h1>` with a `<div>` or a canvas.
- `npm run build` must pass before every commit.

---

### Task 1: Club gradient tokens

**Files:**
- Modify: `frontend/src/app/globals.css` (light theme block near line 146, dark theme block near line 197)

**Interfaces:**
- Consumes: existing `--cf-specialist`, `--cf-expert`, `--cf-candidate`, `--cf-master` tokens, already defined per theme.
- Produces: `--club-gradient-stops` (comma-separated colour list, for JS consumers) and `--club-gradient` (a ready `linear-gradient(...)`, for CSS consumers). Both resolve in light and dark.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_identity.py`:

```python
from playwright.sync_api import sync_playwright

def read_tokens(pg, theme):
    pg.goto("http://localhost:3000/")
    pg.evaluate("(t)=>localStorage.setItem('theme',t)", theme)
    pg.reload(wait_until="networkidle")
    return pg.evaluate("""() => {
        const cs = getComputedStyle(document.documentElement);
        return {
            stops: cs.getPropertyValue('--club-gradient-stops').trim(),
            gradient: cs.getPropertyValue('--club-gradient').trim(),
        };
    }""")

def test_club_gradient_defined_in_both_themes():
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        dark = read_tokens(pg, "dark")
        light = read_tokens(pg, "light")
        b.close()

    for name, tok in (("dark", dark), ("light", light)):
        assert tok["stops"], f"{name}: --club-gradient-stops is empty"
        assert tok["gradient"].startswith("linear-gradient"), f"{name}: {tok['gradient']!r}"
        # Four stops, so three commas separating them.
        assert tok["stops"].count(",") == 3, f"{name}: expected 4 stops, got {tok['stops']!r}"

    # The ladder colours differ per theme, so the gradient must differ too.
    assert dark["stops"] != light["stops"], "gradient does not follow the theme"
```

- [ ] **Step 2: Run test to verify it fails**

Start the dev server first: `cd frontend && npm run dev`

Run: `cd frontend && python -m pytest tests/test_identity.py -v`
Expected: FAIL — `dark: --club-gradient-stops is empty`

- [ ] **Step 3: Write minimal implementation**

In `frontend/src/app/globals.css`, inside the light theme block immediately after `--cf-grandmaster`, add:

```css
  /* The club gradient is the Codeforces rating ladder, not a palette picked for
     looking nice: cyan through blue and purple to orange is the progression
     members are actually climbing. Defined once so every gradient on the site —
     headings, backgrounds, card glows — is the same idea, and retuning the
     identity is a one-line change.

     Stops are exposed separately because JS consumers (React Bits components
     take a colours array) need the list, while CSS consumers want the finished
     gradient. */
  --club-gradient-stops: var(--cf-specialist), var(--cf-expert), var(--cf-candidate), var(--cf-master);
  --club-gradient: linear-gradient(100deg, var(--club-gradient-stops));
```

Add the identical two lines inside the dark theme block, after its own
`--cf-grandmaster`. They reference the same variable names, which resolve to that
theme's rank colours.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && python -m pytest tests/test_identity.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/globals.css frontend/tests/test_identity.py
git commit -m "feat(identity): derive the club gradient from the rating ladder"
```

---

### Task 2: Vendor and adapt GradientText

**Files:**
- Create: `frontend/src/components/site/gradient-text.tsx`
- Modify: `frontend/package.json` (adds `motion`)
- Test: `frontend/tests/test_gradient_text.py`

**Interfaces:**
- Consumes: `--club-gradient-stops` from Task 1.
- Produces: `export default function GradientText({ children, className, colors, animationSpeed, direction }): JSX.Element` — renders an **inline `<span>`**, not a block. Later tasks pass only `children` and rely on the club gradient default.

**Why this needs local edits.** Upstream renders `motion.div` with
`mx-auto flex max-w-fit rounded-[1.25rem] backdrop-blur cursor-pointer`. That is a
centred pill: dropping it inside an `<h1>` centres every heading, caps it to
fit-content, and puts a pointer cursor on non-interactive text. A `<div>` inside
`<h1>` is also invalid — `h1` takes phrasing content. It further sets
`text-transparent` with no fallback and runs `useAnimationFrame` forever.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_gradient_text.py`:

```python
from playwright.sync_api import sync_playwright

def test_gradient_heading_is_inline_visible_and_motion_aware():
    with sync_playwright() as p:
        b = p.chromium.launch()

        pg = b.new_page()
        pg.goto("http://localhost:3000/about", wait_until="networkidle")
        info = pg.evaluate("""() => {
            const el = document.querySelector('h1 [data-gradient-text]');
            if (!el) return null;
            const cs = getComputedStyle(el);
            return {
                tag: el.tagName,
                display: cs.display,
                cursor: cs.cursor,
                image: cs.backgroundImage,
                fallback: el.style.getPropertyValue('--gradient-text-fallback'),
                insideH1: !!el.closest('h1'),
            };
        }""")
        pg.close()

        # Reduced motion must stop the loop, not merely slow it.
        pg2 = b.new_page(reduced_motion="reduce")
        pg2.goto("http://localhost:3000/about", wait_until="networkidle")
        pg2.evaluate("""() => {
            window.__frames = 0;
            const raf = window.requestAnimationFrame;
            window.requestAnimationFrame = (cb) => raf(t => { window.__frames++; return cb(t); });
        }""")
        pg2.wait_for_timeout(1200)
        frames = pg2.evaluate("() => window.__frames")
        pg2.close()
        b.close()

    assert info is not None, "no [data-gradient-text] inside an h1"
    assert info["insideH1"], "gradient text is not inside the heading"
    assert info["tag"] == "SPAN", f"must be a span inside h1, got {info['tag']}"
    assert info["display"] == "inline", f"must render inline, got {info['display']}"
    assert info["cursor"] != "pointer", "non-interactive text must not use a pointer cursor"
    assert "linear-gradient" in info["image"], f"no gradient painted: {info['image']!r}"
    assert info["fallback"], "no solid fallback colour set"
    assert frames < 30, f"animation still running under reduced motion ({frames} frames)"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && python -m pytest tests/test_gradient_text.py -v`
Expected: FAIL — `no [data-gradient-text] inside an h1`

- [ ] **Step 3: Write minimal implementation**

Install the dependency and fetch the upstream source:

```bash
cd frontend
npm install motion
curl -s "https://reactbits.dev/r/GradientText-TS-TW.json" \
  | python -c "import json,sys,io; d=json.load(sys.stdin); io.open('src/components/site/gradient-text.tsx','w',encoding='utf-8',newline='\n').write(d['files'][0]['content'])"
```

Then replace the file's import block, component signature and returned markup so
the file reads as below. Everything else from upstream (the `useAnimationFrame`
body, `backgroundPosition` transform, `gradientStyle`) stays as fetched.

```tsx
"use client";

// Vendored from React Bits (GradientText-TS-TW). Local changes marked `// local:`.
import { useState, useCallback, useEffect, useRef, type ReactNode } from 'react';
import { motion, useMotionValue, useAnimationFrame, useTransform, useReducedMotion } from 'motion/react';

interface GradientTextProps {
  children: ReactNode;
  className?: string;
  colors?: string[];
  animationSpeed?: number;
  direction?: 'horizontal' | 'vertical' | 'diagonal';
  pauseOnHover?: boolean;
  yoyo?: boolean;
}

// local: defaults to the club gradient rather than upstream's purple/pink.
// var() references resolve per theme, so this follows the light/dark switch.
const CLUB_STOPS = [
  'var(--cf-specialist)',
  'var(--cf-expert)',
  'var(--cf-candidate)',
  'var(--cf-master)',
];

export default function GradientText({
  children,
  className = '',
  colors = CLUB_STOPS,
  animationSpeed = 8,
  direction = 'horizontal',
  pauseOnHover = false,
  yoyo = true
}: GradientTextProps) {
  const [isPaused, setIsPaused] = useState(false);
  const progress = useMotionValue(0);
  const elapsedRef = useRef(0);
  const lastTimeRef = useRef<number | null>(null);
  // local: the CSS reduced-motion block cannot stop a rAF loop, so the component
  // has to opt out itself. The gradient still paints; it simply stops moving.
  const reduceMotion = useReducedMotion();

  const animationDuration = animationSpeed * 1000;

  useAnimationFrame(time => {
    if (isPaused || reduceMotion) {   // local: added reduceMotion
      lastTimeRef.current = null;
      return;
    }
    if (lastTimeRef.current === null) {
      lastTimeRef.current = time;
      return;
    }
    const deltaTime = time - lastTimeRef.current;
    lastTimeRef.current = time;
    elapsedRef.current += deltaTime;

    if (yoyo) {
      const fullCycle = animationDuration * 2;
      const cycleTime = elapsedRef.current % fullCycle;
      if (cycleTime < animationDuration) {
        progress.set((cycleTime / animationDuration) * 100);
      } else {
        progress.set(100 - ((cycleTime - animationDuration) / animationDuration) * 100);
      }
    } else {
      progress.set((elapsedRef.current / animationDuration) * 100);
    }
  });

  useEffect(() => {
    elapsedRef.current = 0;
    progress.set(0);
  }, [animationSpeed, yoyo, progress]);

  const backgroundPosition = useTransform(progress, p =>
    direction === 'vertical' ? `50% ${p}%` : `${p}% 50%`
  );

  const handleMouseEnter = useCallback(() => { if (pauseOnHover) setIsPaused(true); }, [pauseOnHover]);
  const handleMouseLeave = useCallback(() => { if (pauseOnHover) setIsPaused(false); }, [pauseOnHover]);

  const gradientAngle =
    direction === 'horizontal' ? 'to right' : direction === 'vertical' ? 'to bottom' : 'to bottom right';
  const gradientColors = [...colors, colors[0]].join(', ');

  const gradientStyle = {
    backgroundImage: `linear-gradient(${gradientAngle}, ${gradientColors})`,
    backgroundSize: direction === 'vertical' ? '100% 300%' : '300% 100%',
    backgroundRepeat: 'repeat' as const,
  };

  // local: a span that renders inline, so it can sit inside an h1 without
  // changing the heading's layout. Upstream returned a centred, fit-content,
  // pointer-cursor pill wrapped in a div, which is invalid inside an h1 and
  // would have centred every page title.
  //
  // local: `color` is set before the gradient and left as the painted value if
  // background-clip:text is unsupported or the image fails to paint. Without it
  // the text is transparent over nothing — invisible headings rather than
  // unstyled ones.
  return (
    <motion.span
      data-gradient-text=""
      className={`inline bg-clip-text text-transparent ${className}`}
      style={{
        ...gradientStyle,
        backgroundPosition,
        WebkitBackgroundClip: 'text',
        color: 'transparent',
        ['--gradient-text-fallback' as string]: 'var(--foreground)',
      }}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
    </motion.span>
  );
}
```

Add the supporting rule to `frontend/src/app/globals.css`:

```css
/* Restores a visible colour where background-clip: text is unsupported. The
   @supports block only applies the transparent fill when clipping works, so a
   failure leaves readable text instead of nothing. */
[data-gradient-text] {
  color: var(--gradient-text-fallback, var(--foreground));
}

@supports (background-clip: text) or (-webkit-background-clip: text) {
  [data-gradient-text] {
    color: transparent;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

The test reads `/about`, so Task 3 must supply the heading. Run this task's build
check now and the test at the end of Task 3:

Run: `cd frontend && npx tsc --noEmit && npm run build`
Expected: no type errors; build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/site/gradient-text.tsx frontend/src/app/globals.css \
        frontend/package.json frontend/package-lock.json frontend/tests/test_gradient_text.py
git commit -m "feat(identity): vendor GradientText, adapted for headings"
```

---

### Task 3: PageTitle primitive and heading rollout

**Files:**
- Modify: `frontend/src/components/site/primitives.tsx`
- Modify: `frontend/src/app/about/page.tsx:25`, `blog/page.tsx:22`, `events/page.tsx:20`, `gallery/page.tsx:18`, `hall-of-fame/page.tsx:20`, `(dashboard)/leaderboard/page.tsx:32`, `(dashboard)/members/page.tsx:36`, `(dashboard)/profile/[id]/page.tsx:27`
- Test: `frontend/tests/test_gradient_text.py` (from Task 2), `frontend/tests/test_headings.py`

**Interfaces:**
- Consumes: `GradientText` from Task 2.
- Produces: `export function PageTitle({ children, className }: { children: ReactNode; className?: string })` — renders `<h1>` with the shared title classes and the gradient applied to its text.

Eight pages repeat the same `<h1>` class string. Wrapping each by hand would
mean eight copies of the same decision. One primitive keeps it in a single place
and is what later phases extend.

The home hero (`page.tsx:170`), the two auth headings, `error.tsx` and
`not-found.tsx` use different sizing and are left alone in this task. Home is
Phase 2's subject; error states should not be decorative.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_headings.py`:

```python
import pytest
from playwright.sync_api import sync_playwright

ROUTES = ["/about", "/blog", "/events", "/gallery", "/hall-of-fame",
          "/leaderboard", "/members"]

def test_every_page_has_exactly_one_gradient_h1():
    problems = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        for route in ROUTES:
            pg.goto(f"http://localhost:3000{route}", wait_until="networkidle")
            found = pg.evaluate("""() => {
                const h1s = [...document.querySelectorAll('h1')];
                return {
                    count: h1s.length,
                    gradient: h1s.filter(h => h.querySelector('[data-gradient-text]')).length,
                    text: (h1s[0]?.innerText || '').trim(),
                };
            }""")
            if found["count"] != 1:
                problems.append(f"{route}: expected 1 h1, found {found['count']}")
            if found["gradient"] != 1:
                problems.append(f"{route}: h1 has no gradient text")
            if not found["text"]:
                problems.append(f"{route}: h1 renders no text")
        b.close()
    assert not problems, "\n".join(problems)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && python -m pytest tests/test_headings.py -v`
Expected: FAIL — `/about: h1 has no gradient text`

- [ ] **Step 3: Write minimal implementation**

Append to `frontend/src/components/site/primitives.tsx`:

```tsx
/**
 * The heading every content page opens with.
 *
 * Eight pages repeated the same class string and would otherwise each need the
 * gradient applied by hand. Keeping it here means the treatment is one decision,
 * and the element stays a real `h1` — the gradient is applied to the text inside
 * it, never by replacing the heading.
 */
export function PageTitle({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <h1
      className={cn(
        "mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance",
        className
      )}
    >
      <GradientText>{children}</GradientText>
    </h1>
  );
}
```

Add the import at the top of `primitives.tsx`:

```tsx
import GradientText from "@/components/site/gradient-text";
```

Then in each of the eight pages, replace the `<h1 …>…</h1>` block with
`<PageTitle>…</PageTitle>`, preserving any width class. For example, in
`frontend/src/app/about/page.tsx`, replace:

```tsx
        <h1 className="mt-6 max-w-[22ch] text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
          Built by students who like hard problems.
        </h1>
```

with:

```tsx
        <PageTitle className="max-w-[22ch]">
          Built by students who like hard problems.
        </PageTitle>
```

Keep each page's existing heading text exactly as it is. Add `PageTitle` to the
existing `@/components/site/primitives` import in each file. `leaderboard/page.tsx`
uses `mt-4` rather than `mt-6`; pass `className="mt-4"` there.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && python -m pytest tests/test_headings.py tests/test_gradient_text.py -v`
Expected: PASS (both files)

Run: `cd frontend && npm run build`
Expected: build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/site/primitives.tsx frontend/src/app frontend/tests/test_headings.py
git commit -m "feat(identity): apply the club gradient to page headings"
```

---

### Task 4: Move the joining links into the navbar

**Files:**
- Modify: `frontend/src/lib/site.ts`
- Modify: `frontend/src/components/site/navbar.tsx`
- Test: `frontend/tests/test_nav_links.py`

**Interfaces:**
- Consumes: the existing `site` object in `frontend/src/lib/site.ts`.
- Produces: `export const utilityLinks: readonly { href: string; label: string; external: boolean }[]` — consumed by the navbar's desktop row and mobile sheet.

This must land **before** Task 5. The footer currently holds the only links to
the GitHub organisation and to joining; removing it first would break both.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_nav_links.py`:

```python
from playwright.sync_api import sync_playwright

ROUTES = ["/", "/about", "/events", "/leaderboard", "/members", "/gallery"]

def test_joining_links_reachable_from_every_page():
    problems = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1440, "height": 900})
        for route in ROUTES:
            pg.goto(f"http://localhost:3000{route}", wait_until="networkidle")
            hrefs = pg.evaluate(
                "() => [...document.querySelectorAll('header a')].map(a => a.getAttribute('href') || '')"
            )
            joined = " ".join(hrefs)
            if "github.com" not in joined:
                problems.append(f"{route}: no GitHub link in the header")
            if "mailto:" not in joined and "/register" not in joined:
                problems.append(f"{route}: no joining link in the header")
        b.close()
    assert not problems, "\n".join(problems)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && python -m pytest tests/test_nav_links.py -v`
Expected: FAIL — `/: no GitHub link in the header`

- [ ] **Step 3: Write minimal implementation**

In `frontend/src/lib/site.ts`, after the existing `navItems` array, add:

```ts
/**
 * Links that used to live only in the footer.
 *
 * The footer now renders on the home page alone, and joining is the club's whole
 * conversion goal — leaving these behind would make the primary call to action
 * unreachable from every other page. They sit apart from `navItems` because they
 * are destinations off the site rather than sections of it.
 */
export const utilityLinks = [
  { href: site.githubUrl, label: "GitHub", external: true },
  { href: site.contactUrl, label: "Join", external: true },
] as const;
```

If `site.githubUrl` and `site.contactUrl` do not already exist on the `site`
object, add them there using the same values the footer currently renders; read
`frontend/src/components/site/footer.tsx` for the exact URLs and do not invent
new ones.

In `frontend/src/components/site/navbar.tsx`, import the new export alongside the
existing one:

```tsx
import { navItems, site, utilityLinks } from "@/lib/site";
```

Render them in the desktop row, immediately before the theme toggle:

```tsx
        <div className="hidden items-center gap-0.5 lg:flex">
          {utilityLinks.map((item) => (
            <a
              key={item.href}
              href={item.href}
              target="_blank"
              rel="noreferrer noopener"
              className="rounded-control px-2.5 py-2 font-mono text-[13px] tracking-[0.06em] uppercase whitespace-nowrap text-fg-muted transition-colors hover:bg-surface-2 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring"
            >
              {item.label}
            </a>
          ))}
        </div>
```

Render the same list inside the mobile sheet, after the existing `navItems` links,
so the links are reachable below the `lg` breakpoint. Match the sheet's existing
link classes rather than the desktop ones.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && python -m pytest tests/test_nav_links.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/site.ts frontend/src/components/site/navbar.tsx frontend/tests/test_nav_links.py
git commit -m "feat(nav): move the GitHub and joining links into the navbar"
```

---

### Task 5: Footer on the home page only

**Files:**
- Modify: `frontend/src/app/layout.tsx:70`
- Create: `frontend/src/components/site/footer-slot.tsx`
- Test: `frontend/tests/test_footer.py`

**Interfaces:**
- Consumes: the existing `Footer` component.
- Produces: `export function FooterSlot(): JSX.Element | null` — renders `Footer` on `/` and nothing elsewhere.

The footer is mounted once in the root layout, which is a server component and
cannot read the current path. A small client component reading `usePathname` keeps
that decision in one place rather than moving the footer into every page.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_footer.py`:

```python
from playwright.sync_api import sync_playwright

def test_footer_renders_on_home_only():
    problems = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        for route, expected in [("/", 1), ("/about", 0), ("/events", 0),
                                ("/leaderboard", 0), ("/gallery", 0)]:
            pg.goto(f"http://localhost:3000{route}", wait_until="networkidle")
            count = pg.evaluate("() => document.querySelectorAll('footer').length")
            if count != expected:
                problems.append(f"{route}: expected {expected} footer(s), found {count}")
        b.close()
    assert not problems, "\n".join(problems)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && python -m pytest tests/test_footer.py -v`
Expected: FAIL — `/about: expected 0 footer(s), found 1`

- [ ] **Step 3: Write minimal implementation**

Create `frontend/src/components/site/footer-slot.tsx`:

```tsx
"use client";

import { usePathname } from "next/navigation";

import { Footer } from "@/components/site/footer";

/**
 * Renders the footer on the home page only.
 *
 * The root layout is a server component and cannot read the current route, so
 * this small client component holds the rule. Keeping it here means the footer
 * stays mounted in one place rather than being repeated per page.
 *
 * The links the footer used to carry alone — GitHub and joining — moved into the
 * navbar first, so they remain reachable everywhere.
 */
export function FooterSlot() {
  const pathname = usePathname();
  return pathname === "/" ? <Footer /> : null;
}
```

In `frontend/src/app/layout.tsx`, replace the `Footer` import with:

```tsx
import { FooterSlot } from "@/components/site/footer-slot";
```

and replace `<Footer />` on line 70 with:

```tsx
          <FooterSlot />
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && python -m pytest tests/test_footer.py tests/test_nav_links.py -v`
Expected: PASS (both — the nav links must still be reachable)

Run: `cd frontend && npm run build`
Expected: build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/layout.tsx frontend/src/components/site/footer-slot.tsx frontend/tests/test_footer.py
git commit -m "feat(layout): render the footer on the home page only"
```

---

### Task 6: Full-site verification sweep

**Files:**
- Create: `frontend/tests/test_sweep.py`

**Interfaces:**
- Consumes: everything from Tasks 1–5.
- Produces: nothing consumed by later tasks. This is the gate before Phase 2.

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/test_sweep.py`:

```python
from playwright.sync_api import sync_playwright

ROUTES = ["/", "/about", "/events", "/hall-of-fame", "/gallery", "/blog",
          "/members", "/leaderboard", "/login", "/register"]
WIDTHS = [390, 1440, 1920, 2560]

def test_no_errors_or_overflow_in_either_theme():
    problems = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        for theme in ["dark", "light"]:
            for width in WIDTHS:
                pg = b.new_page(viewport={"width": width, "height": 900})
                errors = []
                pg.on("pageerror", lambda e: errors.append(str(e)))
                for route in ROUTES:
                    pg.goto(f"http://localhost:3000{route}")
                    pg.evaluate("(t)=>localStorage.setItem('theme',t)", theme)
                    pg.reload(wait_until="networkidle")
                    m = pg.evaluate("""() => ({
                        s: document.documentElement.scrollWidth,
                        c: document.documentElement.clientWidth,
                        webgl: [...document.querySelectorAll('canvas')].filter(c => {
                            try { return !!(c.getContext('webgl') || c.getContext('webgl2')); }
                            catch (e) { return false; }
                        }).length,
                    })""")
                    if m["s"] - m["c"] > 1:
                        problems.append(f"{theme} {width}px {route}: overflows by {m['s'] - m['c']}px")
                    if m["webgl"] > 1:
                        problems.append(f"{theme} {width}px {route}: {m['webgl']} WebGL contexts, max 1")
                if errors:
                    problems.append(f"{theme} {width}px: page errors {errors[:2]}")
                pg.close()
        b.close()
    assert not problems, "\n".join(problems)
```

- [ ] **Step 2: Run the sweep**

Run: `cd frontend && python -m pytest tests/test_sweep.py -v`
Expected: PASS. If it fails, fix the reported route before continuing — do not
adjust the assertion.

- [ ] **Step 3: Record the bundle delta**

```bash
cd frontend && npm run build | tee /tmp/build-after.txt
grep -E "^\S*(Route|○|ƒ)" /tmp/build-after.txt | head -20
```

Compare the First Load JS figures against `main` and note the change in the pull
request body. `motion` is the only dependency added in this phase.

- [ ] **Step 4: Commit**

```bash
git add frontend/tests/test_sweep.py
git commit -m "test: full-site sweep for the identity foundation"
```

- [ ] **Step 5: Open the pull request**

```bash
git push -u origin feat/reactbits-frontend-identity
gh pr create --base main --title "feat: club gradient identity, navbar links, home-only footer" --body-file -
```

State in the body: the gradient is derived from the rating ladder and defined
once; which eight pages changed; that the joining links moved to the navbar
before the footer was restricted, so nothing became unreachable; and the measured
bundle delta.

---

## Self-Review

**Spec coverage.** Phase 1 of the spec lists the `--club-gradient` token
(Task 1), GradientText vendored and applied to headings (Tasks 2–3), navbar link
additions (Task 4), and footer restricted to home (Task 5). The spec's testing
section is covered by Task 6, except the reduced-motion and off-screen checks for
Aurora and Particles, which belong to Phases 2 and 3 as those components do not
exist yet. Reduced motion for gradient text is tested in Task 2.

**Placeholders.** None. Every code step contains the code to write; the one
lookup left to the implementer — the existing GitHub and contact URLs in Task 3 —
is explicitly a read from `footer.tsx` with an instruction not to invent values.

**Type consistency.** `GradientText` is a default export used as
`<GradientText>{children}</GradientText>` in Task 3, matching Task 2's signature.
`PageTitle` takes `{ children, className }` and is called with `className` in
Task 3. `utilityLinks` entries carry `href`, `label` and `external`, and the
navbar renders `href` and `label`. `FooterSlot` is a named export imported as
such in `layout.tsx`.

**Scope.** One phase, six tasks, one shippable pull request.
