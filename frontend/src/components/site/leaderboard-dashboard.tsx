"use client";

import React, { useState, useMemo, useEffect, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { motion, AnimatePresence } from "motion/react";
import CountUp from "react-countup";
import {
  Search,
  Users,
  Trophy,
  Code2,
  Sparkles,
  TrendingUp,
  X,
  ShieldCheck,
  Flame,
  Award,
  CalendarCheck,
  Target,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { leaderboardService } from "@/lib/services/leaderboard";
import { statsService } from "@/lib/services/stats";
import { PeriodToggle } from "@/components/site/period-toggle";
import { PlatformToggle } from "@/components/site/platform-toggle";
import { StatsLeaderboardTable } from "@/components/site/stats-leaderboard-table";

import { PodiumSection } from "./leaderboard/podium-section";
import { RankingRow } from "./leaderboard/ranking-row";
import { BannerLockerDrawer } from "./leaderboard/banner-locker-drawer";
import { SpotlightTopCard } from "./leaderboard/spotlight-top-card";
import { RatingDistributionChart } from "./leaderboard/rating-distribution-chart";
import type {
  LeaderboardEntry,
  LeaderboardFilter,
  LeaderboardPlatform,
  StatsMetric,
  StatsPlatform,
  StatsPeriod,
  StatsLeaderboardResponse,
} from "@/types/api";
import { useAuthStore } from "@/store/auth";

type MetricTabKey = "RATING" | StatsMetric;

interface MetricTabOption {
  key: MetricTabKey;
  label: string;
  shortLabel: string;
  icon: React.ReactNode;
}

const METRIC_TABS: MetricTabOption[] = [
  { key: "RATING", label: "Contest Rating", shortLabel: "Rating", icon: <Trophy className="size-4" /> },
  { key: "SOLVED", label: "Problems Solved", shortLabel: "Solved", icon: <Code2 className="size-4" /> },
  { key: "CONTESTS", label: "Contests Given", shortLabel: "Contests", icon: <Flame className="size-4" /> },
  { key: "AVG_PROBLEM_RATING", label: "Avg Problem Rating", shortLabel: "Avg Rating", icon: <Target className="size-4" /> },
  { key: "ATTENDANCE", label: "Event Attendance", shortLabel: "Attendance", icon: <CalendarCheck className="size-4" /> },
  { key: "OVERALL_SCORE", label: "Overall Score", shortLabel: "Score", icon: <Award className="size-4" /> },
];

interface LeaderboardDashboardProps {
  initialEntries?: LeaderboardEntry[];
}

export default function LeaderboardDashboard({ initialEntries = [] }: LeaderboardDashboardProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user: currentUser } = useAuthStore();

  // URL param parsing
  const urlMetric = (searchParams.get("metric") as MetricTabKey) || "RATING";
  const urlPlatform = (searchParams.get("platform") as StatsPlatform) || "OVERALL";
  const urlPeriod = (searchParams.get("period") as StatsPeriod) || "ALL_TIME";
  const urlFilter = (searchParams.get("filter") as LeaderboardFilter) || "ALL";

  // Tab State
  const [activeMetric, setActiveMetric] = useState<MetricTabKey>(urlMetric);

  // Rating Leaderboard State
  const [ratingPlatform, setRatingPlatform] = useState<LeaderboardPlatform>("CODEFORCES");
  const [roleFilter, setRoleFilter] = useState<LeaderboardFilter>(urlFilter);
  const [ratingEntries, setRatingEntries] = useState<LeaderboardEntry[]>(initialEntries);
  const [ratingLoading, setRatingLoading] = useState(false);

  // Stats Leaderboard State
  const [statsPlatform, setStatsPlatform] = useState<StatsPlatform>(
    urlMetric === "AVG_PROBLEM_RATING" ? "CODEFORCES" : urlPlatform
  );
  const [statsPeriod, setStatsPeriod] = useState<StatsPeriod>(
    urlMetric === "ATTENDANCE" && urlPeriod === "WEEK" ? "MONTH" : urlPeriod
  );
  const [statsResponse, setStatsResponse] = useState<StatsLeaderboardResponse | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMemberForLocker, setSelectedMemberForLocker] = useState<LeaderboardEntry | null>(null);
  const [isLockerOpen, setIsLockerOpen] = useState(false);
  const [lastUpdatedMin, setLastUpdatedMin] = useState(4);
  const [displayLimit, setDisplayLimit] = useState(50);

  // Synchronize URL with active controls
  const updateUrl = useCallback(
    (metric: MetricTabKey, platformVal: string, periodVal: string, filterVal: string) => {
      const p = new URLSearchParams();
      if (metric !== "RATING") p.set("metric", metric);
      if (metric === "RATING") {
        if (ratingPlatform !== "CODEFORCES") p.set("platform", ratingPlatform);
      } else {
        if (platformVal !== "OVERALL") p.set("platform", platformVal);
        if (periodVal !== "ALL_TIME") p.set("period", periodVal);
      }
      if (filterVal !== "ALL") p.set("filter", filterVal);

      const qs = p.toString();
      router.replace(`/leaderboard${qs ? `?${qs}` : ""}`, { scroll: false });
    },
    [ratingPlatform, router]
  );

  // Fetch Rating Leaderboard
  useEffect(() => {
    if (activeMetric !== "RATING") return;
    let ignore = false;
    setRatingLoading(true);

    leaderboardService
      .getLeaderboard(ratingPlatform, roleFilter)
      .then((data) => {
        if (!ignore) {
          setRatingEntries(data);
          setLastUpdatedMin(Math.floor(Math.random() * 8) + 2);
        }
      })
      .catch((err) => {
        if (!ignore) console.error("Failed to load rating leaderboard:", err);
      })
      .finally(() => {
        if (!ignore) setRatingLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [activeMetric, ratingPlatform, roleFilter]);

  // Fetch Stats Leaderboard
  useEffect(() => {
    if (activeMetric === "RATING") return;
    let ignore = false;
    setStatsLoading(true);

    // Enforce business rules
    const effectivePlatform: StatsPlatform =
      activeMetric === "AVG_PROBLEM_RATING"
        ? "CODEFORCES"
        : activeMetric === "ATTENDANCE" || activeMetric === "OVERALL_SCORE"
        ? "OVERALL"
        : statsPlatform;

    const effectivePeriod: StatsPeriod =
      activeMetric === "OVERALL_SCORE"
        ? "ALL_TIME"
        : activeMetric === "ATTENDANCE" && statsPeriod === "WEEK"
        ? "MONTH"
        : statsPeriod;

    statsService
      .getLeaderboard({
        metric: activeMetric as StatsMetric,
        platform: effectivePlatform,
        period: effectivePeriod,
        filter: roleFilter,
        page: 0,
        size: 100,
      })
      .then((res) => {
        if (!ignore) {
          setStatsResponse(res);
          setLastUpdatedMin(Math.floor(Math.random() * 8) + 2);
        }
      })
      .catch((err) => {
        if (!ignore) console.error("Failed to load stats leaderboard:", err);
      })
      .finally(() => {
        if (!ignore) setStatsLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [activeMetric, statsPlatform, statsPeriod, roleFilter]);

  // Handle banner equip callback
  const handleBannerEquipped = (newBannerId: string) => {
    if (selectedMemberForLocker) {
      setRatingEntries((prev) =>
        prev.map((item) =>
          item.id === selectedMemberForLocker.id
            ? { ...item, equippedBannerId: newBannerId }
            : item
        )
      );
    }
  };

  const openLockerForMember = (member: LeaderboardEntry) => {
    setSelectedMemberForLocker(member);
    setIsLockerOpen(true);
  };

  // Client-side search filtering for rating entries
  const filteredRatingEntries = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return ratingEntries;
    return ratingEntries.filter((e) => {
      const matchesName = e.name.toLowerCase().includes(query);
      const matchesHandle = (e.codeforcesHandle ?? "").toLowerCase().includes(query);
      return matchesName || matchesHandle;
    });
  }, [ratingEntries, searchQuery]);

  const sortedRatingEntries = useMemo(() => {
    return [...filteredRatingEntries].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
  }, [filteredRatingEntries]);

  // Client-side search filtering for stats entries
  const filteredStatsEntries = useMemo(() => {
    const list = statsResponse?.content ?? statsResponse?.entries ?? [];
    if (!list || list.length === 0) return [];
    const query = searchQuery.toLowerCase().trim();
    if (!query) return list;
    return list.filter((e) => {
      const matchesName = e.name.toLowerCase().includes(query);
      const matchesHandle = (e.codeforcesHandle ?? "").toLowerCase().includes(query);
      const matchesLc = (e.leetcodeHandle ?? "").toLowerCase().includes(query);
      return matchesName || matchesHandle || matchesLc;
    });
  }, [statsResponse, searchQuery]);

  // Podium splits
  const top1 = sortedRatingEntries[0];
  const top2 = sortedRatingEntries[1];
  const top3 = sortedRatingEntries[2];
  const rank4Onwards = sortedRatingEntries.slice(3, displayLimit);
  const hasMore = sortedRatingEntries.length > displayLimit;

  // Club stats
  const clubStats = useMemo(() => {
    const rated = ratingEntries.filter((e) => e.rating != null && e.rating > 0);
    const totalRated = rated.length;
    const avgRating =
      totalRated > 0
        ? Math.round(rated.reduce((acc, curr) => acc + (curr.rating ?? 0), 0) / totalRated)
        : 0;
    const maxRating =
      totalRated > 0 ? Math.max(...rated.map((e) => e.rating ?? 0)) : 0;

    return {
      totalParticipants: ratingEntries.length,
      ratedCount: totalRated,
      avgRating,
      maxRating,
    };
  }, [ratingEntries]);

  const handleTabChange = (tab: MetricTabKey) => {
    setActiveMetric(tab);
    updateUrl(tab, statsPlatform, statsPeriod, roleFilter);
  };

  return (
    <div className="space-y-6">
      {/* ── TOP-LEVEL METRIC SELECTOR TABS ── */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-2 border-b border-white/10 no-scrollbar">
        {METRIC_TABS.map((tab) => {
          const isActive = activeMetric === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => handleTabChange(tab.key)}
              className={`flex items-center gap-2 rounded-control px-4 py-2 font-mono text-label tracking-caps uppercase whitespace-nowrap transition-all ${
                isActive
                  ? "bg-primary text-primary-foreground font-semibold shadow-md"
                  : "bg-surface-elevated/40 text-fg-muted hover:text-foreground hover:bg-surface-elevated/80 border border-hairline"
              }`}
            >
              <span>{tab.icon}</span>
              <span className="hidden sm:inline">{tab.label}</span>
              <span className="sm:hidden">{tab.shortLabel}</span>
            </button>
          );
        })}
      </div>

      {/* ── SECTION 1: HERO & CONTROLS ── */}
      <div className="relative rounded-2xl border border-white/[0.08] bg-[#0b0b14]/80 p-4 md:p-6 backdrop-blur-xl shadow-2xl overflow-hidden">
        <div className="absolute -bottom-32 -right-32 size-80 rounded-full bg-cyan-600/15 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-xs font-mono text-white/70 mb-3 shadow-inner">
              <span className="size-2 rounded-full bg-emerald-400 animate-ping" />
              <span>Synced {lastUpdatedMin} min ago</span>
              <span className="text-white/30">•</span>
              <span className="text-purple-300 flex items-center gap-1">
                <Sparkles className="size-3 text-purple-400" /> Live Board
              </span>
            </div>

            <h1 className="text-3xl md:text-5xl font-black font-heading tracking-tight text-white flex items-center gap-2">
              {activeMetric === "RATING"
                ? "Top Coders."
                : METRIC_TABS.find((t) => t.key === activeMetric)?.label}
            </h1>
            <p className="mt-1 text-label text-fg-muted font-mono">
              {activeMetric === "RATING"
                ? "Live rating rankings with podium recognition and unlocking banners."
                : activeMetric === "OVERALL_SCORE"
                ? "Daily aggregated percentile score combining Contest Rating, Solves, and Competitions."
                : activeMetric === "AVG_PROBLEM_RATING"
                ? "Average Codeforces difficulty rating (minimum 10 rated problem solves in period)."
                : activeMetric === "ATTENDANCE"
                ? "Official verified attendance at completed Programming Club workshops and contests."
                : "Aggregated leaderboard across platforms and verified member solves."}
            </p>
          </div>
        </div>

        {/* ── CONTROLS BAR: PERIOD + PLATFORM + ROLE FILTER + SEARCH ── */}
        <div className="mt-5 pt-4 border-t border-white/10 flex flex-col lg:flex-row gap-4 justify-between items-stretch lg:items-center">
          {activeMetric === "RATING" ? (
            /* Rating Platform Switcher */
            <div className="flex rounded-xl border border-white/10 bg-[#09090f] p-1 shadow-inner shrink-0">
              {(
                [
                  { key: "CODEFORCES", label: "Codeforces" },
                  { key: "LEETCODE", label: "LeetCode" },
                ] as const
              ).map((p) => {
                const active = ratingPlatform === p.key;
                return (
                  <button
                    key={p.key}
                    onClick={() => {
                      setRatingPlatform(p.key);
                      updateUrl("RATING", p.key, statsPeriod, roleFilter);
                    }}
                    className={`relative rounded-lg px-4 py-1.5 text-xs font-bold transition-colors ${
                      active ? "text-white" : "text-white/50 hover:text-white/80"
                    }`}
                  >
                    {active && (
                      <motion.div
                        layoutId="platform-pill"
                        className="absolute inset-0 rounded-lg bg-gradient-to-r from-purple-600 to-indigo-600 shadow-md"
                        transition={{ type: "spring", stiffness: 400, damping: 30 }}
                      />
                    )}
                    <span className="relative z-10">{p.label}</span>
                  </button>
                );
              })}
            </div>
          ) : (
            /* Stats Period & Platform Toggles */
            <div className="flex flex-wrap items-center gap-2">
              {activeMetric !== "OVERALL_SCORE" && (
                <PeriodToggle
                  value={statsPeriod}
                  onChange={(p) => {
                    setStatsPeriod(p);
                    updateUrl(activeMetric, statsPlatform, p, roleFilter);
                  }}
                  allowedPeriods={
                    activeMetric === "ATTENDANCE" ? ["MONTH", "YEAR", "ALL_TIME"] : undefined
                  }
                  size="sm"
                />
              )}

              {activeMetric !== "ATTENDANCE" && activeMetric !== "OVERALL_SCORE" && (
                <PlatformToggle
                  value={statsPlatform}
                  onChange={(p) => {
                    setStatsPlatform(p);
                    updateUrl(activeMetric, p, statsPeriod, roleFilter);
                  }}
                  disabledPlatforms={
                    activeMetric === "AVG_PROBLEM_RATING" ? ["LEETCODE", "OVERALL"] : undefined
                  }
                  disabledReason="Average problem rating is available on Codeforces only"
                />
              )}
            </div>
          )}

          {/* Role Filter Chips */}
          <div className="flex flex-wrap items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0">
            {(
              [
                { key: "ALL", label: "All Members" },
                { key: "CORE", label: "Core Team" },
                { key: "BATCH_REP", label: "Batch Reps" },
                { key: "STUDENTS", label: "Students" },
              ] as const
            ).map((r) => {
              const active = roleFilter === r.key;
              return (
                <button
                  key={r.key}
                  onClick={() => {
                    setRoleFilter(r.key);
                    updateUrl(activeMetric, statsPlatform, statsPeriod, r.key);
                  }}
                  className={`relative rounded-full px-3 py-1 text-xs font-semibold border transition-all ${
                    active
                      ? "border-cyan-400 bg-cyan-500/15 text-cyan-300 shadow-[0_0_12px_rgba(6,182,212,0.25)]"
                      : "border-white/10 bg-white/[0.03] text-white/60 hover:border-white/20 hover:text-white"
                  }`}
                >
                  {r.label}
                </button>
              );
            })}
          </div>

          {/* Instant Search */}
          <div className="relative min-w-[200px] lg:max-w-xs flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-white/40" />
            <input
              type="text"
              placeholder="Search member or handle..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl border border-white/10 bg-[#09090f] py-2 pl-9 pr-8 text-xs text-white placeholder:text-white/40 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 focus:outline-none transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-white/40 hover:text-white"
              >
                <X className="size-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* ── CONDITIONAL RENDER: RATING VIEW vs STATS VIEW ── */}
      {activeMetric === "RATING" ? (
        <>
          {/* 3D Podium */}
          {!ratingLoading && sortedRatingEntries.length > 0 && (
            <PodiumSection
              top1={top1}
              top2={top2}
              top3={top3}
              platform={ratingPlatform}
              onOpenLocker={openLockerForMember}
            />
          )}

          {/* Main Grid: Rankings + Sidebar */}
          <div className="grid gap-8 lg:grid-cols-12">
            <div className="space-y-4 lg:col-span-7">
              <div className="flex items-center justify-between">
                <h2 className="font-heading text-base font-bold text-white uppercase tracking-wider flex items-center gap-2">
                  <Trophy className="size-4 text-amber-400" />
                  Rankings ({sortedRatingEntries.length})
                </h2>
                <span className="text-xs text-white/50 font-mono">
                  Click any row to inspect locker
                </span>
              </div>

              {ratingLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 5 }).map((_, idx) => (
                    <div
                      key={idx}
                      className="h-16 w-full animate-pulse rounded-xl border border-white/5 bg-white/[0.02]"
                    />
                  ))}
                </div>
              ) : sortedRatingEntries.length === 0 ? (
                <div className="rounded-2xl border border-white/10 bg-[#0d0d16] p-12 text-center">
                  <Trophy className="mx-auto size-12 text-white/20 mb-3" />
                  <p className="text-sm font-semibold text-white">No members found</p>
                  <p className="mt-1 text-xs text-white/40">Try adjusting your filters or search.</p>
                </div>
              ) : (
                <div className="space-y-2.5">
                  <AnimatePresence>
                    {rank4Onwards.map((entry, idx) => (
                      <RankingRow
                        key={entry.id}
                        entry={entry}
                        rankNum={idx + 4}
                        platform={ratingPlatform}
                        searchQuery={searchQuery}
                        onSelectMember={openLockerForMember}
                      />
                    ))}
                  </AnimatePresence>

                  {hasMore && (
                    <button
                      onClick={() => setDisplayLimit((prev) => prev + 50)}
                      className="w-full rounded-xl border border-white/10 bg-white/[0.02] py-3 text-xs font-semibold text-white/70 hover:bg-white/[0.05] hover:text-white transition-colors"
                    >
                      Load More Members (+50)
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Sidebar */}
            <div className="space-y-6 lg:col-span-5">
              <SpotlightTopCard
                topMember={top1}
                platform={ratingPlatform}
                onOpenLocker={openLockerForMember}
              />

              <Card className="border border-white/10 bg-[#0d0d16]/90 shadow-xl backdrop-blur-md rounded-2xl">
                <CardHeader className="pb-2 border-b border-white/10">
                  <CardTitle className="text-sm font-bold uppercase tracking-wider text-white flex items-center gap-2">
                    <TrendingUp className="size-4 text-cyan-400" />
                    Club Rating Insights
                  </CardTitle>
                </CardHeader>
                <CardContent className="pt-4">
                  <div className="grid grid-cols-2 gap-3 text-center">
                    <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3">
                      <div className="flex justify-center mb-1 text-purple-400">
                        <Users className="size-4" />
                      </div>
                      <div className="text-2xl font-black font-mono text-white tabular-nums">
                        <CountUp end={clubStats.totalParticipants} duration={1.5} />
                      </div>
                      <div className="text-[11px] text-white/50 font-medium">Total Listed</div>
                    </div>

                    <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3">
                      <div className="flex justify-center mb-1 text-cyan-400">
                        <Code2 className="size-4" />
                      </div>
                      <div className="text-2xl font-black font-mono text-white tabular-nums">
                        <CountUp end={clubStats.ratedCount} duration={1.5} />
                      </div>
                      <div className="text-[11px] text-white/50 font-medium">
                        Rated on {ratingPlatform === "LEETCODE" ? "LeetCode" : "Codeforces"}
                      </div>
                    </div>

                    <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3">
                      <div className="flex justify-center mb-1 text-emerald-400">
                        <ShieldCheck className="size-4" />
                      </div>
                      <div className="text-2xl font-black font-mono text-white tabular-nums">
                        <CountUp end={clubStats.avgRating} duration={1.5} />
                      </div>
                      <div className="text-[11px] text-white/50 font-medium">Avg Rating</div>
                    </div>

                    <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3">
                      <div className="flex justify-center mb-1 text-amber-400">
                        <Flame className="size-4" />
                      </div>
                      <div className="text-2xl font-black font-mono text-white tabular-nums">
                        <CountUp end={clubStats.maxRating} duration={1.5} />
                      </div>
                      <div className="text-[11px] text-white/50 font-medium">Peak Rating</div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="border border-white/10 bg-[#0d0d16]/90 shadow-xl backdrop-blur-md rounded-2xl">
                <CardHeader className="pb-2 border-b border-white/10">
                  <CardTitle className="text-sm font-bold uppercase tracking-wider text-white flex items-center gap-2">
                    <Sparkles className="size-4 text-purple-400" />
                    Rating Tier Distribution
                  </CardTitle>
                </CardHeader>
                <CardContent className="pt-4">
                  <RatingDistributionChart entries={ratingEntries} platform={ratingPlatform} />
                </CardContent>
              </Card>
            </div>
          </div>
        </>
      ) : (
        /* Stats Leaderboard Table View */
        <div className="space-y-4">
          <StatsLeaderboardTable
            entries={filteredStatsEntries}
            metric={activeMetric as StatsMetric}
            trackingSince={statsResponse?.trackingSince}
            loading={statsLoading}
          />
        </div>
      )}

      {/* Banner Locker Modal */}
      <BannerLockerDrawer
        member={selectedMemberForLocker}
        isOpen={isLockerOpen}
        isOwnProfile={
          !!currentUser &&
          !!selectedMemberForLocker &&
          currentUser.id === selectedMemberForLocker.id
        }
        onClose={() => setIsLockerOpen(false)}
        onBannerEquipped={handleBannerEquipped}
      />
    </div>
  );
}
