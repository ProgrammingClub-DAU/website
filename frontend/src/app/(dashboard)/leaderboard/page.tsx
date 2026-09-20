// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /leaderboard.

// Never statically pre-render — this page fetches live leaderboard data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { leaderboardService } from "@/lib/services/leaderboard";
import LeaderboardDashboard from "@/components/site/leaderboard-dashboard";
import { InDevelopment } from "@/components/site/in-development";
import type { LeaderboardEntry } from "@/types/api";

export const metadata: Metadata = {
  title: "Leaderboard",
  description:
    "Programming Club @ DAU members ranked by Codeforces and LeetCode rating, updated automatically.",
};

export default async function LeaderboardPage() {
  let leaderboard: LeaderboardEntry[] = [];
  try {
    leaderboard = await leaderboardService.getLeaderboard("CODEFORCES", "ALL");
  } catch {
    // API unreachable — render with empty list, page stays functional
  }

  return (
    <>
      <Section className="pt-10 pb-8 md:pt-14">
        <Eyebrow>Leaderboard</Eyebrow>
        <PageTitle className="mt-4">Top Coders.</PageTitle>
        <p className="mt-4 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Club members ranked by Codeforces and LeetCode rating, updated automatically after each
          rated round.
        </p>
      </Section>

      <Section className="pb-10">
        <LeaderboardDashboard initialEntries={leaderboard} />
      </Section>

      <Section className="pb-16">
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
    </>
  );
}
