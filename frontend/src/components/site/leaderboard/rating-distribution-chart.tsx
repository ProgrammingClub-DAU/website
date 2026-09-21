"use client";

import React, { useMemo } from "react";
import { ResponsiveContainer, BarChart, Bar, XAxis, Tooltip, Cell } from "recharts";
import type { LeaderboardEntry, LeaderboardPlatform } from "@/types/api";

interface RatingDistributionChartProps {
  entries: LeaderboardEntry[];
  platform: LeaderboardPlatform;
}

interface TierBucket {
  name: string;
  count: number;
  color: string;
  min: number;
}

const CF_BUCKETS: Omit<TierBucket, "count">[] = [
  { name: "Newbie",       color: "#9ba1a6", min: 0    },
  { name: "Pupil",        color: "#3fbf5f", min: 1200 },
  { name: "Specialist",   color: "#00c2c7", min: 1400 },
  { name: "Expert",       color: "#4c7dff", min: 1600 },
  { name: "Cand. Master", color: "#c066e0", min: 1900 },
  { name: "Master+",      color: "#ff9f45", min: 2100 },
];

const LC_BUCKETS: Omit<TierBucket, "count">[] = [
  { name: "Unrated",  color: "#6b7a99", min: 0    },
  { name: "Knight",   color: "#7c9eff", min: 1850 },
  { name: "Guardian", color: "#c084fc", min: 2100 },
];

export const RatingDistributionChart: React.FC<RatingDistributionChartProps> = ({ entries, platform }) => {
  const data = useMemo((): TierBucket[] => {
    const template = platform === "LEETCODE" ? LC_BUCKETS : CF_BUCKETS;
    const buckets: TierBucket[] = template.map((b) => ({ ...b, count: 0 }));

    entries.forEach((e) => {
      const r = e.rating ?? 0;
      // Walk backwards so the highest matching bucket wins
      for (let i = buckets.length - 1; i >= 0; i--) {
        if (r >= buckets[i].min) {
          buckets[i].count++;
          break;
        }
      }
    });

    return buckets;
  }, [entries, platform]);

  return (
    <div className="w-full">
      <div className="h-44 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
            <XAxis
              dataKey="name"
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              axisLine={false}
              dy={5}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const item = payload[0].payload as TierBucket;
                  return (
                    <div className="rounded-lg border border-white/15 bg-[#12121e] px-3 py-2 shadow-xl backdrop-blur-md">
                      <p className="text-xs font-bold" style={{ color: item.color }}>
                        {item.name}
                      </p>
                      <p className="text-xs text-white/90 font-mono mt-0.5">
                        <span className="font-extrabold text-white">{item.count}</span> members
                      </p>
                    </div>
                  );
                }
                return null;
              }}
              cursor={{ fill: "rgba(255,255,255,0.04)" }}
            />
            <Bar dataKey="count" radius={[4, 4, 0, 0]}>
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <div className="flex items-center justify-between text-[11px] text-white/50 px-2 mt-2 font-mono">
        <span>Tier Distribution</span>
        <span>{entries.length} members</span>
      </div>
    </div>
  );
};
