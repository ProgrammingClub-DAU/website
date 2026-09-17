"use client";

/**
 * The gallery page's photo wall: the dome, and a plain grid beneath it.
 *
 * Both show the same photos, for different people. The dome is the showpiece --
 * drag to rotate, click to open -- but it is a pointer-driven 3D widget, awkward
 * on a phone and opaque to a screen reader. The grid is the way through for
 * everyone else, and on small screens it is the only view shown.
 *
 * Every photo, in either view, opens with its caption and a link back to the
 * event or Hall of Fame entry it belongs to.
 */

import dynamic from "next/dynamic";

import { PhotoGrid } from "@/components/site/photo-grid";
import type { LightboxPhoto } from "@/components/site/photo-lightbox";
import { gallerySourceHref, gallerySourceLabel } from "@/lib/services/gallery";
import type { GalleryPhoto } from "@/types/api";

/**
 * Loaded on the client only: the component measures its container, reads
 * devicePixelRatio and builds a 3D transform on mount, none of which the server
 * can do.
 */
const DomeGallery = dynamic(() => import("@/components/site/dome-gallery"), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse bg-surface-2" aria-hidden="true" />,
});

/** "2026-01-10" -> "10 Jan 2026", read from the string so no timezone moves it. */
function formatDay(isoDate: string | null): string | undefined {
  if (!isoDate) return undefined;
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

/**
 * The dome needs enough tiles to wrap a sphere, and repeats what it is given to
 * fill it. Below this many photos the repetition is obvious, so the dome is held
 * back until there is enough to show.
 */
const DOME_MINIMUM = 8;

export function DomeGalleryStage({ photos }: { photos: GalleryPhoto[] }) {
  if (photos.length === 0) {
    return (
      <div className="container-page pb-22">
        <div className="rounded-panel border border-dashed border-border px-4 py-14 text-center">
          <p className="text-sm text-fg-muted">No photos yet.</p>
          <p className="mt-1 text-xs text-fg-subtle">
            Photos added to events and Hall of Fame entries appear here.
          </p>
        </div>
      </div>
    );
  }

  const tiles = photos.map((photo) => ({
    src: photo.imageUrl,
    alt: photo.caption ?? photo.sourceTitle,
    caption: photo.caption ?? undefined,
    title: photo.sourceTitle,
    date: formatDay(photo.date),
    venue: photo.location ?? undefined,
    href: gallerySourceHref(photo.source, photo.sourceId),
    hrefLabel: gallerySourceLabel(photo.source),
  }));

  const gridPhotos: LightboxPhoto[] = photos.map((photo) => ({
    id: photo.id,
    imageUrl: photo.imageUrl,
    caption: photo.caption,
    title: photo.sourceTitle,
    meta: [formatDay(photo.date), photo.location].filter(Boolean).join(" · ") || undefined,
    href: gallerySourceHref(photo.source, photo.sourceId),
    hrefLabel: gallerySourceLabel(photo.source),
  }));

  const showDome = photos.length >= DOME_MINIMUM;

  return (
    <div className="container-page space-y-14 pb-22">
      {showDome && (
        <div className="hidden sm:block">
          <div className="relative h-[clamp(420px,68vh,760px)] w-full overflow-hidden rounded-panel border border-border">
            <DomeGallery
              images={tiles}
              fit={0.8}
              minRadius={600}
              maxVerticalRotationDeg={0}
              segments={34}
              dragDampening={2}
              grayscale={false}
              // Follows the theme instead of the component's hard-coded colour.
              overlayBlurColor="var(--background)"
              imageBorderRadius="14px"
              openedImageBorderRadius="14px"
              openedImageWidth="min(520px, 82vw)"
              openedImageHeight="min(520px, 62vh)"
            />
          </div>
          <p className="mt-4 font-mono text-label tracking-caps text-fg-subtle uppercase">
            Drag to rotate · click a photo for details · Esc to close
          </p>
        </div>
      )}

      <section>
        <div className="mb-5 flex items-baseline justify-between gap-3 border-b border-hairline pb-3">
          <h2 className="font-heading text-lg font-medium tracking-tight">
            {showDome ? "Browse all photos" : "Photos"}
          </h2>
          <span className="font-mono text-micro text-fg-subtle">
            {photos.length} {photos.length === 1 ? "photo" : "photos"}, newest first
          </span>
        </div>
        <PhotoGrid photos={gridPhotos} showCaptions={false} />
      </section>
    </div>
  );
}
