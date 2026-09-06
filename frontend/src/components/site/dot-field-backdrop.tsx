"use client";

/**
 * Hosts the React Bits dot field as a background, for the home hero and the
 * auth panel.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff. Everything specific to this site —
 * sizing, theming, loading strategy — lives here, as with the dome gallery.
 *
 * The spec called for Aurora behind the hero and Particles behind the auth
 * panel. Both are WebGL and pull in `ogl`, and the instruction after the spec
 * was written was to stay off WebGL. This is Canvas 2D and pulls in nothing,
 * and it does both jobs: a moving field, drawn in the club gradient's colours,
 * that text sits on.
 *
 * Callers supply the mask and the tuning; only the palette and the loading
 * strategy are shared, and those are the parts worth having in one place.
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

type DotFieldBackdropProps = {
  /** Positioning and the mask that fades the field into the page. */
  className: string;
  dotRadius?: number;
  dotSpacing?: number;
  cursorRadius?: number;
  bulgeStrength?: number;
  glowRadius?: number;
};

function readPalette(): Palette {
  const styles = getComputedStyle(document.documentElement);
  return COLOR_TOKENS.map((token) => styles.getPropertyValue(token).trim()) as Palette;
}

export function DotFieldBackdrop({
  className,
  dotRadius = 3,
  dotSpacing = 26,
  cursorRadius = 340,
  bulgeStrength = 44,
  glowRadius = 200,
}: DotFieldBackdropProps) {
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
    <div aria-hidden className={className}>
      <DotField
        dotRadius={dotRadius}
        dotSpacing={dotSpacing}
        cursorRadius={cursorRadius}
        bulgeStrength={bulgeStrength}
        glowRadius={glowRadius}
        gradientFrom={gradientFrom}
        gradientTo={gradientTo}
        glowColor={glowColor}
      />
    </div>
  );
}
