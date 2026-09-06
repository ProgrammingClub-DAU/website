"""The canvas backdrops, and the invariants that broke silently while building them.

Each assertion here corresponds to a bug that produced no error and no visible
symptom in at least one theme, so none of them are covered by the sweep.
"""

from playwright.sync_api import sync_playwright

# Counts every requestAnimationFrame the page schedules. Installed before any
# page script runs so the components' own loops go through it.
COUNT_FRAMES = """(() => {
  window.__frames = 0;
  const raf = window.requestAnimationFrame;
  window.requestAnimationFrame = (cb) => { window.__frames++; return raf(cb); };
})()"""

# Only the 2D canvases can be read back this way. A WebGL context created
# without preserveDrawingBuffer is cleared after each composite, so getImageData
# on it returns nothing regardless of what is on screen — Aurora's and
# Particles' output is checked by screenshot elsewhere, not here.
PAINTED_PIXELS = """() => {
  const c = [...document.querySelectorAll('canvas')].find(el => {
    try { return !(el.getContext('webgl') || el.getContext('webgl2')); }
    catch (e) { return true; }
  });
  if (!c) return null;
  const data = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
  let painted = 0;
  for (let i = 3; i < data.length; i += 4) if (data[i] > 0) painted++;
  return painted;
}"""


def test_at_most_one_webgl_context_per_page():
    """The spec's rule: one WebGL context per page, never two.

    Aurora has the home page and Particles has the auth pages. Browsers cap how
    many contexts are alive at once and silently discard the oldest when the cap
    is passed, so a second one on the same page is a bug that shows up as an
    unrelated component going blank.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1440, "height": 900})
        for route, expected in (("/", 1), ("/login", 1), ("/register", 1),
                                ("/about", 0), ("/gallery", 0)):
            pg.goto(f"http://localhost:3000{route}", wait_until="networkidle")
            pg.wait_for_timeout(900)
            contexts = pg.evaluate("""() => [...document.querySelectorAll('canvas')].filter(c => {
                try { return !!(c.getContext('webgl') || c.getContext('webgl2')); }
                catch (e) { return false; }
            }).length""")
            assert contexts == expected, f"{route}: {contexts} WebGL contexts, expected {expected}"
        b.close()


def test_reduced_motion_stops_the_loop_but_keeps_the_image():
    """prefers-reduced-motion clamps CSS durations and does nothing to rAF.

    Every vendored component had to be changed to stop scheduling frames:
    upstream ParticleText re-scheduled unconditionally, and neither Aurora nor
    Particles checked at all. The second half matters as much as the first —
    stopping the loop before anything is drawn leaves an empty canvas, which
    reads as a page that failed to load rather than as a still image.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1440, "height": 900}, reduced_motion="reduce")
        pg.add_init_script(COUNT_FRAMES)
        pg.goto("http://localhost:3000/", wait_until="networkidle")

        # Measured at the top of the page, where Aurora is on screen. Measuring
        # only after scrolling down would let the off-screen guard hide a broken
        # reduced-motion guard — which is exactly what this test did at first,
        # and it passed against a deliberately broken Aurora.
        pg.wait_for_timeout(1200)
        before = pg.evaluate("window.__frames")
        pg.wait_for_timeout(1500)
        after = pg.evaluate("window.__frames")
        assert after == before, (
            f"{after - before} frames scheduled under reduced motion with the hero in view"
        )

        pg.locator("canvas").last.scroll_into_view_if_needed()
        # Long enough for the wordmark's gather to finish; the loop may run
        # during it, and is only required to stop once it is settled.
        pg.wait_for_timeout(2500)
        before = pg.evaluate("window.__frames")
        pg.wait_for_timeout(1500)
        after = pg.evaluate("window.__frames")
        assert after == before, (
            f"{after - before} frames scheduled under reduced motion with the wordmark in view"
        )

        painted = pg.evaluate(PAINTED_PIXELS)
        assert painted and painted > 1000, f"the wordmark canvas is blank: {painted} pixels"
        b.close()


def test_backdrops_stop_while_off_screen():
    """Without an IntersectionObserver these keep drawing all the way down the page."""
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 1440, "height": 900})
        pg.add_init_script(COUNT_FRAMES)
        pg.goto("http://localhost:3000/", wait_until="networkidle")
        pg.locator("canvas").last.scroll_into_view_if_needed()
        pg.wait_for_timeout(2500)

        visible = """() => [...document.querySelectorAll('canvas')].some(c => {
            const r = c.getBoundingClientRect();
            return r.bottom > 0 && r.top < window.innerHeight;
        })"""

        offset = None
        height = pg.evaluate("document.body.scrollHeight")
        for y in range(0, height - 900, 150):
            pg.evaluate(f"window.scrollTo(0, {y})")
            pg.wait_for_timeout(120)
            if not pg.evaluate(visible):
                offset = y
                break
        assert offset is not None, "no scroll position has both canvases off-screen"

        pg.wait_for_timeout(600)
        before = pg.evaluate("window.__frames")
        pg.wait_for_timeout(1500)
        assert pg.evaluate("window.__frames") == before, "still drawing while off-screen"

        # And it has to come back, or the guard has simply killed the animation.
        pg.evaluate("window.scrollTo(0, 0)")
        pg.wait_for_timeout(400)
        resumed = pg.evaluate("window.__frames")
        pg.wait_for_timeout(800)
        assert pg.evaluate("window.__frames") > resumed, "did not resume on scroll back"
        b.close()


