"use client";

/**
 * Contest rating history as a multi-platform line graph.
 *
 * Implemented with pure SVG and Fritsch-Carlson monotone cubic spline interpolation
 * to guarantee no overshoot (eliminating the recharts bundle overhead).
 *
 * Supports single series (legacy `data` prop) and multi-platform comparison (`series` prop).
 * Codeforces points are coloured by the rank ladder tier; LeetCode points are styled
 * in signature platform blue/orange.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { CF_RANKS, ratingToRank } from "@/lib/cf-ranks";
import { lcRankColor, ratingToLcRank, lcRankName } from "@/lib/lc-ranks";
import { cn } from "@/lib/utils";
import type { LeaderboardPlatform } from "@/types/api";

export type RatingPoint = {
  /** ISO date string, or anything `new Date()` accepts. */
  date: string;
  rating: number;
  contestName?: string;
  rank?: number | null;
};

export interface RatingSeries {
  platform: LeaderboardPlatform;
  points: RatingPoint[];
  label?: string;
  color?: string;
}

const PADDING = { top: 22, right: 20, bottom: 32, left: 48 };
const HEIGHT = 320;
const GRID_LINES = 5;

function rankName(rating: number, platform: LeaderboardPlatform = "CODEFORCES"): string {
  if (platform === "LEETCODE") return lcRankName(rating);
  return CF_RANKS.find((r) => r.key === ratingToRank(rating))?.name ?? "Unrated";
}

function getPointColor(rating: number, platform: LeaderboardPlatform = "CODEFORCES"): string {
  if (platform === "LEETCODE") return lcRankColor(ratingToLcRank(rating));
  return `var(--cf-${ratingToRank(rating)})`;
}

/**
 * Builds a smooth path using monotone cubic interpolation (Fritsch-Carlson).
 */
