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
