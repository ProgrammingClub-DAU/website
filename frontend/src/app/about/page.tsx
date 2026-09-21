import type { Metadata } from "next";
import Link from "next/link";

import BorderGlow from "@/components/site/border-glow";
import { Button } from "@/components/ui/button";
import {
  Eyebrow,
  PageTitle,
  Section,
  SectionHeader,
} from "@/components/site/primitives";
import { PlatformMark, PLATFORM_ACCENT } from "@/components/site/platform-mark";
import { cpIntro, faq, joinSteps, practicePlatforms, whatWeDo } from "@/lib/content/about";
import { HallOfFameTeaser } from "@/components/site/hall-of-fame-teaser";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "About",
  description: `What the ${site.fullName} runs, who it is for, and how to start competitive programming.`,
};

// Regenerated in the background at most every five minutes, so a new Hall
// of Fame entry appears without a redeploy and without every visit waiting
// on the backend.
export const revalidate = 300;

/**
 * HackerRank's place in the platform list.
 *
 * Our own lettermark, as for AtCoder in platform-mark.tsx: a hexagon with the H
 * cut out. It lives here rather than in PlatformMark because HackerRank is a
 * place to practise, not a handle members link, and adding it to PlatformId
 * would put it on every profile form.
 */
function HackerRankMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={className}
      fill="currentColor"
      fillRule="evenodd"
      aria-hidden
      focusable="false"
    >
      <path d="M12 1.5 L21.1 6.75 L21.1 17.25 L12 22.5 L2.9 17.25 L2.9 6.75 Z M8.5 7 L10.5 7 L10.5 11 L13.5 11 L13.5 7 L15.5 7 L15.5 17 L13.5 17 L13.5 13 L10.5 13 L10.5 17 L8.5 17 Z" />
    </svg>
  );
}

/** HackerRank borrows the pupil green; the other four have their own accents. */
const HACKERRANK_ACCENT = "var(--cf-pupil)";

