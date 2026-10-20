"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRightLeft, Trophy, User as UserIcon } from "lucide-react";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";
import type { StatsLeaderboardEntry, StatsMetric } from "@/types/api";

interface StatsLeaderboardTableProps {
  entries: StatsLeaderboardEntry[];
  metric: StatsMetric;
  trackingSince?: string | null;
  loading?: boolean;
  className?: string;
}

export function StatsLeaderboardTable({
  entries,
  metric,
  trackingSince,
  loading = false,
  className,
}: StatsLeaderboardTableProps) {
  const currentUserId = useAuthStore((state) => state.user?.id);

  if (loading) {
    return (
      <div className={cn("w-full space-y-2", className)}>
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="flex h-16 w-full animate-pulse items-center justify-between rounded-card border border-hairline bg-surface-elevated/30 px-4"
          >
            <div className="flex items-center gap-4">
              <div className="size-6 rounded bg-muted/40" />
              <div className="size-9 rounded-full bg-muted/40" />
              <div className="space-y-1.5">
                <div className="h-3.5 w-32 rounded bg-muted/40" />
                <div className="h-2.5 w-20 rounded bg-muted/20" />
              </div>
            </div>
            <div className="h-6 w-24 rounded bg-muted/30" />
          </div>
        ))}
      </div>
    );
  }

  if (!entries || entries.length === 0) {
    return (
      <div className="rounded-panel border border-dashed border-hairline py-16 px-4 text-center">
        <Trophy className="mx-auto size-9 text-muted-foreground/50 mb-3" />
        <h3 className="font-heading text-lg font-medium text-foreground">
          No records found for this period
        </h3>
        <p className="mt-1 text-label text-fg-muted max-w-sm mx-auto">
          {metric === "AVG_PROBLEM_RATING"
            ? "Members need at least 10 rated problem solves in this period to appear here."
            : trackingSince
            ? `LeetCode tracking started on ${trackingSince}. Period totals require at least one baseline snapshot.`
            : "No activity recorded for this platform and timeframe yet."}
        </p>
      </div>
    );
  }

  // Helper to format rank representation (e.g., ties shown as "=3")
  const formatRank = (entry: StatsLeaderboardEntry, index: number) => {
    const isTie =
      (index > 0 && entries[index - 1].value === entry.value) ||
      (index < entries.length - 1 && entries[index + 1].value === entry.value);

    return isTie ? `=${entry.rank}` : `#${entry.rank}`;
  };

  const getMetricSuffix = (metric: StatsMetric) => {
    switch (metric) {
      case "SOLVED":
        return "solved";
      case "CONTESTS":
        return "contests";
      case "AVG_PROBLEM_RATING":
        return "avg rating";
      case "ATTENDANCE":
        return "events";
      case "OVERALL_SCORE":
        return "pts";
      default:
        return "";
    }
  };

  return (
    <div className={cn("w-full overflow-hidden rounded-panel border border-hairline bg-surface/50 backdrop-blur-sm", className)}>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm" role="table">
          <thead className="border-b border-hairline bg-surface-elevated/40 font-mono text-label tracking-caps text-fg-muted uppercase">
            <tr>
              <th scope="col" className="w-16 px-4 py-3 text-center">
                Rank
              </th>
              <th scope="col" className="px-4 py-3">
                Member
              </th>
              <th scope="col" className="hidden sm:table-cell px-4 py-3">
                Role
              </th>
              <th scope="col" className="px-4 py-3 text-right">
                {metric === "AVG_PROBLEM_RATING" ? "Avg Rating" : "Score / Total"}
              </th>
              <th scope="col" className="w-12 px-3 py-3 text-center">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-hairline">
            {entries.map((entry, idx) => {
              const isCurrentUser = currentUserId === entry.userId;
              const rankDisplay = formatRank(entry, idx);
              const isPodium = entry.rank <= 3;

              return (
                <tr
                  key={`${entry.userId}-${entry.rank}`}
                  className={cn(
                    "group transition-colors duration-100 hover:bg-surface-elevated/40",
                    isCurrentUser && "bg-primary/5 ring-1 ring-inset ring-primary/20"
                  )}
                >
                  {/* ── Rank Column ── */}
                  <td className="px-4 py-3 text-center font-mono">
                    <span
                      className={cn(
                        "inline-flex items-center justify-center rounded px-2 py-0.5 text-label font-semibold",
                        entry.rank === 1 && "bg-amber-500/15 text-amber-400 border border-amber-500/30",
                        entry.rank === 2 && "bg-slate-300/15 text-slate-300 border border-slate-300/30",
                        entry.rank === 3 && "bg-amber-700/15 text-amber-600 border border-amber-700/30",
                        !isPodium && "text-muted-foreground"
                      )}
                    >
                      {rankDisplay}
                    </span>
                  </td>

                  {/* ── Member Column ── */}
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <Link
                        href={`/profile/${entry.userId}`}
                        className="relative size-9 shrink-0 overflow-hidden rounded-full border border-hairline bg-surface-elevated focus-visible:outline-2 focus-visible:outline-ring"
                      >
                        {entry.avatarUrl ? (
                          <Image
                            src={entry.avatarUrl}
                            alt={entry.name}
                            fill
                            sizes="36px"
                            className="object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center text-fg-subtle">
                            <UserIcon className="size-4" />
                          </div>
                        )}
                      </Link>

                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <Link
                            href={`/profile/${entry.userId}`}
                            className="font-medium text-foreground hover:text-primary transition-colors truncate focus-visible:outline-2 focus-visible:outline-ring rounded-sm"
                          >
                            {entry.name}
                          </Link>
                          {isCurrentUser && (
                            <span className="rounded bg-primary/20 px-1.5 py-0.2 text-nano font-mono text-primary uppercase">
                              You
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-2 text-nano font-mono text-fg-muted truncate">
                          {entry.codeforcesHandle && (
                            <span>@{entry.codeforcesHandle}</span>
                          )}
                          {entry.leetcodeHandle && (
                            <span className="opacity-70">LC: {entry.leetcodeHandle}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </td>

                  {/* ── Role Column ── */}
                  <td className="hidden sm:table-cell px-4 py-3">
                    <ClubRoleBadge clubRole={entry.clubRole} />
                  </td>

                  {/* ── Metric Value & Basis Column ── */}
                  <td className="px-4 py-3 text-right">
                    <div className="flex flex-col items-end">
                      <div className="font-heading text-base font-semibold text-foreground">
                        {(entry.value ?? 0).toLocaleString()}{" "}
                        <span className="font-mono text-nano text-fg-muted uppercase font-normal">
                          {getMetricSuffix(metric)}
                        </span>
                      </div>
                      {entry.basis && (
                        <div className="text-nano font-mono text-fg-muted truncate max-w-[180px]">
                          {entry.basis}
                        </div>
                      )}
                    </div>
                  </td>

                  {/* ── Action / Compare Column ── */}
                  <td className="px-3 py-3 text-center">
                    {currentUserId && !isCurrentUser ? (
                      <Link
                        href={`/compare?a=${currentUserId}&b=${entry.userId}`}
                        title={`Compare with ${entry.name}`}
                        className="inline-flex size-8 items-center justify-center rounded-control text-fg-subtle transition-colors hover:bg-surface-elevated hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring"
                      >
                        <ArrowRightLeft className="size-3.5" />
                      </Link>
                    ) : (
                      <span className="size-8 inline-block" />
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
