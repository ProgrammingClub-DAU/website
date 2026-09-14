import type { Metadata } from "next";
import Link from "next/link";

import BorderGlow from "@/components/site/border-glow";
import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  PageTitle,
  RankDot,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import {
  PlatformMark,
  PLATFORM_ACCENT,
  PLATFORM_LABEL,
  PLATFORM_URL,
} from "@/components/site/platform-mark";
import {
  calendar,
  faq,
  joinSteps,
  organisation,
  platforms,
  whatWeDo,
} from "@/lib/content/about";
import { hallOfFameTeaser } from "@/lib/content/hall-of-fame";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "About",
  description: `A student club built around practice, not prestige, at ${site.university}.`,
};

export default function AboutPage() {
  return (
    <>
      <Section className="pt-10 pb-14 md:pt-14">
        <Eyebrow>About</Eyebrow>
        <PageTitle className="max-w-[22ch]">
          A student club built around practice, not prestige.
        </PageTitle>
        <p className="mt-6 max-w-[58ch] text-base leading-relaxed text-fg-muted text-pretty">
          {site.fullName} is run by students at {site.university}. We meet to solve
          challenging problems, run campus contests, and help each other level up in algorithmic
          programming — while celebrating every milestone together as a campus family.
        </p>
        <div className="mt-8 flex flex-wrap gap-2.5">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-surface px-3.5 py-1 text-xs font-medium text-fg-muted">
            <span className="size-1.5 rounded-full bg-cf-specialist" />
            150+ Active Solvers
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-surface px-3.5 py-1 text-xs font-medium text-fg-muted">
            <span className="size-1.5 rounded-full bg-cf-candidate" />
            Weekly Contests & Editorials
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-surface px-3.5 py-1 text-xs font-medium text-fg-muted">
            <span className="size-1.5 rounded-full bg-cf-master" />
            Festivals & Gatherings
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-hairline bg-surface px-3.5 py-1 text-xs font-medium text-fg-muted">
            <span className="size-1.5 rounded-full bg-cf-pupil" />
            Zero Entry Barrier
          </span>
        </div>
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
              className="glass-panel rounded-panel p-7 transition-all duration-200 hover:-translate-y-0.5 hover:border-hairline-strong"
            >
              <div className="font-mono text-xs tracking-[0.12em] text-fg-subtle uppercase">
                {item.label}
              </div>
              <p className="mt-4 text-base leading-6 text-pretty">{item.body}</p>
            </div>
          ))}
        </div>
      </Section>

      {/* Community Showcase & Culture Feature */}
      <Section className="pb-16">
        <div className="overflow-hidden rounded-2xl border border-hairline bg-surface transition-all duration-300 hover:border-border hover:shadow-panel">
          {/*
            The photo half of this panel is deliberately absent. It was built
            around /dummyImage.jpg, a placeholder, and a stock-looking filler
            image on the About page undercuts the section it illustrates. The
            copy stands on its own, so the story column spans the full width
            until a real club photograph exists.

            To restore it: put the photo in /public, wrap this in
            `<div className="grid gap-0 lg:grid-cols-12">`, add the image
            column as `lg:col-span-7`, and give this block `lg:col-span-5`.
          */}
          <div>
            {/* Story & Vibe */}
            <div className="flex flex-col justify-between p-8 lg:p-10">
              <div>
                <div className="font-mono text-xs tracking-[0.12em] text-primary uppercase">
                  Culture & Community
                </div>
                <h3 className="mt-2 text-2xl font-bold tracking-tight text-foreground">
                  Code hard, celebrate harder.
                </h3>
                <p className="mt-4 text-[15px] leading-relaxed text-fg-muted text-pretty">
                  We believe the best problem solvers aren&apos;t solitary grinders — they are a tight-knit family.
                  Beyond the Codeforces leaderboards and 5-hour ICPC qualifiers, our club comes alive during
                  festive Navratri Garba nights, Diwali celebrations, batch dinners, and late-night hostel debriefs.
                </p>
              </div>

              <div className="mt-8 grid grid-cols-2 gap-3 border-t border-hairline pt-6">
                <div className="rounded-xl border border-hairline bg-surface-2/50 p-3.5">
                  <div className="text-xl font-bold text-foreground">150+</div>
                  <div className="mt-1 text-xs text-fg-muted">Active members across batches</div>
                </div>
                <div className="rounded-xl border border-hairline bg-surface-2/50 p-3.5">
                  <div className="text-xl font-bold text-foreground">100%</div>
                  <div className="mt-1 text-xs text-fg-muted">Student-run & welcoming</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Section>

      <Section className="pb-16">
        <div className="grid gap-8 md:grid-cols-2 md:gap-10">
          <div className="rounded-panel border border-hairline bg-surface/50 p-8 transition-all hover:border-border">
            <div className="font-mono text-xs tracking-[0.12em] text-primary uppercase">
              Origin Story
            </div>
            <h2 className="mt-2 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
              Where the club came from
            </h2>
            <p className="mt-4 text-base leading-relaxed text-fg-muted text-pretty">
              Founded in 2021 by passionate seniors who wanted to build an enduring competitive programming culture at DAU. What started as whiteboard sessions in hostel common rooms quickly grew into weekly campus rounds and structured workshops.
            </p>
            <p className="mt-4 text-base leading-relaxed text-fg-muted text-pretty">
              Today, the club is a permanent campus fixture with multiple teams qualifying for ICPC Regionals, automated rating syncs, and seniors mentoring incoming batches.
            </p>
          </div>
          <div className="rounded-panel border border-hairline bg-surface/50 p-8 transition-all hover:border-border">
            <div className="font-mono text-xs tracking-[0.12em] text-primary uppercase">
              Inclusivity
            </div>
            <h2 className="mt-2 text-[clamp(1.375rem,2.8vw,1.75rem)] font-semibold tracking-[-0.02em]">
              Who it is for
            </h2>
            <p className="mt-4 text-base leading-relaxed text-fg-muted text-pretty">
              Anyone at DAU who wants to get better at solving problems with code. First-years who have never opened an online judge, students preparing for placement rounds, and seasoned contestants all practice together.
            </p>
            <p className="mt-4 text-base leading-relaxed text-fg-muted text-pretty">
              There is no entrance test, no cutoffs, and no gatekeeping. The only expectation is that you show up, stay curious, and attempt problems.
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
        <SectionHeader
          eyebrow="Where we compete"
          title="Four platforms, one leaderboard."
        />
        <p className="mt-5 max-w-[58ch] text-base leading-6 text-fg-muted text-pretty">
          Members practise wherever they like. These are the four we organise around,
          and the two we can track automatically.
        </p>
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          {platforms.map((platform) => (
            // Each card carries its own hue at rest, not only under the cursor.
            // `colors` drives the mesh fill, whose mask is mostly static, so a
            // single-hue trio tints the card persistently — no `animated` sweep,
            // which would have meant four rAF chains per card running forever
            // for something purely decorative.
            <BorderGlow
              key={platform.id}
              glowColor={PLATFORM_ACCENT[platform.id]}
              colors={[
                PLATFORM_ACCENT[platform.id],
                PLATFORM_ACCENT[platform.id],
                PLATFORM_ACCENT[platform.id],
              ]}
              contentClassName="p-0"
            >
              <a
                href={PLATFORM_URL[platform.id]}
                target="_blank"
                rel="noopener noreferrer"
                style={{ "--accent": PLATFORM_ACCENT[platform.id] } as React.CSSProperties}
                className="group block rounded-panel p-8 transition-transform duration-300 hover:-translate-y-1 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
              >
                <div className="flex items-start gap-4">
                  {/*
                    The mark gets a tinted tile of its own rather than sitting
                    inline at text size. At 24px beside a heading it read as
                    punctuation; at 30px on a 60px plate it becomes the thing
                    the eye lands on, which is what earns the section a look.

                    color-mix against the accent token keeps the tint and ring
                    derived from one value, so a change to PLATFORM_ACCENT
                    carries through the plate, the ring, and the glow together.

                    No `title` on the mark: the platform name is rendered right
                    beside it, so labelling the icon too would have screen
                    readers announce the same word twice for one link.
                  */}
                  <span
                    className="grid size-15 shrink-0 place-items-center rounded-2xl ring-1 transition-all duration-300 group-hover:scale-105"
                    style={{
                      background:
                        "color-mix(in srgb, var(--accent) 14%, transparent)",
                      boxShadow:
                        "0 0 28px color-mix(in srgb, var(--accent) 22%, transparent)",
                      "--tw-ring-color":
                        "color-mix(in srgb, var(--accent) 30%, transparent)",
                    } as React.CSSProperties}
                  >
                    <PlatformMark
                      platform={platform.id}
                      className="size-[30px] text-[var(--accent)]"
                    />
                  </span>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                      <span className="text-lg font-semibold tracking-tight">
                        {PLATFORM_LABEL[platform.id]}
                      </span>
                      {/*
                        The synced badge borrows the accent; "link only" stays
                        deliberately grey. The two states should not read as
                        equal — one is a live feature of this site, the other is
                        a plain outbound link.
                      */}
                      <span
                        className={
                          platform.syncs
                            ? "rounded-full px-2.5 py-0.5 font-mono text-[10px] tracking-[0.08em] text-[var(--accent)] uppercase ring-1"
                            : "rounded-full border border-hairline px-2.5 py-0.5 font-mono text-[10px] tracking-[0.08em] text-fg-subtle uppercase"
                        }
                        style={
                          platform.syncs
                            ? ({
                                background:
                                  "color-mix(in srgb, var(--accent) 12%, transparent)",
                                "--tw-ring-color":
                                  "color-mix(in srgb, var(--accent) 35%, transparent)",
                              } as React.CSSProperties)
                            : undefined
                        }
                      >
                        {platform.syncs ? "Rating synced" : "Link only"}
                      </span>
                    </div>
                    <p className="mt-3 text-sm leading-6 text-fg-muted text-pretty">
                      {platform.body}
                    </p>
                  </div>
                </div>
              </a>
            </BorderGlow>
          ))}
        </div>
      </Section>

      <Section className="pb-16">
        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              label: "Meetings",
              body: "Weekly rounds every Wednesday 6:00 PM and weekend workshops in Lab 3 / SAC. Hybrid participation supported.",
            },
            {
              label: "Team",
              body: "Led by students across batches: coordinators, contest problem setters, and senior ICPC mentors.",
            },
            {
              label: "Contact",
              body: "Drop by our lab sessions, join our active Discord community, or reach us directly at cpclub@dau.ac.in.",
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
        {/* Cards share the row rather than sitting at a fixed 280px, matching
            the same teaser on the home page. With one confirmed record, a lone
            narrow card at the left edge of a wide scroller reads as content
            that failed to load. They still scroll once there are enough. */}
        <ul className="no-scrollbar mt-8 flex gap-4 overflow-x-auto pb-2">
          {hallOfFameTeaser.map((entry) => (
            <li
              key={entry.title}
              className="min-w-70 flex-1 rounded-panel border border-hairline bg-surface p-6 transition-all hover:-translate-y-0.5 hover:border-border hover:bg-surface-3 hover:shadow-panel"
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
        <BorderGlow contentClassName="grid gap-8 p-10 md:grid-cols-2 md:items-center">
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
        </BorderGlow>
      </Section>
    </>
  );
}
