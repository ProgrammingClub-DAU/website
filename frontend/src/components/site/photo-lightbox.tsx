"use client";

/**
 * A photo viewer: one image, large, with what it is and where it belongs.
 *
 * Used by the gallery grid and by the photo sections on event and Hall of Fame
 * pages, so a photo opens the same way wherever it is clicked.
 *
 * Built on the native <dialog> element rather than a positioned div. That buys,
 * without any code here: focus moved into the dialog and back to the thumbnail
 * on close, Escape to close, the rest of the page made inert to screen readers
 * and keyboard, and a top layer that no z-index can accidentally sit above.
 */

import { useCallback, useEffect, useRef } from "react";
import Image from "next/image";
import Link from "next/link";
import { ArrowRight, ChevronLeft, ChevronRight, X } from "lucide-react";

export interface LightboxPhoto {
  id: string | number;
  imageUrl: string;
  caption: string | null;
  /** Bold line under the photo -- usually the event or achievement. */
  title?: string;
  /** Muted line after the title -- date, venue. */
  meta?: string;
  /** Where "view" goes. Omitted on the page the photo already belongs to. */
  href?: string;
  hrefLabel?: string;
}

export function PhotoLightbox({
  photos,
  index,
  onIndexChange,
  onClose,
}: {
  photos: LightboxPhoto[];
  /** Which photo is open, or null when closed. */
  index: number | null;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const open = index !== null && photos[index] !== undefined;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const step = useCallback(
    (delta: number) => {
      if (index === null || photos.length < 2) return;
      onIndexChange((index + delta + photos.length) % photos.length);
    },
    [index, photos.length, onIndexChange]
  );

  // Arrow keys page through. Escape is the dialog's own, surfaced through onClose.
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "ArrowRight") step(1);
      if (event.key === "ArrowLeft") step(-1);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, step]);

  const photo = open ? photos[index!] : null;

  return (
    <dialog
      ref={dialogRef}
      onClose={onClose}
      // A click on the backdrop lands on the dialog element itself.
      onClick={(event) => {
        if (event.target === dialogRef.current) onClose();
      }}
      aria-label={photo?.caption ?? photo?.title ?? "Photo"}
      className="m-auto max-h-none max-w-none bg-transparent p-0 backdrop:bg-black/85 backdrop:backdrop-blur-sm"
    >
      {photo && (
        <figure className="flex w-[min(92vw,960px)] flex-col overflow-hidden rounded-panel border border-border bg-surface shadow-2xl">
          <div className="relative h-[min(68vh,640px)] w-full bg-black">
            <Image
              key={photo.id}
              src={photo.imageUrl}
              alt={photo.caption ?? ""}
              fill
              sizes="92vw"
              className="object-contain"
              priority
            />

            <button
              type="button"
              onClick={onClose}
              aria-label="Close"
              className="absolute top-3 right-3 rounded-full bg-black/60 p-2 text-white transition-colors hover:bg-black/80"
            >
              <X className="size-4" />
            </button>

            {photos.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() => step(-1)}
                  aria-label="Previous photo"
                  className="absolute top-1/2 left-3 -translate-y-1/2 rounded-full bg-black/60 p-2 text-white transition-colors hover:bg-black/80"
                >
                  <ChevronLeft className="size-5" />
                </button>
                <button
                  type="button"
                  onClick={() => step(1)}
                  aria-label="Next photo"
                  className="absolute top-1/2 right-3 -translate-y-1/2 rounded-full bg-black/60 p-2 text-white transition-colors hover:bg-black/80"
                >
                  <ChevronRight className="size-5" />
                </button>
              </>
            )}
          </div>

          <figcaption className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
            <div className="min-w-0 space-y-1">
              {photo.caption && <p className="text-sm text-foreground text-pretty">{photo.caption}</p>}
              {(photo.title || photo.meta) && (
                <p className="text-xs text-fg-muted">
                  {photo.title && <span className="font-semibold text-foreground">{photo.title}</span>}
                  {photo.title && photo.meta && " · "}
                  {photo.meta}
                </p>
              )}
              {photos.length > 1 && (
                <p className="font-mono text-micro text-fg-subtle">
                  {index! + 1} / {photos.length}
                </p>
              )}
            </div>

            {photo.href && (
              <Link
                href={photo.href}
                className="inline-flex shrink-0 items-center gap-1.5 self-start rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground transition-opacity hover:opacity-90 sm:self-auto"
              >
                {photo.hrefLabel ?? "View"}
                <ArrowRight className="size-3.5" />
              </Link>
            )}
          </figcaption>
        </figure>
      )}
    </dialog>
  );
}
