import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  RankDot,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import { calendar, faq, joinSteps, organisation, whatWeDo } from "@/lib/content/about";
import { hallOfFameTeaser } from "@/lib/content/hall-of-fame";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "About",
  description: `A student club built around practice, not prestige, at ${site.university}.`,
};

export default function AboutPage() {
  return (
    <>
      <Section className="pt-16 pb-16 md:pt-24">
        <Eyebrow>About</Eyebrow>
        <h1 className="mt-6 max-w-[22ch] text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
          A student club built around practice, not prestige.
        </h1>
        <p className="mt-6 max-w-[56ch] text-base leading-6 text-fg-muted text-pretty">
          {site.fullName} is run by students at {site.university}. We meet to solve
          problems, run contests, and help each other get measurably better at algorithmic
          programming.
        </p>
      </Section>

      <Section className="pb-16">
        <div className="grid gap-4 md:grid-cols-2">
          {[
            {
              label: "Mission",
              body: "Make competitive programming approachable at DAU: a clear path from a first contest submission to a regional-level team.",
            },
            {
              label: "Vision",
              body: "A campus where every batch has strong solvers who teach the next one, and where results carry over year to year instead of restarting.",
            },
          ].map((item) => (
            <div
              key={item.label}
              className="glass-panel rounded-panel p-7 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
            >
              <div className="font-mono text-xs tracking-[0.12em] text-fg-subtle uppercase">
                {item.label}
              </div>
              <p className="mt-4 text-base leading-6 text-pretty">{item.body}</p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="pb-16">
        <div className="grid gap-10 md:grid-cols-2 md:gap-12">
          <div>
            <h2 className="text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
              Where the club came from
            </h2>
            <p className="mt-4 text-base leading-6 text-fg-muted text-pretty">
              [PLACEHOLDER] Founding year and how the club started — who set it up, and
              what it was set up to fix.
            </p>
            <p className="mt-4 text-base leading-6 text-fg-muted text-pretty">
              [PLACEHOLDER] What has changed since: the contests that became fixtures, the
              batches that carried it, and where the club sits on campus today.
            </p>
          </div>
          <div>
            <h2 className="text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
              Who it is for
            </h2>
            <p className="mt-4 text-base leading-6 text-fg-muted text-pretty">
              Anyone at DAU who wants to get better at solving problems with code.
              First-years who have never opened a judge, students preparing for placement
              rounds, and people already rated on Codeforces all use the club differently,
              and all of that counts as taking part.
            </p>
            <p className="mt-4 text-base leading-6 text-fg-muted text-pretty">
              There is no entrance test and no minimum rating. The only expectation is
              that you turn up and attempt problems.
            </p>
          </div>
        </div>
      </Section>

      <Section className="pb-16">
        <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
          How the club is organised
        </h2>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {organisation.map((item) => (
            <div
              key={item.role}
              className="rounded-panel border border-hairline bg-surface p-6 transition-all hover:-translate-y-0.5 hover:border-border hover:shadow-panel"
            >
              <div className="flex items-center gap-2">
                <span
                  className="size-1.5 rounded-full"
                  style={{ background: item.color }}
                  aria-hidden
                />
                <span className="font-mono text-[11px] tracking-[0.12em] text-fg-muted uppercase">
                  {item.role}
                </span>
              </div>
              <p className="mt-4 text-[15px] leading-[1.5] text-fg-muted text-pretty">
                {item.body}
              </p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="pb-16">
        <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
          The year, roughly
        </h2>
        <p className="mt-4 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The calendar repeats every academic year, so members know what is coming without
          checking announcements.
        </p>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {calendar.map((item) => (
            <div
              key={item.title}
              className="border-t border-border pt-5 transition-all hover:-translate-y-0.5 hover:border-primary"
            >
              <div className="font-mono text-xs tracking-[0.1em] text-primary uppercase">
                {item.when}
              </div>
              <p className="mt-3 text-base font-semibold tracking-tight">{item.title}</p>
              <p className="mt-2 text-[15px] leading-[1.5] text-fg-muted text-pretty">
                {item.body}
              </p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="pb-16">
        <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
          What we do
        </h2>
        <dl className="mt-8 border-t border-hairline">
          {whatWeDo.map((item) => (
            <div
              key={item.title}
              className="grid gap-2 border-b border-hairline py-6 md:grid-cols-[minmax(0,14rem)_minmax(0,1fr)] md:gap-8"
            >
              <dt className="font-mono text-[13px] tracking-[0.08em] uppercase">
                {item.title}
              </dt>
              <dd className="text-base leading-6 text-fg-muted text-pretty">{item.body}</dd>
            </div>
          ))}
        </dl>
      </Section>

      <Section className="pb-16">
        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              label: "Meetings",
              body: "[PLACEHOLDER] Cadence, day, time, and venue — supply from the club's current schedule.",
            },
            {
              label: "Team",
              body: "[PLACEHOLDER] Core team roles and names for the current academic year.",
            },
            {
              label: "Contact",
              body: "[PLACEHOLDER] Club email or the handle to DM about joining.",
            },
          ].map((item) => (
            <div
              key={item.label}
              className="rounded-panel border border-hairline bg-surface p-7 transition-all hover:-translate-y-0.5 hover:border-border hover:shadow-panel"
            >
              <div className="font-mono text-xs tracking-[0.12em] text-fg-subtle uppercase">
                {item.label}
              </div>
              <p className="mt-4 text-base leading-6 text-fg-muted text-pretty">
                {item.body}
              </p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="pb-16">
        <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
          Common questions
        </h2>
        <dl className="mt-8 border-t border-hairline">
          {faq.map((item) => (
            <div
              key={item.q}
              className="grid gap-2 border-b border-hairline py-6 md:grid-cols-2 md:gap-8"
            >
              <dt className="text-base font-semibold tracking-tight text-pretty">
                {item.q}
              </dt>
              <dd className="text-[15px] leading-[1.5] text-fg-muted text-pretty">
                {item.a}
              </dd>
            </div>
          ))}
        </dl>
      </Section>

      <Section className="pb-16">
        <SectionHeader
          eyebrow="Hall of fame"
          title="Past results and alumni."
          action={{ href: "/hall-of-fame", label: "All years" }}
        />
        <ul className="no-scrollbar mt-8 flex gap-4 overflow-x-auto pb-2">
          {hallOfFameTeaser.map((entry) => (
            <li
              key={entry.title}
              className="w-70 flex-none rounded-panel border border-hairline bg-surface p-6 transition-all hover:-translate-y-0.5 hover:border-border hover:bg-surface-3 hover:shadow-panel"
            >
              <div className="flex items-center justify-between gap-3">
                <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                  {entry.year}
                </span>
                <RankDot rank={entry.cf} />
              </div>
              <p className="mt-11 text-[17px] font-semibold tracking-tight">{entry.title}</p>
              <p className="mt-2 text-sm leading-[1.5] text-fg-muted text-pretty">
                {entry.note}
              </p>
            </li>
          ))}
        </ul>
      </Section>

      <Section className="pb-22">
        <div className="glass-panel grid gap-8 rounded-panel p-10 md:grid-cols-2 md:items-center">
          <div>
            <h2 className="text-[clamp(1.5rem,3.2vw,2rem)] font-semibold tracking-[-0.02em]">
              How to join
            </h2>
            <p className="mt-4 max-w-[44ch] text-base leading-6 text-fg-muted text-pretty">
              Open to all DAU students, any batch, any experience level. Create an account
              with your college email, add your Codeforces handle, and come to the next
              round.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Button asChild className="h-10 rounded-full px-5.5">
                <Link href="/login">Create an account</Link>
              </Button>
              <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
                <Link href="/members">Meet the members</Link>
              </Button>
            </div>
          </div>

          <ol className="flex flex-col gap-px overflow-hidden rounded-control bg-hairline">
            {joinSteps.map((step) => (
              <li key={step.n} className="flex items-baseline gap-3.5 bg-surface-2 px-5 py-4.5">
                <span className="font-mono text-xs text-primary">{step.n}</span>
                <span className="text-[15px] leading-[1.45]">{step.text}</span>
              </li>
            ))}
          </ol>
        </div>
      </Section>
    </>
  );
}
