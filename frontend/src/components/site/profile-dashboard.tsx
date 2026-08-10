"use client";

/**
 * Profile Dashboard Page
 *
 * This is a client component because it uses interactive libraries (recharts, react-activity-calendar).
 * It receives data from a server-side wrapper and renders:
 * - Profile header with avatar, name (colored by CF rank), and key stats
 * - GitHub-style activity heat map (green dots for DSA practice days)
 * - Contest rating graph over time (recharts line chart)
 * - Platform-wise breakdown of problems solved
 */

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import type { Profile } from "@/types/api";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";
import { ActivityCalendar } from "react-activity-calendar";
import { User, Trophy, Code, Calendar } from "lucide-react";

// ── Rank color helper for rating values (Codeforces thresholds) ──
function ratingToRankName(rating: number): string {
  if (rating >= 2400) return "Grandmaster";
  if (rating >= 2100) return "Master";
  if (rating >= 1900) return "Candidate Master";
  if (rating >= 1600) return "Expert";
  if (rating >= 1400) return "Specialist";
  if (rating >= 1200) return "Pupil";
  return "Newbie";
}

export default function ProfileDashboard({ profile }: { profile: Profile }) {
  // Resolve the CF rank color using the existing utility
  const getCfRank = (rating: number): import("@/lib/cf-ranks").CfRankKey => {
    if (rating >= 2400) return "grandmaster";
    if (rating >= 2100) return "master";
    if (rating >= 1900) return "candidate";
    if (rating >= 1600) return "expert";
    if (rating >= 1400) return "specialist";
    if (rating >= 1200) return "pupil";
    return "newbie";
  };
  const actualCfRank = getCfRank(profile.rating);
  const nameColor = rankColor(actualCfRank);
  const rankName = CF_RANKS.find((r) => r.key === actualCfRank)?.name ?? actualCfRank;
  const totalSolved = profile.platformStats?.reduce((acc, curr) => acc + curr.solved, 0) || 0;

  return (
    <div className="space-y-8">
      {/* ── Profile Header ── */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
            {/* Avatar */}
            <div
              className="flex size-24 shrink-0 items-center justify-center rounded-full border-2 bg-surface-2"
              style={{ borderColor: nameColor }}
            >
              {profile.avatarUrl ? (
                <img
                  src={profile.avatarUrl}
                  alt={profile.name}
                  className="size-full rounded-full object-cover"
                />
              ) : (
                <User className="size-10 text-fg-muted" />
              )}
            </div>

            {/* Name + handle + rank */}
            <div className="flex-1 text-center sm:text-left">
              <h2
                className="text-2xl font-bold tracking-tight"
                style={{ color: nameColor }}
              >
                {profile.name}
              </h2>
              <p className="mt-1 text-sm text-fg-muted">@{profile.codeforcesHandle}</p>
              <div className="mt-3 flex flex-wrap items-center justify-center gap-3 sm:justify-start">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium capitalize"
                  style={{ color: nameColor, borderColor: nameColor }}
                >
                  <Trophy className="size-3" />
                  {rankName}
                </span>
                <span className="text-sm text-fg-muted">
                  Rating: <strong className="text-foreground">{profile.rating}</strong>
                  <span className="text-fg-subtle"> (max {profile.maxRating})</span>
                </span>
              </div>
            </div>

            {/* Quick stats cards */}
            <div className="grid grid-cols-2 gap-3 text-center">
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{totalSolved}</div>
                <div className="text-[11px] text-fg-muted">Total Solved</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{profile.ratingHistory?.length || 0}</div>
                <div className="text-[11px] text-fg-muted">Contests</div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* ── Platform Stats ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Code className="size-4" />
            Problems Solved by Platform
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {profile.platformStats?.map((ps) => (
              <div
                key={ps.platform}
                className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
              >
                <div className="text-2xl font-bold">{ps.solved}</div>
                <div className="mt-1 text-xs text-fg-muted">{ps.platform}</div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* ── Rating Graph ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Trophy className="size-4" />
            Contest Rating History
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={profile.ratingHistory}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--hairline)" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 11, fill: "var(--fg-muted)" }}
                  tickFormatter={(v: string) => {
                    const d = new Date(v);
                    return d.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
                  }}
                />
                <YAxis
                  domain={["dataMin - 100", "dataMax + 100"]}
                  tick={{ fontSize: 11, fill: "var(--fg-muted)" }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--surface-2)",
                    border: "1px solid var(--border)",
                    borderRadius: "8px",
                    fontSize: "12px",
                  }}
                  labelFormatter={(label: any) => {
                    if (!label) return "";
                    const d = new Date(label as string);
                    return d.toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" });
                  }}
                  formatter={(value: any, _name: any, props: any) => {
                    const contestName = props.payload?.contestName ?? "";
                    return [
                      `${value} (${ratingToRankName(Number(value))})`,
                      contestName,
                    ];
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="rating"
                  stroke="var(--primary)"
                  strokeWidth={2}
                  dot={{ r: 4, fill: "var(--primary)" }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* ── Activity Heat Map (GitHub-style green dots) ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Calendar className="size-4" />
            Practice Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <ActivityCalendar
              data={profile.activityData}
              theme={{
                dark: ["#161b22", "#0e4429", "#006d32", "#26a641", "#39d353"],
                light: ["#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"],
              }}
              labels={{
                totalCount: "{{count}} problems solved in the last year",
              }}
              blockSize={12}
              blockMargin={3}
              fontSize={12}
            />
          </div>
        </CardContent>
      </Card>

      {/* ── Account Info ── */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-fg-muted">Email</span>
            <span>{profile.email}</span>
          </div>
          <Separator />
          <div className="flex justify-between">
            <span className="text-fg-muted">Codeforces Handle</span>
            <span style={{ color: nameColor }}>@{profile.codeforcesHandle}</span>
          </div>
          <Separator />
          <div className="flex justify-between">
            <span className="text-fg-muted">Member Since</span>
            <span>{new Date(profile.createdAt).toLocaleDateString("en-US", { month: "long", year: "numeric" })}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
