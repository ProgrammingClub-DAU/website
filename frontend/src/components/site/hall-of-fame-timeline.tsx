"use client";

import { useEffect, useMemo, useRef, useState } from "react";

import { AnimatedBeam } from "@/components/site/animated-beam";
import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState, RankDot } from "@/components/site/primitives";
import {
  RAIL,
  SPINE_X,
  Timeline,
  TimelineCard,
  TimelineEnd,
  TimelineEntry,
  TimelineGroup,
  TimelineHeadSpine,
  TimelineRoot,
} from "@/components/site/timeline";
import type { HofYear } from "@/lib/content/hall-of-fame";
import { cn } from "@/lib/utils";

/**
 * Reads the beam's two colours off the document.
 *
 * They end up in an SVG `stop-color`, which is a presentation attribute rather
 * than a CSS declaration, so `var(--token)` reaches it unresolved and the
 * gradient renders as nothing. The listener re-reads them on a theme change,
 * which a var() would have handled by itself.
 */
function useBeamColors(): [string, string] {
  const [colors, setColors] = useState<[string, string]>(["#00c2c7", "#c066e0"]);

  useEffect(() => {
    const sync = () => {
      const styles = getComputedStyle(document.documentElement);
      setColors([
        styles.getPropertyValue("--cf-specialist").trim(),
        styles.getPropertyValue("--cf-candidate").trim(),
      ]);
    };
    sync();
    const observer = new MutationObserver(sync);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });
    return () => observer.disconnect();
  }, []);

  return colors;
}

export function HallOfFameTimeline({ years }: { years: HofYear[] }) {
  const [filter, setFilter] = useState("All years");
  const options = ["All years", ...years.map((y) => y.year)];
  const visible = filter === "All years" ? years : years.filter((y) => y.year === filter);

  const containerRef = useRef<HTMLDivElement | null>(null);
  // One stable ref object per visible year, created together rather than read
  // out of a single ref during render — AnimatedBeam holds onto what it is
  // given, so the identity has to survive re-renders.
  const dotRefs = useMemo(
    () =>
      Array.from({ length: visible.length }, () => ({
        current: null as HTMLSpanElement | null,
      })),
    [visible.length]
  );
  const [beamColorStart, beamColorStop] = useBeamColors();

  // The beams are decoration over a spine that is already drawn, so under
  // reduced motion they are simply not rendered — the line stays, the travelling
  // light does not. `motion` does not stop this on its own: the gradient is
  // driven by an explicit animate prop.
  const [animate, setAnimate] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setAnimate(!query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);

  // A beam joins consecutive year markers, so a single visible year has none.
  const beamCount = Math.max(0, visible.length - 1);

  return (
    <>
      <FilterChips
        label="Filter by year"
        options={options}
        value={filter}
        onChange={setFilter}
      />

      <div className="relative mt-10" ref={containerRef}>
        <TimelineRoot label="Club record" />

        {visible.length === 0 ? (
          <EmptyState
            title="No records for that year yet."
            hint="Pick another year, or add the record."
          />
        ) : (
          visible.map((year, yearIndex) => (
            <TimelineGroup key={year.year}>
              <header className={cn("items-center py-4", RAIL)}>
                <div className="relative h-14">
                  <TimelineHeadSpine />
                  <span
                    ref={(node) => {
                      dotRefs[yearIndex].current = node;
                    }}
                    className="absolute top-1/2 size-[11px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary shadow-[0_0_0_4px_var(--primary-soft)]"
                    style={{ left: SPINE_X }}
                    aria-hidden
                  />
                </div>
                <div className="flex flex-wrap items-baseline justify-between gap-3">
                  <div className="flex items-baseline gap-4">
                    <h2 className="font-mono text-[clamp(1.5rem,3vw,2rem)] font-medium tracking-tight">
                      {year.year}
                    </h2>
                    <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                      {String(year.entries.length).padStart(2, "0")} entries
                    </span>
                  </div>
                  <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
                    {year.label}
                  </span>
                </div>
              </header>

              <Timeline>
                {year.entries.map((entry, i) => (
                  <TimelineEntry
                    key={entry.title}
                    isLast={
                      yearIndex === visible.length - 1 && i === year.entries.length - 1
                    }
                  >
                    <TimelineCard>
                      <div className="flex items-center gap-2.5">
                        <RankDot rank={entry.cf} />
                        <span className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase transition-colors [@media(hover:hover)]:group-hover/entry:text-primary group-[.tl-active]/entry:text-primary">
                          {entry.cat}
                        </span>
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold tracking-tight text-pretty">
                          {entry.title}
                        </h3>
                        <p className="mt-2 font-mono text-xs text-fg-muted">
                          {entry.people}
                        </p>
                      </div>
                      <p className="border-t border-hairline pt-3.5 text-[15px] leading-[1.5] text-fg-muted text-pretty">
                        {entry.note}
                      </p>
                    </TimelineCard>
                  </TimelineEntry>
                ))}
              </Timeline>
            </TimelineGroup>
          ))
        )}

        {visible.length > 0 && <TimelineEnd label="Older records pending" />}

        {/* Light travelling down the spine between year markers. Rendered last
            so the refs above it are populated, and pathOpacity is 0 because the
            spine underneath is already drawn — this layer contributes only the
            moving gradient, not a second static line. */}
        {animate &&
          Array.from({ length: beamCount }, (_, i) => (
            <AnimatedBeam
              key={`${visible[i].year}-${visible[i + 1].year}`}
              containerRef={containerRef}
              fromRef={dotRefs[i]}
              toRef={dotRefs[i + 1]}
              curvature={0}
              pathOpacity={0}
              pathWidth={2}
              gradientStartColor={beamColorStart}
              gradientStopColor={beamColorStop}
              duration={4}
              delay={i * 0.6}
            />
          ))}
      </div>
    </>
  );
}
