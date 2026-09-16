import Link from "next/link";
import Image from "next/image";
import { CalendarDays, MapPin, Trophy, ArrowRight } from "lucide-react";

import type { Event } from "@/types/api";

/**
 * The club's events, from the database.
 *
 * Replaces a hand-written list in lib/content/events.ts that the admin panel
 * could not reach: an admin could create an event, take attendance for it and
 * export the sheet, and the public page would still show whatever was typed into
 * that file months earlier.
 *
 * Cards show only what an event always has -- title, date, place, description.
 * Results live on the detail page, and only once published.
 */

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("en-IN", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function EventsList({
  upcoming,
  completed,
}: {
  upcoming: Event[];
  completed: Event[];
}) {
  if (upcoming.length === 0 && completed.length === 0) {
    return (
      <div className="rounded-panel border border-dashed border-border py-14 text-center">
        <CalendarDays className="mx-auto size-6 text-fg-subtle" />
        <p className="mt-3 text-sm text-fg-muted">No events scheduled yet.</p>
        <p className="mt-1 text-xs text-fg-subtle">
          This page fills in as the committee announces rounds and workshops.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      {upcoming.length > 0 && (
        <EventSection
          title="UPCOMING"
          subtitle="Rounds, workshops and sessions still to come."
          events={upcoming}
        />
      )}

      {completed.length > 0 && (
        <EventSection
          title="PAST EVENTS"
          subtitle="What the club has run. Photos and results where they have been published."
          events={completed}
        />
      )}
    </div>
  );
}

function EventSection({
  title,
  subtitle,
  events,
}: {
  title: string;
  subtitle: string;
  events: Event[];
}) {
  return (
    <section>
      <div className="mb-5 flex flex-wrap items-baseline gap-x-3 gap-y-1 border-b border-border pb-3">
        <h2 className="font-mono text-xs font-bold tracking-caps-wide text-primary uppercase">
          {title}
        </h2>
        <span className="font-mono text-micro text-fg-subtle">{events.length}</span>
        <p className="w-full text-sm text-fg-muted sm:w-auto">{subtitle}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {events.map((event) => (
          <EventCard key={event.id} event={event} />
        ))}
      </div>
    </section>
  );
}

function EventCard({ event }: { event: Event }) {
  return (
    <article className="group relative flex flex-col overflow-hidden rounded-panel border border-border bg-surface-2 transition-all hover:-translate-y-1 hover:border-hairline-strong hover:shadow-panel">
      {event.coverImageUrl && (
        <div className="relative aspect-[16/9] w-full overflow-hidden bg-surface-3">
          <Image
            src={event.coverImageUrl}
            alt=""
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            className="object-cover transition-transform duration-500 group-hover:scale-105"
          />
        </div>
      )}

      <div className="flex flex-1 flex-col p-5">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 font-mono text-micro text-fg-subtle">
          <span className="inline-flex items-center gap-1">
            <CalendarDays className="size-3" />
            {formatDate(event.eventDate)}
          </span>
          <span>{formatTime(event.eventDate)}</span>
        </div>

        <h3 className="mt-2 text-base font-bold tracking-tight text-foreground">
          <Link href={`/events/${event.id}`} className="after:absolute after:inset-0 group-hover:underline">
            {event.title}
          </Link>
        </h3>

        <p className="mt-1 inline-flex items-center gap-1 text-xs text-fg-muted">
          <MapPin className="size-3 shrink-0" />
          <span className="truncate">{event.location}</span>
        </p>

        {event.description && (
          <p className="mt-3 line-clamp-2 text-xs leading-relaxed text-fg-muted">
            {event.description}
          </p>
        )}

        <div className="mt-auto flex items-center justify-between gap-2 pt-4">
          {/*
            One badge, and only when the contest link has actually been
            published, so a round that has not opened yet gives nothing away.
            A second badge reading "Turnout" used to sit beside it, which told
            a visitor that a number exists without telling them the number.
          */}
          <div>
            {event.codeforcesContestUrl && (
              <span className="inline-flex items-center gap-1 rounded-full border border-primary/40 bg-primary/10 px-2 py-0.5 font-mono text-nano text-primary uppercase">
                <Trophy className="size-2.5" /> Contest
              </span>
            )}
          </div>

          <span className="inline-flex items-center gap-1 font-mono text-nano text-fg-muted uppercase">
            Details <ArrowRight className="size-3" />
          </span>
        </div>
      </div>
    </article>
  );
}
