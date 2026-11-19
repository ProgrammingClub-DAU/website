import React from "react";
import { Crosshair, Target } from "lucide-react";
import type { TopicStrengthResponse } from "@/types/api";
import { MIN_SOLVES_FOR_FOCUS, topicLabel } from "@/lib/practice";
import { cn } from "@/lib/utils";

interface TopicStrengthChartProps {
  data: TopicStrengthResponse;
  /** "admin" words it about the student ("they") rather than to them ("you"). */
  viewer?: "member" | "admin";
}

/**
 * The member's estimated level in each core topic, on the Codeforces rating
 * scale, with their focus topics marked and their overall level as a guide line.
 */
export function TopicStrengthChart({ data, viewer = "member" }: TopicStrengthChartProps) {
  const admin = viewer === "admin";
  const topics = (data.topics ?? [])
    .filter((t) => t.level != null)
    .sort((a, b) => (b.level ?? 0) - (a.level ?? 0) || a.tag.localeCompare(b.tag));

  if (topics.length === 0) {
    return (
      <div className="rounded-2xl border border-foreground/10 bg-card p-8 text-center text-fg-muted font-mono text-xs">
        No topic data yet. Solve a few problems on Codeforces and check back after the next sync.
      </div>
    );
  }

  const overall = data.overallLevel ?? null;
  const highest = Math.max(...topics.map((t) => t.level ?? 0), overall ?? 0);
  // Bars run from the bottom of the rating scale to just past the highest level.
  const scaleMin = 800;
  const scaleMax = Math.ceil((highest + 100) / 100) * 100;
  const position = (level: number) =>
    Math.min(100, Math.max(3, ((level - scaleMin) / (scaleMax - scaleMin)) * 100));
  const ratedSolves = data.ratedSolves ?? 0;

  return (
    <div className="space-y-4 rounded-2xl border border-foreground/10 bg-card/80 p-5 backdrop-blur-md">
      <div className="flex flex-col gap-3 border-b border-foreground/10 pb-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
            <Target className="size-4 text-primary" aria-hidden />
            {admin ? "Level by topic" : "Your level by topic"}
          </h3>
          <p className="mt-1 max-w-[60ch] text-xs text-fg-muted">
            Estimated from the hardest problems {admin ? "they've" : "you've"} solved in each topic, on the
            Codeforces rating scale. A level of 1400 means {admin ? "they solve" : "you solve"} 1400-rated problems
            about half the time.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-4 font-mono text-nano">
          <span className="flex items-center gap-1.5 text-amber-700 dark:text-amber-400">
            <span className="size-2 rounded-full bg-amber-500" />
            Focus topic
          </span>
          {overall != null && (
            <span className="flex items-center gap-1.5 text-fg-muted">
              <span className="h-3 w-px border-l border-dashed border-foreground/50" />
              Overall ≈ {overall}
            </span>
          )}
        </div>
      </div>

      {ratedSolves < MIN_SOLVES_FOR_FOCUS && (
        <p className="rounded-xl border border-amber-500/25 bg-amber-500/5 px-3 py-2 text-xs text-fg-muted">
          Focus topics unlock at {MIN_SOLVES_FOR_FOCUS} solved rated problems. {admin ? "They have" : "You have"}{" "}
          {ratedSolves}, so these levels are a first estimate.
        </p>
      )}

      <ul className="grid gap-x-8 gap-y-3 pt-1 md:grid-cols-2">
        {topics.map((topic) => {
          const level = topic.level ?? scaleMin;
          return (
            <li key={topic.tag} className="space-y-1.5">
              <div className="flex items-center justify-between gap-2 text-xs">
                <span className="flex items-center gap-1.5 font-semibold text-foreground">
                  {topicLabel(topic.tag)}
                  {topic.weak && (
                    <span className="inline-flex items-center gap-0.5 rounded-full bg-amber-500/15 px-1.5 py-px font-mono text-[9px] font-semibold uppercase tracking-caps text-amber-700 dark:text-amber-400">
                      <Crosshair className="size-2.5" aria-hidden />
                      Focus
                    </span>
                  )}
                </span>
                <span className="font-mono text-fg-muted">
                  <span className={cn("font-bold", topic.weak ? "text-amber-700 dark:text-amber-400" : "text-foreground")}>
                    {level}
                  </span>
                  <span className="text-fg-subtle"> · {topic.memberSolves} solved</span>
                </span>
              </div>
              <div className="relative h-2 w-full overflow-hidden rounded-full bg-foreground/5">
                <div
                  className={cn(
                    "absolute inset-y-0 left-0 rounded-full",
                    topic.weak ? "bg-amber-500" : "bg-primary/80"
                  )}
                  style={{ width: `${position(level)}%` }}
                />
                {overall != null && (
                  <div
                    aria-hidden
                    className="absolute inset-y-0 w-px border-l border-dashed border-foreground/60"
                    style={{ left: `${position(overall)}%` }}
                  />
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
