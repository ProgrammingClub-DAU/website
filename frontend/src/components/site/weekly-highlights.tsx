"use client";

import React, { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Sparkles, Trophy, ArrowRight } from "lucide-react";
import { awardsService } from "@/lib/services/awards";
import { AwardBadge } from "@/components/site/award-badge";
import type { LatestWeeklyAwardsResponseDto, WeeklyAwardDto } from "@/types/api";

/**
 * @param initialData the awards, read on the server. When given, the section
 *   renders with the page instead of as a loading skeleton that fills in later.
 *   Null when the server could not reach the backend; the browser then tries.
 * @param compact the leaderboard's version: one short strip of winners above the
 *   board, and nothing at all when no awards have been computed, so the board
 *   is not pushed down by an empty box. The home page uses the full section.
 */
export function WeeklyHighlights({
  initialData = null,
  compact = false,
}: {
  initialData?: LatestWeeklyAwardsResponseDto | null;
  compact?: boolean;
}) {
  const [data, setData] = useState<LatestWeeklyAwardsResponseDto | null>(initialData);
  const [loading, setLoading] = useState(initialData === null);

  useEffect(() => {
    if (initialData !== null) return;
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
  }, [initialData]);

  if (loading) {
    if (compact) {
      return (
        <div
          aria-hidden
          className="h-56 animate-pulse rounded-2xl border border-foreground/10 bg-card/60 lg:h-[108px]"
        />
      );
    }
    return (
      <div className="rounded-3xl border border-foreground/10 bg-card/80 p-8 text-center backdrop-blur-md animate-pulse">
        <div className="h-4 w-32 bg-foreground/10 rounded mx-auto mb-3" />
        <div className="h-6 w-48 bg-foreground/10 rounded mx-auto" />
      </div>
    );
  }

  if (!data || !data.computed || !data.awards || data.awards.length === 0) {
    if (compact) return null;
    return (
      <section className="relative overflow-hidden rounded-3xl border border-foreground/10 bg-card/70 p-6 md:p-8 backdrop-blur-md">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <span className="text-nano font-mono uppercase tracking-caps text-amber-700 dark:text-amber-400 flex items-center gap-1.5">
              <Sparkles className="size-3.5" />
              Weekly Awards
            </span>
            <h2 className="text-xl font-bold tracking-tight text-foreground">This Week&apos;s Club Honors</h2>
            <p className="text-xs text-fg-muted font-mono">
              Awards compute automatically every Monday at 01:00 IST for the preceding week.
            </p>
          </div>
          <Link
            href="/hall-of-fame"
            className="inline-flex items-center gap-1 text-xs font-mono text-primary hover:text-primary-hover transition-colors"
          >
            Hall of Fame
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

  if (compact) return <HonorsStrip awards={data.awards} weekDateStr={weekDateStr} />;

  return (
    <section className="relative overflow-hidden rounded-3xl border border-foreground/10 bg-card/90 p-6 md:p-8 backdrop-blur-md shadow-2xl">
      {/* Background glow decoration */}
      <div className="absolute -top-24 -right-24 size-96 rounded-full bg-amber-500/10 blur-3xl pointer-events-none" />

      <div className="relative z-10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-foreground/10 pb-4">
          <div>
            <span className="text-nano font-mono uppercase tracking-caps text-amber-700 dark:text-amber-400 flex items-center gap-1.5">
              <Trophy className="size-3.5" />
              Weekly Honors
            </span>
            <h2 className="text-xl font-bold tracking-tight text-foreground mt-1">
              Week of {weekDateStr}
            </h2>
          </div>
          <Link
            href="/hall-of-fame"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-foreground transition-colors"
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
              className="group relative rounded-2xl border border-foreground/10 bg-foreground/[0.02] p-5 hover:border-foreground/20 hover:bg-foreground/[0.04] transition-all flex flex-col justify-between"
            >
              <div className="space-y-4">
                <AwardBadge
                  awardType={award.awardType}
                  metricValue={award.metricValue}
                  platform={award.platform}
                  size="md"
                />

                <Link href={`/profile/${award.userId}`} className="flex items-center gap-3">
                  <MemberAvatar award={award} size={48} />
                  <div>
                    <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors text-sm">
                      {award.userName}
                    </h3>
                    <div className="text-micro font-mono text-fg-muted">
                      {award.codeforcesHandle ? `@${award.codeforcesHandle}` : "Member"}
                    </div>
                  </div>
                </Link>
              </div>

              <div className="mt-4 pt-3 border-t border-foreground/5 flex items-center justify-between text-nano font-mono text-fg-subtle">
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

/** The compact layout: a header line and one row of winners, each a link to their profile. */
function HonorsStrip({ awards, weekDateStr }: { awards: WeeklyAwardDto[]; weekDateStr: string }) {
  return (
    <section
      aria-labelledby="weekly-honors-strip"
      className="relative overflow-hidden rounded-2xl border border-foreground/10 bg-card/80 p-4 backdrop-blur-md"
    >
      <div className="pointer-events-none absolute -top-16 -right-16 size-48 rounded-full bg-amber-500/10 blur-3xl" />

      <div className="relative flex items-center justify-between gap-3">
        <h2
          id="weekly-honors-strip"
          className="flex flex-wrap items-center gap-x-2.5 gap-y-0.5 font-mono text-nano uppercase tracking-caps"
        >
          <span className="flex items-center gap-1.5 text-amber-700 dark:text-amber-400">
            <Trophy className="size-3.5" aria-hidden />
            Weekly Honors
          </span>
          <span className="text-fg-subtle">Week of {weekDateStr}</span>
        </h2>
        <Link
          href="/hall-of-fame"
          className="inline-flex shrink-0 items-center gap-1 font-mono text-xs text-fg-muted transition-colors hover:text-foreground"
        >
          Hall of Fame
          <ArrowRight className="size-3.5" aria-hidden />
        </Link>
      </div>

      <ul className="relative mt-3 grid gap-2 lg:grid-cols-3">
        {awards.map((award) => (
          <li key={award.id}>
            <Link
              href={`/profile/${award.userId}`}
              className="group flex items-center gap-3 rounded-xl border border-foreground/10 bg-foreground/[0.02] p-2.5 transition-colors hover:border-foreground/20 hover:bg-foreground/[0.05]"
            >
              <MemberAvatar award={award} size={40} />
              <span className="flex min-w-0 flex-col items-start gap-1">
                <span className="max-w-full truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary">
                  {award.userName}
                </span>
                <AwardBadge
                  awardType={award.awardType}
                  metricValue={award.metricValue}
                  platform={award.platform}
                  size="sm"
                />
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * The winner's photo, or their initials. Decorative (empty alt): it always sits
 * inside a link that already says the name, which would otherwise be read twice.
 */
function MemberAvatar({ award, size }: { award: WeeklyAwardDto; size: number }) {
  return (
    <span
      className="shrink-0 overflow-hidden rounded-full border border-foreground/10 bg-surface-3"
      style={{ width: size, height: size }}
    >
      {award.avatarUrl ? (
        <Image
          src={award.avatarUrl}
          alt=""
          width={size}
          height={size}
          className="size-full object-cover"
        />
      ) : (
        <span className="flex size-full items-center justify-center text-xs font-bold text-fg-muted">
          {award.userName.slice(0, 2).toUpperCase()}
        </span>
      )}
    </span>
  );
}
