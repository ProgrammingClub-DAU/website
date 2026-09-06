from playwright.sync_api import sync_playwright

# Reads the computed background-position of the [data-gradient-text] span, or
# None if it isn't present (e.g. before Task 3 wires GradientText into a page
# heading). Null-safe so a missing element fails the `info is not None`
# assertion below rather than crashing inside evaluate().
GET_BACKGROUND_POSITION = """() => {
    const el = document.querySelector('h1 [data-gradient-text]');
    return el ? getComputedStyle(el).backgroundPosition : null;
}"""

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

        # Normal motion: background-position must actually change over ~700ms.
        # This is also what proves the sampling technique can detect movement
        # at all, so a "no change" result under reduced motion below means
        # something rather than being a false negative from a broken probe.
        position_a = pg.evaluate(GET_BACKGROUND_POSITION)
        pg.wait_for_timeout(700)
        position_b = pg.evaluate(GET_BACKGROUND_POSITION)
        pg.close()

        # Reduced motion must stop the animation loop, not merely slow it:
        # sample the same computed style twice, ~700ms apart, and require it
        # to be identical.
        #
        # (This replaces an earlier version of this test that counted
        # requestAnimationFrame calls by monkey-patching
        # window.requestAnimationFrame after page load. Framer Motion
        # captures its own rAF reference at module-load time, so that patch
        # may never be observed and the assertion could pass for the wrong
        # reason. Sampling an observable, rendered property is reliable
        # regardless of how the animation library schedules its frames.)
        pg2 = b.new_page(reduced_motion="reduce")
        pg2.goto("http://localhost:3000/about", wait_until="networkidle")
        reduced_a = pg2.evaluate(GET_BACKGROUND_POSITION)
        pg2.wait_for_timeout(700)
        reduced_b = pg2.evaluate(GET_BACKGROUND_POSITION)
        pg2.close()
        b.close()

    assert info is not None, "no [data-gradient-text] inside an h1"
    assert info["insideH1"], "gradient text is not inside the heading"
    assert info["tag"] == "SPAN", f"must be a span inside h1, got {info['tag']}"
    assert info["display"] == "inline", f"must render inline, got {info['display']}"
    assert info["cursor"] != "pointer", "non-interactive text must not use a pointer cursor"
    assert "linear-gradient" in info["image"], f"no gradient painted: {info['image']!r}"
    assert info["fallback"], "no solid fallback colour set"
    assert position_a != position_b, (
        f"background-position did not change under normal motion ({position_a!r}); "
        "the animation may not be running, or this sampling technique can't detect movement"
    )
    assert reduced_a == reduced_b, (
        f"background-position changed under reduced motion ({reduced_a!r} -> {reduced_b!r}); "
        "the animation loop must stop, not merely slow down"
    )
