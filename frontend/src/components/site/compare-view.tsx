"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowLeft, ArrowRight, ArrowUpRight, User as UserIcon, Swords } from "lucide-react";
import { RatingGraph } from "@/components/site/rating-graph";
import { PlatformMark } from "@/components/site/platform-mark";
import { Eyebrow } from "@/components/site/primitives";
import { cn } from "@/lib/utils";
import type { CompareResult } from "@/types/api";

interface CompareViewProps {
  data: CompareResult;
  className?: string;
}

export function CompareView({ data, className }: CompareViewProps) {
  const { memberA, memberB, headToHead } = data;

  const compareMetric = (
    label: string,
    valA: number | null | undefined,
    valB: number | null | undefined,
    suffix: string = ""
  ) => {
    const numA = valA ?? 0;
    const numB = valB ?? 0;
    const hasA = valA !== null && valA !== undefined;
    const hasB = valB !== null && valB !== undefined;

    let leader: "A" | "B" | "TIE" = "TIE";
    if (hasA && hasB) {
      if (numA > numB) leader = "A";
      else if (numB > numA) leader = "B";
    } else if (hasA && !hasB) {
      leader = "A";
    } else if (!hasA && hasB) {
      leader = "B";
    }

    return (
      <div className="flex flex-col rounded-card border border-hairline bg-surface-elevated/30 p-3.5 transition-colors hover:bg-surface-elevated/50 sm:flex-row sm:items-center sm:justify-between">
        {/* Member A Value */}
        <div className="flex items-center gap-2 sm:w-1/3">
          <span
            className={cn(
              "font-heading text-lg font-semibold",
              leader === "A" ? "text-primary" : "text-foreground"
            )}
          >
            {hasA ? numA.toLocaleString() : "–"}
          </span>
          {hasA && suffix && (
            <span className="font-mono text-nano text-fg-muted uppercase">{suffix}</span>
          )}
          {leader === "A" && (
            <span className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 font-mono text-nano text-primary bg-primary/10 border border-primary/20">
              <ArrowLeft className="size-3" /> Ahead
            </span>
          )}
        </div>

        {/* Center Label */}
        <div className="my-1 text-center font-mono text-label tracking-caps text-fg-muted uppercase sm:my-0 sm:w-1/3">
          {label}
        </div>

        {/* Member B Value */}
        <div className="flex items-center justify-end gap-2 sm:w-1/3 text-right">
          {leader === "B" && (
            <span className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 font-mono text-nano text-primary bg-primary/10 border border-primary/20">
              Ahead <ArrowRight className="size-3" />
            </span>
          )}
          <span
            className={cn(
              "font-heading text-lg font-semibold",
              leader === "B" ? "text-primary" : "text-foreground"
            )}
          >
            {hasB ? numB.toLocaleString() : "–"}
          </span>
          {hasB && suffix && (
            <span className="font-mono text-nano text-fg-muted uppercase">{suffix}</span>
          )}
        </div>
      </div>
    );
  };

  // Prepare combined rating series for multi-platform graph
  const combinedSeries = [
    {
      platform: "CODEFORCES" as const,
      points: memberA.ratingHistoryCf.map((p) => ({ ...p, contestName: `${memberA.name}: ${p.contestName}` })),
      label: `${memberA.name} (CF)`,
      color: "var(--cf-expert)",
    },
    {
      platform: "CODEFORCES" as const,
      points: memberB.ratingHistoryCf.map((p) => ({ ...p, contestName: `${memberB.name}: ${p.contestName}` })),
      label: `${memberB.name} (CF)`,
      color: "#f59e0b",
    },
  ].filter((s) => s.points.length > 0);

  return (
    <div className={cn("space-y-8", className)}>
      {/* ── Member Headers Banner ── */}
      <div className="grid grid-cols-2 gap-4 rounded-panel border border-hairline bg-surface/60 p-5 backdrop-blur-md">
        {/* Member A Card */}
        <div className="flex flex-col sm:flex-row items-center gap-3 text-center sm:text-left">
          <div className="relative size-14 shrink-0 overflow-hidden rounded-full border border-hairline bg-surface-elevated">
            {memberA.avatarUrl ? (
              <Image src={memberA.avatarUrl} alt={memberA.name} fill className="object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-fg-subtle">
                <UserIcon className="size-7" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <Link
              href={`/profile/${memberA.id}`}
              className="font-heading text-lg sm:text-xl font-medium text-foreground hover:text-primary transition-colors flex items-center justify-center sm:justify-start gap-1"
            >
              {memberA.name}
              <ArrowUpRight className="size-3.5 opacity-60" />
            </Link>
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 font-mono text-nano text-fg-muted">
              {memberA.codeforcesHandle && (
                <span className="flex items-center gap-1">
                  <PlatformMark platform="codeforces" className="size-3" />
                  @{memberA.codeforcesHandle}
                </span>
              )}
              {memberA.leetcodeHandle && (
                <span className="flex items-center gap-1">
                  <PlatformMark platform="leetcode" className="size-3" />
                  {memberA.leetcodeHandle}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Member B Card */}
        <div className="flex flex-col sm:flex-row-reverse items-center gap-3 text-center sm:text-right">
          <div className="relative size-14 shrink-0 overflow-hidden rounded-full border border-hairline bg-surface-elevated">
            {memberB.avatarUrl ? (
              <Image src={memberB.avatarUrl} alt={memberB.name} fill className="object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-fg-subtle">
                <UserIcon className="size-7" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <Link
              href={`/profile/${memberB.id}`}
              className="font-heading text-lg sm:text-xl font-medium text-foreground hover:text-primary transition-colors flex items-center justify-center sm:justify-end gap-1"
            >
              {memberB.name}
              <ArrowUpRight className="size-3.5 opacity-60" />
            </Link>
            <div className="flex flex-wrap items-center justify-center sm:justify-end gap-2 font-mono text-nano text-fg-muted">
              {memberB.codeforcesHandle && (
                <span className="flex items-center gap-1">
                  <PlatformMark platform="codeforces" className="size-3" />
                  @{memberB.codeforcesHandle}
                </span>
              )}
              {memberB.leetcodeHandle && (
                <span className="flex items-center gap-1">
                  <PlatformMark platform="leetcode" className="size-3" />
                  {memberB.leetcodeHandle}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Head-to-Head Section ── */}
      <div className="rounded-panel border border-hairline bg-surface-elevated/30 p-5 backdrop-blur-sm">
        <div className="flex items-center gap-2 mb-4">
          <Swords className="size-4 text-primary" />
          <Eyebrow>Codeforces Head-to-Head</Eyebrow>
        </div>

        <div className="grid grid-cols-3 gap-2 text-center py-2 border-y border-hairline mb-4 font-mono">
          <div>
            <div className="text-2xl font-heading font-semibold text-primary">{headToHead.aAhead}</div>
            <div className="text-nano text-fg-muted uppercase tracking-caps">{memberA.name} Ahead</div>
          </div>
          <div>
            <div className="text-2xl font-heading font-semibold text-fg-subtle">{headToHead.ties}</div>
            <div className="text-nano text-fg-muted uppercase tracking-caps">Tied Contests</div>
          </div>
          <div>
            <div className="text-2xl font-heading font-semibold text-primary">{headToHead.bAhead}</div>
            <div className="text-nano text-fg-muted uppercase tracking-caps">{memberB.name} Ahead</div>
          </div>
        </div>

        {headToHead.sharedContests.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-hairline font-mono text-label text-fg-muted uppercase tracking-caps">
                <tr>
                  <th className="py-2 px-3">Contest</th>
                  <th className="py-2 px-3 text-center">{memberA.name} Rank</th>
                  <th className="py-2 px-3 text-center">{memberB.name} Rank</th>
                  <th className="py-2 px-3 text-right">Result</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline font-mono text-label">
                {headToHead.sharedContests.map((c) => {
                  const aWon = c.aRank < c.bRank;
                  const bWon = c.bRank < c.aRank;
                  return (
                    <tr key={c.contestId} className="hover:bg-surface-elevated/40">
                      <td className="py-2.5 px-3 font-body text-foreground truncate max-w-xs">{c.contestName}</td>
                      <td className={cn("py-2.5 px-3 text-center", aWon && "font-semibold text-primary")}>
                        #{c.aRank}
                      </td>
                      <td className={cn("py-2.5 px-3 text-center", bWon && "font-semibold text-primary")}>
                        #{c.bRank}
                      </td>
                      <td className="py-2.5 px-3 text-right">
                        {aWon ? (
                          <span className="text-primary">{memberA.name} +</span>
                        ) : bWon ? (
                          <span className="text-primary">{memberB.name} +</span>
                        ) : (
                          <span className="text-muted-foreground">Tie</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-4 text-center font-mono text-label text-fg-muted">
            No shared rated Codeforces contests recorded yet
          </div>
        )}
      </div>

      {/* ── Key Metrics Comparison ── */}
      <div className="space-y-3">
        <Eyebrow>All-Time & Year Statistics</Eyebrow>

        {compareMetric("Codeforces Rating", memberA.rating, memberB.rating)}
        {compareMetric("LeetCode Rating", memberA.leetcodeRating, memberB.leetcodeRating)}
        {compareMetric(
          "Total Problems Solved",
          memberA.allTime.overall.solved,
          memberB.allTime.overall.solved,
          "solved"
        )}
        {compareMetric(
          "Total Contests Given",
          memberA.allTime.overall.contests,
          memberB.allTime.overall.contests,
          "contests"
        )}
        {compareMetric(
          "This Year Solved",
          memberA.year.overall.solved,
          memberB.year.overall.solved,
          "solved"
        )}
        {compareMetric(
          "This Year Contests",
          memberA.year.overall.contests,
          memberB.year.overall.contests,
          "contests"
        )}
        {compareMetric(
          "CF Avg Problem Rating",
          memberA.allTime.codeforces?.avgProblemRating,
          memberB.allTime.codeforces?.avgProblemRating
        )}
      </div>

      {/* ── Overlaid Rating History ── */}
      {combinedSeries.length > 0 && (
        <div className="rounded-panel border border-hairline bg-surface/50 p-5 backdrop-blur-sm">
          <div className="mb-4 flex items-center justify-between">
            <Eyebrow>Contest Rating Trajectory</Eyebrow>
          </div>
          <RatingGraph series={combinedSeries} />
        </div>
      )}
    </div>
  );
}
