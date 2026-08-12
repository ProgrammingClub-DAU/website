// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /members.

// Never statically pre-render — this page fetches live member data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, SampleBadge, Section, SectionHeader } from "@/components/site/primitives";
import { MembersDirectory } from "@/components/site/members-directory";
import { credits } from "@/lib/content/members";
import { dashboardService } from "@/lib/services/dashboard";

export const metadata: Metadata = {
  title: "Members",
  description:
    "Who is in the Programming Club @ DAU and what they look after, plus everyone who built this site.",
};

export default async function MembersPage() {
  // Fetch live members from the backend. Falls back to [] if API is unavailable at build time.
  let apiMembers: Awaited<ReturnType<typeof dashboardService.getMembers>> = [];
  try {
    apiMembers = await dashboardService.getMembers();
  } catch {
    // API unreachable — render with empty list, page stays functional
  }

  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Eyebrow>Community Directory</Eyebrow>
            <h1 className="mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
              OUR COMMUNITY.
            </h1>
          </div>
          <SampleBadge />
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The people building the programming culture at DAU.
        </p>
      </Section>

      <Section className="pb-10">
        <MembersDirectory members={apiMembers} />
      </Section>

      <Section className="pb-10">
        <div className="border-t border-border pt-10">
          <SectionHeader
            eyebrow="Website developer credits"
            title="Everyone who built this site."
          />
          <p className="mt-5 max-w-[56ch] text-base leading-6 text-fg-muted text-pretty">
            This list is not tied to the current committee. Contributors stay here after
            their term ends, and new names are appended as work continues.
          </p>

          <ul className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {credits.map((credit) => (
              <li
                key={credit.name}
                className="glass-panel flex items-center gap-4 rounded-panel p-5.5 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
              >
                <span className="flex size-14 shrink-0 items-center justify-center rounded-full border border-border bg-surface-2 font-mono text-sm text-fg-muted">
                  {credit.initials}
                </span>
                <div className="min-w-0">
                  <p className="font-mono text-[10px] tracking-[0.12em] text-fg-subtle uppercase">
                    {credit.years}
                  </p>
                  <p className="mt-2 text-[15px] font-semibold tracking-tight">
                    {credit.name}
                  </p>
                  <p className="mt-1.5 text-sm leading-5 text-fg-muted text-pretty">
                    {credit.work}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </Section>

      <Section className="pb-22">
        <div className="flex flex-wrap items-center justify-between gap-5 border-t border-hairline pt-8">
          <p className="max-w-[52ch] text-[15px] leading-[1.5] text-fg-muted text-pretty">
            Your profile appears here once you sign in and add a photo and a line about
            yourself.
          </p>
          <Button asChild className="h-10 rounded-full px-5.5">
            <Link href="/login">Claim your profile</Link>
          </Button>
        </div>
      </Section>
    </>
  );
}
