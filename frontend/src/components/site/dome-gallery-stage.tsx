"use client";

/**
 * Hosts the React Bits dome gallery on the /gallery page.
 *
 * Kept separate from the vendored component so that file stays close to its
 * upstream source and is easy to re-diff when React Bits updates it. Everything
 * specific to this site -- sizing, theming, loading strategy, and turning API
 * photos into tiles -- lives here.
 *
 * Every photo opens with its caption, what it belongs to, and a link to that
 * event or Hall of Fame entry.
 */

import dynamic from "next/dynamic";

import { gallerySourceHref, gallerySourceLabel } from "@/lib/services/gallery";
import type { GalleryPhoto } from "@/types/api";

/**
 * Loaded on the client only.
 *
 * The component measures its container, reads `devicePixelRatio` and builds a
 * 3D transform on mount, none of which the server can do. Rendering it eagerly
 * would also put a gesture-driven widget in front of first paint on a page
 * whose text is the part worth showing quickly.
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

export function DomeGalleryStage({ photos }: { photos: GalleryPhoto[] }) {
  if (photos.length === 0) {
    return (
      <div className="container-page pb-22">
        <div className="rounded-panel border border-dashed border-border px-4 py-14 text-center">
          <p className="text-sm text-fg-muted">No photos yet.</p>
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

  return (
    <div className="container-page pb-22">
      <div className="relative h-[clamp(420px,68vh,760px)] w-full overflow-hidden rounded-panel border border-border">
        <DomeGallery
          images={tiles}
          fit={0.8}
          minRadius={600}
          maxVerticalRotationDeg={0}
          segments={34}
          dragDampening={2}
          grayscale={false}
          // Follows the theme instead of the component's hard-coded #120F17.
          // Every use of this value lands in a CSS context -- a custom property,
          // a gradient stop, a background-color -- so a var() reference resolves
          // correctly and tracks the light/dark switch with no JS involved.
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
  );
}
