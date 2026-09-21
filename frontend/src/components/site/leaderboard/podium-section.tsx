"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import Tilt from "react-parallax-tilt";
import CountUp from "react-countup";
import confetti from "canvas-confetti";
import { Crown, Medal, User, Sparkles, ExternalLink } from "lucide-react";

import { BannerGraphic } from "./banner-graphic";
import { getActiveBannerForMember } from "@/lib/banner-config";
import { lcRankName } from "@/lib/lc-ranks";
import type { LeaderboardEntry, LeaderboardPlatform } from "@/types/api";

interface PodiumSectionProps {
  top1?: LeaderboardEntry;
  top2?: LeaderboardEntry;
  top3?: LeaderboardEntry;
  platform: LeaderboardPlatform;
  onOpenLocker?: (entry: LeaderboardEntry) => void;
}

export const PodiumSection: React.FC<PodiumSectionProps> = ({
  top1,
  top2,
  top3,
  platform,
  onOpenLocker,
}) => {
  // Fire celebration confetti when top 1 loads
  useEffect(() => {
    if (top1 && top1.rating) {
      try {
        confetti({
          particleCount: 55,
          spread: 70,
          origin: { y: 0.28 },
          colors: ["#F5B83D", "#CFD5E2", "#D08A5A", "#5E6AD2", "#00C2C7"],
          disableForReducedMotion: true,
        });
      } catch {
        // Safe fallback if canvas is restricted
      }
    }
  }, [top1, platform]);

  const podiumEntries = [
    { place: 2 as const, entry: top2, pillarHeight: "h-16 md:h-20", delay: 0.2 },
    { place: 1 as const, entry: top1, pillarHeight: "h-24 md:h-32", delay: 0.1 },
    { place: 3 as const, entry: top3, pillarHeight: "h-12 md:h-14", delay: 0.3 },
  ];

  return (
    <div className="relative pt-4 pb-1">
      {/* 3D Rising Podium Grid (2, 1, 3) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 md:gap-6 items-end max-w-4xl mx-auto">
        {podiumEntries.map(({ place, entry, pillarHeight, delay }) => {
          if (!entry) {
            return (
              <div
                key={place}
                className="hidden sm:flex flex-col items-center justify-end opacity-40"
              >
                <div className="w-full h-36 rounded-2xl border border-dashed border-white/10 bg-white/[0.02]" />
                <div className={`w-full ${pillarHeight} mt-2 rounded-t-xl bg-white/[0.03] border-t border-white/10`} />
              </div>
            );
          }

          const banner = getActiveBannerForMember(place, entry.equippedBannerId, platform, entry.rating);
          const isGold = place === 1;
          const isSilver = place === 2;
          const isBronze = place === 3;

          return (
            <div
              key={entry.id}
              className={`flex flex-col items-center justify-end ${
                isGold ? "order-1 sm:order-2 z-20" : isSilver ? "order-2 sm:order-1 z-10" : "order-3 sm:order-3 z-10"
              }`}
            >
              {/* Floating Rank Icon / Crown */}
              <div
                className={`relative mb-2 flex items-center justify-center transition-transform ${
                  isGold ? "animate-bounce" : ""
                }`}
                style={{
                  animationDuration: isGold ? "2.6s" : "0s",
                }}
              >
                {isGold && (
                  <div className="relative flex items-center justify-center">
                    <div className="absolute inset-0 size-10 rounded-full bg-amber-400/30 blur-md animate-pulse" />
                    <div className="relative size-10 rounded-full bg-gradient-to-b from-amber-300 to-amber-600 p-[1px] shadow-lg shadow-amber-500/20">
                      <div className="size-full rounded-full bg-[#181105] flex items-center justify-center">
                        <Crown className="size-5 text-amber-300 fill-amber-300 drop-shadow-[0_0_8px_rgba(245,184,61,0.8)]" />
                      </div>
                    </div>
                  </div>
                )}
                {isSilver && (
                  <div className="size-8 rounded-full bg-slate-300/20 p-[1px] shadow-md flex items-center justify-center border border-slate-300/50">
                    <Medal className="size-4 text-slate-200 fill-slate-300/60" />
                  </div>
                )}
                {isBronze && (
                  <div className="size-8 rounded-full bg-amber-800/20 p-[1px] shadow-md flex items-center justify-center border border-amber-600/50">
                    <Medal className="size-4 text-amber-500 fill-amber-600/60" />
                  </div>
                )}
              </div>

              {/* 3D Tilt Card */}
              <Tilt
                tiltMaxAngleX={12}
                tiltMaxAngleY={12}
                perspective={800}
                transitionSpeed={1000}
                scale={1.03}
                className="w-full"
              >
                <div
                  className={`group relative w-full rounded-2xl p-[1.5px] transition-all duration-300 ${
                    isGold
                      ? "shadow-[0_0_35px_rgba(245,184,61,0.25)]"
                      : isSilver
                      ? "shadow-[0_0_25px_rgba(207,213,226,0.15)]"
                      : "shadow-[0_0_25px_rgba(208,138,90,0.15)]"
                  }`}
                  style={{
                    background: `linear-gradient(135deg, ${banner.colors.border} 0%, rgba(255,255,255,0.1) 50%, ${banner.colors.border} 100%)`,
                  }}
                >
                  <div className="relative overflow-hidden rounded-[15px] bg-[#0c0c14] p-3 text-center">
                    {/* Dynamic Banner Art Background */}
                    <BannerGraphic banner={banner} showEffects={true} withScrim={true} />

                    {/* Member Details */}
                    <div className="relative z-10 flex flex-col items-center">
                      {/* Avatar with Halo Border */}
                      <div
                        className={`relative rounded-full p-[2px] mb-2.5 transition-transform duration-300 group-hover:scale-105 ${
                          isGold ? "ring-2 ring-amber-400/60" : ""
                        }`}
                        style={{
                          background: `linear-gradient(135deg, ${banner.colors.accent}, transparent)`,
                        }}
                      >
                        <div className="size-10 md:size-12 rounded-full overflow-hidden bg-surface-2 flex items-center justify-center border border-black/40">
                          {entry.avatarUrl ? (
                            <Image
                              src={entry.avatarUrl}
                              alt={entry.name}
                              width={56}
                              height={56}
                              className="size-full object-cover"
                            />
                          ) : (
                            <User className="size-6 text-fg-muted" />
                          )}
                        </div>
                        <span
                          className="absolute -bottom-1 -right-1 flex size-5 items-center justify-center rounded-full text-[10px] font-black border border-black shadow"
                          style={{
                            backgroundColor: banner.colors.accent,
                            color: isGold ? "#1a1202" : "#0c0d12",
                          }}
                        >
                          #{place}
                        </span>
                      </div>

                      {/* Name & Handle */}
                      <Link
                        href={`/profile/${entry.id}`}
                        className="truncate max-w-[90%] font-heading text-xs md:text-sm font-bold text-white group-hover:underline flex items-center gap-1 drop-shadow-[0_1px_3px_rgba(0,0,0,0.8)]"
                      >
                        {entry.name}
                        <ExternalLink className="size-3 opacity-0 group-hover:opacity-80 transition-opacity" />
                      </Link>

                      {entry.codeforcesHandle && (
                        <p className="truncate max-w-[85%] text-micro text-white/70 font-mono mt-0.5">
                          @{entry.codeforcesHandle}
                        </p>
                      )}

                      {/* Tabular CountUp Rating */}
                      <div className="mt-3 pt-2.5 border-t border-white/10 w-full flex flex-col items-center">
                        <div
                          className="font-mono text-lg md:text-xl font-black tabular-nums tracking-tight drop-shadow-sm"
                          style={{ color: banner.colors.text }}
                        >
                          {entry.rating ? (
                            <CountUp
                              start={0}
                              end={entry.rating}
                              duration={1.8}
                              delay={delay}
                              separator=","
                            />
                          ) : (
                            "—"
                          )}
                        </div>
                        <button
                          type="button"
                          onClick={() => onOpenLocker?.(entry)}
                          className="text-[10px] uppercase font-bold tracking-wider mt-1 flex items-center gap-1 hover:opacity-80 transition-opacity cursor-pointer"
                          style={{ color: banner.colors.accent }}
                        >
                          <Sparkles className="size-2.5" />
                          {platform === "LEETCODE" ? lcRankName(entry.rating) : (entry.tier || "Codeforces")}
                        </button>
                      </div>


                    </div>
                  </div>
                </div>
              </Tilt>

              {/* 3D Rising Pillar */}
              <div
                className={`hidden sm:flex w-full ${pillarHeight} mt-2.5 rounded-t-xl border-t border-x border-white/15 relative overflow-hidden items-center justify-center transition-all duration-500`}
                style={{
                  background: `linear-gradient(180deg, ${banner.colors.primary} 0%, rgba(11, 11, 18, 0.95) 100%)`,
                  boxShadow: `inset 0 1px 0 0 rgba(255,255,255,0.2), 0 10px 30px -10px ${banner.colors.glow}`,
                }}
              >
                {/* Pillar Ambient Lighting */}
                <div
                  className="absolute inset-0 opacity-25"
                  style={{
                    backgroundImage: `radial-gradient(ellipse at top, ${banner.colors.accent}, transparent 70%)`,
                  }}
                />
                <span
                  className="font-mono text-3xl md:text-5xl font-black opacity-45 select-none"
                  style={{ color: banner.colors.accent }}
                >
                  {place}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
