"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth";

/**
 * Smart CTA on the Members page.
 * Logged-in users go to their own profile.
 * Logged-out users go to /login.
 */
export function MembersPageCta() {
  const { isAuthenticated, user } = useAuthStore();

  const href = isAuthenticated && user?.id ? `/profile/${user.id}` : "/login";
  const label = isAuthenticated ? "Go to my profile" : "Claim your profile";

  return (
    <Button asChild className="h-10 rounded-full px-5.5">
      <Link href={href}>{label}</Link>
    </Button>
  );
}
