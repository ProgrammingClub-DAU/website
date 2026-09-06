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
import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  RankDot,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import { CF_RANKS } from "@/lib/cf-ranks";
import { howItWorks, stats } from "@/lib/content/home";
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

        {/* Centred since the hero lost its right-hand panel. Left-aligned text
            with nothing beside it left the whole right half of a wide screen
            empty and read as a layout fault rather than a choice. */}
        <Section className="py-20 text-center md:py-28">
          <div className="mx-auto max-w-[54ch]">
            <Eyebrow className="animate-rise">Competitive programming club</Eyebrow>
            <h1 className="mt-6 text-[clamp(2.375rem,6.4vw,4rem)] leading-none font-[510] tracking-[-0.02em] text-balance">
              A home for problem solvers at DAU.
            </h1>
            <p className="mx-auto mt-6 max-w-[46ch] text-base leading-6 text-fg-muted text-pretty">
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

      {/* Every figure here is a placeholder — confirm against club records before launch. */}
      <Section>
        <dl className="grid grid-cols-2 border-y border-hairline lg:grid-cols-4">
          {stats.map((stat) => (
            <div key={stat.label} className="py-8 pr-6">
              {/* Term before definition: a screen reader pairs them in source order. */}
              <dt className="font-mono text-xs tracking-[0.1em] text-fg-muted uppercase">
                {stat.label}
              </dt>
              <dd className="mt-2.5 text-[2rem] font-semibold tracking-[-0.02em]">
                {stat.value}
              </dd>
            </div>
          ))}
        </dl>
      </Section>

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
