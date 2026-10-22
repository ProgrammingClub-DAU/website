"use client";

import React from "react";
import Image from "next/image";
import { motion } from "motion/react";
import { User, TrendingUp, TrendingDown, Minus } from "lucide-react";

import { BannerGraphic } from "./banner-graphic";
import { getActiveBannerForMember } from "@/lib/banner-config";
import { rankColor, ratingToRank } from "@/lib/cf-ranks";
import { lcRankColor, ratingToLcRank, lcRankName } from "@/lib/lc-ranks";
import type { LeaderboardEntry, LeaderboardPlatform } from "@/types/api";

interface RankingRowProps {
  entry: LeaderboardEntry;
  rankNum: number;
  platform: LeaderboardPlatform;
  searchQuery?: string;
  onSelectMember?: (entry: LeaderboardEntry) => void;
}

export const RankingRow: React.FC<RankingRowProps> = ({
  entry,
  rankNum,
  platform,
  searchQuery = "",
  onSelectMember,
}) => {
  const banner = getActiveBannerForMember(rankNum, entry.equippedBannerId, platform, entry.rating);
  
  const cfRank = ratingToRank(entry.rating);
  const lcRank = ratingToLcRank(entry.rating);
  const color = platform === "CODEFORCES" ? rankColor(cfRank) : lcRankColor(lcRank);

  // Search match highlight helper
  const highlightMatch = (text: string) => {
    if (!searchQuery.trim()) return text;
    const regex = new RegExp(`(${searchQuery.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi");
    const parts = text.split(regex);
    return parts.map((part, i) =>
      regex.test(part) ? (
        <span key={i} className="bg-amber-400/30 text-amber-200 font-bold px-0.5 rounded">
          {part}
        </span>
      ) : (
        part
      )
    );
  };

  // Sparkline of the member's real recent contests. Nothing is drawn for fewer
  // than two: there used to be a made-up fallback curve here.
  const history = entry.contestHistory ?? [];
  const hasTrend = history.length >= 2;
  const minVal = hasTrend ? Math.min(...history) : 0;
  const maxVal = hasTrend ? Math.max(...history) : 0;
  const range = Math.max(maxVal - minVal, 1);
  const points = hasTrend
    ? history
        .map((val, idx) => {
          const x = (idx / (history.length - 1)) * 70;
          const y = 24 - ((val - minVal) / range) * 20;
          return `${x},${y}`;
        })
        .join(" ")
    : "";

  const change = entry.ratingChange;
  const hasChange = change != null;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.98 }}
      transition={{ duration: 0.25 }}
      whileHover={{ y: -2 }}
      className="group relative h-[62px] w-full rounded-xl overflow-hidden border border-white/[0.08] transition-all duration-200 hover:border-white/30 hover:shadow-[0_4px_24px_rgba(0,0,0,0.5)] cursor-pointer focus-visible:outline-2 focus-visible:outline-cyan-400"
      role="button"
      tabIndex={0}
      aria-label={`${entry.name}, rank ${rankNum}. Open banner locker`}
      onClick={() => onSelectMember?.(entry)}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelectMember?.(entry);
        }
      }}
    >
      {/* Background Banner Graphic */}
      <BannerGraphic banner={banner} showEffects={false} withScrim={true} />

      {/* Row Foreground Elements */}
      <div className="relative z-10 size-full flex items-center justify-between px-3 md:px-5 gap-3">
        {/* Left: Rank + Avatar + Name Details */}
        <div className="flex items-center gap-3 min-w-0 flex-1">
          {/* Rank Numeral */}
          <span className="w-8 shrink-0 text-center font-mono text-xs md:text-sm font-bold text-white/60 tabular-nums group-hover:text-white transition-colors">
            #{rankNum}
          </span>

          {/* Avatar with Banner Accent Ring */}
          <div
            className="size-9 shrink-0 rounded-full p-[1.5px] relative"
            style={{
              background: `linear-gradient(135deg, ${banner.colors.accent}, transparent)`,
            }}
          >
            <div className="size-full rounded-full bg-background overflow-hidden flex items-center justify-center">
              {entry.avatarUrl ? (
                <Image
                  src={entry.avatarUrl}
                  alt={entry.name}
                  width={36}
                  height={36}
                  className="size-full object-cover"
                />
              ) : (
                <User className="size-4 text-white/50" />
              )}
            </div>
          </div>

          {/* Name & Handle Overlaid Over Scrim */}
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="truncate text-xs md:text-sm font-bold text-white drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)] group-hover:text-amber-200 transition-colors">
                {highlightMatch(entry.name)}
              </span>
            </div>

            {entry.codeforcesHandle && (
              <p className="truncate text-micro text-white/70 font-mono">
                @{highlightMatch(entry.codeforcesHandle)}
              </p>
            )}
          </div>
        </div>

        {/* Middle: real recent-contest trend and last change */}
        {(hasTrend || hasChange) && (
          <div className="hidden lg:flex items-center gap-2 px-2 shrink-0">
            {hasTrend && (
              <svg
                className="w-[70px] h-[24px] overflow-visible"
                role="img"
                aria-label={`Rating over the last ${history.length} contests`}
              >
                <polyline
                  fill="none"
                  stroke={banner.colors.accent}
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  points={points}
                  opacity="0.85"
                />
              </svg>
            )}
            {hasChange && (
              <div
                className="flex items-center text-nano font-mono tabular-nums"
                title="Rating change in the latest rated contest"
              >
                {change === 0 ? (
                  <span className="text-white/40 flex items-center">
                    <Minus className="size-2.5 mr-0.5" />0
                  </span>
                ) : change > 0 ? (
                  <span className="text-emerald-400 flex items-center">
                    <TrendingUp className="size-3 mr-0.5" />+{change}
                  </span>
                ) : (
                  <span className="text-rose-400 flex items-center">
                    <TrendingDown className="size-3 mr-0.5" />
                    {change}
                  </span>
                )}
              </div>
            )}
          </div>
        )}

        {/* Right: Rating & Tier */}
        <div className="text-right shrink-0">
          <div className="font-mono text-sm md:text-base font-extrabold text-white tabular-nums drop-shadow-[0_1px_2px_rgba(0,0,0,0.8)]">
            {entry.rating !== null && entry.rating > 0 ? entry.rating : "Unrated"}
          </div>
          <div
            className="text-[10px] md:text-micro font-semibold capitalize tracking-wide"
            style={{ color: color }}
          >
            {platform === "LEETCODE" ? lcRankName(entry.rating) : (entry.tier || "Codeforces")}
          </div>
        </div>
      </div>
    </motion.div>
  );
};
