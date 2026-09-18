/**
 * Batch photos -- the club's members, year by year.
 *
 * Admins have been able to upload these since Phase 2 (Admin -> Galleries ->
 * Members), but no public page showed them, so every upload was invisible.
 *
 * A page of its own rather than part of the gallery dome: a batch photo belongs
 * to a year, not to an event or an achievement, so in the dome it would be a
 * tile with nowhere to link, and the dome cannot be browsed by year.
 *
 * The year is a query parameter, so each batch has a shareable address and the
 * page renders on the server with no client-side fetching.
 */

import type { Metadata } from "next";
import Link from "next/link";

import { GalleryTabs } from "@/components/site/gallery-tabs";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { PhotoGrid } from "@/components/site/photo-grid";
import { galleryService } from "@/lib/services/gallery";
import { cn } from "@/lib/utils";
import type { MemberGalleryPhoto } from "@/types/api";

export const metadata: Metadata = {
  title: "Batch photos",
  description: "Photos of each batch of the Programming Club @ DAU.",
};

export const dynamic = "force-dynamic";

export default async function BatchGalleryPage({
  searchParams,
}: {
  searchParams: Promise<{ year?: string }>;
}) {
  const requested = Number((await searchParams).year);

  let years: number[] = [];
  let photos: MemberGalleryPhoto[] = [];
  let unreachable = false;
  let selected: number | null = null;

  try {
    // Newest batch first, whatever order the server returns.
    years = [...(await galleryService.getAvailableBatchYears({ serverRender: true }))].sort((a, b) => b - a);

    // An unknown or missing year falls back to the newest batch rather than an
    // empty page -- a stale link should still land somewhere useful.
    selected = years.includes(requested) ? requested : (years[0] ?? null);

    if (selected !== null) {
      photos = await galleryService.getPhotosByBatch(selected, { serverRender: true });
    }
  } catch (error) {
    console.error("Batch gallery: could not load photos:", error);
    unreachable = true;
  }

  return (
    <>
      <Section className="pt-10 pb-8 md:pt-14">
        <Eyebrow>Gallery</Eyebrow>
        <PageTitle className="max-w-[20ch]">Every batch, together.</PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Photos of the club&apos;s members, by the year they joined.
        </p>
        <GalleryTabs active="batches" />
      </Section>

      <Section className="pb-22">
        {unreachable ? (
          <div className="rounded-panel border border-dashed border-destructive/40 bg-destructive/5 py-12 text-center">
            <p className="text-sm text-destructive">Could not load batch photos.</p>
            <p className="mt-1 text-xs text-fg-muted">The server may be waking up. Reload in a few seconds.</p>
          </div>
        ) : years.length === 0 ? (
          <div className="rounded-panel border border-dashed border-border py-12 text-center">
            <p className="text-sm text-fg-muted">No batch photos yet.</p>
          </div>
        ) : (
          <>
            {/* Plain links, so each batch has its own address and works without JavaScript. */}
            <nav aria-label="Batch year" className="mb-8 flex flex-wrap gap-2">
              {years.map((year) => (
                <Link
                  key={year}
                  href={`/gallery/batches?year=${year}`}
                  aria-current={year === selected ? "page" : undefined}
                  className={cn(
                    "rounded-full border px-4 py-1.5 font-mono text-xs transition-colors",
                    year === selected
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border bg-surface-2 text-fg-muted hover:text-foreground"
                  )}
                >
                  Batch {year}
                </Link>
              ))}
            </nav>

            {photos.length === 0 ? (
              <div className="rounded-panel border border-dashed border-border py-12 text-center">
                <p className="text-sm text-fg-muted">No photos for batch {selected} yet.</p>
              </div>
            ) : (
              <PhotoGrid
                photos={photos.map((photo) => ({
                  id: photo.id,
                  imageUrl: photo.imageUrl,
                  caption: photo.caption,
                  title: `Batch ${photo.batchYear}`,
                }))}
              />
            )}
          </>
        )}
      </Section>
    </>
  );
}
