"use client";

import React from "react";
import { TrendingUp, TrendingDown, Target, Lightbulb } from "lucide-react";
import type { TopicStrengthResponse } from "@/types/api";

interface TopicStrengthChartProps {
  data: TopicStrengthResponse;
}

export function TopicStrengthChart({ data }: TopicStrengthChartProps) {
  if (!data.topics || data.topics.length === 0) {
    return (
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16] p-8 text-center text-fg-muted font-mono text-xs">
        No topic distribution data available yet.
      </div>
    );
  }

  // Find max share to normalize bar widths
  const maxShare = Math.max(
    ...data.topics.map((t) => Math.max(t.memberShare, t.clubShare)),
    0.01
  );

  return (
    <div className="space-y-4 rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/10 pb-3">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-white flex items-center gap-2">
            <Target className="size-4 text-primary" />
            Topic Strength Profile
          </h3>
          <p className="mt-0.5 text-nano text-fg-muted">
            Share of your solves vs club baseline ({data.totalCountedSolves} verified solves analyzed)
          </p>
        </div>
        <div className="flex items-center gap-4 text-nano font-mono">
          <span className="flex items-center gap-1.5 text-primary">
            <span className="size-2 rounded-full bg-primary" />
            Your Share
          </span>
          <span className="flex items-center gap-1.5 text-fg-muted">
            <span className="size-2 rounded-full bg-white/30" />
            Club Share
          </span>
        </div>
      </div>

      <div className="space-y-3 pt-2">
        {data.topics.map((topic) => {
          const memberPercent = Math.round(topic.memberShare * 100);
          const clubPercent = Math.round(topic.clubShare * 100);
          const gapPercent = Math.round(topic.gap * 100);
          const memberBarWidth = Math.min((topic.memberShare / maxShare) * 100, 100);
          const clubBarWidth = Math.min((topic.clubShare / maxShare) * 100, 100);
          const isStrength = topic.gap >= 0;

          return (
            <div key={topic.tag} className="space-y-1.5">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="font-semibold text-white capitalize">
                  {topic.tag.replace(/-/g, " ")}
                </span>
                <div className="flex items-center gap-2 text-micro">
                  <span className="text-fg-muted">{topic.memberSolves} solves</span>
                  <span
                    className={`inline-flex items-center gap-0.5 font-bold ${
                      isStrength ? "text-emerald-400" : "text-amber-400"
                    }`}
                  >
                    {isStrength ? (
                      <TrendingUp className="size-3" />
                    ) : (
                      <TrendingDown className="size-3" />
                    )}
                    {isStrength ? `+${gapPercent}%` : `${gapPercent}%`}
                  </span>
                </div>
              </div>

              {/* Stacked comparison bar */}
              <div className="relative h-2.5 w-full rounded-full bg-white/5 overflow-hidden">
                {/* Club baseline marker */}
                <div
                  className="absolute top-0 bottom-0 bg-white/20 rounded-full"
                  style={{ width: `${clubBarWidth}%` }}
                />
                {/* Member share */}
                <div
                  className={`absolute top-0 bottom-0 rounded-full transition-all duration-500 ${
                    isStrength ? "bg-primary" : "bg-amber-400"
                  }`}
                  style={{ width: `${memberBarWidth}%` }}
                />
              </div>

              <div className="flex items-center justify-between text-[10px] font-mono text-fg-subtle">
                <span>You: {memberPercent}%</span>
                <span>Club: {clubPercent}%</span>
              </div>
            </div>
          );
        })}
      </div>

      {data.note && (
        <div className="mt-3 flex items-start gap-2 rounded-xl bg-white/[0.02] border border-white/5 p-3 text-nano text-fg-muted">
          <Lightbulb className="size-3.5 text-primary shrink-0 mt-0.5" />
          <span>{data.note}</span>
        </div>
      )}
    </div>
  );
}
