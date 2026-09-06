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
