import type { Metadata } from "next";
import { Eyebrow, Section } from "@/components/site/primitives";
import { apiClient } from "@/lib/api-client";
import LeaderboardDashboard from "@/components/site/leaderboard-dashboard";

export const metadata: Metadata = {
  title: "Leaderboard",
  description: "Live CP Club leaderboard, rankings, and active stats.",
};

export default async function LeaderboardPage() {
  const leaderboard = await apiClient.getLeaderboard();

  return (
    <>
      <Section className="pt-16 pb-8 md:pt-24">
        <div>
          <Eyebrow>Leaderboard</Eyebrow>
          <h1 className="mt-4 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
            Top Coders.
          </h1>
        </div>
        <p className="mt-4 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The current standing of our club members and competitive programming activity.
        </p>
      </Section>

      <Section className="pb-16">
        <LeaderboardDashboard entries={leaderboard} />
      </Section>
    </>
  );
}
