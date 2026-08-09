import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, Section } from "@/components/site/primitives";
import { EventsTimeline } from "@/components/site/events-timeline";
import { events, nextEventMeta } from "@/lib/content/events";

export const metadata: Metadata = {
  title: "Events",
  description:
    "Contests, workshops, and ICPC sessions run by the Programming Club @ DAU, plus what is coming next.",
};

export default function EventsPage() {
  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <Eyebrow>Events</Eyebrow>
        <h1 className="mt-6 max-w-[20ch] text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
          What we run, and when.
        </h1>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Contests, workshops, and ICPC sessions the club has held, plus what is coming
          next. Event details below are placeholders until confirmed from club records.
        </p>
      </Section>

      <Section className="pb-14">
        <div className="glass-panel grid gap-8 rounded-panel p-8 md:grid-cols-2 md:items-center">
          <div>
            <div className="flex items-center gap-2">
              <span
                className="size-1.5 animate-pulse-ring rounded-full bg-cf-pupil"
                aria-hidden
              />
              <span className="font-mono text-[11px] tracking-[0.12em] text-fg-muted uppercase">
                Next up
              </span>
            </div>
            <h2 className="mt-4.5 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
              [PLACEHOLDER] Event name
            </h2>
            <p className="mt-3 max-w-[48ch] text-base leading-6 text-fg-muted text-pretty">
              [PLACEHOLDER] One or two lines on format, difficulty range, and who it suits.
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
            {nextEventMeta.map((row) => (
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
        </div>
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
