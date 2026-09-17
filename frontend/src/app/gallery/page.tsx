import type { Metadata } from "next";
import Link from "next/link";
import { ArrowRight } from "lucide-react";

import { DomeGalleryStage } from "@/components/site/dome-gallery-stage";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { galleryService } from "@/lib/services/gallery";
import type { GalleryPhoto } from "@/types/api";

export const metadata: Metadata = {
  title: "Gallery",
  description:
    "Photos from Programming Club @ DAU events and achievements at Dhirubhai Ambani University.",
};

// Photos added to an event this morning have to appear without a redeploy.
export const dynamic = "force-dynamic";

export default async function GalleryPage() {
  let photos: GalleryPhoto[] = [];
  let unreachable = false;

  try {
    photos = await galleryService.listPhotos();
  } catch (error) {
    console.error("Gallery page: could not load photos:", error);
    unreachable = true;
  }

  return (
    <>
      <Section className="pt-10 pb-10 md:pt-14">
        <Eyebrow>Gallery</Eyebrow>
        <PageTitle className="max-w-[20ch]">Where we were.</PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Drag to look around, then open a photo to see which event or achievement it was
          from, when it happened, and where.
        </p>
        <Link
          href="/gallery/batches"
          className="mt-4 inline-flex items-center gap-1.5 font-mono text-xs tracking-caps text-primary uppercase hover:underline"
        >
          Batch photos <ArrowRight className="size-3.5" />
        </Link>
      </Section>

      {unreachable ? (
        <Section className="pb-22">
          <div className="rounded-panel border border-dashed border-destructive/40 bg-destructive/5 py-12 text-center">
            <p className="text-sm text-destructive">Could not load the gallery.</p>
            <p className="mt-1 text-xs text-fg-muted">
              The server may be waking up. Reload in a few seconds.
            </p>
          </div>
        </Section>
      ) : (
        <DomeGalleryStage photos={photos} />
      )}
    </>
  );
}
