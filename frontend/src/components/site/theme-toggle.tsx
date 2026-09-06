"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";

import { AnimatedThemeToggler } from "@/components/site/animated-theme-toggler";

/**
 * The theme switch, using Magic UI's animated toggler.
 *
 * Driven in its controlled mode — `theme` in, `onThemeChange` out — so
 * next-themes stays the single owner of the value and of persistence. Left
 * uncontrolled it writes its own `localStorage` key and toggles the class
 * itself, which would fight the provider and leave the two disagreeing after a
 * reload.
 *
 * The transition uses the View Transitions API where the browser has it, and
 * falls back to an ordinary class swap where it does not.
 */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  // The server cannot know the visitor's theme, so rendering the real control
  // before mount would guarantee a hydration mismatch on one of the two. This
  // placeholder matches the control's footprint so the navbar does not shift.
  if (!mounted) {
    return <div className="size-8 rounded-full bg-surface-2" aria-hidden />;
  }

  return (
    <AnimatedThemeToggler
      theme={resolvedTheme === "dark" ? "dark" : "light"}
      onThemeChange={setTheme}
      variant="circle"
      duration={520}
      aria-label="Toggle color theme"
      className="glass-control [--glass-fill:var(--glass-quiet)] inline-flex size-8 items-center justify-center rounded-full text-fg-muted transition-colors hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring [&_svg]:size-4"
    />
  );
}
