import type { Metadata } from "next";
import { Eyebrow, Section } from "@/components/site/primitives";
import { apiClient } from "@/lib/api-client";
import { rankColor } from "@/lib/cf-ranks";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Leaderboard",
  description: "Live CP Club leaderboard.",
};

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
          {leaderboard.map((entry) => (
            <Card key={entry.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <span className="text-fg-muted">#{entry.rank}</span>
                  <span
                    className="font-bold"
                    style={{ color: rankColor(entry.cfRank) }}
                  >
                    {entry.handle}
                  </span>
                </CardTitle>
                <div 
                  className="text-sm capitalize px-2 py-0.5 rounded-full border border-border"
                  style={{ color: rankColor(entry.cfRank), borderColor: rankColor(entry.cfRank) }}
                >
                  {entry.cfRank}
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex items-baseline gap-2">
                  <div className="text-2xl font-bold">{entry.rating}</div>
                  <div className="text-sm text-fg-muted">Rating</div>
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {entry.solvedCount} problems solved
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      </Section>
    </>
  );
}
