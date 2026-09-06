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
        # Three stops, so two commas separating them. It was four: the fourth
        # was --cf-master, and orange against the indigo --primary read as a
        # separate decoration rather than the same identity.
        assert tok["stops"].count(",") == 2, f"{name}: expected 3 stops, got {tok['stops']!r}"

    # The point of dropping that stop was to keep the gradient in the cool half
    # of the ladder, which is the half --primary sits in. Assert the outcome,
    # not just the count: a warm stop creeping back in is the regression.
    for name, tok in (("dark", dark), ("light", light)):
        for warm in ("#d97706", "#ff9f45", "#dc2626", "#ff4d4d"):
            assert warm not in tok["stops"].lower(), (
                f"{name}: warm stop {warm} is back in --club-gradient-stops"
            )

    # The ladder colours differ per theme, so the gradient must differ too.
    assert dark["stops"] != light["stops"], "gradient does not follow the theme"
