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
