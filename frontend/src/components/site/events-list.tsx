/**
 * The club's events, on the timeline the site already uses for its history.
 *
 * Two runs of it: what is coming, and what has happened. Upcoming reads
 * soonest-first because the next thing matters most; past reads most-recent
 * first, which is how the club's record is usually looked back through. Both
 * orders come from the server.
 *
 * Everything on a card is the event's own. The timeline that stood here before
 * was fed from a hand-written file, so it could show a participant count for an
 * event nobody had attended; here the count appears only once an admin has
 * published it, and the badge only when the event has been given a type.
 */

import Link from "next/link";
import { ArrowUpRight } from "lucide-react";

import {
  Timeline,
  TimelineCard,
  TimelineEnd,
  TimelineEntry,
  TimelineGroup,
  TimelineRoot,
} from "@/components/site/timeline";
import { EVENT_TYPE_LABELS, type Event } from "@/types/api";

function monthYear(iso: string): string {
  return new Date(iso)
    .toLocaleDateString("en-IN", { month: "long", year: "numeric" })
    .toUpperCase();
}

function dayAndTime(iso: string): string {
  const date = new Date(iso);
  return `${date.toLocaleDateString("en-IN", {
    weekday: "short",
    day: "numeric",
  })} · ${date.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}`;
}

export function EventsList({ upcoming, completed }: { upcoming: Event[]; completed: Event[] }) {
  if (upcoming.length === 0 && completed.length === 0) {
    return (
      <div className="rounded-panel border border-dashed border-border py-14 text-center">
        <p className="text-sm text-fg-muted">No events yet.</p>
        <p className="mt-1 text-xs text-fg-subtle">
          Rounds and workshops appear here once the committee schedules them.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-16">
      {upcoming.length > 0 && (
        <EventRun
          heading="Upcoming"
          rootLabel="What is next"
          endLabel="Nothing further scheduled"
          events={upcoming}
        />
      )}

      {completed.length > 0 && (
        <EventRun
          heading="Past events"
          rootLabel="Club timeline"
          endLabel="Start of the record"
          events={completed}
        />
      )}
    </div>
  );
}

function EventRun({
  heading,
  rootLabel,
  endLabel,
  events,
}: {
  heading: string;
  rootLabel: string;
  endLabel: string;
  events: Event[];
}) {
  return (
    <section>
      <div className="flex flex-wrap items-baseline justify-between gap-3">
        <h2 className="font-heading text-[clamp(1.5rem,3.2vw,2rem)] font-medium tracking-[-0.02em]">
          {heading}
        </h2>
        <span className="font-mono text-label tracking-caps text-fg-subtle uppercase">
          {String(events.length).padStart(2, "0")} shown
        </span>
      </div>

      <TimelineGroup className="mt-8">
        <TimelineRoot label={rootLabel} />

        <Timeline>
          {events.map((event, i) => (
            <TimelineEntry key={event.id} isLast={i === events.length - 1}>
              <TimelineCard>
                <div className="flex flex-wrap items-center gap-2.5">
                  <span className="font-mono text-label tracking-caps text-fg-subtle uppercase transition-colors [@media(hover:hover)]:group-hover/entry:text-primary group-[.tl-active]/entry:text-primary">
                    {monthYear(event.eventDate)}
                  </span>
                  {/* Only when the event has actually been given a type. */}
                  {event.eventType && (
                    <span className="ml-auto rounded-full border border-border px-2.5 py-1 font-mono text-micro tracking-caps-wide whitespace-nowrap text-fg-muted uppercase">
                      {EVENT_TYPE_LABELS[event.eventType]}
                    </span>
                  )}
                </div>

                <h3 className="text-lg font-semibold tracking-tight text-pretty">
                  {/* Stretched over the card, so the whole entry is the link. */}
                  <Link href={`/events/${event.id}`} className="after:absolute after:inset-0">
                    {event.title}
                  </Link>
                </h3>

                {event.description && (
                  <p className="line-clamp-3 text-body leading-[1.5] text-fg-muted text-pretty">
                    {event.description}
                  </p>
                )}

                <div className="flex items-center justify-between gap-3 border-t border-hairline pt-3.5">
                  <span className="font-mono text-label tracking-caps text-fg-subtle uppercase">
                    {dayAndTime(event.eventDate)} · {event.location}
                  </span>

                  {/* No turnout marker here. The listing DTO does not carry the
                      figure, and a badge saying a number exists without saying
                      what it is tells a visitor nothing. It is on the event. */}
                  <ArrowUpRight className="size-4 text-fg-subtle transition-colors [@media(hover:hover)]:group-hover/entry:text-primary" />
                </div>
              </TimelineCard>
            </TimelineEntry>
          ))}
        </Timeline>

        <TimelineEnd label={endLabel} />
      </TimelineGroup>
    </section>
  );
}
