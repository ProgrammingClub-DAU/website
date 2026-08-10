import Link from "next/link";
import {
  BarChart3,
  CalendarDays,
  FileText,
  Swords,
  Users,
  type LucideIcon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  RankDot,
  RankLegend,
  SampleBadge,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import { CF_RANKS } from "@/lib/cf-ranks";
import { heroRows, howItWorks, sparkline, stats } from "@/lib/content/home";
import { hallOfFameTeaser } from "@/lib/content/hall-of-fame";
import { cn } from "@/lib/utils";

function HeroPanel() {
  return (
    <div className="relative flex flex-col gap-4">
      <div
        className="pointer-events-none absolute -inset-x-5 -inset-y-10 bg-[radial-gradient(60%_55%_at_60%_40%,var(--primary-soft),transparent_70%)]"
        aria-hidden
      />

      <div className="glass-panel relative overflow-hidden rounded-panel">
        <div className="flex items-center justify-between gap-4 border-b border-hairline px-4 py-3.5">
          <span className="font-mono text-[13px] tracking-[0.1em] text-fg-muted uppercase">
            Club rating
          </span>
          <SampleBadge />
        </div>

        <ul>
          {heroRows.map((row) => (
            <li
              key={row.handle}
              className="flex items-center gap-3 border-b border-hairline px-4 py-3.5 transition-colors hover:bg-surface-2"
            >
              <span className="w-[18px] font-mono text-xs text-fg-subtle">{row.rank}</span>
              <RankDot rank={row.cf} />
              <span className="min-w-0 flex-1 truncate font-mono text-[13px]">{row.handle}</span>
              <span className="w-11 text-right font-mono text-xs text-fg-muted">{row.delta}</span>
              <span className="w-12 text-right font-mono text-sm">{row.rating}</span>
            </li>
          ))}
        </ul>

        <div className="flex items-center justify-between gap-3 px-4 py-3">
          <span className="font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
            Synced from Codeforces
          </span>
          <span className="flex h-[18px] items-end gap-[3px]" aria-hidden>
            {sparkline.map((h, i) => (
              <span
                key={i}
                className="w-1 rounded-sm bg-hairline-strong"
                style={{ height: h }}
              />
            ))}
          </span>
        </div>
      </div>

      <div className="relative grid gap-4 sm:grid-cols-2">
        <div className="glass-panel rounded-panel p-4.5 transition-transform hover:-translate-y-0.5">
          <div className="flex items-center justify-between gap-2">
            <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
              Next round
            </span>
            <span
              className="size-1.5 animate-pulse-ring rounded-full bg-cf-pupil"
              aria-hidden
            />
          </div>
          <p className="mt-3.5 text-[15px] font-semibold tracking-tight">
            [PLACEHOLDER] Weekly Round
          </p>
          <p className="mt-1.5 font-mono text-xs text-fg-muted">
            [PLACEHOLDER] Day · 21:00 IST
          </p>
        </div>

        <div className="glass-panel rounded-panel p-4.5 transition-transform hover:-translate-y-0.5">
          <div className="flex items-center justify-between gap-2">
            <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
              Problem of the day
            </span>
            <span className="font-mono text-[11px] text-cf-expert">1600</span>
          </div>
          <p className="mt-3.5 text-[15px] font-semibold tracking-tight">
            [PLACEHOLDER] Problem title
          </p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {["dp", "greedy"].map((tag) => (
              <span
                key={tag}
                className="rounded-full border border-hairline px-2 py-0.5 font-mono text-[11px] text-fg-muted"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      </div>

      <RankLegend className="relative mt-1" />
    </div>
  );
}

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
      <Section className="grid gap-14 py-16 md:grid-cols-2 md:items-center md:py-24">
        <div>
          <Eyebrow className="animate-rise">Competitive programming club</Eyebrow>
          <h1 className="mt-6 text-[clamp(2.375rem,6.4vw,4rem)] leading-none font-[510] tracking-[-0.02em] text-balance">
            A home for problem solvers at DAU.
          </h1>
          <p className="mt-6 max-w-[44ch] text-base leading-6 text-fg-muted text-pretty">
            Weekly contests, editorials, and a leaderboard synced from Codeforces — for
            everyone from first-time solvers to ICPC regionalists.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button asChild className="h-10 rounded-full px-5.5">
              <Link href="/register">Join the Club</Link>
            </Button>
            <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
              <Link href="/events">See our events</Link>
            </Button>
          </div>
        </div>

        <HeroPanel />
      </Section>

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
        <div className="mx-auto max-w-[1240px] px-6 py-20">
          <div className="glass-panel mx-auto max-w-[680px] rounded-panel px-8 py-14 text-center">
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
          </div>
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
