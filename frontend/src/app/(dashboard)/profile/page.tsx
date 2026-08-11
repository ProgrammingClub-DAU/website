// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /profile.

import type { Metadata } from "next";
import { Eyebrow, Section } from "@/components/site/primitives";
import ProfileDashboard from "@/components/site/profile-dashboard";

export const metadata: Metadata = {
  title: "Profile",
  description: "Your CP Club profile dashboard — rating history, practice streaks, and stats.",
};

/**
 * Profile Page (Server Component)
 *
 * Fetches the user's profile data on the server, then hands it off
 * to the ProfileDashboard client component for interactive rendering
 * (recharts graphs, activity calendar, etc.).
 *
 * TODO: Replace mockUserId with the actual auth session ID
 * once Role 2 finishes the authentication implementation.
 */
export default function ProfilePage() {
  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <div>
          <Eyebrow>Profile</Eyebrow>
          <h1 className="mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
            Dashboard
          </h1>
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Your competitive programming journey at a glance.
        </p>
      </Section>

      <Section className="pb-22">
        <ProfileDashboard />
      </Section>
    </>
  );
}
