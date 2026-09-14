import Link from "next/link";
import {
  BarChart3,
  CalendarDays,
  FileText,
  Swords,
  Users,
  type LucideIcon,
} from "lucide-react";

import BorderGlow from "@/components/site/border-glow";
import { ClubWordmark } from "@/components/site/club-wordmark";
import { AuroraBackdrop } from "@/components/site/aurora-backdrop";
import { ParticlesBackdrop } from "@/components/site/particles-backdrop";
import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  RankDot,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import { PlatformMark } from "@/components/site/platform-mark";
import { RankLadder } from "@/components/site/rank-ladder";
import { CF_RANKS } from "@/lib/cf-ranks";
import { howItWorks } from "@/lib/content/home";
import { site } from "@/lib/site";
import { hallOfFameTeaser } from "@/lib/content/hall-of-fame";
import { cn } from "@/lib/utils";

type Feature = {
  icon: LucideIcon;
  title: string;
  body: string;
  span?: boolean;
  badge?: string;
  footer?: "ranks" | "avatars";
};

const features: Feature[] = [
  {
    icon: BarChart3,
    title: "Live leaderboard, synced from Codeforces",
    body: "Ratings, deltas, and rank colors update from members' Codeforces handles, so standings stay current without anyone editing a spreadsheet.",
    span: true,
    badge: "Signed-in members",
    footer: "ranks",
  },
  {
    icon: FileText,
    title: "Editorials & blog",
    body: "Write-ups from club contests and problem breakdowns, published by members.",
  },
  {
    icon: CalendarDays,
    title: "Contests & events",
    body: "Weekly practice rounds, ICPC prep, and beginner workshops on one calendar.",
    badge: "Coming soon",
  },
  {
    icon: Swords,
    title: "1v1 battles",
    body: "Head-to-head timed problems against another member, with a shared verdict feed.",
    badge: "Coming soon",
  },
  {
    icon: Users,
    title: "Member directory",
    body: "Handles across Codeforces, CodeChef, LeetCode, and AtCoder in one profile per member — useful for finding a team.",
    footer: "avatars",
  },
];

const avatarInitials = ["AR", "MP", "KV", "ND", "+9"];

