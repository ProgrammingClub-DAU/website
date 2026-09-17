"use client";

/**
 * The Hall of Fame, as a timeline grouped by year, newest first.
 *
 * Driven by the entries admins maintain. The layout is the one the page has
 * always had -- the club-record spine, a marker per year with light travelling
 * between them, a card per achievement with its photo pinned beside it -- but
 * every word on it now comes from a record somebody entered, and a card
 * without photos simply has no polaroid rather than a stand-in image.
 */

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowUpRight, Camera, ExternalLink } from "lucide-react";

import { AnimatedBeam } from "@/components/site/animated-beam";
import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState } from "@/components/site/primitives";
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
import { cn } from "@/lib/utils";
import type { HallOfFameEntry } from "@/types/api";

const ALL_YEARS = "All years";

/** "2026-01-10" -> 2026, read from the string so no timezone can move it. */
function yearOf(isoDate: string): string {
  return isoDate.slice(0, 4);
}

/** "2026-01-10" -> "10 JAN 2026", without constructing a Date in local time. */
function formatDay(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  const month = new Date(Date.UTC(y, m - 1, d)).toLocaleDateString("en-IN", {
    month: "short",
    timeZone: "UTC",
  });
  return `${d} ${month} ${y}`.toUpperCase();
}

interface YearGroup {
  year: string;
  entries: HallOfFameEntry[];
}

/** Entries arrive newest first, so grouping in order keeps both levels newest first. */
function groupByYear(entries: HallOfFameEntry[]): YearGroup[] {
  const groups: YearGroup[] = [];
  for (const entry of entries) {
    const year = yearOf(entry.achievedOn);
    const last = groups[groups.length - 1];
    if (last && last.year === year) last.entries.push(entry);
    else groups.push({ year, entries: [entry] });
  }
  return groups;
}

function PhotoStack({ entry, isEven }: { entry: HallOfFameEntry; isEven: boolean }) {
  const photo = entry.photos[0];
  if (!photo) return null;

  const frontRotate = isEven ? "-rotate-2 group-hover/photo:rotate-0" : "rotate-2 group-hover/photo:rotate-0";
  const backRotate = isEven ? "rotate-4 group-hover/photo:rotate-6" : "-rotate-4 group-hover/photo:-rotate-6";

  return (
    <Link
      href={`/hall-of-fame/${entry.id}`}
      aria-label={`Photos from ${entry.heading}`}
      className="group/photo relative flex shrink-0 items-center justify-center self-center p-3 sm:p-4"
    >
      {/* The second frame only appears when there really is a second photo. */}
      {entry.photos.length > 1 && (
        <span
          className={cn(
            "absolute size-28 rounded-xs border-2 border-white bg-white/25 shadow-lg backdrop-blur-xs transition-transform duration-300 sm:size-32 sm:border-[3px] md:size-36",
            backRotate
          )}
          aria-hidden
        />
      )}

      <span
        className={cn(
          "relative block size-28 rounded-xs border-2 border-white bg-white p-1 shadow-2xl transition-all duration-300 group-hover/photo:scale-105 sm:size-32 sm:border-[3px] sm:p-1.5 md:size-36",
          frontRotate
        )}
      >
        <span className="relative block h-full w-full overflow-hidden rounded-xs bg-surface-2">
          <Image
            src={photo.imageUrl}
            alt={photo.caption ?? ""}
            fill
            sizes="144px"
            className="object-cover transition-transform duration-500 ease-out group-hover/photo:scale-105"
          />
        </span>
      </span>

      {entry.photos.length > 1 && (
        <span className="absolute right-1 bottom-1 inline-flex items-center gap-1 rounded-full bg-black/70 px-2 py-0.5 font-mono text-nano text-white">
          <Camera className="size-2.5" />
          {entry.photos.length}
        </span>
      )}
    </Link>
  );
}

