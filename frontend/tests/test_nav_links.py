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


def test_github_link_reachable_from_mobile_sheet():
    # The desktop row (`hidden lg:flex`) is always in the DOM, so a query like
    # `header a` would find the GitHub link even if it were missing from the
    # mobile sheet entirely. This test opens the sheet at a phone-sized
    # viewport and checks real visibility, so it actually fails if the mobile
    # copy of the link is dropped.
    problems = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": 375, "height": 812})
        for route in ROUTES:
            pg.goto(f"http://localhost:3000{route}", wait_until="networkidle")
            pg.click("button[aria-label='Open menu']")

            # The sheet slides in (duration-200); give the animation a moment
            # so the visibility check below isn't racing it.
            pg.wait_for_timeout(400)

            # SheetContent renders through a Radix portal onto document.body,
            # not inside <header>, so the query must not be scoped to header.
            # It also must not be a bare document-wide query: the footer
            # (still rendered on every route ahead of Task 5) carries its own
            # always-visible GitHub link, which would mask a missing mobile
            # sheet link. Scope to the open sheet's portalled container
            # specifically. offsetParent is null for anything CSS-hidden
            # (display:none) or not laid out, so this distinguishes "present
            # in the DOM" from "actually visible to the user".
            visible = pg.evaluate(
                """() => {
                    const sheet = document.querySelector('[data-slot="sheet-content"]');
                    if (!sheet) return false;
                    return [...sheet.querySelectorAll('a[href*="github.com"]')]
                        .some(a => a.offsetParent !== null);
                }"""
            )
            if not visible:
                problems.append(f"{route}: no visible GitHub link in the open mobile sheet")

            # Close the sheet again before navigating to the next route.
            pg.keyboard.press("Escape")
        b.close()
    assert not problems, "\n".join(problems)
