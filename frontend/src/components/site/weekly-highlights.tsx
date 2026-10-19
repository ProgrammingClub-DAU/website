"use client";

import React, { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Sparkles, Trophy, ArrowRight } from "lucide-react";
import { awardsService } from "@/lib/services/awards";
import { AwardBadge } from "@/components/site/award-badge";
import type { LatestWeeklyAwardsResponseDto, WeeklyAwardDto } from "@/types/api";

export function WeeklyHighlights() {
  const [data, setData] = useState<LatestWeeklyAwardsResponseDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;
    awardsService
      .getLatest()
      .then((res) => {
        if (!ignore) setData(res);
      })
      .catch((err) => {
        console.error("Failed to fetch latest weekly awards:", err);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="rounded-3xl border border-white/10 bg-[#0d0d16]/80 p-8 text-center backdrop-blur-md animate-pulse">
        <div className="h-4 w-32 bg-white/10 rounded mx-auto mb-3" />
        <div className="h-6 w-48 bg-white/10 rounded mx-auto" />
      </div>
    );
  }

  if (!data || !data.computed || !data.awards || data.awards.length === 0) {
    return (
      <section className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0d0d16]/70 p-6 md:p-8 backdrop-blur-md">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <span className="text-nano font-mono uppercase tracking-caps text-amber-400 flex items-center gap-1.5">
              <Sparkles className="size-3.5" />
              Weekly Awards
            </span>
            <h2 className="text-xl font-bold tracking-tight text-white">This Week&apos;s Club Honors</h2>
            <p className="text-xs text-fg-muted font-mono">
              Awards compute automatically every Monday at 01:00 IST for the preceding week.
            </p>
          </div>
          <Link
            href="/hall-of-fame"
            className="inline-flex items-center gap-1 text-xs font-mono text-primary hover:text-primary-hover transition-colors"
          >
            Past Winners
            <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </section>
    );
  }

  // Format week date nicely
  const weekDateStr = new Date(data.weekStart).toLocaleDateString("en-IN", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

  return (
    <section className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0d0d16]/90 p-6 md:p-8 backdrop-blur-md shadow-2xl">
      {/* Background glow decoration */}
      <div className="absolute -top-24 -right-24 size-96 rounded-full bg-amber-500/10 blur-3xl pointer-events-none" />

      <div className="relative z-10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/10 pb-4">
          <div>
            <span className="text-nano font-mono uppercase tracking-caps text-amber-400 flex items-center gap-1.5">
              <Trophy className="size-3.5" />
              Weekly Honors
            </span>
            <h2 className="text-xl font-bold tracking-tight text-white mt-1">
              Week of {weekDateStr}
            </h2>
          </div>
          <Link
            href="/hall-of-fame"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
          >
            All-Time Hall of Fame
            <ArrowRight className="size-3.5" />
          </Link>
        </div>

        {/* 3 Award Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {data.awards.map((award: WeeklyAwardDto) => (
            <div
              key={award.id}
              className="group relative rounded-2xl border border-white/10 bg-white/[0.02] p-5 hover:border-white/20 hover:bg-white/[0.04] transition-all flex flex-col justify-between"
            >
              <div className="space-y-4">
                <AwardBadge
                  awardType={award.awardType}
                  metricValue={award.metricValue}
                  platform={award.platform}
                  size="md"
                />

                <Link href={`/profile/${award.userId}`} className="flex items-center gap-3">
                  <div className="size-12 rounded-full overflow-hidden bg-surface-3 border border-white/10 shrink-0">
                    {award.avatarUrl ? (
                      <Image
                        src={award.avatarUrl}
                        alt={award.userName}
                        width={48}
                        height={48}
                        className="size-full object-cover"
                      />
                    ) : (
                      <div className="size-full flex items-center justify-center text-sm font-bold text-fg-muted">
                        {award.userName.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <div>
                    <h3 className="font-semibold text-white group-hover:text-primary transition-colors text-sm">
                      {award.userName}
                    </h3>
                    <div className="text-micro font-mono text-fg-muted">
                      {award.codeforcesHandle ? `@${award.codeforcesHandle}` : "Member"}
                    </div>
                  </div>
                </Link>
              </div>

              <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-nano font-mono text-fg-subtle">
                <span>Verified Activity</span>
                <Link
                  href={`/profile/${award.userId}`}
                  className="text-primary hover:underline flex items-center gap-1"
                >
                  Profile
                  <ArrowRight className="size-3" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
