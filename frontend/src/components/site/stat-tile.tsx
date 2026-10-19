"use client";

import type { ReactNode } from "react";
import { Info } from "lucide-react";
import { cn } from "@/lib/utils";

interface StatTileProps {
  label: string;
  value: number | string | null | undefined;
  basis?: string | null;
  partial?: boolean;
  emptyHint?: string | null;
  icon?: ReactNode;
  loading?: boolean;
  className?: string;
  badge?: ReactNode;
  trend?: {
    direction: "up" | "down" | "flat";
    value: string;
  };
}

export function StatTile({
  label,
  value,
  basis,
  partial,
  emptyHint,
  icon,
  loading = false,
  className,
  badge,
  trend,
}: StatTileProps) {
  if (loading) {
    return (
      <div
        className={cn(
          "rounded-card border border-hairline bg-surface-elevated/40 p-4 backdrop-blur-sm animate-pulse flex flex-col justify-between min-h-[104px]",
          className
        )}
      >
        <div className="flex items-center justify-between">
          <div className="h-3 w-20 rounded bg-muted/40" />
          <div className="size-4 rounded-full bg-muted/30" />
        </div>
        <div className="my-2 h-7 w-24 rounded bg-muted/40" />
        <div className="h-2.5 w-32 rounded bg-muted/20" />
      </div>
    );
  }

  const hasValue = value !== null && value !== undefined;

  return (
    <div
      className={cn(
        "group relative flex flex-col justify-between rounded-card border border-hairline bg-surface-elevated/40 p-4 backdrop-blur-sm transition-all duration-200 hover:border-hairline-bright hover:bg-surface-elevated/60",
        className
      )}
    >
      {/* ── Top Header: Label & Icon/Badge ── */}
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-label tracking-caps text-fg-muted uppercase flex items-center gap-1.5">
          {label}
          {partial && (
            <span
              className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 text-nano font-mono uppercase bg-amber-500/10 text-amber-400 border border-amber-500/20"
              title="Partial data: LeetCode has no baseline for this period"
            >
              Partial
            </span>
          )}
        </span>
        <div className="flex items-center gap-1 text-fg-subtle">
          {badge}
          {icon}
        </div>
      </div>

      {/* ── Main Value / Empty State ── */}
      <div className="my-2.5 flex items-baseline gap-2">
        {hasValue ? (
          <>
            <span className="font-heading text-2xl sm:text-3xl font-medium tracking-tight text-foreground">
              {typeof value === "number" ? value.toLocaleString() : value}
            </span>
            {trend && (
              <span
                className={cn(
                  "font-mono text-micro font-semibold",
                  trend.direction === "up" && "text-emerald-400",
                  trend.direction === "down" && "text-rose-400",
                  trend.direction === "flat" && "text-muted-foreground"
                )}
              >
                {trend.direction === "up" ? "↑" : trend.direction === "down" ? "↓" : "–"} {trend.value}
              </span>
            )}
          </>
        ) : (
          <div className="flex items-center gap-1.5 text-fg-subtle py-1">
            <Info className="size-3.5 shrink-0 opacity-70" />
            <span className="font-mono text-label italic">
              {emptyHint || "No data recorded"}
            </span>
          </div>
        )}
      </div>

      {/* ── Footer Basis or Explanatory Line ── */}
      <div className="min-h-[16px] text-nano text-fg-muted font-mono truncate">
        {hasValue && basis ? (
          <span className="opacity-90">{basis}</span>
        ) : hasValue && partial ? (
          <span className="text-amber-400/90">Includes Codeforces only</span>
        ) : !hasValue && emptyHint ? (
          <span className="text-fg-subtle">Period baseline pending</span>
        ) : null}
      </div>
    </div>
  );
}
