"use client";

import React from "react";
import Link from "next/link";
import Image from "next/image";
import { Flame, Trophy, User, ArrowRight, Sparkles } from "lucide-react";
import { BannerGraphic } from "./banner-graphic";
import { getActiveBannerForMember } from "@/lib/banner-config";
import type { LeaderboardEntry, LeaderboardPlatform } from "@/types/api";

interface SpotlightTopCardProps {
  topMember: LeaderboardEntry | null;
  platform: LeaderboardPlatform;
  onOpenLocker?: (member: LeaderboardEntry) => void;
}

export const SpotlightTopCard: React.FC<SpotlightTopCardProps> = ({
  topMember,
  platform,
  onOpenLocker,
}) => {
  if (!topMember) return null;

  const banner = getActiveBannerForMember(1, topMember.equippedBannerId, platform, topMember.rating);

  return (
    <div className="relative rounded-2xl p-[1.5px] overflow-hidden group">
      {/* Animated Rotating Border Beam (Magic UI / Aceternity Style) */}
      <div
        className="absolute inset-[-100%] animate-spin-slow opacity-80 pointer-events-none"
        style={{
          background:
            "conic-gradient(from 0deg, transparent 0deg, #f5b83d 60deg, #9b8cff 120deg, transparent 180deg)",
        }}
      />

      <div className="relative rounded-[15px] bg-[#0d0d16] p-4 sm:p-5 overflow-hidden border border-white/10 shadow-2xl">
        {/* Banner art behind the spotlight card */}
        <BannerGraphic banner={banner} showEffects={true} withScrim={true} />

        <div className="relative z-10">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2 text-amber-300">
              <Flame className="size-4 animate-pulse fill-amber-400" />
              <span className="text-[11px] font-bold uppercase tracking-wider font-mono">
                Reigning Champion ({platform === "LEETCODE" ? "LeetCode" : "Codeforces"})
              </span>
            </div>
            <span className="flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-400/20 text-amber-300 border border-amber-400/40">
              <Trophy className="size-3" /> #1 Rank
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="size-14 shrink-0 rounded-full p-[2px] bg-gradient-to-tr from-amber-400 to-purple-400 shadow-md">
              <div className="size-full rounded-full bg-background overflow-hidden flex items-center justify-center">
                {topMember.avatarUrl ? (
                  <Image
                    src={topMember.avatarUrl}
                    alt={topMember.name}
                    width={56}
                    height={56}
                    className="size-full object-cover"
                  />
                ) : (
                  <User className="size-6 text-white/50" />
                )}
              </div>
            </div>

            <div className="min-w-0 flex-1">
              <Link
                href={`/profile/${topMember.id}`}
                className="font-heading text-base font-bold text-white hover:underline truncate block drop-shadow-[0_1px_2px_rgba(0,0,0,0.8)]"
              >
                {topMember.name}
              </Link>
              {topMember.codeforcesHandle && (
                <p className="text-micro text-white/70 font-mono">
                  @{topMember.codeforcesHandle}
                </p>
              )}
              <div className="mt-1.5 flex items-center gap-2 flex-wrap">
                <span className="text-xs font-black text-amber-300 font-mono tabular-nums">
                  {topMember.rating ?? "—"} rating
                </span>
              </div>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-white/10 flex items-center justify-between gap-2">
            {onOpenLocker && (
              <button
                onClick={() => onOpenLocker(topMember)}
                className="text-xs font-semibold text-white/80 hover:text-white flex items-center gap-1 transition-colors"
              >
                <Sparkles className="size-3 text-purple-400" />
                Inspect Banner Locker
              </button>
            )}
            <Link
              href={`/profile/${topMember.id}`}
              className="text-xs font-semibold text-amber-400 hover:text-amber-300 flex items-center gap-1 transition-colors ml-auto"
            >
              View Profile <ArrowRight className="size-3" />
            </Link>
          </div>
        </div>
      </div>

      <style jsx>{`
        @keyframes spin-slow {
          0% {
            transform: rotate(0deg);
          }
          100% {
            transform: rotate(360deg);
          }
        }
        .animate-spin-slow {
          animation: spin-slow 8s linear infinite;
        }
      `}</style>
    </div>
  );
};
