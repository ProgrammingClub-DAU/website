"use client";

import React from "react";
import { Crown, Flame, Rocket, Award } from "lucide-react";
import type { AwardType } from "@/types/api";

interface AwardBadgeProps {
  awardType: AwardType;
  metricValue?: number;
  platform?: "CODEFORCES" | "LEETCODE" | null;
  size?: "sm" | "md" | "lg";
}

export function AwardBadge({ awardType, metricValue, platform, size = "md" }: AwardBadgeProps) {
  const config = {
    CODER_OF_THE_WEEK: {
      label: "Coder of the Week",
      icon: <Crown className={size === "sm" ? "size-3" : size === "lg" ? "size-5" : "size-4"} />,
      bg: "bg-amber-500/15 border-amber-500/30 text-amber-300",
      valueSuffix: "pts",
    },
    MOST_SOLVED: {
      label: "Most Solved",
      icon: <Flame className={size === "sm" ? "size-3" : size === "lg" ? "size-5" : "size-4"} />,
      bg: "bg-orange-500/15 border-orange-500/30 text-orange-400",
      valueSuffix: "solves",
    },
    BIGGEST_RATING_JUMP: {
      label: "Biggest Rating Jump",
      icon: <Rocket className={size === "sm" ? "size-3" : size === "lg" ? "size-5" : "size-4"} />,
      bg: "bg-cyan-500/15 border-cyan-500/30 text-cyan-300",
      valueSuffix: platform ? `on ${platform === "CODEFORCES" ? "CF" : "LC"}` : "pts",
    },
  }[awardType] || {
    label: "Club Award",
    icon: <Award className="size-4" />,
    bg: "bg-white/10 border-white/20 text-white",
    valueSuffix: "",
  };

  const sizeClasses = {
    sm: "px-2 py-0.5 text-[10px] gap-1",
    md: "px-2.5 py-1 text-xs gap-1.5",
    lg: "px-3.5 py-1.5 text-sm gap-2",
  }[size];

  return (
    <span
      className={`inline-flex items-center rounded-full border font-mono font-semibold tracking-tight shadow-xs ${config.bg} ${sizeClasses}`}
    >
      {config.icon}
      <span>{config.label}</span>
      {metricValue != null && (
        <span className="opacity-90 font-bold ml-0.5">
          ({awardType === "BIGGEST_RATING_JUMP" ? `+${metricValue}` : metricValue}{" "}
          {config.valueSuffix})
        </span>
      )}
    </span>
  );
}
