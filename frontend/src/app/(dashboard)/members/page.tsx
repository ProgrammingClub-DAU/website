// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /members.

// Never statically pre-render — this page fetches live member data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";

import { Eyebrow, PageTitle, Section, SectionHeader } from "@/components/site/primitives";
import { MembersDirectory } from "@/components/site/members-directory";
import { credits } from "@/lib/content/members";
import { dashboardService } from "@/lib/services/dashboard";

export const metadata: Metadata = {
  title: "Members",
  description:
    "The committee that runs the Programming Club @ DAU this term, and everyone who built this site.",
};

/** "Madhav Thesiya" -> "MT". */
function initials(name: string): string {
  return name
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default async function MembersPage() {
  // Only the committee. The page lists who runs the club, and the full
  // membership is not that -- it was a second section of everyone who had ever
  // signed in, which told a visitor nothing and buried the people it exists to
  // show.
  let team: Awaited<ReturnType<typeof dashboardService.getTeam>> = [];
  let unreachable = false;

  try {
    team = await dashboardService.getTeam();
  } catch (error) {
    // Logged, not swallowed: this runs on the server, so it lands in the
    // hosting logs, which is the only place anyone can see why a public page
    // came up empty.
    console.error("Members page: could not load the committee:", error);
    unreachable = true;
  }
  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Eyebrow>The committee</Eyebrow>
            <PageTitle>Who runs the club.</PageTitle>
          </div>
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The core team and batch representatives for this term.
        </p>
      </Section>

      <Section className="pb-10">
        <MembersDirectory team={team} unreachable={unreachable} />
      </Section>

      <Section className="pb-22">
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
            {credits.map((name) => (
              <li
                key={name}
                className="glass-panel flex items-center gap-4 rounded-panel p-4.5 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
              >
                <span
                  aria-hidden
                  className="flex size-11 shrink-0 items-center justify-center rounded-full border border-border bg-surface-2 font-mono text-xs text-fg-muted"
                >
                  {initials(name)}
                </span>
                <p className="min-w-0 text-body font-semibold tracking-tight">{name}</p>
              </li>
            ))}
          </ul>
        </div>
      </Section>

    </>
  );
}
