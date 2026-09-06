"use client";

/**
 * Hosts the React Bits Aurora behind the home hero.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * colours, theme, loading strategy — lives here.
 *
 * This is the spec's original choice for the slot. It holds a WebGL context, so
 * the spec's rule applies: at most one per page, and never two. Aurora has the
 * home page; Particles has the auth pages.
 */

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

/**
 * Client-only. It opens a WebGL context, measures its container and compiles
 * shaders on mount, none of which the server can do — and the hero's text is
 * the part worth showing first.
 */
const Aurora = dynamic(() => import("@/components/site/aurora"), { ssr: false });

/**
 * The club gradient's three stops. Read off the document rather than passed as
 * `var(--token)`: they are parsed by ogl's Color, which has no CSS cascade to
 * resolve a custom property against. The rank tokens are plain hex, so they
 * come back usable as they are.
 */
const COLOR_TOKENS = ["--cf-specialist", "--cf-expert", "--cf-candidate"] as const;

type Theme = { stops: string[]; light: boolean };

function readTheme(): Theme {
  const root = document.documentElement;
  const styles = getComputedStyle(root);
  return {
    stops: COLOR_TOKENS.map((token) => styles.getPropertyValue(token).trim()),
    light: !root.classList.contains("dark"),
  };
}

export function AuroraBackdrop({ className }: { className: string }) {
  const [theme, setTheme] = useState<Theme | null>(null);

  useEffect(() => {
    const sync = () => setTheme(readTheme());
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

  if (!theme) return null;

  return (
    <div aria-hidden className={className}>
      {/* Keyed on the theme so the shader is rebuilt when it changes. The
          colours are read live inside the render loop, but `lightMode` picks a
          different branch in the fragment shader, and under reduced motion the
          loop is stopped and would never draw the new one. */}
      <Aurora
        key={theme.light ? "light" : "dark"}
        colorStops={theme.stops}
        lightMode={theme.light}
        amplitude={1.15}
        blend={0.62}
        speed={1.2}
      />
    </div>
  );
}
