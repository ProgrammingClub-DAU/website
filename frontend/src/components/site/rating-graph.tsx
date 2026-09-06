"use client";

/**
 * Contest rating history as a line graph.
 *
 * Written rather than pulled from a library. It replaces a recharts line chart
 * that cost about 104 kB gzipped on this route — an open finding in the frontend
 * audit — to draw roughly two hundred lines of SVG. Nothing else on the site uses
 * recharts, so this removes the dependency outright.
 *
 * Dots are coloured by the Codeforces rank the rating sat in at that moment, so
 * the graph shows progression through the ladder rather than a single accent
 * line. That is the same rank palette the club gradient is built from.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { CF_RANKS, ratingToRank } from "@/lib/cf-ranks";
import { cn } from "@/lib/utils";

export type RatingPoint = {
  /** ISO date string, or anything `new Date()` accepts. */
  date: string;
  rating: number;
  contestName?: string;
};

/** Vertical room for the value axis; horizontal room so end dots are not clipped. */
const PADDING = { top: 18, right: 16, bottom: 26, left: 44 };
const HEIGHT = 300;
const GRID_LINES = 5;

function rankName(rating: number): string {
  return CF_RANKS.find((r) => r.key === ratingToRank(rating))?.name ?? "Unrated";
}

/**
 * Builds a smooth path using monotone cubic interpolation.
 *
 * A plain Catmull-Rom spline overshoots between points, which on a rating graph
 * would draw dips and spikes that never happened — the curve would claim the
 * member dropped below a rating they never held. Monotone interpolation clamps
 * the tangents so the line only moves in the direction the data does.
 */
function monotonePath(pts: { x: number; y: number }[]): string {
  const n = pts.length;
  if (n === 0) return "";
  if (n === 1) return `M ${pts[0].x} ${pts[0].y}`;
  if (n === 2) return `M ${pts[0].x} ${pts[0].y} L ${pts[1].x} ${pts[1].y}`;

  // Secant slopes between consecutive points.
  const dx: number[] = [];
  const dy: number[] = [];
  const slope: number[] = [];
  for (let i = 0; i < n - 1; i++) {
    dx[i] = pts[i + 1].x - pts[i].x;
    dy[i] = pts[i + 1].y - pts[i].y;
    slope[i] = dx[i] === 0 ? 0 : dy[i] / dx[i];
  }

  // Tangents, averaged then clamped where the direction changes.
  const tangent: number[] = new Array(n);
  tangent[0] = slope[0];
  tangent[n - 1] = slope[n - 2];
  for (let i = 1; i < n - 1; i++) {
    if (slope[i - 1] * slope[i] <= 0) {
      // A local extremum: flatten, or the curve overshoots past it.
      tangent[i] = 0;
    } else {
      tangent[i] = (slope[i - 1] + slope[i]) / 2;
    }
  }

  // Fritsch-Carlson limiter keeps each segment monotone.
  for (let i = 0; i < n - 1; i++) {
    if (slope[i] === 0) {
      tangent[i] = 0;
      tangent[i + 1] = 0;
      continue;
    }
    const a = tangent[i] / slope[i];
    const b = tangent[i + 1] / slope[i];
    const h = Math.hypot(a, b);
    if (h > 3) {
      const t = 3 / h;
      tangent[i] = t * a * slope[i];
      tangent[i + 1] = t * b * slope[i];
    }
  }

  let d = `M ${pts[0].x} ${pts[0].y}`;
  for (let i = 0; i < n - 1; i++) {
    const third = dx[i] / 3;
    d += ` C ${pts[i].x + third} ${pts[i].y + tangent[i] * third}, ${pts[i + 1].x - third} ${
      pts[i + 1].y - tangent[i + 1] * third
    }, ${pts[i + 1].x} ${pts[i + 1].y}`;
  }
  return d;
}