function monotonePath(pts: { x: number; y: number }[]): string {
  const n = pts.length;
  if (n === 0) return "";
  if (n === 1) return `M ${pts[0].x} ${pts[0].y}`;
  if (n === 2) return `M ${pts[0].x} ${pts[0].y} L ${pts[1].x} ${pts[1].y}`;

  const dx: number[] = [];
  const dy: number[] = [];
  const slope: number[] = [];
  for (let i = 0; i < n - 1; i++) {
    dx[i] = pts[i + 1].x - pts[i].x;
    dy[i] = pts[i + 1].y - pts[i].y;
    slope[i] = dx[i] === 0 ? 0 : dy[i] / dx[i];
  }

  const tangent: number[] = new Array(n);
  tangent[0] = slope[0];
  tangent[n - 1] = slope[n - 2];
  for (let i = 1; i < n - 1; i++) {
    if (slope[i - 1] * slope[i] <= 0) {
      tangent[i] = 0;
    } else {
      tangent[i] = (slope[i - 1] + slope[i]) / 2;
    }
  }

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

interface RatingGraphProps {
  data?: RatingPoint[];
  platform?: LeaderboardPlatform;
  series?: RatingSeries[];
  className?: string;
  showLegend?: boolean;
}

export function RatingGraph({
  data,
  platform = "CODEFORCES",
  series,
  className,
  showLegend = true,
}: RatingGraphProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [width, setWidth] = useState(0);
  const [activeItem, setActiveItem] = useState<{
    seriesIdx: number;
    pointIdx: number;
  } | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // Normalize inputs to standard series list
  const activeSeries: RatingSeries[] = useMemo(() => {
    if (series && series.length > 0) {
      return series.filter((s) => s.points && s.points.length > 0);
    }
    if (data && data.length > 0) {
      return [
        {
          platform,
          points: data,
          label: platform === "CODEFORCES" ? "Codeforces" : "LeetCode",
          color: platform === "CODEFORCES" ? "var(--cf-expert)" : "#FFA116",
        },
      ];
    }
    return [];
  }, [series, data, platform]);

  const geometry = useMemo(() => {
    if (width === 0 || activeSeries.length === 0) return null;

    const innerW = Math.max(1, width - PADDING.left - PADDING.right);
    const innerH = HEIGHT - PADDING.top - PADDING.bottom;

    // Collect all points across series
    const allTimes: number[] = [];
    const allRatings: number[] = [];

    activeSeries.forEach((s) => {
      s.points.forEach((p) => {
        allTimes.push(new Date(p.date).getTime());
        allRatings.push(p.rating);
      });
    });

    if (allTimes.length === 0) return null;

    const tMin = Math.min(...allTimes);
    const tMax = Math.max(...allTimes);
    const rMin = Math.min(...allRatings) - 100;
    const rMax = Math.max(...allRatings) + 100;

    const spanT = tMax - tMin;
    const spanR = rMax - rMin;

    const x = (t: number) =>
      PADDING.left + (spanT === 0 ? innerW / 2 : ((t - tMin) / spanT) * innerW);
    const y = (r: number) => PADDING.top + innerH - ((r - rMin) / spanR) * innerH;

    const plottedSeries = activeSeries.map((s, sIdx) => {
      const sortedPoints = [...s.points].sort(
        (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
      );
      const points = sortedPoints.map((p) => ({
        x: x(new Date(p.date).getTime()),
        y: y(p.rating),
        seriesIdx: sIdx,
        ...p,
      }));
      return {
        ...s,
        points,
        path: monotonePath(points),
      };
    });

    const ticks = Array.from({ length: GRID_LINES }, (_, i) => {
      const value = rMin + (spanR * i) / (GRID_LINES - 1);
      return { value: Math.round(value), y: y(value) };
    });

    return { plottedSeries, ticks, tMin, tMax };
  }, [activeSeries, width]);

  const handleMove = useCallback(
    (event: React.PointerEvent<SVGSVGElement>) => {
      if (!geometry) return;
      const rect = event.currentTarget.getBoundingClientRect();
      const px = event.clientX - rect.left;

      let bestDist = Infinity;
      let closest: { seriesIdx: number; pointIdx: number } | null = null;

      geometry.plottedSeries.forEach((s, sIdx) => {
        s.points.forEach((p, pIdx) => {
          const dist = Math.abs(p.x - px);
          if (dist < bestDist) {
            bestDist = dist;
            closest = { seriesIdx: sIdx, pointIdx: pIdx };
          }
        });
      });

      setActiveItem(closest);
    },
    [geometry]
  );

  if (activeSeries.length === 0) {
    return (
      <div className="flex h-48 w-full items-center justify-center rounded-panel border border-dashed border-hairline text-label text-fg-muted font-mono">
        No contest rating history recorded
      </div>
    );
  }

  const activePoint =
    geometry && activeItem !== null
      ? geometry.plottedSeries[activeItem.seriesIdx]?.points[activeItem.pointIdx]
      : null;

  return (
    <div ref={containerRef} className={cn("relative w-full", className)}>
      {/* ── Optional Multi-Series Legend ── */}
      {showLegend && activeSeries.length > 1 && (
        <div className="mb-3 flex flex-wrap items-center gap-4 text-label font-mono">
          {activeSeries.map((s) => (
            <div key={s.platform} className="flex items-center gap-2">
              <span
                className="size-2.5 rounded-full"
                style={{
                  backgroundColor:
                    s.color || (s.platform === "LEETCODE" ? "#FFA116" : "var(--cf-expert)"),
                }}
              />
              <span className="text-fg-muted uppercase tracking-caps">
                {s.label || s.platform}
              </span>
            </div>
          ))}
        </div>
      )}

      {geometry && (
        <svg
          width={width}
          height={HEIGHT}
          role="img"
          aria-label="Contest rating history multi-platform progression chart"
          tabIndex={0}
          onPointerMove={handleMove}
          onPointerLeave={() => setActiveItem(null)}
          onBlur={() => setActiveItem(null)}
          className="touch-none rounded-panel focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        >
          {/* ── Background Grid & Value Labels ── */}
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
                className="fill-fg-subtle font-mono text-micro"
              >
                {tick.value}
              </text>
            </g>
          ))}

          {/* ── Series Curve Paths ── */}
          {geometry.plottedSeries.map((s) => {
            const strokeColor =
              s.color || (s.platform === "LEETCODE" ? "#FFA116" : "var(--cf-expert)");
            return (
              <path
                key={s.platform}
                d={s.path}
                fill="none"
                stroke={strokeColor}
                strokeWidth={2.2}
                strokeLinecap="round"
                strokeLinejoin="round"
                className="transition-all duration-150"
              />
            );
          })}

          {/* ── Active Hover Vertical Crosshair ── */}
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

          {/* ── Data Points / Markers ── */}
          {geometry.plottedSeries.map((s, sIdx) =>
            s.points.map((p, pIdx) => {
              const isSelected =
                activeItem?.seriesIdx === sIdx && activeItem?.pointIdx === pIdx;
              const ptColor =
                s.platform === "CODEFORCES"
                  ? getPointColor(p.rating, "CODEFORCES")
                  : s.color || "#FFA116";

              return (
                <circle
                  key={`${p.date}-${sIdx}-${pIdx}`}
                  cx={p.x}
                  cy={p.y}
                  r={isSelected ? 6.5 : 4}
                  fill={ptColor}
                  stroke="var(--background)"
                  strokeWidth={2}
                  className="transition-transform duration-100"
                />
              );
            })
          )}
        </svg>
      )}

      {/* ── Floating Tooltip ── */}
      {activePoint && (
        <div
          className="pointer-events-none absolute z-10 min-w-[160px] -translate-x-1/2 rounded-control border border-border bg-surface-elevated/95 p-2.5 shadow-xl backdrop-blur-md"
          style={{
            left: Math.min(Math.max(activePoint.x, 90), Math.max(90, width - 90)),
            top: Math.max(0, activePoint.y - 84),
          }}
        >
          <div className="flex items-center justify-between gap-2 border-b border-hairline pb-1">
            <span className="font-mono text-nano text-fg-subtle uppercase tracking-caps">
              {geometry?.plottedSeries[activeItem!.seriesIdx]?.platform}
            </span>
            <span className="font-mono text-nano text-fg-muted">
              {new Date(activePoint.date).toLocaleDateString("en-US", {
                month: "short",
                day: "numeric",
                year: "numeric",
              })}
            </span>
          </div>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-heading text-base font-semibold text-foreground">
              {activePoint.rating}
            </span>
            <span
              className="text-label font-mono font-medium"
              style={{
                color: getPointColor(
                  activePoint.rating,
                  geometry?.plottedSeries[activeItem!.seriesIdx]?.platform
                ),
              }}
            >
              {rankName(
                activePoint.rating,
                geometry?.plottedSeries[activeItem!.seriesIdx]?.platform
              )}
            </span>
          </div>
          {activePoint.contestName && (
            <p className="mt-0.5 line-clamp-1 text-nano text-fg-muted">
              {activePoint.contestName}
            </p>
          )}
        </div>
      )}

      {/* ── Hidden Accessibility Table for Screen Readers ── */}
      <table className="sr-only">
        <caption>Contest rating history table</caption>
        <thead>
          <tr>
            <th scope="col">Platform</th>
            <th scope="col">Date</th>
            <th scope="col">Contest</th>
            <th scope="col">Rating</th>
          </tr>
        </thead>
        <tbody>
          {activeSeries.map((s) =>
            s.points.map((p, i) => (
              <tr key={`${s.platform}-${p.date}-${i}`}>
                <td>{s.platform}</td>
                <td>{new Date(p.date).toLocaleDateString()}</td>
                <td>{p.contestName ?? "Contest"}</td>
                <td>{p.rating}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
