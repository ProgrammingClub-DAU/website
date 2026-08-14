/**
 * Route-level error boundary.
 *
 * Without this, a throw anywhere in a page — a null field in a filter, a failed
 * fetch — replaces the entire document with Next's bare 500 page: no navbar, no
 * footer, no way back. This keeps the shell alive and offers a retry.
 */

"use client";

import { useEffect } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Eyebrow, Section } from "@/components/site/primitives";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Surfaced in the browser console and the server log, so a report from a
    // member ("the leaderboard broke") is traceable to a stack.
    console.error("Route error:", error);
  }, [error]);

  return (
    <Section className="flex min-h-[60vh] flex-col justify-center py-24">
      <Eyebrow>Something went wrong</Eyebrow>
      <h1 className="mt-6 max-w-[20ch] text-[clamp(2rem,5vw,3rem)] leading-[1.05] font-[510] tracking-[-0.02em] text-balance">
        This page didn&apos;t load.
      </h1>
      <p className="mt-5 max-w-[46ch] text-base leading-6 text-fg-muted text-pretty">
        The problem is on our side, not yours. Trying again often works — the
        backend may still be waking up.
      </p>

      {error.digest && (
        <p className="mt-4 font-mono text-xs text-fg-subtle">
          Reference: {error.digest}
        </p>
      )}

      <div className="mt-8 flex flex-wrap gap-3">
        <Button onClick={reset} className="h-10 rounded-full px-5.5">
          Try again
        </Button>
        <Button asChild variant="outline" className="h-10 rounded-full px-5.5">
          <Link href="/">Back to home</Link>
        </Button>
      </div>
    </Section>
  );
}
