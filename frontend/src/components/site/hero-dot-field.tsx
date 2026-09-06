"use client";

/**
 * Hosts the React Bits dot field behind the home hero.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * sizing, theming, loading strategy — lives here, as with the dome gallery.
 *
 * The spec called for Aurora in this slot. Aurora is WebGL and pulls in `ogl`;
 * this is Canvas 2D and pulls in nothing, which is the constraint that came
 * after the spec was written. It does the same job: a moving field the hero
 * text sits on, drawn in the club gradient's colours.
 */

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

/**
 * Client-only. The component measures its container and reads
 * `devicePixelRatio` on mount, neither of which the server can do, and the
 * hero's text is the part worth showing first.
 */
const DotField = dynamic(() => import("@/components/site/dot-field"), {
  ssr: false,
});

/**
 * Read off the document rather than passed as `var(--token)`, because these end
 * up in a canvas gradient and an SVG stop — a canvas has no CSS cascade, so
 * `var()` would reach it as an unparseable string and the dots would not draw.
 */
const COLOR_TOKENS = ["--dot-field-from", "--dot-field-to", "--dot-field-glow"] as const;

type Palette = [from: string, to: string, glow: string];

function readPalette(): Palette {
  const styles = getComputedStyle(document.documentElement);
  return COLOR_TOKENS.map((token) => styles.getPropertyValue(token).trim()) as Palette;
}

export function HeroDotField() {
  const [palette, setPalette] = useState<Palette | null>(null);

  useEffect(() => {
    const sync = () => setPalette(readPalette());
    sync();

    // Resolved colours cannot follow the theme on their own the way a var()
    // would, so re-read them when the theme provider swaps the class on <html>.
    const observer = new MutationObserver(sync);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => observer.disconnect();
  }, []);

  // Nothing to draw until the tokens have been read, which is one frame after
  // mount. The hero is legible without it; this is a background.
  if (!palette) return null;

  const [gradientFrom, gradientTo, glowColor] = palette;

  return (
    <div
      aria-hidden
      className="hero-field-mask pointer-events-none absolute inset-0 -z-10"
    >
      <DotField
        dotRadius={3}
        dotSpacing={26}
        cursorRadius={340}
        bulgeStrength={44}
        glowRadius={200}
        gradientFrom={gradientFrom}
        gradientTo={gradientTo}
        glowColor={glowColor}
      />
    </div>
  );
}
