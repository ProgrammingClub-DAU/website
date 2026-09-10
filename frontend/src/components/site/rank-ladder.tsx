"use client";

import { useEffect, useRef, useState } from "react";

import { CF_RANKS } from "@/lib/cf-ranks";
import { cn } from "@/lib/utils";

/**
 * Bar height as a fraction of the ladder's full height, for the nth rank.
 *
 * Derived rather than written out as a literal array so it cannot fall out of
 * step with {@link CF_RANKS} if a band is ever added or removed. The exponent
 * bends the line slightly: an even rise reads as a ramp, whereas the climb
 * getting steeper towards the top is the point being made.
 *
 * The lowest bar keeps a real height. Scaling honestly from zero would leave
 * Newbie as a sliver, and Newbie is where most of the club actually is.
 */
function barScale(index: number, count: number): number {
  const FLOOR = 0.26;
  return FLOOR + (1 - FLOOR) * Math.pow(index / (count - 1), 1.15);
}

/**
 * The Codeforces rating ladder, drawn as a staircase.
 *
 * This is the only place on the site that explains what the coloured dots
 * beside every member's name mean. They appear on the leaderboard, in the
 * directory and on the Hall of Fame cards with no legend anywhere near them,
 * so a first-time visitor sees seven arbitrary colours.
 *
 * The bars carry no information a screen reader needs — the rank name and its
 * rating floor are both real text — so they are hidden from the accessibility
 * tree and the list reads as a plain list of bands.
 */
export function RankLadder({ className }: { className?: string }) {
  const ref = useRef<HTMLUListElement | null>(null);

  // Two flags rather than one, so the section degrades to a static chart
  // instead of to an empty strip. Server-rendered markup has neither flag set
  // and every bar is at full height; `armed` is only set once JS is running,
  // and only then can a bar be collapsed. A reader with no JS, or with the
  // observer unavailable, sees the finished staircase rather than nothing.
  const [armed, setArmed] = useState(false);
  const [revealed, setRevealed] = useState(false);

  useEffect(() => {
    const node = ref.current;
    if (!node || typeof IntersectionObserver === "undefined") return;

    setArmed(true);

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;
        setRevealed(true);
        observer.disconnect();
      },
      // Enough of the ladder has to be on screen that the growth is watched
      // rather than caught halfway through while scrolling past it.
      { threshold: 0.35 }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  const collapsed = armed && !revealed;
  const last = CF_RANKS.length - 1;

  return (
    <ul
      ref={ref}
      className={cn("flex items-end gap-1.5 sm:gap-3", className)}
      style={{ ["--ladder-h" as string]: "clamp(9rem, 20vw, 16rem)" }}
    >
      {CF_RANKS.map((rank, i) => {
        // The bars grow left to right, so the eye travels the ladder in the
        // direction the ranks ascend.
        const delay = `${i * 70}ms`;

        return (
          <li key={rank.key} className="flex min-w-0 flex-1 flex-col items-center">
            <span
              className="mb-2 font-mono text-[10px] tracking-[0.06em] text-fg-subtle transition-opacity duration-500 sm:text-[11px]"
              style={{ opacity: collapsed ? 0 : 1, transitionDelay: `${i * 70 + 220}ms` }}
            >
              {rank.min}
              {i === last ? "+" : ""}
            </span>

            <span
              aria-hidden
              className="w-full rounded-t-[6px]"
              style={{
                height: `calc(var(--ladder-h) * ${barScale(i, CF_RANKS.length)})`,
                // Solid at the top fading downward, so the bar reads as light
                // rather than as a flat block of colour sitting on the band.
                background: `linear-gradient(180deg, ${rank.color} 0%, color-mix(in srgb, ${rank.color} 55%, transparent) 55%, color-mix(in srgb, ${rank.color} 10%, transparent) 100%)`,
                // Blur radius scaled by --glow-strength, the token this theme
                // already uses to damp glows: an indigo halo that reads as
                // atmosphere on the dark page spreads into a fuzzy edge on
                // white. Light halves it, dark leaves it at full size.
                boxShadow: `0 0 calc(30px * var(--glow-strength)) -6px color-mix(in srgb, ${rank.color} 65%, transparent), inset 0 1px 0 color-mix(in srgb, white 55%, transparent)`,
                transform: collapsed ? "scaleY(0)" : "scaleY(1)",
                transformOrigin: "bottom",
                transition: `transform 720ms cubic-bezier(0.22, 1, 0.36, 1) ${delay}`,
              }}
            />

            {/* The full name is always in the DOM for a screen reader; the two
                visible spans are an abbreviation swap for narrow viewports,
                where "Candidate Master" cannot fit in a seventh of the width. */}
            <span className="mt-3 text-center font-mono text-[10px] leading-tight tracking-[0.06em] text-fg-muted uppercase sm:text-[11px]">
              <span className="sr-only">{rank.name}</span>
              <span aria-hidden className="sm:hidden">
                {rank.short}
              </span>
              <span aria-hidden className="hidden sm:inline">
                {rank.name}
              </span>
            </span>
          </li>
        );
      })}
    </ul>
  );
}
