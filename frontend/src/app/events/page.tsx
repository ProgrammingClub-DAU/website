import type { Metadata } from "next";
import Link from "next/link";

import BorderGlow from "@/components/site/border-glow";
import { Button } from "@/components/ui/button";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { EventsTimeline } from "@/components/site/events-timeline";
import { events, nextEvent } from "@/lib/content/events";

export const metadata: Metadata = {
  title: "Events",
  description:
    "Contests, workshops, and ICPC sessions run by the Programming Club @ DAU, plus what is coming next.",
};

/** The four kinds of event the club runs, shown while nothing is scheduled. */
const EVENT_KINDS = [
  { k: "Flagship", v: "The campus-wide contest the club is known for." },
  { k: "Contests", v: "Shorter rated rounds, run through the term." },
  { k: "Workshops", v: "Teaching sessions, aimed at first-time solvers." },
  { k: "ICPC", v: "Team practice and preparation for the regionals." },
];

export default function EventsPage() {
  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Events</Eyebrow>
        <PageTitle className="max-w-[20ch]">
          What we run, and when.
        </PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Contests, workshops, and ICPC sessions the club has held, plus what is coming
          next.
        </p>
      </Section>

      <Section className="pb-14">
        <BorderGlow contentClassName="grid gap-8 p-8 md:grid-cols-2 md:items-center">
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
                  <span className="font-mono text-[11px] tracking-[0.12em] text-fg-muted uppercase">
                    Next up
                  </span>
                </div>
                <h2 className="mt-4.5 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
                  {nextEvent.title}
                </h2>
                <p className="mt-3 max-w-[48ch] text-base leading-6 text-fg-muted text-pretty">
                  {nextEvent.summary}
                </p>
                <div className="mt-6.5 flex flex-wrap gap-3">
                  <Button asChild className="h-10 rounded-full px-5.5">
                    <Link href="/login">Register</Link>
                  </Button>
                  <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
                    <Link href="/about">About the club</Link>
                  </Button>
                </div>
              </div>

              <dl className="flex flex-col gap-px overflow-hidden rounded-control bg-hairline">
                {nextEvent.meta.map((row) => (
                  <div
                    key={row.k}
                    className="flex items-baseline justify-between gap-4 bg-surface-2 px-4.5 py-3.5"
                  >
                    <dt className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                      {row.k}
                    </dt>
                    <dd className="text-right font-mono text-xs">{row.v}</dd>
                  </div>
                ))}
              </dl>
            </>
          ) : (
            <>
              <div>
                <div className="flex items-center gap-2">
                  <span className="size-1.5 rounded-full bg-fg-subtle" aria-hidden />
                  <span className="font-mono text-[11px] tracking-[0.12em] text-fg-muted uppercase">
                    Nothing scheduled
                  </span>
                </div>
                <h2 className="mt-4.5 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em] text-pretty">
                  The next round has not been announced.
                </h2>
                <p className="mt-3 max-w-[48ch] text-base leading-6 text-fg-muted text-pretty">
                  Rounds, workshops and ICPC sessions are announced here first. Join the
                  club and you will see the next one as soon as it is set.
                </p>
                <div className="mt-6.5 flex flex-wrap gap-3">
                  <Button asChild className="h-10 rounded-full px-5.5">
                    <Link href="/register">Join the Club</Link>
                  </Button>
                  <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
                    <Link href="/about">About the club</Link>
                  </Button>
                </div>
              </div>

              {/* What the club runs, in place of the detail table. Real
                  information rather than a shape: it keeps the panel balanced
                  on two columns without inventing a date, a venue and a format
                  to fill the space. */}
              <dl className="flex flex-col gap-px overflow-hidden rounded-control bg-hairline">
                {EVENT_KINDS.map((kind) => (
                  <div key={kind.k} className="bg-surface-2 px-4.5 py-3.5">
                    <dt className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                      {kind.k}
                    </dt>
                    <dd className="mt-1 text-[13px] leading-[1.45] text-fg-muted text-pretty">
                      {kind.v}
                    </dd>
                  </div>
                ))}
              </dl>
            </>
          )}
        </BorderGlow>
      </Section>

      <Section className="pb-10">
        <EventsTimeline events={events} />
      </Section>

      <Section className="pt-6 pb-22">
        <div className="flex flex-wrap items-center justify-between gap-5 border-t border-hairline pt-8">
          <p className="max-w-[52ch] text-[15px] leading-[1.5] text-fg-muted text-pretty">
            Have an idea for a round, a workshop, or a guest session? Members can propose
            events and set problems for them.
          </p>
          <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
            <Link href="/login">Propose an event</Link>
          </Button>
        </div>
      </Section>
    </>
  );
}
