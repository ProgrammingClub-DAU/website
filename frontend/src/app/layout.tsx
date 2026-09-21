import type { Metadata } from "next";
import Script from "next/script";
import { Geist, Geist_Mono, Space_Grotesk } from "next/font/google";
import "./globals.css";

import { ThemeProvider } from "@/components/site/theme-provider";
import { Navbar } from "@/components/site/navbar";
import { FooterSlot } from "@/components/site/footer-slot";
import { site } from "@/lib/site";

import { siteUrl } from "@/lib/site-url";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

/**
 * The display face, used only for page and section headings.
 *
 * Space Grotesk descends from Space Mono, so it carries the same squared
 * terminals and single-storey `a` as the Geist Mono labels that run through
 * every card and eyebrow on this site. Headings therefore read as the same
 * voice as the labels rather than as a third unrelated typeface, while still
 * being visibly not the body text — which Geist Sans set against itself was
 * never going to be.
 *
 * Body copy stays on Geist Sans: it is the more comfortable face at 15px over
 * several lines, and a display grotesque is not meant to carry paragraphs.
 */
const spaceGrotesk = Space_Grotesk({
  variable: "--font-space-grotesk",
  subsets: ["latin"],
  // Headings use one weight each, so the two that are actually rendered are
  // named rather than shipping the whole variable range.
  weight: ["500", "700"],
});

const description =
  "Contests, lectures, and a leaderboard synced from Codeforces and LeetCode, run by students at Dhirubhai Ambani University, Gandhinagar.";

export const metadata: Metadata = {
  // Share cards need absolute URLs. See lib/site-url.ts for where this comes from.
  metadataBase: new URL(siteUrl),
  title: {
    default: `${site.fullName} — Competitive programming at DAU`,
    template: `%s — ${site.fullName}`,
  },
  description,
  applicationName: site.fullName,
  openGraph: {
    type: "website",
    siteName: site.fullName,
    title: `${site.fullName} — Competitive programming at DAU`,
    description,
    locale: "en_IN",
  },
  twitter: { card: "summary_large_image", title: site.fullName, description },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} ${spaceGrotesk.variable} h-full`}
    >
      <body className="flex min-h-full flex-col">

        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem={false}
          disableTransitionOnChange
        >
          <a
            href="#main"
            className="sr-only rounded-control bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:absolute focus:top-3 focus:left-3 focus:z-100"
          >
            Skip to content
          </a>
          <Navbar />
          {/* overflow-x: clip because BorderGlow's outer glow is drawn outside
              the card it belongs to — inset: -40px — so on a narrow viewport it
              reached past the page gutter and gave the document 15px of
              horizontal scroll.

              `clip` rather than `hidden`: hidden would make this a scroll
              container, and it is an ancestor of the sticky navbar's scrollport.
              And here rather than on html or body, because Chromium's viewport
              propagation rules mean neither of those actually clips. The
              vertical axis stays visible, so the glow still bleeds above and
              below the card, and main is viewport-width, so on wide screens the
              glow still reaches into the gutters. */}
          {/* The opening identity: draws the logo, fills it, then docks it
              beside the wordmark. No `once`: it plays on every full page
              load, so a refresh replays it. Client-side navigation between
              pages does not remount the root layout, so moving around the site
              does not. `for` makes the main content inert while it plays and
              restores whatever state it had. Escape dismisses it, and it is
              skipped outright under prefers-reduced-motion. */}
          <programming-club-intro overlay="" for="main" />

          <main id="main" className="flex-1 overflow-x-clip">
            {children}
          </main>
          <FooterSlot />

          {/* beforeInteractive so the element is defined as early as possible:
              it is the first thing on screen, and a late upgrade would show
              the page before the intro that is meant to precede it. */}
          <Script src="/club-intro.js" strategy="beforeInteractive" />
        </ThemeProvider>
      </body>
    </html>
  );
}
