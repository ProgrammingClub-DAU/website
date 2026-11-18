"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth";

/**
 * "Join the Club" for visitors; "Go to your profile" for members already signed in.
 *
 * The home page is static, so it cannot know who is looking. This used to send
 * signed-in members to the sign-in page they had already been through.
 *
 * Renders the signed-out version on the server and for the first client render,
 * then switches once mounted, so the HTML the server sent and the first render
 * agree (no hydration mismatch).
 */
export function JoinCta({ className }: { className?: string }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const signedIn = mounted && isAuthenticated;
  return (
    <Button asChild className={className}>
      <Link href={signedIn ? "/profile" : "/login"}>{signedIn ? "Go to your profile" : "Join the Club"}</Link>
    </Button>
  );
}
