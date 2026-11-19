"use client";

import type { ReactNode } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { DailySolves } from "@/types/api";

/** Platform colours shared by the activity and rating charts. */
export const CF_COLOR = "var(--primary)";
export const LC_COLOR = "#f59e0b";

/** "2026-11-12" -> "12 Nov". */
function shortDate(isoDate: string): string {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

/**
 * Problems solved per day, Codeforces and LeetCode stacked. Used on member
 * profiles and the admin's CP Practice Group student page.
 */
export function DailySolvesChart({ daily }: { daily: DailySolves[] }) {
  return (
    <div className="h-56 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={daily.map((d) => ({ ...d, label: shortDate(d.date) }))} margin={{ top: 4, right: 4, left: -24, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--hairline)" />
          <XAxis dataKey="label" tick={{ fill: "var(--fg-subtle)", fontSize: 10 }} tickLine={false} axisLine={false} interval={4} />
          <YAxis allowDecimals={false} tick={{ fill: "var(--fg-subtle)", fontSize: 10 }} tickLine={false} axisLine={false} />
          <Tooltip
            cursor={{ fill: "var(--hairline)" }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null;
              const d = payload[0].payload as DailySolves;
              return (
                <ChartTip
                  title={new Date(`${d.date}T00:00:00`).toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" })}
                >
                  <p>
                    Codeforces: <strong>{d.codeforces}</strong>
                  </p>
                  <p>
                    LeetCode: <strong>{d.leetcode ?? "—"}</strong>
                  </p>
                </ChartTip>
              );
            }}
          />
          <Bar dataKey="codeforces" stackId="solved" fill={CF_COLOR} />
          <Bar dataKey="leetcode" stackId="solved" fill={LC_COLOR} radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function ChartLegend() {
  return (
    <div className="flex items-center gap-3 font-mono text-[11px] text-fg-muted">
      <span className="flex items-center gap-1.5">
        <span className="size-2 rounded-full" style={{ background: CF_COLOR }} />
        Codeforces
      </span>
      <span className="flex items-center gap-1.5">
        <span className="size-2 rounded-full" style={{ background: LC_COLOR }} />
        LeetCode
      </span>
    </div>
  );
}

export function ChartTip({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 text-xs text-popover-foreground shadow-xl">
      <p className="mb-1 font-semibold">{title}</p>
      <div className="space-y-0.5 font-mono text-fg-muted">{children}</div>
    </div>
  );
}
