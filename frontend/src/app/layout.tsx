import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

import { ThemeProvider } from "@/components/site/theme-provider";
import { Navbar } from "@/components/site/navbar";
import { FooterSlot } from "@/components/site/footer-slot";
import { site } from "@/lib/site";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const description =
  "Weekly contests, editorials, and a leaderboard synced from Codeforces, run by students at Dhirubhai Ambani University, Gandhinagar.";

export const metadata: Metadata = {
  // Set NEXT_PUBLIC_SITE_URL at deploy time so share cards resolve absolute URLs.
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
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
      className={`${geistSans.variable} ${geistMono.variable} h-full`}
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
          <main id="main" className="flex-1 overflow-x-clip">
            {children}
          </main>
          <FooterSlot />
        </ThemeProvider>
      </body>
    </html>
  );
}