export function RatingGraph({
  data,
  className,
}: {
  data: RatingPoint[];
  className?: string;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [width, setWidth] = useState(0);
  const [active, setActive] = useState<number | null>(null);

  // The SVG is drawn in pixels rather than scaled with a viewBox, so labels stay
  // at a readable size instead of stretching with the container.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const sorted = useMemo(
    () => [...data].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()),
    [data]
  );

  const geometry = useMemo(() => {
    if (width === 0 || sorted.length === 0) return null;

    const innerW = Math.max(1, width - PADDING.left - PADDING.right);
    const innerH = HEIGHT - PADDING.top - PADDING.bottom;

    const times = sorted.map((d) => new Date(d.date).getTime());
    const ratings = sorted.map((d) => d.rating);

    const tMin = Math.min(...times);
    const tMax = Math.max(...times);
    // Pad the value axis so the highest and lowest dots are not flush with the edge.
    const rMin = Math.min(...ratings) - 100;
    const rMax = Math.max(...ratings) + 100;

    // A single contest, or several on one day, would make every timestamp equal
    // and divide by zero — centre horizontally instead. The rating axis has no
    // equivalent case: it is always padded by ±100 above, so spanR is never zero.
    const spanT = tMax - tMin;
    const spanR = rMax - rMin;

    const x = (t: number) =>
      PADDING.left + (spanT === 0 ? innerW / 2 : ((t - tMin) / spanT) * innerW);
    const y = (r: number) => PADDING.top + innerH - ((r - rMin) / spanR) * innerH;

    const points = sorted.map((d, i) => ({
      x: x(times[i]),
      y: y(d.rating),
      ...d,
    }));

    const ticks = Array.from({ length: GRID_LINES }, (_, i) => {
      const value = rMin + (spanR * i) / (GRID_LINES - 1);
      return { value: Math.round(value), y: y(value) };
    });

    return { points, ticks, path: monotonePath(points) };
  }, [sorted, width]);

  /** Nearest point to the pointer, so the whole plot is a hit area. */
  const handleMove = useCallback(
    (event: React.PointerEvent<SVGSVGElement>) => {
      if (!geometry) return;
      const rect = event.currentTarget.getBoundingClientRect();
      const px = event.clientX - rect.left;
      let nearest = 0;
      let best = Infinity;
      geometry.points.forEach((p, i) => {
        const dist = Math.abs(p.x - px);
        if (dist < best) {
          best = dist;
          nearest = i;
        }
      });
      setActive(nearest);
    },
    [geometry]
  );

  const handleKey = useCallback(
    (event: React.KeyboardEvent<SVGSVGElement>) => {
      if (!geometry) return;
      if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
        event.preventDefault();
        const step = event.key === "ArrowRight" ? 1 : -1;
        setActive((current) => {
          const next = (current ?? -1) + step;
          return Math.max(0, Math.min(geometry.points.length - 1, next));
        });
      } else if (event.key === "Escape") {
        setActive(null);
      }
    },
    [geometry]
  );

  if (sorted.length === 0) return null;

  const activePoint = geometry && active !== null ? geometry.points[active] : null;
  const latest = sorted[sorted.length - 1];
  const first = sorted[0];
  const delta = latest.rating - first.rating;

  return (
    <div ref={containerRef} className={cn("relative w-full", className)}>
      {geometry && (
        <svg
          width={width}
          height={HEIGHT}
          role="group"
          tabIndex={0}
          aria-label={`Contest rating history: ${sorted.length} contests, from ${first.rating} to ${latest.rating}, ${delta >= 0 ? "up" : "down"} ${Math.abs(delta)} points. Use the arrow keys to read each contest.`}
          onPointerMove={handleMove}
          onPointerLeave={() => setActive(null)}
          onKeyDown={handleKey}
          onBlur={() => setActive(null)}
          className="touch-none rounded-panel focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        >
          {geometry.ticks.map((tick) => (
            <g key={tick.value}>
              <line
                x1={PADDING.left}
                x2={width - PADDING.right}
                y1={tick.y}
                y2={tick.y}
                stroke="var(--hairline)"
                strokeDasharray="4 6"
              />
              <text
                x={PADDING.left - 10}
                y={tick.y + 4}
                textAnchor="end"
                className="fill-fg-subtle font-mono text-[10px]"
              >
                {tick.value}
              </text>
            </g>
          ))}

          <path
            d={geometry.path}
            fill="none"
            stroke="var(--cf-expert)"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {activePoint && (
            <line
              x1={activePoint.x}
              x2={activePoint.x}
              y1={PADDING.top}
              y2={HEIGHT - PADDING.bottom}
              stroke="var(--border)"
              strokeDasharray="3 4"
            />
          )}

          {geometry.points.map((p, i) => (
            <circle
              key={`${p.date}-${i}`}
              cx={p.x}
              cy={p.y}
              r={active === i ? 6.5 : 4.5}
              fill={`var(--cf-${ratingToRank(p.rating)})`}
              stroke="var(--background)"
              strokeWidth={2}
            />
          ))}
        </svg>
      )}

      {activePoint && (
        <div
          className="pointer-events-none absolute z-10 min-w-[150px] -translate-x-1/2 rounded-control border border-border bg-surface-2 px-3 py-2 shadow-lg"
          style={{
            left: Math.min(Math.max(activePoint.x, 80), Math.max(80, width - 80)),
            top: Math.max(0, activePoint.y - 78),
          }}
        >
          <p className="font-mono text-[10px] tracking-[0.08em] text-fg-subtle uppercase">
            {new Date(activePoint.date).toLocaleDateString("en-US", {
              month: "short",
              day: "numeric",
              year: "numeric",
            })}
          </p>
          <p className="mt-0.5 text-sm font-semibold">
            {activePoint.rating}{" "}
            <span style={{ color: `var(--cf-${ratingToRank(activePoint.rating)})` }}>
              {rankName(activePoint.rating)}
            </span>
          </p>
          {activePoint.contestName && (
            <p className="mt-0.5 line-clamp-2 text-[11px] text-fg-muted">
              {activePoint.contestName}
            </p>
          )}
        </div>
      )}

      {/* The SVG carries a summary, but the individual results are only in the
          drawing. This table is the same data in a form a screen reader can walk
          through, which recharts did not provide either. */}
      <table className="sr-only">
        <caption>Contest rating history</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Contest</th>
            <th scope="col">Rating</th>
            <th scope="col">Rank</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((d, i) => (
            <tr key={`${d.date}-${i}`}>
              <td>{new Date(d.date).toLocaleDateString("en-US")}</td>
              <td>{d.contestName ?? "Contest"}</td>
              <td>{d.rating}</td>
              <td>{rankName(d.rating)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
