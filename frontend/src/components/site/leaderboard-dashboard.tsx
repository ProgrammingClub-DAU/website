"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { rankColor, CF_RANKS, type CfRankKey } from "@/lib/cf-ranks";
import type { LeaderboardEntry } from "@/types/api";
import { Search, Flame, Users, Code2, CalendarCheck, Trophy, User } from "lucide-react";
import Image from "next/image";

function getCfRank(rating: number): CfRankKey {
  if (rating >= 2400) return "grandmaster";
  if (rating >= 2100) return "master";
  if (rating >= 1900) return "candidate";
  if (rating >= 1600) return "expert";
  if (rating >= 1400) return "specialist";
  if (rating >= 1200) return "pupil";
  return "newbie";
}

function getRankName(key: CfRankKey): string {
  return CF_RANKS.find((r) => r.key === key)?.name ?? key;
}

type TabType = "Current" | "Overall";
type RoleFilter = "All" | "Core" | "Batch Rep" | "Students";

interface LeaderboardDashboardProps {
  entries: LeaderboardEntry[];
}

export default function LeaderboardDashboard({ entries }: LeaderboardDashboardProps) {
  const [tab, setTab] = useState<TabType>("Current");
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("All");

  // 1. Filter by Tab (Current = active in last 2 years)
  const tabFilteredEntries = useMemo(() => {
    if (tab === "Overall") return entries;

    const twoYearsAgo = new Date();
    twoYearsAgo.setFullYear(twoYearsAgo.getFullYear() - 2);

    return entries.filter((e) => {
      if (!e.lastEventDate) return true; // default include if date unparsed
      return new Date(e.lastEventDate) >= twoYearsAgo;
    });
  }, [entries, tab]);

  // 2. Filter by Search + Role
  const filteredEntries = useMemo(() => {
    return tabFilteredEntries.filter((e) => {
      // Search check
      const query = searchQuery.toLowerCase();
      const matchesSearch =
        e.name.toLowerCase().includes(query) ||
        e.codeforcesHandle.toLowerCase().includes(query);

      if (!matchesSearch) return false;

      // Role check
      const role = e.clubRole ?? "Club Participant";
      if (roleFilter === "All") return true;
      if (roleFilter === "Core") {
        return (
          role === "Convenor" ||
          role === "Deputy Convenor" ||
          role === "Core Member" ||
          role === "Associate Core Member"
        );
      }
      if (roleFilter === "Batch Rep") {
        return role === "Batch Representative";
      }
      if (roleFilter === "Students") {
        return role === "Club Participant" || !e.clubRole;
      }

      return true;
    });
  }, [tabFilteredEntries, searchQuery, roleFilter]);

  // Sort by rating descending
  const sortedEntries = useMemo(() => {
    return [...filteredEntries].sort((a, b) => b.rating - a.rating);
  }, [filteredEntries]);

  // Podium splits (#1 center, #2 left, #3 right)
  const top1 = sortedEntries[0];
  const top2 = sortedEntries[1];
  const top3 = sortedEntries[2];
  const rank4Onwards = sortedEntries.slice(3);

  // Overall Club Stats (computed dynamically from entries)
  const clubStats = useMemo(() => {
    const totalParticipants = entries.length;
    const totalSolved = entries.reduce((acc, curr) => acc + (curr.solvedCount || 0), 0);
    const activeThisYear = entries.filter((e) => (e.yearlyActivityCount || 0) > 0).length;
    const contests = 20; // Dynamic count of club contests held
    return { totalParticipants, totalSolved, activeThisYear, contests };
  }, [entries]);

  // Most Active Member of the Year
  const mostActiveMember = useMemo(() => {
    if (entries.length === 0) return null;
    return [...entries].sort(
      (a, b) => (b.yearlyActivityCount || b.solvedCount || 0) - (a.yearlyActivityCount || a.solvedCount || 0)
    )[0];
  }, [entries]);

  return (
    <div className="grid gap-8 lg:grid-cols-12">
      {/* ── LEFT COLUMN (~60% / 7 cols) ── */}
      <div className="space-y-6 lg:col-span-7">
        {/* Tabs & Search / Filters Controls */}
        <div className="space-y-4">
          {/* Main Tabs */}
          <div className="flex rounded-panel border border-border bg-surface-2 p-1 max-w-xs">
            {(["Current", "Overall"] as TabType[]).map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`flex-1 rounded-control py-1.5 text-xs font-medium transition-all ${
                  tab === t
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-fg-muted hover:text-foreground"
                }`}
              >
                {t}
              </button>
            ))}
          </div>

          {/* Search + Role Filters */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
              <input
                type="text"
                placeholder="Search members by name or handle..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-control border border-border bg-surface-2 py-2 pl-9 pr-4 text-xs text-foreground placeholder:text-fg-muted focus:border-primary focus:outline-none"
              />
            </div>

            {/* Role Filter Buttons */}
            <div className="flex flex-wrap gap-1.5">
              {(["All", "Core", "Batch Rep", "Students"] as RoleFilter[]).map((r) => (
                <button
                  key={r}
                  onClick={() => setRoleFilter(r)}
                  className={`rounded-full border px-2.5 py-1 text-[11px] font-medium transition-all ${
                    roleFilter === r
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-fg-muted hover:border-hairline-strong hover:text-foreground"
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* TOP 3 PODIUM */}
        {sortedEntries.length > 0 && (
          <div className="pt-2">
            <h3 className="mb-3 text-xs font-semibold tracking-wider text-fg-muted uppercase">
              Top Performers
            </h3>
            <div className="grid grid-cols-3 gap-3 items-end">
              {/* #2 Silver (Left) */}
              {top2 ? (
                <PodiumCard entry={top2} place={2} />
              ) : (
                <div className="h-36 rounded-panel border border-dashed border-border/50" />
              )}

              {/* #1 Gold (Center, Dominant) */}
              {top1 ? (
                <PodiumCard entry={top1} place={1} />
              ) : (
                <div className="h-44 rounded-panel border border-dashed border-border/50" />
              )}

              {/* #3 Bronze (Right) */}
              {top3 ? (
                <PodiumCard entry={top3} place={3} />
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
                const cfRank = getCfRank(entry.rating);
                const color = rankColor(cfRank);
                return (
                  <Link
                    key={entry.id}
                    href="/profile"
                    className="flex items-center justify-between rounded-panel border border-border bg-surface-2 px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="w-6 text-center font-mono text-xs font-bold text-fg-muted">
                        #{rankNum}
                      </span>
                      <div className="flex size-8 shrink-0 items-center justify-center rounded-full border border-border bg-background">
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
                            {entry.codeforcesHandle}
                          </span>
                          {entry.clubRole && entry.clubRole !== "Club Participant" && (
                            <span className="rounded-full border border-border bg-background px-1.5 py-0.2 text-[9px] text-fg-muted">
                              {entry.clubRole}
                            </span>
                          )}
                        </div>
                        <p className="truncate text-[11px] text-fg-muted">{entry.name}</p>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <div className="text-sm font-bold">{entry.rating}</div>
                      <div
                        className="text-[10px] capitalize font-medium"
                        style={{ color }}
                      >
                        {getRankName(cfRank)}
                      </div>
                    </div>
                  </Link>
                );
              })}
            </div>
          ) : sortedEntries.length <= 3 ? (
            <p className="py-4 text-center text-xs text-fg-muted">
              End of list.
            </p>
          ) : (
            <p className="py-6 text-center text-xs text-fg-muted">
              No members found matching your search.
            </p>
          )}
        </div>
      </div>

      {/* ── RIGHT COLUMN (~40% / 5 cols) ── */}
      <div className="space-y-6 lg:col-span-5">
        {/* 🔥 Most Active Member of the Year */}
        {mostActiveMember && (
          <Card className="relative overflow-hidden border-amber-500/30 bg-gradient-to-br from-amber-500/5 via-surface-2 to-surface-2">
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2 text-amber-400">
                <Flame className="size-4 animate-bounce" />
                <span className="text-xs font-bold tracking-wide uppercase">
                  Most Active Member of the Year
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <Link
                href="/profile"
                className="group flex items-center gap-4 rounded-panel border border-amber-500/20 bg-background/50 p-3 transition-all hover:border-amber-500/40"
              >
                <div className="flex size-12 shrink-0 items-center justify-center rounded-full border-2 border-amber-400/50 bg-surface-2">
                  {mostActiveMember.avatarUrl ? (
                    <Image
                      src={mostActiveMember.avatarUrl}
                      alt={mostActiveMember.name}
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
                    style={{ color: rankColor(getCfRank(mostActiveMember.rating)) }}
                  >
                    {mostActiveMember.name}
                  </h4>
                  <p className="text-xs text-fg-muted">
                    @{mostActiveMember.codeforcesHandle}
                  </p>
                  <div className="mt-1 text-xs font-semibold text-amber-400">
                    🔥 {mostActiveMember.solvedCount || (mostActiveMember.yearlyActivityCount! * 30)} activities
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
              <Trophy className="size-4" />
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
                <div className="text-[11px] text-fg-muted">Participants</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-3">
                <div className="flex justify-center mb-1 text-cf-master">
                  <Code2 className="size-4" />
                </div>
                <div className="text-xl font-bold">{clubStats.totalSolved}</div>
                <div className="text-[11px] text-fg-muted">Problems Solved</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-3">
                <div className="flex justify-center mb-1 text-cf-candidate">
                  <Flame className="size-4" />
                </div>
                <div className="text-xl font-bold">{clubStats.activeThisYear}</div>
                <div className="text-[11px] text-fg-muted">Active This Year</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-3">
                <div className="flex justify-center mb-1 text-cf-expert">
                  <CalendarCheck className="size-4" />
                </div>
                <div className="text-xl font-bold">{clubStats.contests}</div>
                <div className="text-[11px] text-fg-muted">Contests</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// ── Podium Card Sub-Component ──
function PodiumCard({ entry, place }: { entry: LeaderboardEntry; place: 1 | 2 | 3 }) {
  const cfRank = getCfRank(entry.rating);
  const color = rankColor(cfRank);

  const placeConfig = {
    1: {
      medal: "🥇",
      height: "h-48",
      borderColor: "border-amber-400/50",
      glow: "shadow-[0_0_20px_rgba(251,191,36,0.15)]",
      badgeBg: "bg-amber-400/10 text-amber-400 border-amber-400/30",
    },
    2: {
      medal: "🥈",
      height: "h-40",
      borderColor: "border-slate-300/40",
      glow: "shadow-[0_0_15px_rgba(203,213,225,0.1)]",
      badgeBg: "bg-slate-300/10 text-slate-300 border-slate-300/30",
    },
    3: {
      medal: "🥉",
      height: "h-36",
      borderColor: "border-amber-700/40",
      glow: "shadow-[0_0_15px_rgba(180,83,9,0.1)]",
      badgeBg: "bg-amber-700/10 text-amber-600 border-amber-700/30",
    },
  }[place];

  return (
    <Link
      href="/profile"
      className={`group relative flex flex-col justify-between rounded-panel border ${placeConfig.borderColor} bg-surface-2 p-3 text-center transition-all hover:-translate-y-1 ${placeConfig.height} ${placeConfig.glow}`}
    >
      {/* Top medal indicator */}
      <div className="absolute -top-3 left-1/2 -translate-x-1/2 text-lg">
        {placeConfig.medal}
      </div>

      <div className="pt-2">
        <div className="mx-auto flex size-10 items-center justify-center rounded-full border border-border bg-background">
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
        <h4
          className="mt-2 truncate text-xs font-bold group-hover:underline"
          style={{ color }}
        >
          {entry.name}
        </h4>
        <p className="truncate text-[10px] text-fg-muted">@{entry.codeforcesHandle}</p>
      </div>

      <div className="mb-1">
        <div className="text-base font-extrabold">{entry.rating}</div>
        <div
          className="text-[9px] font-medium capitalize truncate"
          style={{ color }}
        >
          {getRankName(cfRank)}
        </div>
      </div>
    </Link>
  );
}
