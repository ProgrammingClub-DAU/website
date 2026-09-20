// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /profile.

import type { Metadata } from "next";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import ProfileDashboard from "@/components/site/profile-dashboard";
import { dashboardService } from "@/lib/services/dashboard";

/** Best-effort name fetch — never throws, so a cold backend never 404s the page. */
async function fetchName(userId: string): Promise<string | null> {
  try {
    const profile = await dashboardService.getUserProfileById(userId);
    return profile.name?.trim() || null;
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const name = await fetchName(id);
  return {
    title: name ?? "Member Profile",
    description: name
      ? `${name}'s Codeforces and LeetCode ratings and profile at Programming Club @ DAU.`
      : "A Programming Club @ DAU member's Codeforces and LeetCode ratings and profile links.",
  };
}

export default async function ProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  // Fetch name server-side so the heading is populated before client JS runs.
  const name = await fetchName(id);

  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Profile</Eyebrow>
        <PageTitle>{name ?? "Member Profile"}</PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          A competitive programming journey at a glance.
        </p>
      </Section>

      <Section className="pb-22">
        <ProfileDashboard userId={id} />
      </Section>
    </>
  );
}