export default function HomePage() {
  return (
    <>
      {/* Aurora is full-bleed, so it hangs off this wrapper rather than off the
          Section, which is width-capped by container-page. `isolate` keeps its
          negative z-index inside this subtree. */}
      <div className="relative isolate overflow-hidden">
        <AuroraBackdrop className="hero-aurora-mask pointer-events-none absolute inset-x-0 top-0 -z-10 h-[560px] opacity-[var(--aurora-strength)]" />

        {/* Dust drifting over the wash. Aurora moves slowly underneath; these
            move independently on top, in the rank ladder's own colours. */}
        <ParticlesBackdrop className="hero-field-mask pointer-events-none absolute inset-x-0 top-0 -z-10 h-[560px]" />

        {/* Centred since the hero lost its right-hand panel. Left-aligned text
            with nothing beside it left the whole right half of a wide screen
            empty and read as a layout fault rather than a choice. */}
        <Section className="pt-12 pb-20 text-center md:pt-16 md:pb-28">
          {/* The backdrops run through this block rather than around it, so
              the text carries its own legibility. See .text-halo. */}
          <div className="mx-auto max-w-[54ch]">
            <Eyebrow className="text-halo animate-rise">Competitive programming club</Eyebrow>
            <h1 className="text-halo mt-6 text-[clamp(2.375rem,6.4vw,4rem)] leading-none font-[510] tracking-[-0.02em] text-balance">
              A home for problem solvers at DAU.
            </h1>
            <p className="text-halo mx-auto mt-6 max-w-[46ch] text-base leading-6 text-fg-muted text-pretty">
              Weekly contests, editorials, and a leaderboard synced from Codeforces — for
              everyone from first-time solvers to ICPC regionalists.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Button asChild className="h-10 rounded-full px-5.5">
                <Link href="/register">Join the Club</Link>
              </Button>
              <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
                <Link href="/events">See our events</Link>
              </Button>
            </div>
          </div>
        </Section>
      </div>

      <Section className="py-16">
        <SectionHeader eyebrow="What the club runs" title="Everything in one place." />

        <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <div
                key={feature.title}
                className={cnFeature(feature.span)}
              >
                <div className="flex items-start justify-between gap-3">
                  <span className="flex size-8 items-center justify-center rounded-control border border-border text-fg-muted">
                    <Icon className="size-4" />
                  </span>
                  {feature.badge && (
                    <span className="rounded-full border border-border px-2 py-1 font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
                      {feature.badge}
                    </span>
                  )}
                </div>

                <div className="mt-5">
                  <h3 className="text-lg font-semibold tracking-tight">{feature.title}</h3>
                  <p className="mt-2.5 max-w-[56ch] text-base leading-6 text-fg-muted text-pretty">
                    {feature.body}
                  </p>
                </div>

                {feature.footer === "ranks" && (
                  <div className="mt-auto flex flex-wrap gap-2 pt-5" aria-hidden>
                    {CF_RANKS.map((r) => (
                      <span
                        key={r.key}
                        className="h-1.5 w-5.5 rounded-full opacity-85"
                        style={{ background: r.color }}
                      />
                    ))}
                  </div>
                )}

                {feature.footer === "avatars" && (
                  <div className="mt-auto flex pt-5 pl-2" aria-hidden>
                    {avatarInitials.map((initials) => (
                      <span
                        key={initials}
                        className="-ml-2 flex size-9 items-center justify-center rounded-full border border-border bg-surface-2 font-mono text-xs text-fg-muted"
                      >
                        {initials}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </Section>

      {/*
        The one full-bleed, saturated block above the closing band.

        Every other section on this page is a grid of bordered panels with the
        same hover lift, so by the third one the page has stopped offering the
        eye anything new. This breaks that run: it runs edge to edge, it is the
        only place with real colour, and it is a chart rather than a card.

        It also earns the space rather than only filling it. The rank dots
        appear on the Hall of Fame cards above, on the leaderboard and through
        the member directory, and nothing anywhere explains them — a visitor
        sees seven arbitrary colours. This is the legend.
      */}
      <section
        className="relative isolate overflow-hidden border-y border-hairline"
        style={{ background: "var(--band)" }}
      >
        {/* Warm at the tall end, cool at the low end — the ladder's own two
            extremes, bled into the band so the block is lit from beneath the
            bars rather than sitting on flat colour. */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 -z-10"
          style={{
            background:
              "radial-gradient(58% 62% at 84% 106%, color-mix(in srgb, var(--cf-grandmaster) 16%, transparent), transparent 68%), radial-gradient(52% 58% at 12% 106%, color-mix(in srgb, var(--cf-specialist) 15%, transparent), transparent 70%)",
          }}
        />

        <div className="container-page pt-16 md:pt-20">
          <SectionHeader eyebrow="The ladder" title="Everyone starts grey." />

          {/* The link pairs with the paragraph rather than with the heading.
              SectionHeader's own action slot wraps underneath the title on a
              narrow screen, which left it stranded between the heading and the
              body copy — reading as an interruption rather than as a way out of
              the section. Here it sits beside the paragraph on a wide screen and
              below it on a phone, which is right in both. */}
          <div className="mt-5 flex flex-col gap-5 md:flex-row md:items-end md:justify-between md:gap-10">
            <p className="max-w-[58ch] text-base leading-6 text-fg-muted text-pretty">
              The coloured dots beside every name on this site are Codeforces ranks. This
              is the whole ladder. Where a member sits is decided by rating alone, and it
              moves again after every rated round.
            </p>
            <Link
              href="/leaderboard"
              className="shrink-0 rounded-control py-1.5 font-mono text-[13px] tracking-[0.06em] text-primary uppercase transition-colors hover:opacity-80 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
            >
              See the board →
            </Link>
          </div>
        </div>

        {/* Wider than container-page on purpose: the staircase spanning further
            than the text above it is what makes the section read as full-bleed
            rather than as one more contained block. */}
        <div className="mx-auto w-full max-w-[1600px] px-6 pt-12 pb-16 sm:px-10 md:pb-20">
          <RankLadder />
          <p className="mt-8 text-center font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
            Rating floor of each band
          </p>
        </div>
      </section>

      <Section className="pb-16">
        <SectionHeader
          eyebrow="How it works"
          title="Three steps to being on the board."
        />
        <ol className="mt-10 grid gap-4 sm:grid-cols-3">
          {howItWorks.map((step) => (
            <li
              key={step.n}
              className="border-t border-border pt-5 transition-all hover:-translate-y-0.5 hover:border-primary"
            >
              <span className="font-mono text-xs tracking-[0.1em] text-primary">
                Step {step.n}
              </span>
              <p className="mt-3 text-[17px] font-semibold tracking-tight">{step.title}</p>
              <p className="mt-2 text-[15px] leading-[1.45] text-fg-muted text-pretty">
                {step.body}
              </p>
              {/*
                Only step 02 names platforms, so only step 02 shows their marks.
                They sit inline under the copy rather than in a section of their
                own: four logos in a row with no surrounding purpose reads as a
                sponsor strip, whereas here they simply show which handles the
                sentence above is talking about.

                aria-hidden, because the step body already lists all four by
                name — the marks repeat that visually for someone scanning.
              */}
              {step.platforms ? (
                <span className="mt-3.5 flex items-center gap-3 text-fg-subtle" aria-hidden>
                  {step.platforms.map((id) => (
                    <PlatformMark key={id} platform={id} className="size-4" />
                  ))}
                </span>
              ) : null}
            </li>
          ))}
        </ol>
      </Section>

      <Section className="pb-16">
        <SectionHeader
          eyebrow="Hall of fame"
          title="The people who set the bar."
          action={{ href: "/hall-of-fame", label: "All years" }}
        />
        {/* Cards grow to share the row rather than sitting at a fixed 280px.
            The club has one confirmed record, and a single narrow card marooned
            at the left edge of a wide scroller reads as content that failed to
            load. They still scroll once there are enough to overflow. */}
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

      <section
        className="border-t border-hairline"
        style={{ background: "var(--band)" }}
      >
        <div className="container-page py-20">
          {/* A wordmark, not a heading: the component carries the text for a
              screen reader and the card's own h2 below is untouched. */}
          <ClubWordmark text={site.name} />

          <BorderGlow
            className="mx-auto max-w-[680px]"
            contentClassName="px-8 py-14 text-center"
          >
            <h2 className="text-[clamp(1.75rem,4vw,2.5rem)] font-semibold tracking-[-0.02em]">
              Ready to compete?
            </h2>
            <p className="mx-auto mt-4 max-w-[46ch] text-base leading-6 text-fg-muted">
              Bring your handle. We&apos;ll bring the problems, the rounds, and people to
              solve them with.
            </p>
            <Button asChild className="mt-8 h-10 rounded-full px-6">
              <Link href="/register">Join the Club</Link>
            </Button>
          </BorderGlow>
        </div>
      </section>
    </>
  );
}

function cnFeature(span?: boolean) {
  return cn(
    "flex flex-col rounded-panel border border-hairline bg-surface p-7 transition-all",
    "hover:-translate-y-0.5 hover:border-border hover:bg-surface-3 hover:shadow-panel",
    span && "sm:col-span-2"
  );
}
