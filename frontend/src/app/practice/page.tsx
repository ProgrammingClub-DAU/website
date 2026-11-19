"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertCircle,
  ArrowRight,
  BookmarkCheck,
  Brain,
  Clock,
  Crosshair,
  ExternalLink,
  FastForward,
  Gauge,
  RotateCcw,
  Shuffle,
  Sparkles,
  Target,
} from "lucide-react";
import { practiceService } from "@/lib/services/practice";
import { apiErrorMessage } from "@/lib/api-error";
import { contestService } from "@/lib/services/contests";
import { statsService } from "@/lib/services/stats";
import { TopicStrengthChart } from "@/components/site/topic-strength-chart";
import { useAuthStore } from "@/store/auth";
import type {
  PracticeSuggestionsResponseDto,
  PracticeSuggestion,
  MyUpsolvesDto,
  TopicStrengthResponse,
  TryAgainProblem,
} from "@/types/api";
import { MIN_SOLVES_FOR_FOCUS, timeAgo, topicLabel, verdictLabel } from "@/lib/practice";
import { cn, serverDate } from "@/lib/utils";

const HOW_IT_WORKS = [
  {
    icon: Brain,
    title: "We read your solves",
    body: "Every problem you've solved on Codeforces, synced automatically.",
  },
  {
    icon: Gauge,
    title: "We estimate your level",
    body: "Overall and in 18 topics, from the hardest problems you've solved in each.",
  },
  {
    icon: Target,
    title: "We pick the right problems",
    body: "In your weakest topics, where you have a 40–70% chance: hard enough to learn from, doable enough to finish.",
  },
] as const;

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
      // Skips cover both lists: remove it from whichever showed it.
      setSuggestions((prev) =>
        prev
          ? {
              ...prev,
              suggestions: prev.suggestions.filter((p) => (p.problemId ?? p.id) !== problemId),
              tryAgain: prev.tryAgain?.filter((p) => p.problemId !== problemId),
            }
          : null
      );
    } catch (err) {
      console.error("Failed to skip problem:", err);
      alert(`Failed to skip problem: ${apiErrorMessage(err, "Please try again.")}`);
    } finally {
      setSkippingId(null);
    }
  };

  const noHandle = suggestions?.reason === "NO_CODEFORCES_HANDLE";
  const tryAgain = suggestions?.tryAgain ?? [];

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-12">
      {/* Header */}
      <div className="space-y-3">
        <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono uppercase tracking-caps text-primary">
          <Sparkles className="size-3.5" aria-hidden />
          Smart practice · New
        </div>
        <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-foreground">
          Practice picked for you
        </h1>
        <p className="text-sm text-fg-muted max-w-2xl">
          Problems chosen from your own Codeforces history: in the topics you&apos;re weakest at, at a
          difficulty you can just about manage, plus the ones you tried and haven&apos;t cracked yet. A
          new mix every day.
        </p>
      </div>

      {/* How it works: shown to everyone, so a visitor knows what signing in gets them. */}
      <ol className="grid gap-3 sm:grid-cols-3">
        {HOW_IT_WORKS.map((step, i) => {
          const Icon = step.icon;
          return (
            <li
              key={step.title}
              className="flex gap-3 rounded-2xl border border-foreground/10 bg-card/70 p-4 backdrop-blur-md"
            >
              <span className="flex size-9 shrink-0 items-center justify-center rounded-xl border border-primary/25 bg-primary/10 text-primary">
                <Icon className="size-4.5" aria-hidden />
              </span>
              <div>
                <p className="text-sm font-semibold text-foreground">
                  <span className="mr-1.5 font-mono text-xs text-primary">{i + 1}.</span>
                  {step.title}
                </p>
                <p className="mt-1 text-xs leading-relaxed text-fg-muted">{step.body}</p>
              </div>
            </li>
          );
        })}
      </ol>

      {!isAuthenticated ? (
        <div className="rounded-3xl border border-foreground/10 bg-card/80 p-8 md:p-12 text-center backdrop-blur-md shadow-2xl space-y-4">
          <div className="inline-flex size-14 items-center justify-center rounded-2xl bg-primary/15 text-primary border border-primary/30">
            <Target className="size-7" aria-hidden />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground">
            Sign in to get your problems
          </h2>
          <p className="text-sm text-fg-muted max-w-md mx-auto">
            Sign in with your DAU account and link your Codeforces handle. Your levels and today&apos;s
            problems are ready after the first sync, usually within minutes.
          </p>
          <div className="pt-2">
            <Link
              href="/login"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-lg shadow-primary/20"
            >
              Sign In with DAU Account
              <ArrowRight className="size-4" aria-hidden />
            </Link>
          </div>
        </div>
      ) : loading ? (
        <div className="rounded-2xl border border-foreground/10 bg-card/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Estimating your levels and picking today&apos;s problems…
        </div>
      ) : noHandle ? (
        <div className="rounded-2xl border border-amber-500/25 bg-amber-500/5 p-6 text-center space-y-3">
          <AlertCircle className="size-8 text-amber-600 dark:text-amber-400 mx-auto" aria-hidden />
          <h2 className="font-semibold text-foreground text-base">Link your Codeforces handle</h2>
          <p className="text-xs text-fg-muted max-w-md mx-auto">
            Smart practice works from your Codeforces history. Add your handle on your profile and your
            problems appear after the first sync.
          </p>
          <Link
            href="/profile"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-amber-700 hover:text-amber-800 dark:text-amber-400 dark:hover:text-amber-300"
          >
            Edit Profile
            <ArrowRight className="size-3" aria-hidden />
          </Link>
        </div>
      ) : (
        <div className="space-y-12">
          {suggestions && <LevelSummary suggestions={suggestions} topics={topicStrength} />}

          {/* Today's picks */}
          <section className="space-y-5">
            <div className="flex flex-col gap-2 border-b border-foreground/10 pb-4 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground">
                  <Sparkles className="size-4 text-amber-500" aria-hidden />
                  Today&apos;s problems
                </h2>
                <p className="mt-1 text-xs text-fg-muted">
                  Each shows why it was picked and your estimated chance of solving it.
                </p>
              </div>
              <span className="inline-flex items-center gap-1.5 font-mono text-nano text-fg-subtle">
                <Shuffle className="size-3" aria-hidden />
                New mix every day
              </span>
            </div>

            {!suggestions || suggestions.suggestions.length === 0 ? (
              <div className="rounded-2xl border border-foreground/10 bg-card/70 p-12 text-center text-fg-muted font-mono text-xs">
                No problems to suggest right now. Keep solving, or check back after the next sync.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {suggestions.suggestions.map((prob) => (
                  <SuggestionCard
                    key={prob.problemId ?? prob.problemKey}
                    problem={prob}
                    skipping={skippingId === (prob.problemId ?? prob.id)}
                    onSkip={handleSkip}
                  />
                ))}
              </div>
            )}
          </section>

          {/* Try again */}
          <section className="space-y-5">
            <div className="border-b border-foreground/10 pb-4">
              <h2 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground">
                <RotateCcw className="size-4 text-rose-500" aria-hidden />
                Try again
              </h2>
              <p className="mt-1 text-xs text-fg-muted">
                Problems you submitted to on Codeforces and haven&apos;t solved yet. Going back to a problem
                that beat you is one of the fastest ways to improve.
              </p>
            </div>
            {tryAgain.length === 0 ? (
              <div className="rounded-2xl border border-foreground/10 bg-card/70 p-8 text-center text-fg-muted font-mono text-xs">
                Nothing to retry: every problem you&apos;ve attempted is solved. (Failed attempts appear here
                after your next sync.)
              </div>
            ) : (
              <ul className="space-y-2.5">
                {tryAgain.map((prob) => (
                  <TryAgainRow
                    key={prob.problemId}
                    problem={prob}
                    skipping={skippingId === prob.problemId}
                    onSkip={handleSkip}
                  />
                ))}
              </ul>
            )}
          </section>

          {/* Topic levels */}
          {topicStrength && topicStrength.reason == null && (
            <section>
              <TopicStrengthChart data={topicStrength} />
            </section>
          )}

          {/* Pending Upsolves Section */}
          <section className="space-y-6 pt-6 border-t border-foreground/10">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h2 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
                  <BookmarkCheck className="size-4 text-cyan-600 dark:text-cyan-400" aria-hidden />
                  Pending Club Contest Upsolves
                </h2>
                <p className="text-xs text-fg-muted mt-1 font-mono">
                  Problems from club contests you entered that you haven&apos;t solved yet.
                </p>
              </div>
              {myUpsolves?.cfSyncedAt && (
                <div className="text-nano font-mono text-fg-subtle flex items-center gap-1">
                  <Clock className="size-3" aria-hidden />
                  Synced: {serverDate(myUpsolves.cfSyncedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST
                </div>
              )}
            </div>

            {!myUpsolves || myUpsolves.contests.length === 0 ? (
              <div className="rounded-2xl border border-foreground/10 bg-card/70 p-12 text-center text-fg-muted font-mono text-xs">
                No pending upsolves. You&apos;ve solved all attempted problems in club contests, or haven&apos;t participated in any yet!
              </div>
            ) : (
              <div className="space-y-4">
                {myUpsolves.contests.map((c) => (
                  <div
                    key={c.clubContestId}
                    className="rounded-2xl border border-foreground/10 bg-card/80 p-5 backdrop-blur-md space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <Link
                        href={`/contests/${c.clubContestId}`}
                        className="text-base font-bold text-foreground hover:text-cyan-600 dark:hover:text-cyan-400 transition-colors flex items-center gap-2"
                      >
                        {c.name}
                        <ArrowRight className="size-3.5 text-fg-muted" aria-hidden />
                      </Link>
                      <span className="text-nano font-mono text-cyan-700 dark:text-cyan-400">
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
                          className="group flex items-center justify-between p-3 rounded-xl border border-foreground/5 bg-foreground/[0.02] hover:bg-foreground/[0.04] hover:border-cyan-500/30 transition-all"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <span className="size-6 rounded-md bg-foreground/5 flex items-center justify-center font-mono font-bold text-xs text-foreground shrink-0">
                              {p.index}
                            </span>
                            <span className="text-xs font-medium text-fg-muted group-hover:text-foreground truncate">
                              {p.name}
                            </span>
                          </div>
                          {p.rating && (
                            <span className="text-[10px] font-mono text-amber-700 dark:text-amber-300 ml-2 shrink-0">
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

/** The member's overall level and focus topics: what today's problems are built around. */
function LevelSummary({
  suggestions,
  topics,
}: {
  suggestions: PracticeSuggestionsResponseDto;
  topics: TopicStrengthResponse | null;
}) {
  const levelOf = (tag: string) => topics?.topics.find((t) => t.tag === tag)?.level ?? null;
  const ratedSolves = topics?.ratedSolves ?? null;

  return (
    <section className="relative overflow-hidden rounded-3xl border border-primary/20 bg-card/80 p-6 backdrop-blur-md md:p-8">
      <div aria-hidden className="pointer-events-none absolute -top-24 -right-24 size-80 rounded-full bg-primary/10 blur-3xl" />
      <div className="relative grid gap-6 md:grid-cols-[auto_1fr] md:items-center md:gap-10">
        <div>
          <p className="font-mono text-nano uppercase tracking-caps text-fg-muted">Your level</p>
          <p className="mt-1 font-mono text-5xl font-black tabular-nums tracking-tight text-foreground">
            {suggestions.overallLevel ?? "—"}
          </p>
          <p className="mt-1 text-xs text-fg-muted">on the Codeforces rating scale</p>
        </div>

        <div className="space-y-3">
          <p className="flex items-center gap-1.5 font-mono text-nano uppercase tracking-caps text-amber-700 dark:text-amber-400">
            <Crosshair className="size-3.5" aria-hidden />
            Focus topics
          </p>
          {suggestions.weakTags.length > 0 ? (
            <>
              <div className="flex flex-wrap gap-2">
                {suggestions.weakTags.map((tag) => {
                  const level = levelOf(tag);
                  return (
                    <span
                      key={tag}
                      className="inline-flex items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-sm font-semibold text-foreground"
                    >
                      {topicLabel(tag)}
                      {level != null && (
                        <span className="font-mono text-xs font-bold text-amber-700 dark:text-amber-400">≈ {level}</span>
                      )}
                    </span>
                  );
                })}
              </div>
              <p className="text-xs text-fg-muted">
                Your lowest topics among those that come up often at your level. Most of today&apos;s
                problems build these.
              </p>
            </>
          ) : (
            <p className="text-sm text-fg-muted">
              {ratedSolves != null && ratedSolves < MIN_SOLVES_FOR_FOCUS
                ? `Focus topics unlock at ${MIN_SOLVES_FOR_FOCUS} solved rated problems (you have ${ratedSolves}). Until then, today's problems match your overall level.`
                : "Today's problems match your overall level."}
            </p>
          )}
        </div>
      </div>
    </section>
  );
}

function ChanceMeter({ chance }: { chance: number }) {
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between font-mono text-nano">
        <span className="text-fg-muted">Estimated chance</span>
        <span className="font-bold text-foreground">~{chance}%</span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-foreground/5">
        <div
          className="h-full rounded-full bg-gradient-to-r from-amber-500 to-emerald-500"
          style={{ width: `${Math.min(100, Math.max(4, chance))}%` }}
        />
      </div>
    </div>
  );
}

function SuggestionCard({
  problem,
  skipping,
  onSkip,
}: {
  problem: PracticeSuggestion;
  skipping: boolean;
  onSkip: (problemId: number) => void;
}) {
  const pid = problem.problemId ?? problem.id ?? 0;
  const reason = problem.focusTag
    ? `Builds ${topicLabel(problem.focusTag)}, one of your focus topics (your level there ≈ ${problem.focusLevel}).`
    : problem.focusLevel != null
      ? `Matches your overall level (≈ ${problem.focusLevel}).`
      : null;

  return (
    <div className="group flex flex-col justify-between rounded-2xl border border-foreground/10 bg-card/80 p-5 shadow-lg backdrop-blur-md transition-all hover:border-foreground/20">
      <div className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          {problem.focusTag ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-2 py-0.5 font-mono text-nano font-semibold uppercase tracking-caps text-amber-700 dark:text-amber-400">
              <Crosshair className="size-3" aria-hidden />
              {topicLabel(problem.focusTag)}
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 font-mono text-nano font-semibold uppercase tracking-caps text-primary">
              <Gauge className="size-3" aria-hidden />
              Your level
            </span>
          )}
          <span className="flex items-center gap-1.5">
            <span className="rounded-md border border-foreground/10 bg-foreground/5 px-2 py-0.5 font-mono text-nano font-bold text-foreground">
              {problem.problemKey}
            </span>
            {problem.rating && (
              <span className="rounded-md border border-amber-500/30 bg-amber-500/15 px-2 py-0.5 font-mono text-nano font-bold text-amber-700 dark:text-amber-300">
                ★ {problem.rating}
              </span>
            )}
          </span>
        </div>

        <div>
          <h3 className="line-clamp-1 text-base font-bold text-foreground transition-colors group-hover:text-primary">
            {problem.name}
          </h3>
          {reason && <p className="mt-1 text-xs leading-relaxed text-fg-muted">{reason}</p>}
          <div className="mt-2 flex flex-wrap gap-1">
            {problem.tags
              .filter((t) => !t.startsWith("*"))
              .slice(0, 4)
              .map((tag) => (
                <span
                  key={tag}
                  className="rounded border border-foreground/5 bg-foreground/[0.03] px-1.5 py-0.5 font-mono text-[10px] text-fg-subtle"
                >
                  {tag}
                </span>
              ))}
          </div>
        </div>

        {problem.chance != null && <ChanceMeter chance={problem.chance} />}
      </div>

      <div className="mt-5 flex items-center justify-between gap-2 border-t border-foreground/5 pt-3">
        <button
          type="button"
          onClick={() => onSkip(pid)}
          disabled={skipping}
          className="inline-flex items-center gap-1 font-mono text-[11px] text-fg-subtle transition-colors hover:text-rose-500 disabled:opacity-50"
        >
          <FastForward className="size-3.5" aria-hidden />
          {skipping ? "Skipping..." : "Don't show again"}
        </button>
        <a
          href={problem.url}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3 py-1.5 font-mono text-xs font-semibold text-primary shadow-xs transition-all hover:bg-primary/20"
        >
          Solve on CF
          <ExternalLink className="size-3.5" aria-hidden />
        </a>
      </div>
    </div>
  );
}

function TryAgainRow({
  problem,
  skipping,
  onSkip,
}: {
  problem: TryAgainProblem;
  skipping: boolean;
  onSkip: (problemId: number) => void;
}) {
  return (
    <li className="flex flex-col gap-3 rounded-2xl border border-foreground/10 bg-card/80 p-4 backdrop-blur-md sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0 space-y-1.5">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full border border-rose-500/30 bg-rose-500/10 px-2 py-0.5 font-mono text-nano font-semibold text-rose-700 dark:text-rose-400">
            {verdictLabel(problem.lastVerdict)}
          </span>
          <span className="font-mono text-nano text-fg-subtle">{timeAgo(serverDate(problem.lastAttemptAt))}</span>
        </div>
        <p className="truncate text-sm font-semibold text-foreground">
          <span className="mr-2 font-mono text-xs text-fg-muted">{problem.problemKey}</span>
          {problem.name}
        </p>
        <p className="font-mono text-nano text-fg-muted">
          {problem.rating ? `★ ${problem.rating}` : "Unrated"}
          {problem.chance != null && ` · ~${problem.chance}% chance for you now`}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-3">
        <button
          type="button"
          onClick={() => onSkip(problem.problemId)}
          disabled={skipping}
          className="inline-flex items-center gap-1 font-mono text-[11px] text-fg-subtle transition-colors hover:text-rose-500 disabled:opacity-50"
        >
          <FastForward className="size-3.5" aria-hidden />
          {skipping ? "Skipping..." : "Don't show again"}
        </button>
        <a
          href={problem.url}
          target="_blank"
          rel="noreferrer"
          className={cn(
            "inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 font-mono text-xs font-semibold shadow-xs transition-all",
            "border-rose-500/30 bg-rose-500/10 text-rose-700 hover:bg-rose-500/20 dark:text-rose-300"
          )}
        >
          Retry on CF
          <ExternalLink className="size-3.5" aria-hidden />
        </a>
      </div>
    </li>
  );
}
