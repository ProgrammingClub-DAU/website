"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { Trophy, Calendar, Users, ExternalLink, ArrowRight, Clock, Sparkles } from "lucide-react";
import { contestService } from "@/lib/services/contests";
import { seasonService } from "@/lib/services/seasons";
import type { ClubContestSummaryDto, SeasonDto } from "@/types/api";

export default function ContestsPage() {
  const [contests, setContests] = useState<ClubContestSummaryDto[]>([]);
  const [seasons, setSeasons] = useState<SeasonDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;
    Promise.all([contestService.list(), seasonService.list()])
      .then(([contestList, seasonList]) => {
        if (!ignore) {
          setContests(contestList);
          setSeasons(seasonList);
        }
      })
      .catch((err) => {
        console.error("Failed to load contests data:", err);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-12">
      {/* Page Header */}
      <div className="space-y-3">
        <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono uppercase tracking-caps text-primary">
          <Trophy className="size-3.5" />
          Club Competitions
        </div>
        <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
          Contests & Championship
        </h1>
        <p className="text-sm text-fg-muted max-w-2xl">
          Official DAU mashups, gym rounds, and season championships. Every contest synchronizes with Codeforces to generate club-only standings and upsolve tracking.
        </p>
      </div>

      {/* Seasons Section */}
      {seasons.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-center justify-between border-b border-white/10 pb-3">
            <h2 className="text-lg font-bold uppercase tracking-wider text-white flex items-center gap-2">
              <Sparkles className="size-4 text-amber-400" />
              Championship Seasons
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {seasons.map((season) => (
              <Link
                key={season.id}
                href={`/contests/seasons/${season.id}`}
                className="group relative rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md hover:border-amber-500/40 hover:bg-[#0d0d16] transition-all flex flex-col justify-between shadow-lg"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
                      Season
                    </span>
                    <span className="text-nano font-mono text-fg-subtle">
                      {season.countBest != null
                        ? `Best ${season.countBest} Counted`
                        : "All Contests Counted"}
                    </span>
                  </div>
                  <h3 className="text-lg font-bold text-white group-hover:text-amber-300 transition-colors">
                    {season.name}
                  </h3>
                  <p className="text-micro font-mono text-fg-muted flex items-center gap-1">
                    <Calendar className="size-3.5" />
                    {season.startsOn} to {season.endsOn}
                  </p>
                </div>

                <div className="mt-5 pt-3 border-t border-white/5 flex items-center justify-between text-xs font-mono text-amber-400">
                  <span>View Championship Standings</span>
                  <ArrowRight className="size-3.5 group-hover:translate-x-1 transition-transform" />
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Contests List Section */}
      <section className="space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h2 className="text-lg font-bold uppercase tracking-wider text-white flex items-center gap-2">
            <Trophy className="size-4 text-primary" />
            Club Contests
          </h2>
          <span className="text-xs font-mono text-fg-muted">
            {contests.length} {contests.length === 1 ? "contest" : "contests"}
          </span>
        </div>

        {loading ? (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-fg-muted font-mono text-xs animate-pulse">
            Loading contests...
          </div>
        ) : contests.length === 0 ? (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-fg-muted font-mono text-xs">
            No club contests registered yet. Check back soon!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {contests.map((contest) => {
              const durationHours = Math.floor(contest.durationSeconds / 3600);
              const durationMins = Math.floor((contest.durationSeconds % 3600) / 60);

              return (
                <div
                  key={contest.id}
                  className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md flex flex-col justify-between hover:border-white/20 transition-all shadow-lg"
                >
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-primary/20 text-primary border border-primary/30">
                          {contest.kind}
                        </span>
                        {contest.seasonName && (
                          <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-white/5 text-fg-muted border border-white/10">
                            {contest.seasonName}
                          </span>
                        )}
                      </div>
                      {contest.url && (
                        <a
                          href={contest.url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-fg-muted hover:text-white transition-colors"
                          title="View on Codeforces"
                        >
                          <ExternalLink className="size-4" />
                        </a>
                      )}
                    </div>

                    <div>
                      <h3 className="text-lg font-bold text-white tracking-tight">
                        {contest.name}
                      </h3>
                      <div className="flex flex-wrap items-center gap-4 mt-2 text-micro font-mono text-fg-muted">
                        <span className="flex items-center gap-1">
                          <Calendar className="size-3.5" />
                          {new Date(contest.startsAt).toLocaleDateString("en-IN", {
                            day: "numeric",
                            month: "short",
                            year: "numeric",
                          })}
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="size-3.5" />
                          {durationHours}h {durationMins > 0 ? `${durationMins}m` : ""}
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="size-3.5" />
                          {contest.participantCount} Participants
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 pt-4 border-t border-white/5 flex items-center justify-between gap-2">
                    <Link
                      href={`/contests/${contest.id}?tab=upsolve`}
                      className="text-xs font-mono text-cyan-400 hover:text-cyan-300 transition-colors"
                    >
                      Upsolve Matrix
                    </Link>
                    <Link
                      href={`/contests/${contest.id}`}
                      className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3.5 py-1.5 text-xs font-semibold text-foreground hover:border-primary/50 hover:bg-surface-3 transition-colors shadow-xs"
                    >
                      Ranklist
                      <ArrowRight className="size-3.5" />
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