export default function AboutPage() {
  return (
    <>
      <Section className="pt-10 pb-14 md:pt-14">
        <Eyebrow>About</Eyebrow>
        <PageTitle className="max-w-[22ch]">
          A student club built around practice, not prestige.
        </PageTitle>
        <p className="mt-6 max-w-[58ch] text-base leading-relaxed text-fg-muted text-pretty">
          {site.fullName} is run by students at {site.university}. It is for anyone at DAU who
          wants to get better at solving problems with code: first-years who have never opened
          an online judge, students preparing for placement rounds, and seasoned contestants
          alike.
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

      <Section className="pb-16">
        <SectionHeader eyebrow="Through the year" title="What we do." />
        <ul className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {whatWeDo.map((item, i) => (
            <li
              key={item.title}
              className="flex items-baseline gap-3.5 rounded-panel border border-hairline bg-surface px-5 py-4.5 transition-all hover:-translate-y-0.5 hover:border-border"
            >
              <span className="font-mono text-xs text-primary">
                {String(i + 1).padStart(2, "0")}
              </span>
              <span className="text-body leading-[1.45] font-medium text-pretty">
                {item.title}
                {item.short ? <span className="text-fg-muted"> ({item.short})</span> : null}
              </span>
            </li>
          ))}
        </ul>
        <p className="mt-6 text-sm text-fg-muted">
          Dates, results and photos for each are on the{" "}
          <Link href="/events" className="text-primary underline-offset-4 hover:underline">
            events page
          </Link>
          .
        </p>
      </Section>

      <Section className="pb-16">
        <SectionHeader eyebrow="Getting started" title="New to competitive programming?" />
        <div className="mt-8 grid gap-4 md:grid-cols-3">
          {[
            { label: "What it is", items: cpIntro.what },
            { label: "Why do it", items: cpIntro.why },
            { label: "What you get out of it", items: cpIntro.benefits },
          ].map((column) => (
            <div key={column.label} className="rounded-panel border border-hairline bg-surface p-7">
              <div className="font-mono text-xs tracking-[0.12em] text-fg-subtle uppercase">
                {column.label}
              </div>
              <ul className="mt-4 flex flex-col gap-2.5">
                {column.items.map((item) => (
                  <li key={item} className="flex gap-2.5 text-body leading-[1.5] text-fg-muted">
                    <span aria-hidden className="mt-[0.6em] size-1 shrink-0 rounded-full bg-primary" />
                    <span className="text-pretty">{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <h3 className="mt-12 text-lg font-semibold tracking-tight">Where to practise</h3>
        <p className="mt-2 max-w-[58ch] text-base leading-6 text-fg-muted text-pretty">
          All five are free. Codeforces and LeetCode ratings also feed this site&apos;s
          leaderboard once you add your handle.
        </p>
        <ul className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {practicePlatforms.map((platform) => {
            const accent =
              platform.id === "hackerrank" ? HACKERRANK_ACCENT : PLATFORM_ACCENT[platform.id];
            return (
              <li key={platform.id}>
                <a
                  href={platform.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ "--accent": accent } as React.CSSProperties}
                  className="group flex h-full items-center gap-3.5 rounded-panel border border-hairline bg-surface p-4 transition-all hover:-translate-y-0.5 hover:border-[var(--accent)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring lg:flex-col lg:items-start"
                >
                  <span
                    className="grid size-11 shrink-0 place-items-center rounded-xl"
                    style={{ background: "color-mix(in srgb, var(--accent) 14%, transparent)" }}
                  >
                    {platform.id === "hackerrank" ? (
                      <HackerRankMark className="size-[22px] text-[var(--accent)]" />
                    ) : (
                      <PlatformMark platform={platform.id} className="size-[22px] text-[var(--accent)]" />
                    )}
                  </span>
                  <span className="min-w-0">
                    <span className="block font-semibold tracking-tight">
                      {platform.label}
                      <span className="sr-only"> (opens in a new tab)</span>
                    </span>
                    <span className="mt-0.5 block font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                      {platform.syncs ? "Rating synced" : "Practice"}
                    </span>
                  </span>
                </a>
              </li>
            );
          })}
        </ul>
      </Section>

      <Section className="pb-16">
        <h2 className="font-heading text-[clamp(1.5rem,3.2vw,2rem)] font-medium tracking-[-0.02em]">
          Common questions
        </h2>
        <dl className="mt-8 border-t border-hairline">
          {faq.map((item) => (
            <div
              key={item.q}
              className="grid gap-2 border-b border-hairline py-7 md:grid-cols-2 md:gap-8"
            >
              <dt className="text-base font-semibold tracking-tight text-pretty">
                {item.q}
              </dt>
              <dd className="text-body leading-[1.6] text-fg-muted text-pretty">
                {item.a}
              </dd>
            </div>
          ))}
        </dl>
      </Section>

      <HallOfFameTeaser title="Past results and alumni." />

      <Section className="pb-22">
        <BorderGlow contentClassName="grid gap-8 p-6 sm:p-10 md:grid-cols-2 md:items-center">
          <div>
            <h2 className="font-heading text-[clamp(1.5rem,3.2vw,2rem)] font-medium tracking-[-0.02em]">
              How to join
            </h2>
            <p className="mt-4 max-w-[44ch] text-base leading-6 text-fg-muted text-pretty">
              Open to all DAU students, any batch, any experience level.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Button asChild className="h-10 rounded-full px-5.5">
                <Link href="/login">Sign in</Link>
              </Button>
              <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
                <Link href="/members">Meet the committee</Link>
              </Button>
            </div>
          </div>

          <ol className="flex flex-col gap-px overflow-hidden rounded-control bg-hairline">
            {joinSteps.map((step) => (
              <li key={step.n} className="flex items-baseline gap-3.5 bg-surface-2 px-5 py-4.5">
                <span className="font-mono text-xs text-primary">{step.n}</span>
                <span className="text-body leading-[1.45]">{step.text}</span>
              </li>
            ))}
          </ol>
        </BorderGlow>
      </Section>
    </>
  );
}
