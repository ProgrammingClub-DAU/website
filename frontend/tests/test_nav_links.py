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
