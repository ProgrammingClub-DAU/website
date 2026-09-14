import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { HallOfFameTimeline } from "@/components/site/hall-of-fame-timeline";
import { hallOfFame } from "@/lib/content/hall-of-fame";

export const metadata: Metadata = {
  title: "Hall of Fame",
  description:
    "ICPC standings, contest wins, and alumni the Programming Club @ DAU keeps a record of.",
};

export default function HallOfFamePage() {
  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Hall of fame</Eyebrow>
        <PageTitle className="max-w-[20ch]">
          Results, year by year.
        </PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          ICPC standings, contest wins, and alumni the club keeps a record of. Records
          from earlier sessions are still being gathered.
        </p>
      </Section>

      <Section className="pb-6">
        <HallOfFameTimeline years={hallOfFame} />
      </Section>

      <Section className="pt-10 pb-22">
        <div className="flex flex-wrap items-center justify-between gap-5 border-t border-hairline pt-8">
          <p className="max-w-[52ch] text-[15px] leading-[1.5] text-fg-muted text-pretty">
            Have a result that belongs here? Send the contest, year, and standing to the
            core team and it gets added with a source.
          </p>
          <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
            <Link href="/login">Submit a result</Link>
          </Button>
        </div>
      </Section>
    </>
  );
}