def test_auth_backdrop_costs_nothing_on_mobile():
    """The panel is display:none below lg, so its loop must never start.

    This matters more with Particles than it did with the Canvas 2D field it
    replaced: a WebGL context opened behind a hidden panel is one of a small,
    browser-capped budget, spent on something nobody can see.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 420, "height": 800})
        pg.add_init_script(COUNT_FRAMES)
        pg.goto("http://localhost:3000/login", wait_until="networkidle")
        pg.wait_for_timeout(1000)
        before = pg.evaluate("window.__frames")
        pg.wait_for_timeout(1200)
        assert pg.evaluate("window.__frames") == before, "drawing behind a display:none panel"
        b.close()


def test_border_glow_card_surface_stays_opaque():
    """BorderGlow's mesh layer sits at z-index -1, so it paints over the card.

    The only thing keeping the mesh off the card's face is that layer's flat
    fill clipped to the padding box. A translucent surface lets the mesh through
    and swallows the text — silently, and only where the mesh happens to fall.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        for theme in ("dark", "light"):
            pg = b.new_page(viewport={"width": 1280, "height": 900})
            pg.goto("http://localhost:3000/about")
            pg.evaluate("(t) => localStorage.setItem('theme', t)", theme)
            pg.reload(wait_until="networkidle")
            pg.wait_for_timeout(500)
            alpha = pg.evaluate("""() => {
                const card = document.querySelector('.isolate');
                const bg = getComputedStyle(card).backgroundColor;
                const m = bg.match(/rgba?\\(([^)]+)\\)/);
                const parts = m[1].split(',').map(s => parseFloat(s));
                return parts.length > 3 ? parts[3] : 1;
            }""")
            assert alpha == 1, f"{theme}: card surface is translucent (alpha {alpha})"
            pg.close()
        b.close()


def test_glass_buttons_carry_the_club_gradient_edge():
    """The edge is a masked ::before, and it has to resolve in both themes.

    A var() that fails to resolve here leaves no error and no border, just a
    button that quietly stops matching the rest of the site.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        for theme, stop in (("dark", "rgb(0, 194, 199)"), ("light", "rgb(0, 134, 139)")):
            pg = b.new_page(viewport={"width": 1440, "height": 900})
            pg.goto("http://localhost:3000/")
            pg.evaluate("(t) => localStorage.setItem('theme', t)", theme)
            pg.reload(wait_until="networkidle")
            pg.wait_for_timeout(400)
            found = pg.evaluate("""() => [...document.querySelectorAll('[data-slot="button"]')]
                .filter(el => ['default', 'outline'].includes(el.dataset.variant))
                .map(el => {
                    const before = getComputedStyle(el, '::before');
                    return {
                        variant: el.dataset.variant,
                        image: before.backgroundImage,
                        mask: before.maskImage || before.webkitMaskImage,
                        backdrop: getComputedStyle(el).backdropFilter,
                    };
                })""")
            assert found, f"{theme}: no glass buttons on the home page"
            for entry in found:
                assert stop in entry["image"], f"{theme}: gradient missing from {entry}"
                assert "gradient" in entry["mask"], f"{theme}: ring mask missing from {entry}"
                # The hand-written -webkit- prefix made Lightning CSS drop the
                # standard property, and Chromium no longer supports the alias.
                assert entry["backdrop"].startswith("blur("), (
                    f"{theme}: backdrop-filter is {entry['backdrop']!r}, blur is gone"
                )
            pg.close()
        b.close()


def test_aurora_still_paints_under_reduced_motion():
    """A stopped loop that never drew would pass the frame count and leave a blank hero.

    Checked without reading pixels: the hero is captured as-is and again with
    the Aurora layer hidden. Identical bytes mean it was contributing nothing.
    """
    with sync_playwright() as p:
        b = p.chromium.launch()
        for theme in ("dark", "light"):
            pg = b.new_page(viewport={"width": 1440, "height": 900}, reduced_motion="reduce")
            pg.goto("http://localhost:3000/")
            pg.evaluate("(t) => localStorage.setItem('theme', t)", theme)
            pg.reload(wait_until="networkidle")
            pg.wait_for_timeout(1500)

            band = {"x": 0, "y": 56, "width": 1440, "height": 260}
            with_aurora = pg.screenshot(clip=band)

            hidden = pg.evaluate("""() => {
                const el = document.querySelector('main canvas')?.closest('[aria-hidden]');
                if (!el) return false;
                el.style.display = 'none';
                return true;
            }""")
            assert hidden, f"{theme}: could not find the Aurora layer to hide"
            pg.wait_for_timeout(300)
            without_aurora = pg.screenshot(clip=band)

            assert with_aurora != without_aurora, (
                f"{theme}: the hero looks the same with Aurora hidden, so it drew nothing"
            )
            pg.close()
        b.close()
