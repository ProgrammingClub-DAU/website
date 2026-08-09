"use client";

import { useState } from "react";

import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState } from "@/components/site/primitives";
import {
  Timeline,
  TimelineCard,
  TimelineEnd,
  TimelineEntry,
  TimelineGroup,
  TimelineRoot,
} from "@/components/site/timeline";
import { rankColor } from "@/lib/cf-ranks";
import { eventTypes, type ClubEvent } from "@/lib/content/events";

export function EventsTimeline({ events }: { events: ClubEvent[] }) {
  const [filter, setFilter] = useState<string>("All events");
  const visible =
    filter === "All events" ? events : events.filter((e) => e.type === filter);

  return (
    <>
      <FilterChips
        label="Filter by event type"
        options={eventTypes}
        value={filter}
        onChange={setFilter}
      />

      <div className="mt-10 flex flex-wrap items-baseline justify-between gap-3">
        <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
          Past events
        </h2>
        <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
          {String(visible.length).padStart(2, "0")} shown
        </span>
      </div>

      <TimelineGroup className="mt-8">
        <TimelineRoot label="Club timeline" />

        {visible.length === 0 ? (
          <EmptyState
            title="Nothing logged under that type yet."
            hint="Switch back to All events, or add the record."
          />
        ) : (
          <>
            <Timeline>
              {visible.map((event, i) => (
                <TimelineEntry key={event.title} isLast={i === visible.length - 1}>
                  <TimelineCard>
                    <div className="flex flex-wrap items-center gap-2.5">
                      <span className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase transition-colors group-hover/entry:text-primary">
                        {event.date}
                      </span>
                      <span className="ml-auto rounded-full border border-border px-2.5 py-1 font-mono text-[10px] tracking-[0.12em] whitespace-nowrap text-fg-muted uppercase">
                        {event.type}
                      </span>
                    </div>
                    <h3 className="text-lg font-semibold tracking-tight text-pretty">
                      {event.title}
                    </h3>
                    <p className="text-[15px] leading-[1.5] text-fg-muted text-pretty">
                      {event.body}
                    </p>
                    <div className="flex items-center justify-between gap-3 border-t border-hairline pt-3.5">
                      <span className="font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
                        {event.meta}
                      </span>
                      <span className="flex gap-1" aria-hidden>
                        {event.dots.map((dot) => (
                          <span
                            key={dot}
                            className="size-1.5 rounded-full"
                            style={{ background: rankColor(dot) }}
                          />
                        ))}
                      </span>
                    </div>
                  </TimelineCard>
                </TimelineEntry>
              ))}
            </Timeline>
            <TimelineEnd label="Earlier records pending" />
          </>
        )}
      </TimelineGroup>
    </>
  );
}
