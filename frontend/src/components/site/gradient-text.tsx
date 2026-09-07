"use client";

// Vendored from React Bits (GradientText-TS-TW). Local changes marked `// local:`.
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import {
  motion,
  useAnimationFrame,
  useMotionValue,
  useReducedMotion,
  useTransform,
  type MotionStyle,
} from 'motion/react';

interface GradientTextProps {
  children: ReactNode;
  className?: string;
  colors?: string[];
  animationSpeed?: number;
  direction?: 'horizontal' | 'vertical' | 'diagonal';
  pauseOnHover?: boolean;
  yoyo?: boolean;
}

type Direction = NonNullable<GradientTextProps['direction']>;

// local: defaults to the club gradient rather than upstream's purple/pink.
// var() references resolve per theme, so this follows the light/dark switch.
const CLUB_STOPS = [
  'var(--cf-specialist)',
  'var(--cf-expert)',
  'var(--cf-candidate)',
];

function gradientImageStyle(colors: string[], direction: Direction) {
  const gradientAngle =
    direction === 'horizontal' ? 'to right' : direction === 'vertical' ? 'to bottom' : 'to bottom right';
  const gradientColors = [...colors, colors[0]].join(', ');

  return {
    backgroundImage: `linear-gradient(${gradientAngle}, ${gradientColors})`,
    backgroundSize: direction === 'vertical' ? '100% 300%' : '300% 100%',
    backgroundRepeat: 'repeat' as const,
  };
}

interface GradientSpanProps {
  children: ReactNode;
  className: string;
  style: MotionStyle;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
}

// local: a span that renders inline, so it can sit inside an h1 without
// changing the heading's layout. Upstream returned a centred, fit-content,
// pointer-cursor pill wrapped in a div, which is invalid inside an h1 and
// would have centred every page title.
//
// local: no `color` is set here, and `text-transparent` is deliberately not
// in the class list. `[data-gradient-text]` in globals.css owns the colour:
// it sets a real, visible fallback by default, and its `@supports` block
// switches to `color: transparent` only where background-clip:text actually
// works. An inline `color` (or a Tailwind class that maps to one) always
// wins over that selector rule regardless of `@supports`, which would make
// every heading invisible whenever clipping fails to paint — exactly the
// failure this fallback exists to prevent. The `--gradient-text-fallback`
// custom property is still set inline so the CSS fallback colour can be
// themed per instance if a caller ever needs to override it.
function GradientSpan({ children, className, style, onMouseEnter, onMouseLeave }: GradientSpanProps) {
  return (
    <motion.span
      data-gradient-text=""
      className={`inline bg-clip-text ${className}`}
      style={style}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      {children}
    </motion.span>
  );
}

interface StaticGradientTextProps {
  children: ReactNode;
  className: string;
  colors: string[];
  direction: Direction;
}

// local: split out from the animated variant so `GradientText` can avoid
// calling `useAnimationFrame` entirely when reduced motion is requested (see
// the comment on `GradientText` below for why that split is necessary).
// Renders the identical markup as `AnimatedGradientText` — same span, same
// `data-gradient-text` attribute, same classes — just with a fixed
// `backgroundPosition` instead of one driven by a motion value.
function StaticGradientText({ children, className, colors, direction }: StaticGradientTextProps) {
  return (
    <GradientSpan
      className={className}
      style={{
        ...gradientImageStyle(colors, direction),
        backgroundPosition: direction === 'vertical' ? '50% 0%' : '0% 50%',
        WebkitBackgroundClip: 'text',
        ['--gradient-text-fallback' as string]: 'var(--foreground)',
      }}
    >
      {children}
    </GradientSpan>
  );
}

interface AnimatedGradientTextProps {
  children: ReactNode;
  className: string;
  colors: string[];
  animationSpeed: number;
  direction: Direction;
  pauseOnHover: boolean;
  yoyo: boolean;
}

function AnimatedGradientText({
  children,
  className,
  colors,
  animationSpeed,
  direction,
  pauseOnHover,
  yoyo,
}: AnimatedGradientTextProps) {
  const [isPaused, setIsPaused] = useState(false);
  const progress = useMotionValue(0);
  const elapsedRef = useRef(0);
  const lastTimeRef = useRef<number | null>(null);

  const animationDuration = animationSpeed * 1000;

  useAnimationFrame(time => {
    if (isPaused) {
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
    // local: `progress` added to the dependency array. It's referenced inside
    // the effect (`progress.set(0)`), so exhaustive-deps requires it; upstream
    // omitted it. `useMotionValue` returns a stable object reference for the
    // life of the component, so adding it does not cause extra re-runs.
  }, [animationSpeed, yoyo, progress]);

  const backgroundPosition = useTransform(progress, p =>
    direction === 'vertical' ? `50% ${p}%` : `${p}% 50%`
  );

  const handleMouseEnter = useCallback(() => { if (pauseOnHover) setIsPaused(true); }, [pauseOnHover]);
  const handleMouseLeave = useCallback(() => { if (pauseOnHover) setIsPaused(false); }, [pauseOnHover]);

  return (
    <GradientSpan
      className={className}
      style={{
        ...gradientImageStyle(colors, direction),
        backgroundPosition,
        WebkitBackgroundClip: 'text',
        ['--gradient-text-fallback' as string]: 'var(--foreground)',
      }}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
    </GradientSpan>
  );
}

export default function GradientText({
  children,
  className = '',
  colors = CLUB_STOPS,
  animationSpeed = 8,
  direction = 'horizontal',
  pauseOnHover = false,
  yoyo = true,
}: GradientTextProps) {
  // local: upstream calls `useAnimationFrame` unconditionally. That hook
  // subscribes to requestAnimationFrame with `keepAlive: true` and re-arms
  // itself every frame *before* invoking its callback, so returning early
  // from inside the callback under reduced motion (as a naive `if
  // (reduceMotion) return` would) stops the visible movement but not the
  // rAF loop itself — it keeps firing for the life of the mounted component.
  // The only way to actually stop the loop is to not call the hook at all,
  // and React hooks cannot be called conditionally inside one component, so
  // the animated and static renders are split into two internal components
  // and this component picks one based on `useReducedMotion()`. Both render
  // identical markup, so consumers and tests cannot tell them apart
  // structurally.
  const reduceMotion = useReducedMotion();

  if (reduceMotion) {
    return (
      <StaticGradientText className={className} colors={colors} direction={direction}>
        {children}
      </StaticGradientText>
    );
  }

  return (
    <AnimatedGradientText
      className={className}
      colors={colors}
      animationSpeed={animationSpeed}
      direction={direction}
      pauseOnHover={pauseOnHover}
      yoyo={yoyo}
    >
      {children}
    </AnimatedGradientText>
  );
}
