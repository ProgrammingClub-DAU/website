"use client";

import { useEffect, useRef } from "react";

/**
 * The logo animation, played on demand — the same draw-and-fill the opening
 * identity uses, without the wordmark.
 *
 * `mark-only` is what drops the text: the mark then keeps the centred position
 * it already starts from instead of docking aside to leave room for words that
 * are not there, and the run finishes once the fill lands rather than holding a
 * static logo for the wordmark's half of the timeline.
 *
 * Mounted only while playing. The component replays on every mount unless it is
 * given `once`, so mounting is the trigger — there is no imperative start to
 * coordinate with React, and every play begins from a clean element.
 */
export function ClubMarkReveal({ onClose }: { onClose: () => void }) {
  const ref = useRef<HTMLElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    // Fires for every ending the component has — completion, Escape, and the
    // immediate bail under reduced motion — so this unmounts on all of them
    // rather than only the happy path.
    const done = () => onClose();
    el.addEventListener("intro-complete", done);

    // The element is defined by a beforeInteractive script, but a failure to
    // load it would leave an inert tag on screen with nothing to end it.
    const bail = window.setTimeout(() => {
      if (!customElements.get("programming-club-intro")) onClose();
    }, 1000);

    return () => {
      el.removeEventListener("intro-complete", done);
      window.clearTimeout(bail);
    };
  }, [onClose]);

  return <programming-club-intro ref={ref} overlay="" mark-only="" for="main" />;
}

export default ClubMarkReveal;