/**
 * Reads the beam's two colours off the document.
 *
 * They end up in an SVG `stop-color`, which is a presentation attribute rather
 * than a CSS declaration, so `var(--token)` reaches it unresolved and the
 * gradient renders as nothing. The observer re-reads them on a theme change.
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
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["class"] });
    return () => observer.disconnect();
  }, []);

  return colors;
}

export function HallOfFameTimeline({ entries }: { entries: HallOfFameEntry[] }) {
  const years = useMemo(() => groupByYear(entries), [entries]);
  const [filter, setFilter] = useState(ALL_YEARS);
  const options = [ALL_YEARS, ...years.map((y) => y.year)];
  const visible = filter === ALL_YEARS ? years : years.filter((y) => y.year === filter);

  const containerRef = useRef<HTMLDivElement | null>(null);
  // One stable ref object per visible year. AnimatedBeam holds on to what it is
  // given, so the identity has to survive re-renders.
  const dotRefs = useMemo(
    () => Array.from({ length: visible.length }, () => ({ current: null as HTMLSpanElement | null })),
    [visible.length]
  );
  const [beamColorStart, beamColorStop] = useBeamColors();

  // Decoration over a spine that is already drawn, so under reduced motion the
  // travelling light is simply not rendered.
  const [animate, setAnimate] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setAnimate(!query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);

  if (entries.length === 0) {
    return (
      <EmptyState
        title="No achievements recorded yet."
        hint="Results appear here as the committee adds them."
      />
    );
  }

  const beamCount = Math.max(0, visible.length - 1);

  return (
    <>
      {/* A filter is only worth showing once there is more than one year to pick. */}
      {years.length > 1 && (
        <FilterChips label="Filter by year" options={options} value={filter} onChange={setFilter} />
      )}

      <div className="relative mt-10" ref={containerRef}>
        <TimelineRoot label="Club record" />

        {visible.map((year, yearIndex) => (
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
              <div className="flex items-baseline gap-4">
                <h2 className="font-mono text-[clamp(1.5rem,3vw,2rem)] font-medium tracking-tight">
                  {year.year}
                </h2>
                <span className="font-mono text-label tracking-caps text-fg-subtle uppercase">
                  {year.entries.length} {year.entries.length === 1 ? "entry" : "entries"}
                </span>
              </div>
            </header>

            <Timeline>
              {year.entries.map((entry, i) => (
                <TimelineEntry
                  key={entry.id}
                  isLast={yearIndex === visible.length - 1 && i === year.entries.length - 1}
                >
                  <div className="flex flex-col gap-6 sm:flex-row sm:items-center md:gap-8">
                    <TimelineCard className="relative flex-1">
                      <span className="font-mono text-label tracking-caps text-fg-subtle uppercase transition-colors [@media(hover:hover)]:group-hover/entry:text-primary group-[.tl-active]/entry:text-primary">
                        {formatDay(entry.achievedOn)}
                      </span>

                      <div>
                        <h3 className="text-lg font-semibold tracking-tight text-pretty">
                          {/* Stretched over the card: the whole card opens the entry. */}
                          <Link href={`/hall-of-fame/${entry.id}`} className="after:absolute after:inset-0">
                            {entry.heading}
                          </Link>
                        </h3>
                        {entry.subheading && (
                          <p className="mt-1.5 font-mono text-xs text-fg-muted">{entry.subheading}</p>
                        )}
                      </div>

                      {entry.details && (
                        <p className="line-clamp-3 border-t border-hairline pt-3.5 text-body leading-[1.5] whitespace-pre-line text-fg-muted text-pretty">
                          {entry.details}
                        </p>
                      )}

                      <div className="flex flex-wrap items-center gap-2 pt-1">
                        {/* Above the stretched link, so each chip is its own target. */}
                        {entry.links.map((link) => (
                          <a
                            key={link.url}
                            href={link.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="relative z-10 inline-flex items-center gap-1 rounded-full border border-border bg-background/40 px-2.5 py-1 font-mono text-micro text-fg-muted transition-colors hover:border-hairline-strong hover:text-foreground"
                          >
                            {link.label}
                            <ExternalLink className="size-2.5 opacity-60" />
                          </a>
                        ))}
                        <ArrowUpRight className="ml-auto size-4 text-fg-subtle transition-colors [@media(hover:hover)]:group-hover/entry:text-primary" />
                      </div>
                    </TimelineCard>

                    <PhotoStack entry={entry} isEven={i % 2 === 0} />
                  </div>
                </TimelineEntry>
              ))}
            </Timeline>
          </TimelineGroup>
        ))}

        <TimelineEnd label="Start of the record" />

        {/* Light travelling down the spine between year markers. Rendered last
            so the refs above are populated; pathOpacity 0 because the spine
            underneath is already drawn. */}
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
