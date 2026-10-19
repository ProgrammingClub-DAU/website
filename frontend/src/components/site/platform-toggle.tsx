"use client";

import { Layers } from "lucide-react";
import { PlatformMark } from "@/components/site/platform-mark";
import { cn } from "@/lib/utils";
import type { StatsPlatform } from "@/types/api";

interface PlatformOption {
  key: StatsPlatform;
  label: string;
  icon: React.ReactNode;
}

const PLATFORMS: PlatformOption[] = [
  {
    key: "OVERALL",
    label: "Overall",
    icon: <Layers className="size-3.5" />,
  },
  {
    key: "CODEFORCES",
    label: "Codeforces",
    icon: <PlatformMark platform="codeforces" className="size-3.5" />,
  },
  {
    key: "LEETCODE",
    label: "LeetCode",
    icon: <PlatformMark platform="leetcode" className="size-3.5" />,
  },
];

interface PlatformToggleProps {
  value: StatsPlatform;
  onChange: (platform: StatsPlatform) => void;
  allowedPlatforms?: StatsPlatform[];
  disabledPlatforms?: StatsPlatform[];
  disabledReason?: string;
  className?: string;
}

export function PlatformToggle({
  value,
  onChange,
  allowedPlatforms,
  disabledPlatforms = [],
  disabledReason = "Not supported for this metric",
  className,
}: PlatformToggleProps) {
  const options = allowedPlatforms
    ? PLATFORMS.filter((p) => allowedPlatforms.includes(p.key))
    : PLATFORMS;

  return (
    <div
      role="radiogroup"
      aria-label="Select platform"
      className={cn(
        "inline-flex items-center gap-1 rounded-control bg-surface-elevated/70 p-1 border border-hairline backdrop-blur-sm",
        className
      )}
    >
      {options.map((platform) => {
        const isSelected = value === platform.key;
        const isDisabled = disabledPlatforms.includes(platform.key);

        return (
          <button
            key={platform.key}
            type="button"
            role="radio"
            aria-checked={isSelected}
            disabled={isDisabled}
            title={isDisabled ? disabledReason : undefined}
            onClick={() => !isDisabled && onChange(platform.key)}
            className={cn(
              "group relative flex items-center gap-1.5 rounded-[calc(var(--radius-control)-2px)] px-3 py-1.5 font-mono text-label tracking-caps uppercase transition-all duration-150 focus-visible:outline-2 focus-visible:outline-ring",
              isDisabled && "opacity-35 cursor-not-allowed hover:bg-transparent",
              isSelected && !isDisabled
                ? "bg-primary text-primary-foreground font-semibold shadow-xs"
                : !isDisabled && "text-muted-foreground hover:text-foreground hover:bg-surface/50"
            )}
          >
            <span
              className={cn(
                "transition-transform group-hover:scale-105",
                isSelected && !isDisabled ? "text-primary-foreground" : "text-muted-foreground"
              )}
            >
              {platform.icon}
            </span>
            <span>{platform.label}</span>
          </button>
        );
      })}
    </div>
  );
}
