import type { Metadata } from "next";
import Link from "next/link";

import BorderGlow from "@/components/site/border-glow";
import { Button } from "@/components/ui/button";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { EventsList } from "@/components/site/events-list";
import { InDevelopment } from "@/components/site/in-development";
import { eventsService } from "@/lib/services/events";
import type { Event } from "@/types/api";

// The listing is live, so it must not be baked at build time: an event created
// this morning has to appear without a redeploy.
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Events",
  description:
    "Contests and lectures run by the Programming Club @ DAU, with results and photos, plus what is coming next.",
};


export default async function EventsPage() {
  // Both listings in one round trip each, in parallel. Empty on failure: this
  // page renders at request time against a backend that can be asleep on a free
  // tier, and an events page with no events beats a 500.
  let upcoming: Event[] = [];
  let completed: Event[] = [];

  try {
    [upcoming, completed] = await Promise.all([
      eventsService.listUpcoming(),
      eventsService.listCompleted(),
    ]);
  } catch {
    // Backend unreachable - the page still renders its explanatory panel.
  }

  // The soonest upcoming event is the one pinned at the top. Taken from the
  // list rather than configured separately, so it cannot go stale.
  const nextEvent = upcoming[0] ?? null;

  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Events</Eyebrow>
        <PageTitle className="max-w-[20ch]">
          What we run, and when.
        </PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Contests and lectures the club has held, with results and photos, plus what is
          coming next.
        </p>
      </Section>

      <Section className="pb-14">
        <BorderGlow contentClassName="grid gap-8 p-6 sm:p-8 md:grid-cols-2 md:items-center">
          {nextEvent ? (
            <>
              <div>
                <div className="flex items-center gap-2">
                  {/* The pulse only runs when something is actually scheduled.
                      A live indicator beating away next to no event is the kind
                      of detail that quietly teaches a visitor to distrust the
                      rest of the page. */}
                  <span
                    className="size-1.5 animate-pulse-ring rounded-full bg-cf-pupil"
                    aria-hidden
                  />
                  <span className="font-mono text-label tracking-caps text-fg-muted uppercase">
                    Next up
                  </span>
                </div>
                <h2 className="mt-4.5 font-heading text-[clamp(1.375rem,2.8vw,1.75rem)] font-medium tracking-[-0.02em]">
                  {nextEvent.title}
                </h2>
                {nextEvent.description && (
                  <p className="mt-3 max-w-[48ch] text-base leading-6 text-fg-muted text-pretty line-clamp-3">
                    {nextEvent.description}
                  </p>
                )}
                <div className="mt-6.5 flex flex-wrap gap-3">
                  <Button asChild className="h-10 rounded-full px-5.5">
                    <Link href={`/events/${nextEvent.id}`}>Event details</Link>
                  </Button>
                </div>
              </div>

              {/* Every row is read off the event, so there is no way for this
                  table to disagree with the event it describes. */}
              <dl className="flex flex-col gap-px overflow-hidden rounded-control bg-hairline">
                {[
                  {
                    k: "Date",
                    v: new Date(nextEvent.eventDate).toLocaleDateString("en-IN", {
                      day: "numeric",
                      month: "long",
                      year: "numeric",
                    }),
                  },
                  {
                    k: "Time",
                    v: new Date(nextEvent.eventDate).toLocaleTimeString("en-IN", {
                      hour: "2-digit",
                      minute: "2-digit",
                    }),
                  },
                  { k: "Venue", v: nextEvent.location },
                ].map((row) => (
                  <div
                    key={row.k}
                    className="flex items-baseline justify-between gap-4 bg-surface-2 px-4.5 py-3.5"
                  >
                    <dt className="font-mono text-label tracking-caps text-fg-subtle uppercase">
                      {row.k}
                    </dt>
                    <dd className="text-right font-mono text-xs">{row.v}</dd>
                  </div>
                ))}
              </dl>
            </>
          ) : (
            // Nothing scheduled. Says so, and stops -- the previous version
            // filled the space with a table describing four kinds of event the
            // club supposedly runs, written to fill a two-column layout rather
            // than from anything the club had said.
            <div className="md:col-span-2">
              <div className="flex items-center gap-2">
                <span className="size-1.5 rounded-full bg-fg-subtle" aria-hidden />
                <span className="font-mono text-label tracking-caps text-fg-muted uppercase">
                  Nothing scheduled
                </span>
              </div>
              <h2 className="mt-4.5 font-heading text-[clamp(1.375rem,2.8vw,1.75rem)] font-medium tracking-[-0.02em] text-pretty">
                The next round has not been announced.
              </h2>
              <p className="mt-3 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
                Rounds and workshops are announced here first.
              </p>
            </div>
          )}
        </BorderGlow>
      </Section>

      <Section className="pb-10">
        <EventsList upcoming={upcoming} completed={completed} />
      </Section>

      <Section className="border-t border-hairline pb-16 pt-14">
        <InDevelopment
          title="Registration and the club calendar"
          items={[
            "RSVP for events",
            "Club calendar",
            "Club contests",
            "Season championship",
          ]}
        />
      </Section>

    </>
  );
}
