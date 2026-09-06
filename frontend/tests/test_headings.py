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
