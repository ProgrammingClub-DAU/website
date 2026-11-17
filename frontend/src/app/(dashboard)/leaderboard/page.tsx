// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /leaderboard.

// Rendered per request, never at build time (the backend may be asleep during a
// deploy). The data itself comes from a 60-second cache, so a visit is instant;
// see lib/public-data.ts.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import { Section } from "@/components/site/primitives";
import { getPublicLeaderboard } from "@/lib/public-data";
import LeaderboardDashboard from "@/components/site/leaderboard-dashboard";
import type { LeaderboardEntry } from "@/types/api";

export const metadata: Metadata = {
  title: "Leaderboard | Top Coders",
  description:
    "Programming Club @ DAU members ranked by Codeforces and LeetCode rating, updated automatically with unlocked banners and 3D podium.",
};

export default async function LeaderboardPage() {
  let leaderboard: LeaderboardEntry[] = [];
  try {
    leaderboard = await getPublicLeaderboard();
  } catch {
    // API unreachable — render with empty list, page stays functional
  }

  return (
    <div className="relative min-h-screen bg-[#0B0B12] text-[#e8e8f0] selection:bg-purple-500/30 selection:text-purple-200">
      {/* Subtle animated grid background with radial aurora */}
      <div
        className="pointer-events-none absolute inset-0 size-full opacity-20"
        style={{
          backgroundImage: `
            linear-gradient(to right, rgba(255, 255, 255, 0.05) 1px, transparent 1px),
            linear-gradient(to bottom, rgba(255, 255, 255, 0.05) 1px, transparent 1px)
          `,
          backgroundSize: "40px 40px",
          maskImage: "radial-gradient(ellipse 60% 50% at 50% 10%, #000 70%, transparent 100%)",
        }}
      />

      {/* Atmospheric Aurora Glow Blobs */}
      <div className="pointer-events-none absolute top-10 left-1/2 -translate-x-1/2 w-[700px] h-[350px] bg-gradient-to-tr from-cyan-500/10 via-slate-500/5 to-transparent blur-[120px] rounded-full" />

      <Section className="relative z-10 pt-10 md:pt-14 pb-16">
        <LeaderboardDashboard initialEntries={leaderboard} />
      </Section>
    </div>
  );
}
