"use client";

import type { CSSProperties, MouseEvent, ReactNode } from "react";

import { cn } from "@/lib/utils";

/*
 * Branching timeline: a vertical spine with a curved connector out to each card.
 *
 * The connector SVG stretches with `preserveAspectRatio="none"`, so the branch
 * always lands exactly on the card edge at any rail width. `non-scaling-stroke`
 * keeps the line crisp through that stretch. Nothing round goes inside the SVG
 * (a circle would stretch into an ellipse) — the end node is a positioned span.
 *
 * The spine sits at 36.36% of the rail because the path starts at x=32 in an
 * 88-unit viewBox, so the two stay aligned at every rail width.
 *
 * Pulses live on ONE track per group rather than per segment. Per-segment
 * pulses each start at their own segment's top, which reads as dots appearing
 * from nowhere mid-line instead of one stream leaving the year head.
 */

/** Branch viewBox. The spine sits at 32/88 of it, hence SPINE_X. */
const VIEW_W = 88;
const VIEW_H = 60;
export const SPINE_X = "36.36%";
/** Connector endpoint, in viewBox units. */
const NODE_Y = 46;
/**
 * The same point in CSS units. The rail is `h-15` (3.75rem), so this is
 * NODE_Y/VIEW_H of that — expressed in rem so it tracks the root font size
 * instead of assuming 60px.
 */
const NODE_Y_CSS = `${(NODE_Y / VIEW_H) * 3.75}rem`;

/** One batch: three dots, each trailing the last. */
const PULSE_DELAYS = ["0s", "0.45s", "0.9s"];
/** Loop length for a run of REFERENCE_RUN, including the gap before the next batch. */
const BASE_CYCLE = 3;
const REFERENCE_RUN = 420;
const MIN_CYCLE = 1.8;
const MAX_CYCLE = 6.5;

export const RAIL =
  "grid grid-cols-[3.5rem_minmax(0,1fr)] sm:grid-cols-[5.5rem_minmax(0,1fr)]";

const BRANCH_PATH = "M32 0 V20 C32 42 32 46 88 46";

/** Last card aimed at, per group — see the early-return in `aimDots`. */
const lastAimed = new WeakMap<Element, Element>();

/**
 * Aims the dots at the hovered card.
 *
 * Builds ONE path in real pixel coordinates covering the whole route — down
 * the spine from the head, then out along the branch — and hands it to each
 * dot as an `offset-path`. One path means one continuous journey rather than
 * separate per-segment animations handing off, and it keeps the dots clear of
 * the branch SVG, whose `preserveAspectRatio="none"` makes stroke-dash length
 * maths unreliable (the same combination GSAP and anime.js both warn about).
 *
 * Runs by delegation on plain DOM, so hovering never touches React state.
 */
function aimDots(event: MouseEvent<HTMLDivElement>) {
  const group = event.currentTarget;
  const entry = (event.target as Element).closest(".tl-entry");
  const branch = entry?.querySelector(".tl-branch");
  const head = group.querySelector(".tl-head");
  if (!branch || !head) return;

  // mouseover bubbles from every descendant, and each measurement below
  // forces layout while :hover is still animating. Only re-aim when the
  // pointer actually moves to a different card.
  if (lastAimed.get(group) === entry) return;
  lastAimed.set(group, entry!);

  const groupBox = group.getBoundingClientRect();
  const branchBox = branch.getBoundingClientRect();

  // Start where the line itself starts, below the heading chip — starting at
  // the group's own top would launch dots from behind the chip.
  const startY = head.getBoundingClientRect().top - groupBox.top;

  // The branch is drawn in an 88x60 viewBox stretched to fill the rail, so
  // both axes need their own scale. Verticals are only 1:1 at a 16px root
  // font size; the rail is sized in rem, so never assume it.
  const kx = branchBox.width / VIEW_W;
  const ky = branchBox.height / VIEW_H;
  const x = 32 * kx;
  const top = branchBox.top - groupBox.top;
  const knee = top + 20 * ky;
  const curve = top + 42 * ky;
  const end = top + NODE_Y * ky;

  const d = `M ${x},${startY} V ${knee} C ${x},${curve} ${x},${end} ${branchBox.width},${end}`;

  // A fixed duration would make a dot cover a long run much faster than a
  // short one. Scaling by the square root of the distance lets speed rise
  // with length, but far more gently than the run does.
  const run = Math.max(end - startY, 1);
  const cycle = Math.min(
    MAX_CYCLE,
    Math.max(MIN_CYCLE, BASE_CYCLE * Math.sqrt(run / REFERENCE_RUN))
  );
  group.style.setProperty("--tl-cycle", `${cycle.toFixed(2)}s`);

  group
    .querySelectorAll<HTMLElement>(".tl-dot")
    .forEach((dot) => (dot.style.offsetPath = `path("${d}")`));
}

