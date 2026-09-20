/**
 * The latest Hall of Fame entries, as a row of cards.
 *
 * Shown on the home and about pages, which each carried an identical copy of
 * this markup reading from a hand-written file. It now fetches the real
 * entries itself, so both pages stay in step with what admins record.
 *
 * Renders nothing at all when there is nothing to show, or when the backend
 * cannot be reached. A "Hall of fame" heading over an empty row reads as a
 * broken page, and this section is a teaser, not the record -- the full page
 * says plainly when it cannot load.
 */

import Image from "next/image";
import Link from "next/link";

import { Section, SectionHeader } from "@/components/site/primitives";
import { hallOfFameService } from "@/lib/services/hall-of-fame";
import type { HallOfFameEntry } from "@/types/api";

const TEASER_COUNT = 4;

/** "2026-01-10" -> "JAN 2026", read from the string so no timezone moves it. */
function monthYear(isoDate: string): string {
  const [y, m] = isoDate.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, 1))
    .toLocaleDateString("en-IN", { month: "short", year: "numeric", timeZone: "UTC" })
    .toUpperCase();
}

export async function HallOfFameTeaser({ title }: { title: string }) {
  let entries: HallOfFameEntry[] = [];
  try {
    // Default timeout, not the long server-render one: these pages are
    // regenerated in the background, and a teaser is not worth holding them for.
    entries = (await hallOfFameService.list()).slice(0, TEASER_COUNT);
  } catch {
    return null;
  }

  if (entries.length === 0) return null;

  return (
    <Section className="pb-16">
      <SectionHeader
        eyebrow="Hall of fame"
        title={title}
        action={{ href: "/hall-of-fame", label: "All years" }}
      />
      {/* Cards share the row rather than sitting at a fixed width, so one or
          two entries do not look like content that failed to load. They still
          scroll once there are enough to overflow. */}
      <ul className="no-scrollbar mt-8 flex gap-4 overflow-x-auto pb-2">
        {entries.map((entry) => {
          const photo = entry.photos[0];
          return (
            <li
              key={entry.id}
              className="group relative min-w-64 max-w-80 flex-1 overflow-hidden rounded-panel border border-hairline bg-surface transition-all hover:-translate-y-0.5 hover:border-border hover:bg-surface-3 hover:shadow-panel"
            >
              <div className="flex items-start gap-4 p-5">
                {/* Compact square thumbnail — no giant aspect-ratio block */}
                {photo ? (
                  <span className="relative block size-14 shrink-0 overflow-hidden rounded-md border border-hairline bg-surface-2">
                    <Image
                      src={photo.imageUrl}
                      alt=""
                      fill
                      sizes="56px"
                      className="object-cover transition-transform duration-500 group-hover:scale-110"
                    />
                  </span>
                ) : (
                  /* Placeholder icon when no photo */
                  <span className="flex size-14 shrink-0 items-center justify-center rounded-md border border-hairline bg-surface-2 text-2xl">
                    🏆
                  </span>
                )}

                <div className="min-w-0 flex-1">
                  <span className="font-mono text-label tracking-caps text-fg-subtle uppercase">
                    {monthYear(entry.achievedOn)}
                  </span>
                  <p className="mt-1.5 text-sm font-semibold leading-[1.4] tracking-tight text-pretty">
                    <Link href={`/hall-of-fame/${entry.id}`} className="after:absolute after:inset-0">
                      {entry.heading}
                    </Link>
                  </p>
                  {entry.subheading && (
                    <p className="mt-1 line-clamp-2 text-xs leading-[1.5] text-fg-muted text-pretty">
                      {entry.subheading}
                    </p>
                  )}
                </div>
              </div>

              {/* Subtle bottom strip that grows on hover */}
              <div className="h-0.5 w-0 bg-primary transition-all duration-300 group-hover:w-full" aria-hidden />
            </li>
          );
        })}
      </ul>
    </Section>
  );
}
