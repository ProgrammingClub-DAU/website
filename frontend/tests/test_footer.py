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
