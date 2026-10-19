"use client";

import { cn } from "@/lib/utils";
import type { StatsPeriod } from "@/types/api";

const PERIODS: { key: StatsPeriod; label: string; shortLabel: string }[] = [
  { key: "WEEK", label: "This Week", shortLabel: "Week" },
  { key: "MONTH", label: "This Month", shortLabel: "Month" },
  { key: "YEAR", label: "This Year", shortLabel: "Year" },
  { key: "ALL_TIME", label: "All Time", shortLabel: "All Time" },
];

interface PeriodToggleProps {
  value: StatsPeriod;
  onChange: (period: StatsPeriod) => void;
  allowedPeriods?: StatsPeriod[];
  className?: string;
  size?: "sm" | "default";
}

export function PeriodToggle({
  value,
  onChange,
  allowedPeriods,
  className,
  size = "default",
}: PeriodToggleProps) {
  const options = allowedPeriods
    ? PERIODS.filter((p) => allowedPeriods.includes(p.key))
    : PERIODS;

  return (
    <div
      role="radiogroup"
      aria-label="Select time period"
      className={cn(
        "inline-flex items-center gap-1 rounded-control bg-surface-elevated/70 p-1 border border-hairline backdrop-blur-sm",
        className
      )}
    >
      {options.map((period) => {
        const isSelected = value === period.key;
        return (
          <button
            key={period.key}
            type="button"
            role="radio"
            aria-checked={isSelected}
            onClick={() => onChange(period.key)}
            className={cn(
              "relative rounded-[calc(var(--radius-control)-2px)] font-mono tracking-caps uppercase transition-all duration-150 focus-visible:outline-2 focus-visible:outline-ring",
              size === "sm" ? "px-2.5 py-1 text-nano" : "px-3 py-1.5 text-label",
              isSelected
                ? "bg-primary text-primary-foreground font-semibold shadow-xs"
                : "text-muted-foreground hover:text-foreground hover:bg-surface/50"
            )}
          >
            <span className="hidden sm:inline">{period.label}</span>
            <span className="sm:hidden">{period.shortLabel}</span>
          </button>
        );
      })}
    </div>
  );
}
