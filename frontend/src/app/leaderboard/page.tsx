import type { Metadata } from "next";
import { Eyebrow, Section } from "@/components/site/primitives";
import { apiClient } from "@/lib/api-client";
import { rankColor, type CfRankKey } from "@/lib/cf-ranks";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Leaderboard",
  description: "Live CP Club leaderboard.",
};

function getCfRank(rating: number): CfRankKey {
  if (rating >= 2400) return "grandmaster";
  if (rating >= 2100) return "master";
  if (rating >= 1900) return "candidate";
  if (rating >= 1600) return "expert";
  if (rating >= 1400) return "specialist";
  if (rating >= 1200) return "pupil";
  return "newbie";
}

export default async function LeaderboardPage() {
  const leaderboard = await apiClient.getLeaderboard();

  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <div>
          <Eyebrow>Leaderboard</Eyebrow>
          <h1 className="mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
            Top Coders.
          </h1>
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The current standing of our club members.
        </p>
      </Section>

      <Section className="pb-10">
        <div className="grid gap-4">
          {leaderboard.map((entry, index) => {
            const cfRank = getCfRank(entry.rating);
            return (
            <Card key={entry.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <span className="text-fg-muted">#{index + 1}</span>
                  <span
                    className="font-bold"
                    style={{ color: rankColor(cfRank) }}
                  >
                    {entry.codeforcesHandle}
                  </span>
                </CardTitle>
                <div 
                  className="text-sm capitalize px-2 py-0.5 rounded-full border border-border"
                  style={{ color: rankColor(cfRank), borderColor: rankColor(cfRank) }}
                >
                  {cfRank}
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex items-baseline gap-2">
                  <div className="text-2xl font-bold">{entry.rating}</div>
                  <div className="text-sm text-fg-muted">Rating</div>
                </div>
              </CardContent>
            </Card>
          )})}
        </div>
      </Section>
    </>
  );
}
