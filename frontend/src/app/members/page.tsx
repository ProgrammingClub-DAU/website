import type { Metadata } from "next";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, SampleBadge, Section } from "@/components/site/primitives";
import { MembersDirectory } from "@/components/site/members-directory";
import { dashboardService } from "@/lib/services/dashboard";

export const metadata: Metadata = {
  title: "Members",
  description:
    "Who is in the Programming Club @ DAU and what they look after, plus everyone who built this site.",
};

export default async function MembersPage() {
  // Fetch members data from API service
  const apiMembers = await dashboardService.getMembers();

  return (
    <>
      <Section className="pt-16 pb-8 md:pt-20">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Eyebrow>Community Directory</Eyebrow>
            <h1 className="mt-4 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
              OUR COMMUNITY.
            </h1>
          </div>
          <SampleBadge />
        </div>
        <p className="mt-4 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          The people building the programming culture at DAU.
        </p>
      </Section>

      <Section className="pb-12">
        <MembersDirectory members={apiMembers} />
      </Section>

      <Section className="pb-20">
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
