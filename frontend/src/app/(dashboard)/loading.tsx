/**
 * Loading UI for the data-driven dashboard routes.
 *
 * These pages await a backend call before emitting any HTML, and the backend
 * sleeps on its free tier — measured at over 8 seconds of completely blank
 * screen on a cold start, with client-side navigation showing no feedback at
 * all. This streams the page shell immediately so the wait is visibly a wait.
 */

import { Section } from "@/components/site/primitives";

function Bar({ className = "" }: { className?: string }) {
  return (
    <div
      className={`animate-pulse rounded-control bg-surface-2 ${className}`}
      aria-hidden
    />
  );
}

export default function Loading() {
  return (
    <Section className="py-16 md:py-24">
      {/* Announced once rather than per-skeleton, so screen readers get a single
          "loading" rather than a stream of meaningless nodes. */}
      <p role="status" aria-live="polite" className="sr-only">
        Loading content…
      </p>

      <Bar className="h-4 w-40" />
      <Bar className="mt-6 h-12 w-full max-w-md" />
      <Bar className="mt-5 h-5 w-full max-w-lg" />

      <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Bar key={i} className="h-32" />
        ))}
      </div>
    </Section>
  );
}
