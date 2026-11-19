"use client";

import { useEffect, useState } from "react";
import { CalendarDays, Code2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartLegend, DailySolvesChart } from "@/components/site/daily-solves-chart";
import { RecentSolvesList } from "@/components/site/recent-solves-list";
import { TopicStrengthChart } from "@/components/site/topic-strength-chart";
import { statsService } from "@/lib/services/stats";
import type { MemberActivity, TopicStrengthResponse } from "@/types/api";

/**
 * A member's recent activity on their profile: problems solved per day over the
 * last 30 days, their level by topic, and their latest Codeforces solves. All of
 * it is public, from the member's own Codeforces and LeetCode history. Focus
 * topics are shown to the owner only (see TopicStrengthChart's "public" viewer).
 */
export function ProfileActivity({ userId, isOwner }: { userId: string; isOwner: boolean }) {
  const [activity, setActivity] = useState<MemberActivity | null>(null);
  const [topics, setTopics] = useState<TopicStrengthResponse | null>(null);

  useEffect(() => {
    let ignore = false;
    const id = Number(userId);
    statsService
      .getActivity(id)
      .then((a) => {
        if (!ignore) setActivity(a);
      })
      .catch(() => undefined);
    statsService
      .getTopics(id)
      .then((t) => {
        if (!ignore) setTopics(t);
      })
      .catch(() => undefined);
    return () => {
      ignore = true;
    };
  }, [userId]);

  const showTopics = topics != null && topics.reason == null && topics.topics.some((t) => t.level != null);
  if (!activity && !showTopics) return null;

  const cf = activity?.daily.reduce((sum, d) => sum + d.codeforces, 0) ?? 0;
  const lc = activity?.daily.reduce((sum, d) => sum + (d.leetcode ?? 0), 0) ?? 0;
  const activeDays = activity?.daily.filter((d) => d.codeforces > 0 || (d.leetcode ?? 0) > 0).length ?? 0;

  return (
    <>
      {activity && (
        <Card>
          <CardHeader className="flex flex-col gap-3 pb-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <CalendarDays className="size-4 text-primary" />
                Last 30 days
              </CardTitle>
              <p className="mt-1 font-mono text-label text-fg-muted">Problems solved per day on Codeforces and LeetCode.</p>
            </div>
            <ChartLegend />
          </CardHeader>
          <CardContent className="space-y-4 pt-3">
            <div className="grid grid-cols-3 gap-3">
              <Figure value={cf + lc} label="problems solved" />
              <Figure value={`${cf} · ${lc}`} label="Codeforces · LeetCode" />
              <Figure value={`${activeDays}/30`} label="active days" />
            </div>
            <DailySolvesChart daily={activity.daily} />
          </CardContent>
        </Card>
      )}

      {showTopics && <TopicStrengthChart data={topics} viewer={isOwner ? "member" : "public"} />}

      {activity && activity.recentSolves.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <Code2 className="size-4 text-primary" />
              Recent Codeforces solves
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-1">
            <RecentSolvesList solves={activity.recentSolves} columns={2} />
          </CardContent>
        </Card>
      )}
    </>
  );
}

function Figure({ value, label }: { value: number | string; label: string }) {
  return (
    <div className="rounded-xl border border-border bg-surface-2/60 px-3 py-2.5 text-center">
      <p className="font-mono text-xl font-black tabular-nums text-foreground">{value}</p>
      <p className="mt-0.5 text-[11px] text-fg-muted">{label}</p>
    </div>
  );
}
