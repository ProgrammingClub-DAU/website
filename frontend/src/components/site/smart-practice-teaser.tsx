import Link from "next/link";
import { Crosshair, Gauge, RotateCcw } from "lucide-react";

import { Section, SectionHeader } from "@/components/site/primitives";
import { Button } from "@/components/ui/button";

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

/**
 * The home page's introduction to skill-based practice: what it does, and a
 * small example of what a member sees. Static, so it renders with the page.
 */
export function SmartPracticeTeaser() {
  return (
    <Section className="pb-16">
      <div className="relative isolate overflow-hidden rounded-panel border border-hairline bg-surface p-6 md:p-10">
        <div
          aria-hidden
          className="pointer-events-none absolute -top-32 -right-24 -z-10 size-[28rem] rounded-full bg-primary/10 blur-3xl"
        />
        <div className="grid gap-10 lg:grid-cols-[1.1fr_1fr] lg:items-center">
          <div>
            <SectionHeader eyebrow="New · Smart practice" title="Practice picked for you." />
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
            <Button asChild className="mt-8 h-10 rounded-full px-5.5">
              <Link href="/practice">Open my practice &rarr;</Link>
            </Button>
          </div>

          <figure className="space-y-3">
            <div className="rounded-2xl border border-hairline bg-card p-5 shadow-panel">
              <div className="flex items-baseline justify-between">
                <p className="font-mono text-micro tracking-caps text-fg-muted uppercase">Level by topic</p>
                <p className="font-mono text-micro text-fg-subtle">overall ≈ {EXAMPLE_OVERALL}</p>
              </div>
              <ul className="mt-4 space-y-3">
                {EXAMPLE_LEVELS.map(({ topic, level, focus }) => (
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
                      <div
                        className={focus ? "h-full rounded-full bg-amber-500" : "h-full rounded-full bg-primary/80"}
                        style={{ width: `${scale(level)}%` }}
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

            <div className="rounded-2xl border border-hairline bg-card p-5 shadow-panel">
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
                  <div className="h-full w-[43%] rounded-full bg-gradient-to-r from-amber-500 to-emerald-500" />
                </div>
              </div>
            </div>
            <figcaption className="text-center font-mono text-micro tracking-caps text-fg-subtle uppercase">
              Example of what a member sees
            </figcaption>
          </figure>
        </div>
      </div>
    </Section>
  );
}
