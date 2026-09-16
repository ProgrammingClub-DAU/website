// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /members.

// Never statically pre-render — this page fetches live member data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";

import { Eyebrow, PageTitle, Section, SectionHeader } from "@/components/site/primitives";
import { MembersDirectory } from "@/components/site/members-directory";
import { MembersPageCta } from "@/components/site/members-page-cta";
import { credits } from "@/lib/content/members";
import { dashboardService } from "@/lib/services/dashboard";

export const metadata: Metadata = {
  title: "Members",
  description:
    "Who is in the Programming Club @ DAU and what they look after, plus everyone who built this site.",
};

export default async function MembersPage() {
  // Two lists: the committee in club hierarchy order, and the searchable
  // membership.
  //
  // allSettled, not all. With Promise.all a failure in either call threw away
  // both results, so one slow endpoint emptied the entire page -- and the catch
  // then rendered "No members yet", which is a claim about the club rather than
  // an admission that the fetch failed. Each list now stands or falls alone.
  const [teamResult, directoryResult] = await Promise.allSettled([
    dashboardService.getTeam(),
    dashboardService.getDirectory(),
  ]);

  const team = teamResult.status === "fulfilled" ? teamResult.value : [];
  const directory =
    directoryResult.status === "fulfilled" ? directoryResult.value : { members: [], total: 0 };

  // Logged, not swallowed. This runs on the server, so it lands in the hosting
  // logs -- the only place anyone can see why a public page came up empty.
  if (teamResult.status === "rejected") {
    console.error("Members page: could not load the committee:", teamResult.reason);
  }
  if (directoryResult.status === "rejected") {
    console.error("Members page: could not load the directory:", directoryResult.reason);
  }

  const unreachable =
    teamResult.status === "rejected" && directoryResult.status === "rejected";

  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Eyebrow>Community Directory</Eyebrow>
            <PageTitle>
              OUR COMMUNITY.
            </PageTitle>
          </div>
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The people building the programming culture at DAU.
        </p>
      </Section>

      <Section className="pb-10">
        <MembersDirectory
          team={team}
          members={directory.members}
          total={directory.total}
          unreachable={unreachable}
        />
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
                  <p className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                    {credit.years}
                  </p>
                  <p className="mt-2 text-body font-semibold tracking-tight">
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
          <p className="max-w-[52ch] text-body leading-[1.5] text-fg-muted text-pretty">
            Your profile appears here once you sign in and add a photo and a line about
            yourself.
          </p>
          <MembersPageCta />
        </div>
      </Section>
    </>
  );
}
