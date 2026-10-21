"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  Code2,
  ExternalLink,
  FastForward,
  BookmarkCheck,
  Target,
  Sparkles,
  AlertCircle,
  Clock,
  ArrowRight,
} from "lucide-react";
import { practiceService } from "@/lib/services/practice";
import { contestService } from "@/lib/services/contests";
import { statsService } from "@/lib/services/stats";
import { TopicStrengthChart } from "@/components/site/topic-strength-chart";
import { useAuthStore } from "@/store/auth";
import type {
  PracticeSuggestionsResponseDto,
  PracticeSuggestion,
  MyUpsolvesDto,
  TopicStrengthResponse,
} from "@/types/api";

export default function PracticePage() {
  const { isAuthenticated, user: currentUser } = useAuthStore();

  const [suggestions, setSuggestions] = useState<PracticeSuggestionsResponseDto | null>(null);
  const [myUpsolves, setMyUpsolves] = useState<MyUpsolvesDto | null>(null);
  const [topicStrength, setTopicStrength] = useState<TopicStrengthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [skippingId, setSkippingId] = useState<number | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    let ignore = false;
    const topicPromise = currentUser?.id
      ? statsService.getTopics(currentUser.id).catch((err) => {
          console.error("Failed to load topic strength:", err);
          return null;
        })
      : Promise.resolve(null);

    Promise.all([
      practiceService.getSuggestions().catch((err) => {
        console.error("Failed to load practice suggestions:", err);
        return null;
      }),
      contestService.getMyUpsolves().catch((err) => {
        console.error("Failed to load pending upsolves:", err);
        return null;
      }),
      topicPromise,
    ])
      .then(([sugRes, upsolveRes, topicRes]) => {
        if (!ignore) {
          setSuggestions(sugRes);
          setMyUpsolves(upsolveRes);
          setTopicStrength(topicRes);
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [isAuthenticated, currentUser?.id]);

  const handleSkip = async (problemId: number) => {
    setSkippingId(problemId);
    try {
      await practiceService.skipProblem(problemId);
      // Optimistically remove from state
      setSuggestions((prev) =>
        prev
          ? {
              ...prev,
              suggestions: prev.suggestions.filter(
                (p) => (p.problemId ?? p.id) !== problemId
              ),
            }
          : null
      );
    } catch (err) {
      console.error("Failed to skip problem:", err);
      alert("Failed to skip problem. Please try again.");
    } finally {
      setSkippingId(null);
    }
  };

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-12">
      {/* Header */}
      <div className="space-y-3">
        <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono uppercase tracking-caps text-primary">
          <Code2 className="size-3.5" />
          Member Practice
        </div>
        <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
          Personalized Practice & Upsolving
        </h1>
        <p className="text-sm text-fg-muted max-w-2xl">
          Adaptive problem recommendations targeted at your current rating threshold and growth areas, combined with your pending club contest upsolves.
        </p>
      </div>

      {/* Auth Guard State */}
      {!isAuthenticated ? (
        <div className="rounded-3xl border border-white/10 bg-[#0d0d16]/80 p-8 md:p-12 text-center backdrop-blur-md shadow-2xl space-y-4">
          <div className="inline-flex size-14 items-center justify-center rounded-2xl bg-primary/15 text-primary border border-primary/30">
            <Target className="size-7" />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-white">
            Sign In for Personalized Practice
          </h2>
          <p className="text-sm text-fg-muted max-w-md mx-auto">
            Log in with your DAU account and link your Codeforces handle to unlock adaptive problem sets tuned to your rating band.
          </p>
          <div className="pt-2">
            <Link
              href="/login"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-lg shadow-primary/20"
            >
              Sign In with DAU Account
              <ArrowRight className="size-4" />
            </Link>
          </div>
        </div>
      ) : loading ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Analyzing your solves and tailoring recommendations...
        </div>
      ) : (
        <div className="space-y-10">
          {/* Topic Strength Breakdown */}
          {topicStrength && (
            <section className="space-y-4">
              <TopicStrengthChart data={topicStrength} />
            </section>
          )}

          {/* Practice Recommendations Section */}
          <section className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
              <div>
                <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                  <Sparkles className="size-4 text-amber-400" />
                  Recommended for You
                </h2>
                {suggestions && (
                  <div className="flex flex-wrap items-center gap-2 mt-2">
                    {suggestions.ratingBandMin != null && suggestions.ratingBandMax != null && (
                      <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-primary/15 text-primary border border-primary/30">
                        Target Band: {suggestions.ratingBandMin} - {suggestions.ratingBandMax}
                      </span>
                    )}
                    {suggestions.weakTags && suggestions.weakTags.length > 0 && (
                      <span className="text-nano font-mono text-fg-muted flex items-center gap-1">
                        Focus topics:{" "}
                        <strong className="text-foreground">
                          {suggestions.weakTags.join(", ")}
                        </strong>
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>

            {suggestions?.reason === "NO_CODEFORCES_HANDLE" ? (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-6 text-center space-y-3">
                <AlertCircle className="size-8 text-amber-400 mx-auto" />
                <h3 className="font-semibold text-white text-base">
                  No Codeforces Handle Linked
                </h3>
                <p className="text-xs text-fg-muted max-w-md mx-auto">
                  To receive personalized problem suggestions, link your Codeforces handle in your profile settings.
                </p>
                <Link
                  href="/profile"
                  className="inline-flex items-center gap-1.5 text-xs font-mono text-amber-400 hover:text-amber-300"
                >
                  Edit Profile
                  <ArrowRight className="size-3" />
                </Link>
              </div>
            ) : !suggestions || suggestions.suggestions.length === 0 ? (
              <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-fg-muted font-mono text-xs">
                No active suggestions right now. Keep solving or check back after the next sync!
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {suggestions.suggestions.map((prob: PracticeSuggestion) => {
                  const pid = prob.problemId ?? prob.id ?? 0;
                  const isSkipping = skippingId === pid;

                  return (
                    <div
                      key={pid || prob.problemKey}
                      className="group rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md hover:border-white/20 transition-all flex flex-col justify-between shadow-lg"
                    >
                      <div className="space-y-3">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-nano font-mono font-bold px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-white">
                            {prob.problemKey}
                          </span>
                          {prob.rating && (
                            <span className="text-nano font-mono font-bold px-2 py-0.5 rounded-md bg-amber-500/15 text-amber-300 border border-amber-500/30">
                              ★ {prob.rating}
                            </span>
                          )}
                        </div>

                        <div>
                          <h3 className="text-base font-bold text-white group-hover:text-primary transition-colors line-clamp-1">
                            {prob.name}
                          </h3>
                          <div className="flex flex-wrap gap-1 mt-2">
                            {prob.tags.slice(0, 3).map((tag) => (
                              <span
                                key={tag}
                                className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-white/[0.03] text-fg-subtle border border-white/5"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>

                      <div className="mt-5 pt-3 border-t border-white/5 flex items-center justify-between gap-2">
                        <button
                          type="button"
                          onClick={() => handleSkip(pid)}
                          disabled={isSkipping}
                          className="inline-flex items-center gap-1 text-[11px] font-mono text-fg-subtle hover:text-rose-400 transition-colors disabled:opacity-50"
                        >
                          <FastForward className="size-3.5" />
                          {isSkipping ? "Skipping..." : "Don't show again"}
                        </button>

                        <a
                          href={prob.url}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3 py-1.5 text-xs font-mono font-semibold text-primary hover:bg-primary/20 transition-all shadow-xs"
                        >
                          Solve on CF
                          <ExternalLink className="size-3.5" />
                        </a>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {/* Pending Upsolves Section */}
          <section className="space-y-6 pt-6 border-t border-white/10">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                  <BookmarkCheck className="size-4 text-cyan-400" />
                  Pending Club Contest Upsolves
                </h2>
                <p className="text-xs text-fg-muted mt-1 font-mono">
                  Problems from club contests you entered that you haven&apos;t solved yet.
                </p>
              </div>
              {myUpsolves?.cfSyncedAt && (
                <div className="text-nano font-mono text-fg-subtle flex items-center gap-1">
                  <Clock className="size-3" />
                  Synced: {new Date(myUpsolves.cfSyncedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST
                </div>
              )}
            </div>

            {!myUpsolves || myUpsolves.contests.length === 0 ? (
              <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-fg-muted font-mono text-xs">
                No pending upsolves. You&apos;ve solved all attempted problems in club contests, or haven&apos;t participated in any yet!
              </div>
            ) : (
              <div className="space-y-4">
                {myUpsolves.contests.map((c) => (
                  <div
                    key={c.clubContestId}
                    className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <Link
                        href={`/contests/${c.clubContestId}`}
                        className="text-base font-bold text-white hover:text-cyan-400 transition-colors flex items-center gap-2"
                      >
                        {c.name}
                        <ArrowRight className="size-3.5 text-fg-muted" />
                      </Link>
                      <span className="text-nano font-mono text-cyan-400">
                        {c.pending.length} pending
                      </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                      {c.pending.map((p) => (
                        <a
                          key={p.index}
                          href={p.url}
                          target="_blank"
                          rel="noreferrer"
                          className="group flex items-center justify-between p-3 rounded-xl border border-white/5 bg-white/[0.02] hover:bg-white/[0.04] hover:border-cyan-500/30 transition-all"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <span className="size-6 rounded-md bg-white/5 flex items-center justify-center font-mono font-bold text-xs text-white shrink-0">
                              {p.index}
                            </span>
                            <span className="text-xs font-medium text-fg-muted group-hover:text-white truncate">
                              {p.name}
                            </span>
                          </div>
                          {p.rating && (
                            <span className="text-[10px] font-mono text-amber-300 ml-2 shrink-0">
                              ★{p.rating}
                            </span>
                          )}
                        </a>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
