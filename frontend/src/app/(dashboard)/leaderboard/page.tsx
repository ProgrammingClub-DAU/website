// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /leaderboard.

// Never statically pre-render — this page fetches live leaderboard data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import { Section } from "@/components/site/primitives";
import { leaderboardService } from "@/lib/services/leaderboard";
import LeaderboardDashboard from "@/components/site/leaderboard-dashboard";
import { InDevelopment } from "@/components/site/in-development";
import type { LeaderboardEntry } from "@/types/api";

export const metadata: Metadata = {
  title: "Leaderboard | Top Coders",
  description:
    "Programming Club @ DAU members ranked by Codeforces and LeetCode rating, updated automatically with unlocked banners and 3D podium.",
};

export default async function LeaderboardPage() {
  let leaderboard: LeaderboardEntry[] = [];
  try {
    leaderboard = await leaderboardService.getLeaderboard("CODEFORCES", "ALL");
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
      <div className="pointer-events-none absolute top-10 left-1/2 -translate-x-1/2 w-[700px] h-[350px] bg-gradient-to-tr from-purple-600/10 via-cyan-500/10 to-transparent blur-[120px] rounded-full" />

      <Section className="relative z-10 pt-10 md:pt-14 pb-8">
        <LeaderboardDashboard initialEntries={leaderboard} />
      </Section>

      <Section className="relative z-10 pb-16">
        <InDevelopment
          title="More ways to rank"
          body="Rankings built from what members solve and how they do in contests, not only from their current rating."
          items={[
            "Top solvers this week",
            "Top solvers this month",
            "Top solvers this year",
            "Most contests",
            "Overall score",
          ]}
        />
      </Section>
    </div>
  );
}
