"use client";

import { useState } from "react";

import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState, RankDot } from "@/components/site/primitives";
import {
  RAIL,
  SPINE_X,
  Timeline,
  TimelineCard,
  TimelineEnd,
  TimelineEntry,
  TimelineGroup,
  TimelineHeadSpine,
  TimelineRoot,
} from "@/components/site/timeline";
import type { HofYear } from "@/lib/content/hall-of-fame";
import { cn } from "@/lib/utils";

export function HallOfFameTimeline({ years }: { years: HofYear[] }) {
  const [filter, setFilter] = useState("All years");
  const options = ["All years", ...years.map((y) => y.year)];
  const visible = filter === "All years" ? years : years.filter((y) => y.year === filter);

  return (
    <>
      <FilterChips
        label="Filter by year"
        options={options}
        value={filter}
        onChange={setFilter}
      />

      <div className="mt-10">
        <TimelineRoot label="Club record" />

        {visible.length === 0 ? (
          <EmptyState
            title="No records for that year yet."
            hint="Pick another year, or add the record."
          />
        ) : (
          visible.map((year, yearIndex) => (
            <TimelineGroup key={year.year}>
              <header className={cn("items-center py-4", RAIL)}>
                <div className="relative h-14">
                  <TimelineHeadSpine />
                  <span
                    className="absolute top-1/2 size-[11px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary shadow-[0_0_0_4px_var(--primary-soft)]"
                    style={{ left: SPINE_X }}
                    aria-hidden
                  />
                </div>
                <div className="flex flex-wrap items-baseline justify-between gap-3">
                  <div className="flex items-baseline gap-4">
                    <h2 className="font-mono text-[clamp(1.5rem,3vw,2rem)] font-medium tracking-tight">
                      {year.year}
                    </h2>
                    <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                      {String(year.entries.length).padStart(2, "0")} entries
                    </span>
                  </div>
                  <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                    {year.label}
                  </span>
                </div>
              </header>

              <Timeline>
                {year.entries.map((entry, i) => (
                  <TimelineEntry
                    key={entry.title}
                    isLast={
                      yearIndex === visible.length - 1 && i === year.entries.length - 1
                    }
                  >
                    <TimelineCard>
                      <div className="flex items-center gap-2.5">
                        <RankDot rank={entry.cf} />
                        <span className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase transition-colors [@media(hover:hover)]:group-hover/entry:text-primary group-[.tl-active]/entry:text-primary">
                          {entry.cat}
                        </span>
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold tracking-tight text-pretty">
                          {entry.title}
                        </h3>
                        <p className="mt-2 font-mono text-xs text-fg-muted">
                          {entry.people}
                        </p>
                      </div>
                      <p className="border-t border-hairline pt-3.5 text-[15px] leading-[1.5] text-fg-muted text-pretty">
                        {entry.note}
                      </p>
                    </TimelineCard>
                  </TimelineEntry>
                ))}
              </Timeline>
            </TimelineGroup>
          ))
        )}

        {visible.length > 0 && <TimelineEnd label="Older records pending" />}
      </div>
    </>
  );
}
