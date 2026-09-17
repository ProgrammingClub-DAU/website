"use client";

/**
 * A grid of thumbnails that open in the lightbox.
 *
 * The one photo layout on the site's detail pages and under the gallery dome,
 * so every photo behaves the same: a tap opens it large, with its caption and,
 * where it helps, a link to where it belongs.
 */

import { useState } from "react";
import Image from "next/image";

import { PhotoLightbox, type LightboxPhoto } from "@/components/site/photo-lightbox";
import { cn } from "@/lib/utils";

export function PhotoGrid({
  photos,
  className,
  showCaptions = true,
}: {
  photos: LightboxPhoto[];
  className?: string;
  /** Captions under each thumbnail. Off for dense grids, where the lightbox shows them. */
  showCaptions?: boolean;
}) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  if (photos.length === 0) return null;

  return (
    <>
      <ul className={cn("grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4", className)}>
        {photos.map((photo, i) => (
          <li key={photo.id}>
            <button
              type="button"
              onClick={() => setOpenIndex(i)}
              className="group block w-full overflow-hidden rounded-panel border border-border bg-surface-2 text-left transition-all hover:border-hairline-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
            >
              <span className="relative block aspect-[4/3] w-full overflow-hidden bg-surface-3">
                <Image
                  src={photo.imageUrl}
                  alt={photo.caption ?? photo.title ?? ""}
                  fill
                  sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
                  className="object-cover transition-transform duration-500 group-hover:scale-105"
                />
              </span>
              {showCaptions && (photo.caption || photo.title) && (
                <span className="block px-3 py-2">
                  <span className="line-clamp-2 text-xs text-fg-muted">
                    {photo.caption ?? photo.title}
                  </span>
                </span>
              )}
            </button>
          </li>
        ))}
      </ul>

      <PhotoLightbox
        photos={photos}
        index={openIndex}
        onIndexChange={setOpenIndex}
        onClose={() => setOpenIndex(null)}
      />
    </>
  );
}
