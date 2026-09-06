"use client";

/**
 * Hosts the React Bits pixel blast behind the auth pages.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * colour, tuning, loading strategy — lives here.
 *
 * A field of pixels that ripples where the pointer goes and bursts where it
 * clicks. Pixels are the right register for a programming club, and it is the
 * most interactive of the options: the ripple grid it replaced only bent around
 * the cursor, and Particles before that only drifted.
 *
 * It is the one thing on the site with a dependency cost worth stating: three
 * and postprocessing, about 210 kB gzipped. It is dynamically imported and
 * reached only from /login and /register, so no other route pays for it, and it
 * is cached after the first visit. The ripple grid remains the free alternative
 * — it uses ogl, which Aurora already brings in.
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
const PixelBlast = dynamic(() => import("@/components/site/pixel-blast"), {
  ssr: false,
});

type Theme = { color: string; light: boolean };

function readTheme(): Theme {
  const root = document.documentElement;
  return {
    // One colour, not a gradient, so it is the middle of the club gradient —
    // the stop that sits closest to --primary. Read off the document rather
    // than passed as var(--token): it goes to THREE.Color, which has no CSS
    // cascade to resolve a custom property against.
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
      <PixelBlast
        key={theme.light ? "light" : "dark"}
        variant="square"
        color={theme.color}
        pixelSize={4}
        patternScale={2.4}
        patternDensity={1.05}
        pixelSizeJitter={0.4}
        speed={0.45}
        edgeFade={0.35}
        enableRipples
        rippleSpeed={0.35}
        rippleThickness={0.11}
        rippleIntensityScale={1.4}
        liquid
        liquidStrength={0.09}
        liquidRadius={1.1}
        transparent
      />
    </div>
  );
}
