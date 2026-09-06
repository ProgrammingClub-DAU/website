"use client";

// Vendored from React Bits (GradientText-TS-TW). Local changes marked `// local:`.
import { useState, useCallback, useEffect, useRef, type ReactNode } from 'react';
import { motion, useMotionValue, useAnimationFrame, useTransform, useReducedMotion } from 'motion/react';

interface GradientTextProps {
  children: ReactNode;
  className?: string;
  colors?: string[];
  animationSpeed?: number;
  direction?: 'horizontal' | 'vertical' | 'diagonal';
  pauseOnHover?: boolean;
  yoyo?: boolean;
}

// local: defaults to the club gradient rather than upstream's purple/pink.
// var() references resolve per theme, so this follows the light/dark switch.
const CLUB_STOPS = [
  'var(--cf-specialist)',
  'var(--cf-expert)',
  'var(--cf-candidate)',
  'var(--cf-master)',
];

export default function GradientText({
  children,
  className = '',
  colors = CLUB_STOPS,
  animationSpeed = 8,
  direction = 'horizontal',
  pauseOnHover = false,
  yoyo = true
}: GradientTextProps) {
  const [isPaused, setIsPaused] = useState(false);
  const progress = useMotionValue(0);
  const elapsedRef = useRef(0);
  const lastTimeRef = useRef<number | null>(null);
  // local: the CSS reduced-motion block cannot stop a rAF loop, so the component
  // has to opt out itself. The gradient still paints; it simply stops moving.
  const reduceMotion = useReducedMotion();

  const animationDuration = animationSpeed * 1000;

  useAnimationFrame(time => {
    if (isPaused || reduceMotion) {   // local: added reduceMotion
      lastTimeRef.current = null;
      return;
    }
    if (lastTimeRef.current === null) {
      lastTimeRef.current = time;
      return;
    }
    const deltaTime = time - lastTimeRef.current;
    lastTimeRef.current = time;
    elapsedRef.current += deltaTime;

    if (yoyo) {
      const fullCycle = animationDuration * 2;
      const cycleTime = elapsedRef.current % fullCycle;
      if (cycleTime < animationDuration) {
        progress.set((cycleTime / animationDuration) * 100);
      } else {
        progress.set(100 - ((cycleTime - animationDuration) / animationDuration) * 100);
      }
    } else {
      progress.set((elapsedRef.current / animationDuration) * 100);
    }
  });

  useEffect(() => {
    elapsedRef.current = 0;
    progress.set(0);
  }, [animationSpeed, yoyo, progress]);

  const backgroundPosition = useTransform(progress, p =>
    direction === 'vertical' ? `50% ${p}%` : `${p}% 50%`
  );

  const handleMouseEnter = useCallback(() => { if (pauseOnHover) setIsPaused(true); }, [pauseOnHover]);
  const handleMouseLeave = useCallback(() => { if (pauseOnHover) setIsPaused(false); }, [pauseOnHover]);

  const gradientAngle =
    direction === 'horizontal' ? 'to right' : direction === 'vertical' ? 'to bottom' : 'to bottom right';
  const gradientColors = [...colors, colors[0]].join(', ');

  const gradientStyle = {
    backgroundImage: `linear-gradient(${gradientAngle}, ${gradientColors})`,
    backgroundSize: direction === 'vertical' ? '100% 300%' : '300% 100%',
    backgroundRepeat: 'repeat' as const,
  };

  // local: a span that renders inline, so it can sit inside an h1 without
  // changing the heading's layout. Upstream returned a centred, fit-content,
  // pointer-cursor pill wrapped in a div, which is invalid inside an h1 and
  // would have centred every page title.
  //
  // local: `color` is set before the gradient and left as the painted value if
  // background-clip:text is unsupported or the image fails to paint. Without it
  // the text is transparent over nothing — invisible headings rather than
  // unstyled ones.
  return (
    <motion.span
      data-gradient-text=""
      className={`inline bg-clip-text text-transparent ${className}`}
      style={{
        ...gradientStyle,
        backgroundPosition,
        WebkitBackgroundClip: 'text',
        color: 'transparent',
        ['--gradient-text-fallback' as string]: 'var(--foreground)',
      }}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
    </motion.span>
  );
}
