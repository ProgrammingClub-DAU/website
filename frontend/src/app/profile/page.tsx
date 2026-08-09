import type { Metadata } from "next";
import { Eyebrow, Section } from "@/components/site/primitives";
import { apiClient } from "@/lib/api-client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Profile",
  description: "Your CP Club profile.",
};

export default async function ProfilePage() {
  // TODO: Replace with actual auth session ID once Role 2 finishes auth implementation
  const mockUserId = "logged-in-user-id";
  const profile = await apiClient.getProfile(mockUserId);

  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <div>
          <Eyebrow>Profile</Eyebrow>
          <h1 className="mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance">
            {profile.name}
          </h1>
        </div>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Manage your account and view your standing.
        </p>
      </Section>

      <Section className="pb-10">
        <Card>
          <CardHeader>
            <CardTitle>Account Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <div className="text-sm font-medium text-fg-muted">Email</div>
              <div>{profile.email}</div>
            </div>
            <div>
              <div className="text-sm font-medium text-fg-muted">Codeforces Handle</div>
              <div>{profile.handle}</div>
            </div>
            <div>
              <div className="text-sm font-medium text-fg-muted">Current Rank</div>
              <div className="capitalize">{profile.cfRank}</div>
            </div>
            <div>
              <div className="text-sm font-medium text-fg-muted">Joined</div>
              <div>{new Date(profile.joinedAt).toLocaleDateString()}</div>
            </div>
          </CardContent>
        </Card>
      </Section>
    </>
  );
}
