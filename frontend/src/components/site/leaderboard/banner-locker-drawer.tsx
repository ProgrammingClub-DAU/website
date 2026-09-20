"use client";

import React, { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { motion, AnimatePresence } from "motion/react";
import {
  X,
  Lock,
  Check,
  Sparkles,
  Crown,
  User,
  ExternalLink,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { BannerGraphic } from "./banner-graphic";
import {
  RATING_BANNERS,
  RANK_BANNERS,
  getBannerConfig,
  getRarityBadgeStyle,
  isBannerUnlocked,
  type BannerConfig,
} from "@/lib/banner-config";
import { leaderboardService } from "@/lib/services/leaderboard";
import type { LeaderboardEntry } from "@/types/api";

interface BannerLockerDrawerProps {
  member: LeaderboardEntry | null;
  isOpen: boolean;
  onClose: () => void;
  onBannerEquipped?: (newBannerId: string) => void;
}

export const BannerLockerDrawer: React.FC<BannerLockerDrawerProps> = ({
  member,
  isOpen,
  onClose,
  onBannerEquipped,
}) => {
  const [selectedBannerId, setSelectedBannerId] = useState<string | null>(null);
  const [shakingBannerId, setShakingBannerId] = useState<string | null>(null);
  const [equipping, setEquipping] = useState(false);
  const [equipError, setEquipError] = useState<string | null>(null);
  const [equipSuccess, setEquipSuccess] = useState<string | null>(null);

  if (!isOpen || !member) return null;

  const currentOrMaxRating = member.maxRating ?? member.rating ?? 0;
  const currentEquippedBanner = member.equippedBannerId ?? "rookie";
  const activePreviewBanner = getBannerConfig(selectedBannerId ?? currentEquippedBanner);

  // Trigger shake animation for locked banner
  const handleLockedClick = (bannerId: string) => {
    setShakingBannerId(bannerId);
    if (bannerId === "creator-vip") {
      setEquipError(
        "This Red VIP Banner is exclusively reserved for the 6 platform engineers who built this website (featured in Members & Credits)."
      );
    } else {
      setEquipError(`Reach ${getBannerConfig(bannerId).minRating} rating to unlock this banner.`);
    }
    setTimeout(() => {
      setShakingBannerId(null);
    }, 600);
  };

  // Equip banner handler
  const handleEquipBanner = async (banner: BannerConfig) => {
    if (!isBannerUnlocked(banner, currentOrMaxRating, member)) {
      handleLockedClick(banner.id);
      return;
    }

    setEquipping(true);
    setEquipError(null);
    setEquipSuccess(null);

    try {
      await leaderboardService.equipBanner(banner.id);
      setEquipSuccess(`Successfully equipped ${banner.name} banner!`);
      onBannerEquipped?.(banner.id);
      setSelectedBannerId(banner.id);
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : "Failed to equip banner. Check requirements.";
      setEquipError(errorMsg);
    } finally {
      setEquipping(false);
    }
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-md">
        {/* Backdrop click */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0"
          onClick={onClose}
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          className="relative z-10 w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl border border-white/15 bg-[#0e0e18] shadow-2xl p-4 sm:p-6"
        >
          {/* Header */}
          <div className="flex items-center justify-between pb-4 border-b border-white/10">
            <div>
              <h3 className="font-heading text-lg sm:text-xl font-bold text-white flex items-center gap-2">
                <Sparkles className="size-5 text-purple-400" />
                Banner Locker
              </h3>
              <p className="text-xs text-white/60 mt-0.5">
                Earn rating. Unlock banners. Rank banners override while in top 3.
              </p>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-white/50 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="size-5" />
            </button>
          </div>

          {/* Live Preview Card */}
          <div className="my-4">
            <div className="text-xs font-semibold text-white/70 uppercase tracking-wider mb-2 flex items-center justify-between">
              <span>Equipped Banner Preview</span>
              <span className="text-[11px] font-mono text-amber-400">
                Max Rating: {currentOrMaxRating}
              </span>
            </div>

            <div className="relative h-[84px] w-full rounded-xl overflow-hidden border border-white/20 p-3 shadow-lg">
              <BannerGraphic banner={activePreviewBanner} showEffects={true} withScrim={true} />

              <div className="relative z-10 size-full flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div
                    className="size-11 rounded-full p-[1.5px] overflow-hidden"
                    style={{
                      background: `linear-gradient(135deg, ${activePreviewBanner.colors.accent}, transparent)`,
                    }}
                  >
                    <div className="size-full rounded-full bg-background overflow-hidden flex items-center justify-center">
                      {member.avatarUrl ? (
                        <Image
                          src={member.avatarUrl}
                          alt={member.name}
                          width={44}
                          height={44}
                          className="size-full object-cover"
                        />
                      ) : (
                        <User className="size-5 text-white/60" />
                      )}
                    </div>
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-white drop-shadow-[0_1px_3px_rgba(0,0,0,0.8)]">
                      {member.name}
                    </h4>
                    <p className="text-micro text-white/70 font-mono">
                      {member.codeforcesHandle ? `@${member.codeforcesHandle}` : "Club Member"}
                    </p>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-xs font-bold flex items-center justify-end gap-1.5" style={{ color: activePreviewBanner.colors.accent }}>
                    {equipping && <Loader2 className="size-3 animate-spin text-purple-400" />}
                    {activePreviewBanner.name}
                  </div>
                  <div className="text-nano text-white/60 uppercase">
                    {activePreviewBanner.rarity}
                  </div>
                </div>
              </div>
            </div>

            {/* Error or Success notification */}
            {equipError && (
              <div className="mt-2 flex items-center gap-1.5 text-xs text-rose-400 bg-rose-500/10 border border-rose-500/20 px-3 py-1.5 rounded-lg">
                <AlertCircle className="size-3.5 shrink-0" />
                <span>{equipError}</span>
              </div>
            )}
            {equipSuccess && (
              <div className="mt-2 flex items-center gap-1.5 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-3 py-1.5 rounded-lg">
                <Check className="size-3.5 shrink-0" />
                <span>{equipSuccess}</span>
              </div>
            )}
          </div>

          {/* Section 1: Rating Banners Grid */}
          <div className="mt-6">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white/80 mb-3">
              Rating &amp; Special Banners
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {RATING_BANNERS.map((banner) => {
                const unlocked = isBannerUnlocked(banner, currentOrMaxRating, member);
                const isEquipped = currentEquippedBanner === banner.id;
                const rarityStyle = getRarityBadgeStyle(banner.rarity, banner.id);
                const isCreatorBanner = banner.id === "creator-vip";
                const progressPct = Math.min(
                  100,
                  banner.minRating === 0
                    ? 100
                    : Math.round((currentOrMaxRating / banner.minRating) * 100)
                );
                const isShaking = shakingBannerId === banner.id;

                return (
                  <motion.div
                    key={banner.id}
                    animate={
                      isShaking
                        ? {
                            x: [-8, 8, -6, 6, -3, 3, 0],
                            transition: { duration: 0.5 },
                          }
                        : {}
                    }
                    onClick={() => {
                      setSelectedBannerId(banner.id);
                      if (unlocked) {
                        handleEquipBanner(banner);
                      } else {
                        handleLockedClick(banner.id);
                      }
                    }}
                    className={`relative rounded-xl overflow-hidden border p-3 cursor-pointer transition-all duration-200 ${
                      isEquipped
                        ? isCreatorBanner
                          ? "border-rose-500 ring-2 ring-rose-500/50 shadow-[0_0_22px_rgba(244,63,94,0.45)]"
                          : "border-purple-400 ring-2 ring-purple-500/40 shadow-[0_0_20px_rgba(168,85,247,0.35)]"
                        : unlocked
                        ? isCreatorBanner
                          ? "border-rose-500/40 hover:border-rose-400 hover:scale-[1.01] shadow-[0_0_15px_rgba(244,63,94,0.2)]"
                          : "border-white/20 hover:border-white/40 hover:scale-[1.01]"
                        : "border-white/10 opacity-70 hover:opacity-90"
                    }`}
                  >
                    {/* Background Banner Graphic */}
                    <BannerGraphic
                      banner={banner}
                      showEffects={unlocked}
                      withScrim={true}
                      className={!unlocked ? "grayscale contrast-125 brightness-50" : ""}
                    />

                    {/* Content */}
                    <div className="relative z-10 flex flex-col justify-between h-full min-h-[64px]">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-sm font-bold text-white drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">
                              {banner.name}
                            </span>
                            <span
                              className={`text-[9px] font-bold px-1.5 py-0.5 rounded border ${rarityStyle.border} ${rarityStyle.bg} ${rarityStyle.text}`}
                            >
                              {isCreatorBanner ? "RED VIP" : banner.rarity}
                            </span>
                          </div>
                          <p className="text-[10px] text-white/70 line-clamp-1 mt-0.5">
                            {banner.description}
                          </p>
                        </div>

                        {/* Status Icon */}
                        {isEquipped ? (
                          <span
                            className={`flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full shrink-0 ${
                              isCreatorBanner
                                ? "text-rose-300 bg-rose-500/25 border border-rose-400/50"
                                : "text-purple-300 bg-purple-500/20 border border-purple-400/40"
                            }`}
                          >
                            <Check className="size-3" /> Equipped
                          </span>
                        ) : unlocked ? (
                          <span
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full shrink-0 ${
                              isCreatorBanner
                                ? "text-rose-300 bg-rose-500/20 border border-rose-400/40"
                                : "text-emerald-400 bg-emerald-500/15 border border-emerald-400/30"
                            }`}
                          >
                            {isCreatorBanner ? "VIP Unlocked" : "Unlocked"}
                          </span>
                        ) : isCreatorBanner ? (
                          <div className="flex items-center gap-1 text-[10px] font-semibold text-rose-300/80 bg-rose-950/60 border border-rose-500/30 px-2 py-0.5 rounded-full shrink-0">
                            <Crown className="size-3 text-rose-400" /> 6 Creators Only
                          </div>
                        ) : (
                          <div className="flex items-center gap-1 text-[10px] font-semibold text-white/50 bg-black/40 px-2 py-0.5 rounded-full shrink-0">
                            <Lock className="size-3 text-white/60" /> Reach {banner.minRating}
                          </div>
                        )}
                      </div>

                      {/* Progress Bar or Info for Locked Banners */}
                      {!unlocked && (
                        <div className="mt-2 pt-2 border-t border-white/10">
                          {isCreatorBanner ? (
                            <div className="flex items-center justify-between text-[10px] text-rose-300/80 font-mono">
                              <span>Platform Engineers</span>
                              <Link
                                href="/members"
                                onClick={(e) => e.stopPropagation()}
                                className="underline hover:text-rose-200"
                              >
                                View 6 Creators
                              </Link>
                            </div>
                          ) : (
                            <>
                              <div className="flex justify-between text-[10px] text-white/60 font-mono mb-1">
                                <span>Progress</span>
                                <span>
                                  {currentOrMaxRating} / {banner.minRating} ({progressPct}%)
                                </span>
                              </div>
                              <div className="h-1.5 w-full rounded-full bg-white/10 overflow-hidden">
                                <div
                                  className="h-full rounded-full bg-gradient-to-r from-purple-500 to-cyan-400 transition-all duration-500"
                                  style={{ width: `${progressPct}%` }}
                                />
                              </div>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </div>

          {/* Section 2: Top 3 Exclusive Rank Banners */}
          <div className="mt-8 pt-6 border-t border-white/10">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-amber-300 flex items-center gap-1.5">
                <Crown className="size-4 text-amber-400" />
                Top 3 Rank Banners (Exclusive)
              </h4>
              <span className="text-[10px] text-amber-300/70 font-mono">
                Auto-assigned while holding rank
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {Object.values(RANK_BANNERS).map((rankBanner) => {
                const holdsRank = member.rank === rankBanner.rank;
                return (
                  <div
                    key={rankBanner.id}
                    className={`relative rounded-xl overflow-hidden border p-3 ${
                      holdsRank
                        ? "border-amber-400 ring-2 ring-amber-400/40 shadow-[0_0_20px_rgba(245,184,61,0.3)]"
                        : "border-white/10 opacity-75"
                    }`}
                  >
                    <BannerGraphic banner={rankBanner} showEffects={holdsRank} withScrim={true} />

                    <div className="relative z-10 flex flex-col justify-between h-full min-h-[60px]">
                      <div>
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-white">
                            {rankBanner.name}
                          </span>
                          <span className="text-[9px] font-bold px-1 py-0.5 rounded bg-amber-400/20 text-amber-300 border border-amber-400/40">
                            #{rankBanner.rank}
                          </span>
                        </div>
                        <p className="text-[10px] text-white/60 mt-1">
                          {rankBanner.description}
                        </p>
                      </div>

                      <div className="mt-2 text-[10px] font-mono font-semibold">
                        {holdsRank ? (
                          <span className="text-amber-300 flex items-center gap-1">
                            <Check className="size-3" /> Currently Active
                          </span>
                        ) : (
                          <span className="text-white/40 flex items-center gap-1">
                            <Lock className="size-3" /> Reach #{rankBanner.rank} to equip
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Modal Footer / Profile link */}
          <div className="mt-6 pt-4 border-t border-white/10 flex items-center justify-between">
            <Link
              href={`/profile/${member.id}`}
              className="text-xs font-semibold text-purple-400 hover:text-purple-300 flex items-center gap-1 transition-colors"
            >
              Go to Full Profile <ExternalLink className="size-3" />
            </Link>
            <button
              onClick={onClose}
              className="rounded-lg px-4 py-1.5 text-xs font-semibold bg-white/10 hover:bg-white/20 text-white transition-colors"
            >
              Close Locker
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
