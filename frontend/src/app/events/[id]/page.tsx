/**
 * One event's public page.
 *
 * Server-rendered and readable signed out, like the rest of the site. What it
 * shows of the results is decided by the server, not here: the contest link, the
 * podium and the turnout each arrive only once an admin has published them, so
 * this page renders whatever it was given and never has to ask whether it is
 * allowed to.
 */

import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { ArrowLeft, CalendarDays, MapPin, Trophy, Users, ExternalLink } from "lucide-react";

import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { PhotoGrid } from "@/components/site/photo-grid";
import { eventsService } from "@/lib/services/events";
import type { EventDetail, EventWinner } from "@/types/api";

export const dynamic = "force-dynamic";

async function loadEvent(id: string): Promise<EventDetail | null> {
  const numeric = Number(id);
  if (!Number.isInteger(numeric) || numeric <= 0) return null;

  try {
    return await eventsService.getEventDetail(numeric);
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const event = await loadEvent((await params).id);
  if (!event) return { title: "Event not found" };

  return {
    title: event.title,
    description: event.description ?? `${event.title} at ${event.location}.`,
  };
}

const MEDAL = ["🥇", "🥈", "🥉"];

export default async function EventDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const event = await loadEvent((await params).id);
  if (!event) notFound();

  const date = new Date(event.eventDate);
  const podium = [...event.winners].sort((a, b) => a.position - b.position);

  // An event with no published contest link shows no results section at all.
  // A workshop is not a contest with an empty podium.
  const hasResults =
    Boolean(event.codeforcesContestUrl) || podium.length > 0 || event.attendeeCount !== null;

  return (
    <>
      <Section className="pt-10 pb-8 md:pt-14">
        <Link
          href="/events"
          className="inline-flex items-center gap-1.5 font-mono text-micro tracking-caps-wide text-fg-muted uppercase transition-colors hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" /> All events
        </Link>

        <div className="mt-5">
          <Eyebrow>{event.status === "COMPLETED" ? "Past event" : "Upcoming"}</Eyebrow>
          <PageTitle>{event.title}</PageTitle>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 font-mono text-xs text-fg-muted">
          <span className="inline-flex items-center gap-1.5">
            <CalendarDays className="size-3.5 text-primary" />
            {date.toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })}
            {" · "}
            {date.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <MapPin className="size-3.5 text-primary" />
            {event.location}
          </span>
        </div>

        {event.description && (
          <p className="mt-6 max-w-[62ch] text-base leading-6 text-fg-muted text-pretty whitespace-pre-line">
            {event.description}
          </p>
        )}
      </Section>

      {event.coverImageUrl && (
        <Section className="pb-8">
          <div className="relative aspect-[21/9] w-full overflow-hidden rounded-panel border border-border bg-surface-2">
            <Image
              src={event.coverImageUrl}
              alt=""
              fill
              sizes="100vw"
              className="object-cover"
              priority
            />
          </div>
        </Section>
      )}

      {hasResults && (
        <Section className="pb-10">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {event.codeforcesContestUrl && (
              <a
                href={event.codeforcesContestUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="group flex flex-col justify-between rounded-panel border border-border bg-surface-2 p-5 transition-colors hover:border-primary"
              >
                <span className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                  Contest
                </span>
                <span className="mt-2 inline-flex items-center gap-1.5 text-sm font-semibold text-foreground group-hover:text-primary">
                  Open on Codeforces
                  <ExternalLink className="size-3.5" />
                </span>
              </a>
            )}

            {event.attendeeCount !== null && (
              <div className="rounded-panel border border-border bg-surface-2 p-5">
                <span className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                  Attended
                </span>
                <span className="mt-2 flex items-center gap-2 text-2xl font-bold tracking-tight text-foreground">
                  <Users className="size-5 text-primary" />
                  {event.attendeeCount}
                </span>
              </div>
            )}
          </div>

          {podium.length > 0 && (
            <div className="mt-8">
              <h2 className="mb-4 inline-flex items-center gap-2 font-mono text-xs font-bold tracking-caps-wide text-primary uppercase">
                <Trophy className="size-3.5" /> Winners
              </h2>
              <div className="grid gap-4 sm:grid-cols-3">
                {podium.map((winner) => (
                  <WinnerCard key={winner.userId} winner={winner} />
                ))}
              </div>
            </div>
          )}
        </Section>
      )}

      {event.photos.length > 0 && (
        <Section className="pb-22">
          <h2 className="mb-4 font-mono text-xs font-bold tracking-caps-wide text-primary uppercase">
            Photos
          </h2>
          {/* The same grid and lightbox as the gallery, without a "view event"
              link -- the visitor is already on the event. */}
          <PhotoGrid
            photos={event.photos.map((photo) => ({
              id: photo.id,
              imageUrl: photo.imageUrl,
              caption: photo.caption,
              title: event.title,
              meta: date.toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" }),
            }))}
          />
        </Section>
      )}
    </>
  );
}

/** A placing, linking to the member rather than just naming them. */
function WinnerCard({ winner }: { winner: EventWinner }) {
  return (
    <article className="group relative flex items-center gap-3 rounded-panel border border-border bg-surface-2 p-4 transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
      <span className="text-2xl" aria-hidden>
        {MEDAL[winner.position - 1] ?? "🏅"}
      </span>

      <div className="flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-full border border-border bg-surface-3">
        {winner.avatarUrl ? (
          <Image
            src={winner.avatarUrl}
            alt=""
            width={44}
            height={44}
            className="size-full object-cover"
          />
        ) : (
          <span className="font-mono text-sm font-bold text-foreground">
            {winner.name.slice(0, 2).toUpperCase()}
          </span>
        )}
      </div>

      <div className="min-w-0">
        <h3 className="truncate text-sm font-bold tracking-tight text-foreground">
          <Link
            href={`/profile/${winner.userId}`}
            className="after:absolute after:inset-0 group-hover:underline"
          >
            {winner.name}
          </Link>
        </h3>
        {winner.codeforcesHandle && (
          <p className="truncate font-mono text-nano text-fg-muted">@{winner.codeforcesHandle}</p>
        )}
      </div>
    </article>
  );
}
