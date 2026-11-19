"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";

/**
 * Marks its element `data-inview="true"` the first time it scrolls into view, so
 * CSS can run an entrance from it (`group-data-[inview=true]/name:...`). Stays
 * marked after that. Without IntersectionObserver it is marked straight away.
 */
export function InView({
  children,
  className,
  threshold = 0.25,
}: {
  children: ReactNode;
  className?: string;
  threshold?: number;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    if (typeof IntersectionObserver === "undefined") {
      setInView(true);
      return;
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          observer.disconnect();
        }
      },
      { threshold }
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [threshold]);

  return (
    <div ref={ref} className={className} data-inview={inView ? "true" : "false"}>
      {children}
    </div>
  );
}
