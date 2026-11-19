import type { CSSProperties } from "react";
import Link from "next/link";
import { Crosshair, Gauge, RotateCcw, Sparkles } from "lucide-react";

import { InView } from "@/components/site/in-view";
import { Eyebrow, Section } from "@/components/site/primitives";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const POINTS = [
  {
    icon: Gauge,
    text: "Your level in 18 topics, estimated from the hardest problems you've solved in each.",
  },
  {
    icon: Crosshair,
    text: "Problems in your weakest topics where you have a 40–70% chance: hard, but doable.",
  },
  {
    icon: RotateCcw,
    text: "Problems you tried and missed, brought back so you can crack them.",
  },
] as const;

/** An illustration of the practice page, not a member's data. */
const EXAMPLE_LEVELS = [
  { topic: "Greedy", level: 1520, focus: false },
  { topic: "Math", level: 1430, focus: false },
  { topic: "Graphs", level: 1260, focus: true },
  { topic: "DP", level: 1150, focus: true },
] as const;
const EXAMPLE_OVERALL = 1450;
const scale = (level: number) => ((level - 800) / (1700 - 800)) * 100;

/** A bar that grows to `width` once the preview scrolls into view, after `delayMs`. */
function GrowBar({ width, delayMs, className }: { width: number; delayMs: number; className: string }) {
  return (
    <div
      className={cn(
        "h-full w-0 rounded-full transition-[width] duration-1000 ease-out group-data-[inview=true]/preview:w-(--bar)",
        className
      )}
      style={{ "--bar": `${width}%`, transitionDelay: `${delayMs}ms` } as CSSProperties}
    />
  );
}

/**
 * The home page's introduction to skill-based practice, placed right under the
 * hero: a light runs around its border, the "New" tag shines, and the example
 * fills in as it scrolls into view. Every effect is CSS, so the reduced-motion
 * rule in globals.css stops them all; only the in-view flag needs script.
 */
export function SmartPracticeTeaser() {
  return (
    <Section className="pt-4 pb-16">
      <div className="spotlight-border rounded-panel p-[1.5px] shadow-[0_0_80px_-28px_var(--primary)]">
        <div className="relative isolate overflow-hidden rounded-[calc(var(--radius-panel)-1.5px)] bg-surface p-6 md:p-10">
          <div
            aria-hidden
            className="pointer-events-none absolute -top-32 -right-24 -z-10 size-[28rem] rounded-full bg-primary/10 blur-3xl"
          />
          <div
            aria-hidden
            className="pointer-events-none absolute -bottom-40 -left-24 -z-10 size-[24rem] rounded-full bg-amber-500/10 blur-3xl"
          />

          <div className="grid gap-10 lg:grid-cols-[1.1fr_1fr] lg:items-center">
            <div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="shine inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 font-mono text-micro font-semibold tracking-caps text-primary uppercase">
                  <span className="relative flex size-1.5">
                    <span className="absolute inline-flex size-full animate-ping rounded-full bg-primary opacity-60" />
                    <span className="relative inline-flex size-1.5 rounded-full bg-primary" />
                  </span>
                  New
                </span>
                <Eyebrow>Smart practice</Eyebrow>
              </div>
              <h2 className="mt-4 font-heading text-[clamp(1.75rem,3.8vw,2.5rem)] font-medium tracking-[-0.02em] text-balance">
                Practice picked{" "}
                <span className="bg-gradient-to-r from-primary to-amber-500 bg-clip-text text-transparent">
                  for you.
                </span>
              </h2>
              <p className="mt-5 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
                Link your Codeforces handle and the site works out what to solve next, from your own
                history. A new set every day, different for every member.
              </p>
              <ul className="mt-6 space-y-3">
                {POINTS.map(({ icon: Icon, text }) => (
                  <li key={text} className="flex gap-3 text-body text-foreground">
                    <span className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
                      <Icon className="size-3.5" aria-hidden />
                    </span>
                    <span className="text-pretty">{text}</span>
                  </li>
                ))}
              </ul>
              <Button asChild className="mt-8 h-10 rounded-full px-5.5 shadow-[0_0_24px_-6px_var(--primary)]">
                <Link href="/practice">
                  <Sparkles className="size-4" aria-hidden />
                  Open my practice &rarr;
                </Link>
              </Button>
            </div>

            <InView className="group/preview">
              <figure className="space-y-3">
                <div className="rounded-2xl border border-hairline bg-card p-5 shadow-panel">
                  <div className="flex items-baseline justify-between">
                    <p className="font-mono text-micro tracking-caps text-fg-muted uppercase">Level by topic</p>
                    <p className="font-mono text-micro text-fg-subtle">overall ≈ {EXAMPLE_OVERALL}</p>
                  </div>
                  <ul className="mt-4 space-y-3">
                    {EXAMPLE_LEVELS.map(({ topic, level, focus }, i) => (
                      <li key={topic} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-semibold text-foreground">
                            {topic}
                            {focus && (
                              <span className="ml-1.5 rounded-full bg-amber-500/15 px-1.5 py-px font-mono text-[9px] font-semibold tracking-caps text-amber-700 uppercase dark:text-amber-400">
                                Focus
                              </span>
                            )}
                          </span>
                          <span className="font-mono font-bold text-foreground">{level}</span>
                        </div>
                        <div className="relative h-1.5 overflow-hidden rounded-full bg-foreground/5">
                          <GrowBar
                            width={scale(level)}
                            delayMs={150 + i * 140}
                            className={focus ? "bg-amber-500" : "bg-primary/80"}
                          />
                          <div
                            className="absolute inset-y-0 w-px border-l border-dashed border-foreground/60"
                            style={{ left: `${scale(EXAMPLE_OVERALL)}%` }}
                          />
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>

                <div className="animate-float">
                  <div className="rounded-2xl border border-amber-500/30 bg-card p-5 shadow-panel">
                    <div className="flex items-center justify-between gap-2">
                      <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-2 py-0.5 font-mono text-nano font-semibold tracking-caps text-amber-700 uppercase dark:text-amber-400">
                        <Crosshair className="size-3" aria-hidden />
                        DP
                      </span>
                      <span className="rounded-md border border-amber-500/30 bg-amber-500/15 px-2 py-0.5 font-mono text-nano font-bold text-amber-700 dark:text-amber-300">
                        ★ 1200
                      </span>
                    </div>
                    <p className="mt-3 text-sm font-semibold text-foreground">A 1200-rated DP problem</p>
                    <p className="mt-1 text-xs text-fg-muted">
                      Builds DP, one of your focus topics (your level there ≈ 1150).
                    </p>
                    <div className="mt-3 space-y-1">
                      <div className="flex justify-between font-mono text-nano">
                        <span className="text-fg-muted">Estimated chance</span>
                        <span className="font-bold text-foreground">~43%</span>
                      </div>
                      <div className="h-1.5 overflow-hidden rounded-full bg-foreground/5">
                        <GrowBar
                          width={43}
                          delayMs={800}
                          className="bg-gradient-to-r from-amber-500 to-emerald-500"
                        />
                      </div>
                    </div>
                  </div>
                </div>
                <figcaption className="text-center font-mono text-micro tracking-caps text-fg-subtle uppercase">
                  Example of what a member sees
                </figcaption>
              </figure>
            </InView>
          </div>
        </div>
      </div>
    </Section>
  );
}