/** Wraps a head plus its entries: the run of spine a batch of pulses travels. */
export function TimelineGroup({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn("tl-group relative", className)}
      style={{ "--tl-cycle": `${BASE_CYCLE}s` } as CSSProperties}
      onMouseOver={aimDots}
    >
      {PULSE_DELAYS.map((delay) => (
        <span
          key={delay}
          className="tl-dot"
          style={{ "--tl-d": delay } as CSSProperties}
          aria-hidden
        />
      ))}
      {children}
    </div>
  );
}

/** The spine stub under a root chip or year heading — where a batch starts. */
export function TimelineHeadSpine() {
  return (
    <div
      className="tl-head absolute inset-y-0 w-px"
      style={{ left: SPINE_X }}
      aria-hidden
    />
  );
}

export function Timeline({ children }: { children: ReactNode }) {
  return <ul className="list-none">{children}</ul>;
}

export function TimelineRoot({ label }: { label: string }) {
  return (
    <div>
      <span className="inline-flex items-center gap-2 rounded-control border border-border bg-surface px-3.5 py-2.5 font-mono text-xs tracking-[0.12em] whitespace-nowrap uppercase">
        <span className="size-[7px] rounded-full bg-primary" aria-hidden />
        {label}
      </span>
      <div className={RAIL}>
        <div className="relative h-7">
          <TimelineHeadSpine />
        </div>
      </div>
    </div>
  );
}

export function TimelineEntry({
  children,
  isLast = false,
}: {
  children: ReactNode;
  isLast?: boolean;
}) {
  return (
    <li className={cn("tl-entry group/entry items-stretch", RAIL)}>
      <div className="relative">
        {/*
         * Two segments, split at the branch point. Only the part above the
         * branch is on the path to this card, so hovering must not light the
         * run continuing down to the next one.
         */}
        <div
          className="tl-spine tl-spine-top absolute w-px"
          style={{ left: SPINE_X, top: 0, height: NODE_Y_CSS }}
          aria-hidden
        />
        {!isLast && (
          <div
            className="tl-spine tl-spine-rest absolute bottom-0 w-px"
            style={{ left: SPINE_X, top: NODE_Y_CSS }}
            aria-hidden
          />
        )}

        <svg
          viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
          preserveAspectRatio="none"
          className="tl-branch relative block h-15 w-full text-hairline-strong transition-colors group-hover/entry:text-primary"
          aria-hidden
        >
          <path
            d={BRANCH_PATH}
            fill="none"
            stroke="currentColor"
            strokeWidth={1.25}
            vectorEffect="non-scaling-stroke"
          />
        </svg>

        <span
          className="absolute right-0 size-[7px] -translate-y-1/2 translate-x-1/2 rounded-full border-2 border-hairline-strong bg-background transition-colors group-hover/entry:border-primary group-hover/entry:bg-primary"
          style={{ top: NODE_Y_CSS }}
          aria-hidden
        />
      </div>

      <div className="max-w-[54rem] pt-8 pb-6">{children}</div>
    </li>
  );
}

export function TimelineEnd({ label }: { label: string }) {
  return (
    <div className={cn("items-center", RAIL)}>
      <div className="relative h-2">
        <span
          className="absolute top-1/2 size-[7px] -translate-x-1/2 -translate-y-1/2 rounded-full border border-fg-subtle"
          style={{ left: SPINE_X }}
          aria-hidden
        />
      </div>
      <span className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase">
        {label}
      </span>
    </div>
  );
}

export function TimelineCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <article
      className={cn(
        "glass-panel flex flex-col gap-3.5 rounded-panel p-6 transition-all",
        "group-hover/entry:-translate-y-0.5 group-hover/entry:border-primary",
        className
      )}
    >
      {children}
    </article>
  );
}
