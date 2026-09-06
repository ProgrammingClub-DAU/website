"use client";

/**
 * Hosts the React Bits ripple grid behind the auth pages.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * colour, tuning, loading strategy — lives here.
 *
 * Chosen over Particles, which the spec named, for two reasons. It reacts to
 * the pointer rather than only drifting: the grid ripples away from the cursor,
 * so the page responds to being touched. And a lattice is closer to what this
 * club is about than a starfield — the page already ends on a row of
 * rank-coloured dots, and a grid is the same idea at the scale of the viewport.
 *
 * It holds a WebGL context, so the spec's rule applies: at most one per page,
 * and never two. This has the auth pages; Aurora has the home page.
 */

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

/**
 * Client-only. It opens a WebGL context, measures its container and compiles
 * shaders on mount, none of which the server can do — and on an auth page the
 * form is the part worth showing first.
 */
const RippleGrid = dynamic(() => import("@/components/site/ripple-grid"), {
  ssr: false,
});

type Theme = { color: string; light: boolean };

function readTheme(): Theme {
  const root = document.documentElement;
  return {
    // One colour, not a gradient, so it is the middle of the club gradient —
    // the stop that sits closest to --primary. Read off the document rather
    // than passed as var(--token): the component parses it to a vec3, with no
    // CSS cascade to resolve a custom property against.
    color: getComputedStyle(root).getPropertyValue("--cf-expert").trim(),
    light: !root.classList.contains("dark"),
  };
}

export function AuthBackdrop({ className }: { className: string }) {
  const [theme, setTheme] = useState<Theme | null>(null);

  useEffect(() => {
    const sync = () => setTheme(readTheme());
    sync();

    // A resolved colour cannot follow the theme the way a var() would, so
    // re-read it when the theme provider swaps the class on <html>.
    const observer = new MutationObserver(sync);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => observer.disconnect();
  }, []);

  if (!theme) return null;

  return (
    // Not pointer-events-none, unlike the other backdrops: this one is meant to
    // be touched. It sits behind the form, so it only receives the events the
    // column above it does not.
    <div aria-hidden className={className}>
      <RippleGrid
        key={theme.light ? "light" : "dark"}
        gridColor={theme.color}
        lightMode={theme.light}
        opacity={0.6}
        gridSize={11}
        gridThickness={13}
        rippleIntensity={0.06}
        glowIntensity={0.12}
        fadeDistance={1.6}
        vignetteStrength={2.2}
        mouseInteraction
        mouseInteractionRadius={1.2}
      />
    </div>
  );
}
