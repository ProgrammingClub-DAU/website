"use client";

import React, { useState, useMemo, useEffect } from "react";
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
  RotateCcw,
  ShieldCheck,
  Flame,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { leaderboardService } from "@/lib/services/leaderboard";
import { MOCK_LEADERBOARD_ENTRIES } from "@/lib/mock-leaderboard";
import { PodiumSection } from "./leaderboard/podium-section";
import { RankingRow } from "./leaderboard/ranking-row";
import { BannerLockerDrawer } from "./leaderboard/banner-locker-drawer";
import { SpotlightTopCard } from "./leaderboard/spotlight-top-card";
import { RatingDistributionChart } from "./leaderboard/rating-distribution-chart";
import type { LeaderboardEntry, LeaderboardFilter, LeaderboardPlatform } from "@/types/api";

interface LeaderboardDashboardProps {
  initialEntries?: LeaderboardEntry[];
}

export default function LeaderboardDashboard({ initialEntries = [] }: LeaderboardDashboardProps) {
  const [platform, setPlatform] = useState<LeaderboardPlatform>("CODEFORCES");
  const [roleFilter, setRoleFilter] = useState<LeaderboardFilter>("ALL");
  // ── [LIVE / DUMMY DATA TOGGLE]: Switch between `initialEntries` and `MOCK_LEADERBOARD_ENTRIES` ──
  const [entries, setEntries] = useState<LeaderboardEntry[]>(initialEntries);
  // const [entries, setEntries] = useState<LeaderboardEntry[]>(MOCK_LEADERBOARD_ENTRIES);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMemberForLocker, setSelectedMemberForLocker] = useState<LeaderboardEntry | null>(null);
  const [isLockerOpen, setIsLockerOpen] = useState(false);
  const [lastUpdatedMin, setLastUpdatedMin] = useState(4);

  useEffect(() => {
    let ignore = false;

    // ── [DUMMY RANKINGS PREVIEW MODE] (Uncomment below to preview dummy rankings) ──
    // setEntries(MOCK_LEADERBOARD_ENTRIES);

    // ── [LIVE BACKEND DATA] (Default) ──
    leaderboardService
      .getLeaderboard(platform, roleFilter)
      .then((data) => {
        if (!ignore) {
          setEntries(data);
          setLastUpdatedMin(Math.floor(Math.random() * 8) + 2);
        }
      })
      .catch((err) => {
        if (!ignore) {
          console.error("Failed to load leaderboard data:", err);
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [platform, roleFilter]);

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

  // Club stats
  const clubStats = useMemo(() => {
    const rated = entries.filter((e) => e.rating != null && e.rating > 0);
    const totalRated = rated.length;
    const avgRating =
      totalRated > 0
        ? Math.round(rated.reduce((acc, curr) => acc + (curr.rating ?? 0), 0) / totalRated)
        : 0;
    const maxRating =
      totalRated > 0
        ? Math.max(...rated.map((e) => e.rating ?? 0))
        : 0;

    return {
      totalParticipants: entries.length,
      ratedCount: totalRated,
      avgRating,
      maxRating,
    };
  }, [entries]);

  const topRatedMember = useMemo(() => {
    const rated = entries.filter((e) => e.rating != null && e.rating > 0);
    if (rated.length === 0) return null;
    return [...rated].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))[0];
  }, [entries]);

  // Handle banner equip callback
  const handleBannerEquipped = (newBannerId: string) => {
    if (selectedMemberForLocker) {
      setEntries((prev) =>
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

  // Letter-by-letter reveal animation for Hero title
  const titleText = "Top Coders.";

  return (
    <div className="space-y-6">
      {/* ── SECTION 1: HERO & CONTROLS ── */}
      <div className="relative rounded-2xl border border-white/[0.08] bg-[#0b0b14]/80 p-4 md:p-6 backdrop-blur-xl shadow-2xl overflow-hidden">
        {/* Ambient background glow */}
        <div className="absolute -bottom-32 -right-32 size-80 rounded-full bg-cyan-600/15 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            {/* Live update pill */}
            <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-xs font-mono text-white/70 mb-3 shadow-inner">
              <span className="size-2 rounded-full bg-emerald-400 animate-ping" />
              <span>Synced {lastUpdatedMin} min ago</span>
              <span className="text-white/30">•</span>
              <span className="text-purple-300 flex items-center gap-1">
                Live Standings
              </span>
            </div>

            {/* Letter-by-letter reveal heading */}
            <h1 className="font-heading text-2xl sm:text-3xl md:text-4xl font-black tracking-tight text-white flex flex-wrap">
              {titleText.split("").map((char, index) => (
                <motion.span
                  key={index}
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.4,
                    delay: index * 0.04,
                    ease: [0.2, 0.8, 0.2, 1],
                  }}
                  className={char === "." ? "text-amber-400" : "bg-gradient-to-r from-white via-white/90 to-white/70 bg-clip-text text-transparent"}
                >
                  {char}
                </motion.span>
              ))}
            </h1>

            {/* <p className="mt-2 text-sm md:text-base text-white/60 max-w-xl text-pretty">
              Earn rating. Unlock exclusive animated banners. Climb the 3D podium to claim the crown.
            </p> */}
          </div>

        </div>

        {/* ── CONTROLS BAR: PLATFORM + FILTER CHIPS + SEARCH ── */}
        <div className="mt-5 pt-4 border-t border-white/10 flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
          {/* Sliding Pill Platform Switcher */}
          <div className="flex rounded-xl border border-white/10 bg-[#09090f] p-1 shadow-inner shrink-0">
            {(
              [
                { key: "CODEFORCES", label: "Codeforces" },
                { key: "LEETCODE", label: "LeetCode" },
              ] as const
            ).map((p) => {
              const active = platform === p.key;
              return (
                <button
                  key={p.key}
                  onClick={() => setPlatform(p.key)}
                  className={`relative rounded-lg px-4 py-1.5 text-xs font-bold transition-colors ${active ? "text-white" : "text-white/50 hover:text-white/80"
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

          {/* Role Filter Chips */}
          <div className="flex flex-wrap items-center gap-1.5 overflow-x-auto pb-1 md:pb-0">
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
                  onClick={() => setRoleFilter(r.key)}
                  className={`relative rounded-full px-3 py-1 text-xs font-semibold border transition-all ${active
                    ? "border-cyan-400 bg-cyan-500/15 text-cyan-300 shadow-[0_0_12px_rgba(6,182,212,0.25)]"
                    : "border-white/10 bg-white/[0.03] text-white/60 hover:border-white/20 hover:text-white"
                    }`}
                >
                  {r.label}
                </button>
              );
            })}
          </div>

          {/* Instant Search with Clear */}
          <div className="relative min-w-[240px] md:max-w-xs flex-1">
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

      {/* ── SECTION 2: 3D PODIUM ── */}
      {!loading && sortedEntries.length > 0 && (
        <PodiumSection
          top1={top1}
          top2={top2}
          top3={top3}
          platform={platform}
          onOpenLocker={openLockerForMember}
        />
      )}

      {/* ── MAIN CONTENT GRID: RANKINGS (7 cols) + SIDEBAR (5 cols) ── */}
      <div className="grid gap-8 lg:grid-cols-12">
        {/* Left Column: Rankings List (7 cols) */}
        <div className="space-y-4 lg:col-span-7">
          <div className="flex items-center justify-between">
            <h2 className="font-heading text-base font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <Trophy className="size-4 text-amber-400" />
              Rankings ({sortedEntries.length})
            </h2>
            <span className="text-xs text-white/50 font-mono">
              Click any row to inspect locker
            </span>
          </div>

          {/* Loading state */}
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, idx) => (
                <div
                  key={idx}
                  className="h-[62px] w-full rounded-xl border border-white/10 bg-white/[0.02] animate-pulse"
                />
              ))}
            </div>
          ) : rank4Onwards.length > 0 ? (
            <AnimatePresence mode="popLayout">
              <div className="space-y-2.5">
                {rank4Onwards.map((entry, index) => (
                  <RankingRow
                    key={entry.id}
                    entry={entry}
                    rankNum={index + 4}
                    platform={platform}
                    searchQuery={searchQuery}
                    onSelectMember={openLockerForMember}
                  />
                ))}
              </div>
            </AnimatePresence>
          ) : sortedEntries.length > 0 && sortedEntries.length <= 3 ? (
            <div className="rounded-xl border border-white/10 bg-[#0c0c14] p-8 text-center">
              <p className="text-sm text-white/60">
                All top performers are featured on the podium above.
              </p>
            </div>
          ) : (
            /* Friendly Empty State */
            <div className="rounded-2xl border border-white/10 bg-[#0c0c14] p-10 text-center space-y-3">
              <div className="mx-auto size-12 rounded-full bg-white/[0.05] flex items-center justify-center text-white/40">
                <Users className="size-6" />
              </div>
              <h3 className="text-base font-bold text-white">No members found</h3>
              <p className="text-xs text-white/50 max-w-sm mx-auto">
                No club members match your search &quot;{searchQuery}&quot; under the {roleFilter} filter.
              </p>
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-cyan-400 hover:text-cyan-300 pt-2"
                >
                  <RotateCcw className="size-3" /> Clear search query
                </button>
              )}
            </div>
          )}
        </div>

        {/* Right Column: Fintech Sidebar (5 cols) */}
        <div className="space-y-6 lg:col-span-5">
          {/* Spotlight Card with Rotating Border Beam */}
          <SpotlightTopCard
            topMember={topRatedMember}
            platform={platform}
            onOpenLocker={openLockerForMember}
          />

          {/* Club Stats Fintech Card */}
          <Card className="border border-white/10 bg-[#0d0d16]/90 shadow-xl backdrop-blur-md rounded-2xl">
            <CardHeader className="pb-3 border-b border-white/10">
              <CardTitle className="text-sm font-bold uppercase tracking-wider text-white flex items-center gap-2">
                <TrendingUp className="size-4 text-cyan-400" />
                Fintech Club Metrics
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
                    Rated on {platform === "LEETCODE" ? "LeetCode" : "Codeforces"}
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

          {/* Rating Distribution Breakdown Card */}
          <Card className="border border-white/10 bg-[#0d0d16]/90 shadow-xl backdrop-blur-md rounded-2xl">
            <CardHeader className="pb-2 border-b border-white/10">
              <CardTitle className="text-sm font-bold uppercase tracking-wider text-white flex items-center gap-2">
                <Sparkles className="size-4 text-purple-400" />
                Rating Tier Distribution
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <RatingDistributionChart entries={entries} platform={platform} />
            </CardContent>
          </Card>
        </div>
      </div>

      {/* ── BANNER LOCKER MODAL / DRAWER ── */}
      <BannerLockerDrawer
        member={selectedMemberForLocker}
        isOpen={isLockerOpen}
        onClose={() => setIsLockerOpen(false)}
        onBannerEquipped={handleBannerEquipped}
      />
    </div>
  );
}
