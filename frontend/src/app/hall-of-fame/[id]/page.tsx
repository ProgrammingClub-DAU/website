/**
 * One Hall of Fame entry.
 *
 * Where a gallery photo of an achievement leads, and where a timeline card
 * opens: the full write-up, every link, and every photo.
 */

import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, CalendarDays, ExternalLink } from "lucide-react";

import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { PhotoGrid } from "@/components/site/photo-grid";
import { hallOfFameService } from "@/lib/services/hall-of-fame";
import type { HallOfFameEntry } from "@/types/api";

export const dynamic = "force-dynamic";

async function loadEntry(id: string): Promise<HallOfFameEntry | null> {
  const numeric = Number(id);
  if (!Number.isInteger(numeric) || numeric <= 0) return null;
  try {
    return await hallOfFameService.get(numeric, { serverRender: true });
  } catch {
    return null;
  }
}

/** "2026-01-10" -> "10 January 2026", read from the string so no timezone moves it. */
function formatLongDate(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  });
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const entry = await loadEntry((await params).id);
  if (!entry) return { title: "Not found" };
  return {
    title: entry.heading,
    description: entry.subheading ?? entry.details?.slice(0, 160) ?? "A Programming Club @ DAU achievement.",
  };
}

export default async function HallOfFameEntryPage({ params }: { params: Promise<{ id: string }> }) {
  const entry = await loadEntry((await params).id);
  if (!entry) notFound();

  const date = formatLongDate(entry.achievedOn);

  return (
    <>
      <Section className="pt-10 pb-8 md:pt-14">
        <Link
          href="/hall-of-fame"
          className="inline-flex items-center gap-1.5 font-mono text-micro tracking-caps-wide text-fg-muted uppercase transition-colors hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" /> Hall of Fame
        </Link>

        <div className="mt-5">
          <Eyebrow>Hall of fame</Eyebrow>
          <PageTitle className="max-w-[24ch]">{entry.heading}</PageTitle>
        </div>

        {entry.subheading && (
          <p className="mt-4 text-lg text-foreground text-pretty">{entry.subheading}</p>
        )}

        <p className="mt-4 inline-flex items-center gap-1.5 font-mono text-xs text-fg-muted">
          <CalendarDays className="size-3.5 text-primary" />
          {date}
        </p>

        {entry.details && (
          <p className="mt-6 max-w-[64ch] text-base leading-7 whitespace-pre-line text-fg-muted text-pretty">
            {entry.details}
          </p>
        )}

        {entry.links.length > 0 && (
          <ul className="mt-6 flex flex-wrap gap-2">
            {entry.links.map((link) => (
              <li key={link.url}>
                <a
                  href={link.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 rounded-full border border-border bg-surface-2 px-3.5 py-1.5 text-xs font-medium text-foreground transition-colors hover:border-primary hover:text-primary"
                >
                  {link.label}
                  <ExternalLink className="size-3" />
                </a>
              </li>
            ))}
          </ul>
        )}
      </Section>

      {entry.photos.length > 0 && (
        <Section className="pb-22">
          <h2 className="mb-4 font-mono text-xs font-bold tracking-caps-wide text-primary uppercase">
            Photos
          </h2>
          <PhotoGrid
            photos={entry.photos.map((photo) => ({
              id: photo.id,
              imageUrl: photo.imageUrl,
              caption: photo.caption,
              title: entry.heading,
              meta: date,
            }))}
          />
        </Section>
      )}
    </>
  );
}
