"use client";

import { useState, useMemo, useEffect, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { rankColor, ratingToRank, CF_RANKS, type CfRankKey } from "@/lib/cf-ranks";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { leaderboardService } from "@/lib/services/leaderboard";
import type { LeaderboardEntry, LeaderboardFilter, LeaderboardPlatform } from "@/types/api";
import { Search, Flame, Users, Code2, Trophy, User, Loader2 } from "lucide-react";

function getRankName(key: CfRankKey): string {
  return CF_RANKS.find((r) => r.key === key)?.name ?? key;
}

interface LeaderboardDashboardProps {
  initialEntries?: LeaderboardEntry[];
}

export default function LeaderboardDashboard({ initialEntries = [] }: LeaderboardDashboardProps) {
  const [platform, setPlatform] = useState<LeaderboardPlatform>("CODEFORCES");
  const [roleFilter, setRoleFilter] = useState<LeaderboardFilter>("ALL");
  const [entries, setEntries] = useState<LeaderboardEntry[]>(initialEntries);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const fetchLeaderboard = useCallback(async (p: LeaderboardPlatform, f: LeaderboardFilter) => {
    setLoading(true);
    try {
      const data = await leaderboardService.getLeaderboard(p, f);
      setEntries(data);
    } catch (err) {
      console.error("Failed to load leaderboard data:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  // Re-fetch whenever platform or roleFilter changes
  useEffect(() => {
    fetchLeaderboard(platform, roleFilter);
  }, [platform, roleFilter, fetchLeaderboard]);

  // Client-side search filtering
  const filteredEntries = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return entries;
    return entries.filter((e) => {
      const matchesName = e.name.toLowerCase().includes(query);
      const matchesHandle = (e.codeforcesHandle ?? "").toLowerCase().includes(query);
      return matchesName || matchesHandle;
    });
  }, [entries, searchQuery]);

  // Sort by rating descending
  const sortedEntries = useMemo(() => {
    return [...filteredEntries].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
  }, [filteredEntries]);

  // Podium splits (#1 center, #2 left, #3 right)
  const top1 = sortedEntries[0];
  const top2 = sortedEntries[1];
  const top3 = sortedEntries[2];
  const rank4Onwards = sortedEntries.slice(3);

  const clubStats = useMemo(
    () => ({
      totalParticipants: entries.length,
      rated: entries.filter((e) => e.rating != null && e.rating > 0).length,
    }),
    [entries]
  );

  const topRatedMember = useMemo(() => {
    const rated = entries.filter((e) => e.rating != null && e.rating > 0);
    if (rated.length === 0) return null;
    return [...rated].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))[0];
  }, [entries]);

  return (
    <div className="grid gap-8 lg:grid-cols-12">
      {/* ── LEFT COLUMN (~60% / 7 cols) ── */}
      <div className="space-y-6 lg:col-span-7">
        {/* Platform & Filter Controls */}
        <div className="space-y-4">
          {/* Platform Pills */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex rounded-panel border border-border bg-surface-2 p-1">
              {(
                [
                  { key: "CODEFORCES", label: "Codeforces" },
                  { key: "LEETCODE", label: "LeetCode" },
                ] as const
              ).map((p) => (
                <button
                  key={p.key}
                  onClick={() => setPlatform(p.key)}
                  className={`rounded-control px-4 py-1.5 text-xs font-semibold transition-all ${
                    platform === p.key
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-fg-muted hover:text-foreground"
                  }`}
                >
                  {p.label}
                </button>
              ))}
            </div>

            {/* Role Filter Buttons */}
            <div className="flex flex-wrap gap-1.5">
              {(
                [
                  { key: "ALL", label: "All" },
                  { key: "CORE", label: "Core" },
                  { key: "BATCH_REP", label: "Batch Rep" },
                  { key: "STUDENTS", label: "Students" },
                ] as const
              ).map((r) => (
                <button
                  key={r.key}
                  onClick={() => setRoleFilter(r.key)}
                  className={`rounded-full border px-3 py-1 text-label font-medium transition-all ${
                    roleFilter === r.key
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-fg-muted hover:border-hairline-strong hover:text-foreground"
                  }`}
                >
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          {/* Search Input */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
            <input
              type="text"
              placeholder={`Search ${platform === "CODEFORCES" ? "Codeforces" : "LeetCode"} members by name or handle...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-control border border-border bg-surface-2 py-2 pl-9 pr-4 text-xs text-foreground placeholder:text-fg-muted focus:border-primary focus:outline-none"
            />
          </div>
        </div>

        {loading ? (
          <div className="flex min-h-[300px] flex-col items-center justify-center font-mono text-xs text-fg-muted uppercase">
            <Loader2 className="mb-2 size-6 animate-spin text-primary" />
            Loading {platform} standings...
          </div>
        ) : (
          <>
            {/* TOP 3 PODIUM */}
            {sortedEntries.length > 0 && (
              <div className="pt-2">
                <h3 className="mb-3 text-xs font-semibold tracking-wider text-fg-muted uppercase">
                  Top Performers ({platform === "CODEFORCES" ? "Codeforces" : "LeetCode"})
                </h3>
                <div className="grid grid-cols-3 gap-3 items-end">
                  {/* #2 Silver (Left) */}
                  {top2 ? (
                    <PodiumCard entry={top2} place={2} platform={platform} />
                  ) : (
                    <div className="h-36 rounded-panel border border-dashed border-border/50" />
                  )}

                  {/* #1 Gold (Center, Dominant) */}
                  {top1 ? (
                    <PodiumCard entry={top1} place={1} platform={platform} />
                  ) : (
                    <div className="h-44 rounded-panel border border-dashed border-border/50" />
                  )}

                  {/* #3 Bronze (Right) */}
                  {top3 ? (
                    <PodiumCard entry={top3} place={3} platform={platform} />
                  ) : (
                    <div className="h-36 rounded-panel border border-dashed border-border/50" />
                  )}
                </div>
              </div>
            )}

            {/* RANK 4+ LIST */}
            <div className="space-y-2">
              <h3 className="text-xs font-semibold tracking-wider text-fg-muted uppercase">
                Rankings
              </h3>
              {rank4Onwards.length > 0 ? (
                <div className="space-y-2">
                  {rank4Onwards.map((entry, index) => {
                    const rankNum = index + 4;
                    const cfRank = ratingToRank(entry.rating);
                    const color = platform === "CODEFORCES" ? rankColor(cfRank) : "var(--primary)";
                    return (
                      <Link
                        key={entry.id}
                        href={`/profile/${entry.id}`}
                        className="flex items-center justify-between rounded-panel border border-border bg-surface-2 px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <span className="w-6 text-center font-mono text-xs font-bold text-fg-muted">
                            #{rankNum}
                          </span>
                          <div className="flex size-8 shrink-0 items-center justify-center rounded-full border border-border bg-background overflow-hidden">
                            {entry.avatarUrl ? (
                              <Image
                                src={entry.avatarUrl}
                                alt={entry.name}
                                width={32}
                                height={32}
                                className="size-full rounded-full object-cover"
                              />
                            ) : (
                              <User className="size-4 text-fg-muted" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span
                                className="truncate text-sm font-semibold hover:underline"
                                style={{ color }}
                              >
                                {entry.codeforcesHandle ? `@${entry.codeforcesHandle}` : entry.name}
                              </span>
                              <ClubRoleBadge clubRole={entry.clubRole} showIcon={false} />
                            </div>
                            <p className="truncate text-label text-fg-muted">{entry.name}</p>
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <div className="text-sm font-bold">
                            {entry.rating !== null && entry.rating > 0 ? entry.rating : "Unrated"}
                          </div>
                          {platform === "CODEFORCES" && entry.rating ? (
                            <div
                              className="text-micro capitalize font-medium"
                              style={{ color }}
                            >
                              {getRankName(cfRank)}
                            </div>
                          ) : (
                            <div className="text-micro text-fg-muted">
                              {platform === "LEETCODE" ? "LeetCode" : "Codeforces"}
                            </div>
                          )}
                        </div>
                      </Link>
                    );
                  })}
                </div>
              ) : sortedEntries.length <= 3 && sortedEntries.length > 0 ? (
                <p className="py-4 text-center text-xs text-fg-muted">End of list.</p>
              ) : (
                <p className="py-6 text-center text-xs text-fg-muted">
                  No members found matching your filter criteria.
                </p>
              )}
            </div>
          </>
        )}
      </div>

      {/* ── RIGHT COLUMN (~40% / 5 cols) ── */}
      <div className="space-y-6 lg:col-span-5">
        {/* Highest-rated member */}
        {topRatedMember && (
          <Card className="relative overflow-hidden border-amber-500/30 bg-gradient-to-br from-amber-500/5 via-surface-2 to-surface-2">
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2 text-amber-400">
                <Flame className="size-4" />
                <span className="text-xs font-bold tracking-wide uppercase">
                  Top Ranked ({platform})
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <Link
                href={`/profile/${topRatedMember.id}`}
                className="group flex items-center gap-4 rounded-panel border border-amber-500/20 bg-background/50 p-3 transition-all hover:border-amber-500/40"
              >
                <div className="flex size-12 shrink-0 items-center justify-center rounded-full border-2 border-amber-400/50 bg-surface-2 overflow-hidden">
                  {topRatedMember.avatarUrl ? (
                    <Image
                      src={topRatedMember.avatarUrl}
                      alt={topRatedMember.name}
                      width={48}
                      height={48}
                      className="size-full rounded-full object-cover"
                    />
                  ) : (
                    <User className="size-6 text-amber-400" />
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <h4
                    className="truncate text-base font-bold group-hover:underline"
                    style={{
                      color:
                        platform === "CODEFORCES"
                          ? rankColor(ratingToRank(topRatedMember.rating))
                          : "var(--primary)",
                    }}
                  >
                    {topRatedMember.name}
                  </h4>
                  {topRatedMember.codeforcesHandle && (
                    <p className="text-xs text-fg-muted">@{topRatedMember.codeforcesHandle}</p>
                  )}
                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-xs font-semibold text-amber-400">
                      {topRatedMember.rating} rating
                    </span>
                    <ClubRoleBadge clubRole={topRatedMember.clubRole} showIcon={false} />
                  </div>
                </div>
              </Link>
            </CardContent>
          </Card>
        )}

        {/* Club Stats Panel */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Trophy className="size-4 text-primary" />
              Club Stats
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-3 text-center">
              <div className="rounded-panel border border-border bg-surface-2 p-3">
                <div className="flex justify-center mb-1 text-primary">
                  <Users className="size-4" />
                </div>
                <div className="text-xl font-bold">{clubStats.totalParticipants}</div>
                <div className="text-label text-fg-muted">Members Listed</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-3">
                <div className="flex justify-center mb-1 text-cf-master">
                  <Code2 className="size-4" />
                </div>
                <div className="text-xl font-bold">{clubStats.rated}</div>
                <div className="text-label text-fg-muted">Rated on {platform}</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// ── Podium Card Sub-Component ──
function PodiumCard({
  entry,
  place,
  platform,
}: {
  entry: LeaderboardEntry;
  place: 1 | 2 | 3;
  platform: LeaderboardPlatform;
}) {
  const cfRank = ratingToRank(entry.rating);
  const color = platform === "CODEFORCES" ? rankColor(cfRank) : "var(--primary)";

  const placeConfig = {
    1: {
      medal: "🥇",
      height: "h-48",
      borderColor: "border-amber-400/50",
      glow: "shadow-[0_0_20px_rgba(251,191,36,0.15)]",
    },
    2: {
      medal: "🥈",
      height: "h-40",
      borderColor: "border-slate-300/40",
      glow: "shadow-[0_0_15px_rgba(203,213,225,0.1)]",
    },
    3: {
      medal: "🥉",
      height: "h-36",
      borderColor: "border-amber-700/40",
      glow: "shadow-[0_0_15px_rgba(180,83,9,0.1)]",
    },
  }[place];

  return (
    <Link
      href={`/profile/${entry.id}`}
      className={`group relative flex flex-col justify-between rounded-panel border ${placeConfig.borderColor} bg-surface-2 p-3 text-center transition-all hover:-translate-y-1 ${placeConfig.height} ${placeConfig.glow}`}
    >
      <div className="absolute -top-3 left-1/2 -translate-x-1/2 text-lg">
        {placeConfig.medal}
      </div>

      <div className="pt-2">
        <div className="mx-auto flex size-10 items-center justify-center rounded-full border border-border bg-background overflow-hidden">
          {entry.avatarUrl ? (
            <Image
              src={entry.avatarUrl}
              alt={entry.name}
              width={40}
              height={40}
              className="size-full rounded-full object-cover"
            />
          ) : (
            <User className="size-5 text-fg-muted" />
          )}
        </div>
        <h4 className="mt-2 truncate text-xs font-bold group-hover:underline" style={{ color }}>
          {entry.name}
        </h4>
        <p className="truncate text-micro text-fg-muted">
          {entry.codeforcesHandle ? `@${entry.codeforcesHandle}` : ""}
        </p>
      </div>

      <div className="mb-1">
        <div className="text-base font-extrabold">{entry.rating ?? "—"}</div>
        <div className="text-nano font-medium capitalize truncate" style={{ color }}>
          {platform === "CODEFORCES" ? getRankName(cfRank) : "LeetCode"}
        </div>
      </div>
    </Link>
  );
}
