"use client";

/**
 * Hosts the React Bits Particles behind the auth panels.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * colours, tuning, loading strategy — lives here.
 *
 * This is the spec's original choice for the slot, and its reasoning holds: the
 * page ends on a row of rank-coloured dots, so particles in the same colours
 * finish an idea it already starts. It covers the whole viewport rather than a
 * single panel, so the count and the spread are both higher than they were.
 *
 * It holds a WebGL context, so the spec's rule applies: at most one per page,
 * and never two. Particles has the auth pages; Aurora has the home page.
 */

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

/**
 * Client-only. It opens a WebGL context, measures its container and compiles
 * shaders on mount, none of which the server can do — and on an auth page the
 * form is the part worth showing first.
 */
const Particles = dynamic(() => import("@/components/site/particles"), {
  ssr: false,
});

/**
 * The rank ladder, which is what the panel's "RANK TRACK" row below already
 * shows. Read off the document rather than passed as `var(--token)`: the
 * component parses them as hex directly, with no CSS cascade to resolve a
 * custom property against.
 */
const COLOR_TOKENS = [
  "--cf-pupil",
  "--cf-specialist",
  "--cf-expert",
  "--cf-candidate",
] as const;

function readPalette(): string[] {
  const styles = getComputedStyle(document.documentElement);
  return COLOR_TOKENS.map((token) => styles.getPropertyValue(token).trim());
}

export function ParticlesBackdrop({ className }: { className: string }) {
  const [palette, setPalette] = useState<string[] | null>(null);

  useEffect(() => {
    const sync = () => setPalette(readPalette());
    sync();

    // Resolved colours cannot follow the theme the way a var() would, so
    // re-read them when the theme provider swaps the class on <html>.
    const observer = new MutationObserver(sync);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => observer.disconnect();
  }, []);

  if (!palette) return null;

  return (
    <div aria-hidden className={className}>
      {/* Keyed on the palette so the buffers are rebuilt on a theme change: the
          colours are baked into a vertex attribute at construction, and the
          effect's dependency list does not include them. */}
      <Particles
        key={palette.join()}
        particleColors={palette}
        particleCount={260}
        particleSpread={16}
        speed={0.08}
        particleBaseSize={95}
        sizeRandomness={0.8}
        alphaParticles
        moveParticlesOnHover
        particleHoverFactor={0.6}
      />
    </div>
  );
}
