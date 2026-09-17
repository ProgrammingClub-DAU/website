import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { HallOfFameTimeline } from "@/components/site/hall-of-fame-timeline";
import { hallOfFameService } from "@/lib/services/hall-of-fame";
import type { HallOfFameEntry } from "@/types/api";

export const metadata: Metadata = {
  title: "Hall of Fame",
  description:
    "ICPC standings, contest wins, and milestones the Programming Club @ DAU keeps a record of.",
};

// An entry an admin adds this morning has to be here without a redeploy.
export const dynamic = "force-dynamic";

export default async function HallOfFamePage() {
  let entries: HallOfFameEntry[] = [];
  let unreachable = false;

  try {
    entries = await hallOfFameService.list({ serverRender: true });
  } catch (error) {
    // Logged server-side, and said on the page: an unreachable backend must not
    // read as a club with no achievements.
    console.error("Hall of Fame page: could not load entries:", error);
    unreachable = true;
  }

  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Hall of fame</Eyebrow>
        <PageTitle className="max-w-[20ch]">Results, year by year.</PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          ICPC standings, contest wins and milestones the club keeps a record of, newest first.
        </p>
      </Section>

      <Section className="pb-6">
        {unreachable ? (
          <div className="rounded-panel border border-dashed border-destructive/40 bg-destructive/5 py-12 text-center">
            <p className="text-sm text-destructive">Could not load the Hall of Fame.</p>
            <p className="mt-1 text-xs text-fg-muted">
              The server may be waking up. Reload in a few seconds.
            </p>
          </div>
        ) : (
          <HallOfFameTimeline entries={entries} />
        )}
      </Section>

      <Section className="pt-10 pb-22">
        <div className="flex flex-wrap items-center justify-between gap-5 border-t border-hairline pt-8">
          <p className="max-w-[52ch] text-body leading-[1.5] text-fg-muted text-pretty">
            Have a result that belongs here? Send the contest, date and standing to the core
            team, with a link to the official ranklist.
          </p>
          {/* To the people who can add it -- previously this pointed at sign-in,
              which is not where anyone submits anything. */}
          <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
            <Link href="/members">Contact the core team</Link>
          </Button>
        </div>
      </Section>
    </>
  );
}
