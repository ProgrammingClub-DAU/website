"use client";

/**
 * The club's name, drawn as particles, above the closing call to action.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff, as with the dome gallery and the dot
 * field. Everything specific to this site lives here.
 *
 * This is a wordmark, not a heading: the canvas carries the text for a screen
 * reader through the component's own sr-only span, and the real <h2> below it
 * is untouched. Replacing a heading with a canvas would make the existing
 * heading-order findings in the frontend audit worse.
 */

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

/**
 * Client-only. It measures its container, reads `devicePixelRatio`, and samples
 * rendered glyphs out of a scratch canvas to find its particle positions —
 * none of which the server can do.
 */
const ParticleText = dynamic(() => import("@/components/site/particle-text"), {
  ssr: false,
});

/**
 * Particles are coloured left to right by mixing between these two, so the
 * wordmark runs cyan into purple: the same two ends as --club-gradient.
 *
 * Read off the document rather than passed as `var(--token)` because the
 * component parses them with a 6-digit-hex regex and falls back on anything
 * else. The rank tokens are plain hex, so they come back usable as they are.
 */
const COLOR_TOKENS = ["--cf-specialist", "--cf-candidate"] as const;

type Palette = [base: string, highlight: string];

function readPalette(): Palette {
  const styles = getComputedStyle(document.documentElement);
  return COLOR_TOKENS.map((token) => styles.getPropertyValue(token).trim()) as Palette;
}

export function ClubWordmark({ text }: { text: string }) {
  const [palette, setPalette] = useState<Palette | null>(null);

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

  if (!palette) {
    // Reserve the height so the card below does not jump when this mounts.
    return <div className="mx-auto h-[clamp(90px,14vw,170px)] max-w-[46rem]" aria-hidden />;
  }

  const [color, highlightColor] = palette;

  return (
    // The component's own container is `h-full min-h-[240px]`, so the height
    // has to come from a parent rather than from a class passed in — those
    // would sit at the same specificity and collapse the canvas to nothing.
    // Capped near the width of the text itself. Particles are coloured by their
    // x position across the whole canvas, so a wordmark centred in a full-width
    // container only ever samples the middle of the gradient and comes out one
    // colour. Fitting the canvas to the text spends the full cyan-to-purple run
    // on it.
    <div className="mx-auto h-[clamp(90px,14vw,170px)] max-w-[46rem]">
      <ParticleText
        text={text}
        color={color}
        highlightColor={highlightColor}
        className="min-h-0!"
        fontSize="clamp(2.25rem,7vw,5rem)"
        fontWeight={600}
        particleSize={2}
        density={4}
        scatter={140}
        idleDrift={0.5}
        repelRadius={110}
        pointerRepel={34}
      />
    </div>
  );
}
