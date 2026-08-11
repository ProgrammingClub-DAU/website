"use client";

import { useEffect, useState } from "react";
import { Eyebrow, Section } from "@/components/site/primitives";
import { dashboardService } from "@/lib/services/dashboard";
import { useAuthStore } from "@/store/auth";
import type { Profile } from "@/types/api";

export default function ProfilePage() {
  const { isAuthenticated, user } = useAuthStore();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadProfile() {
      if (!isAuthenticated || !user) {
        setLoading(false);
        return;
      }

      setLoading(true);
      setProfile(await dashboardService.getProfile());
      setLoading(false);
    }

    void loadProfile();
  }, [isAuthenticated, user]);

  if (loading) {
    return (
      <Section className="pt-16 pb-10 text-center md:pt-24">
        <p className="font-mono tracking-wider text-fg-muted uppercase">Loading profile...</p>
      </Section>
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <Section className="pt-16 pb-10 text-center md:pt-24">
        <p className="font-mono tracking-wider text-fg-muted uppercase">Please log in to view your profile.</p>
      </Section>
    );
  }

  if (!profile) {
    return (
      <Section className="pt-16 pb-10 text-center md:pt-24">
        <p className="font-mono tracking-wider text-fg-muted uppercase">Unable to load profile.</p>
      </Section>
    );
  }

  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <div>
          <Eyebrow>Profile</Eyebrow>
          <h1 className="mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
            {profile.name}
          </h1>
        </div>
        <div className="mt-6 space-y-3 text-base leading-6 text-fg-muted">
          <p>Email: <span className="font-medium text-foreground">{profile.email}</span></p>
          <p>Codeforces Handle: <span className="font-medium text-foreground">{profile.codeforcesHandle || "N/A"}</span></p>
          <p>Rating: <span className="font-medium text-foreground">{profile.rating}</span></p>
        </div>
      </Section>

      <Section className="pb-22">
        <div className="rounded-xl border border-dashed border-border bg-surface-2 p-8 text-center text-fg-muted">
          <p className="mb-2 font-medium text-foreground">Phase 2 Features</p>
          <p className="text-sm">Activity calendar, platform stats, contest history, and event history will be available here in Phase 2.</p>
        </div>
      </Section>
    </>
  );
}
