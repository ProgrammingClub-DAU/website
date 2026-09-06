import type { Metadata } from "next";

import { DomeGalleryStage } from "@/components/site/dome-gallery-stage";
import { Eyebrow, PageTitle, Section } from "@/components/site/primitives";
import { galleryTiles } from "@/lib/content/gallery";

export const metadata: Metadata = {
  title: "Gallery",
  description:
    "Photos from Programming Club contests, workshops and ICPC trips at Dhirubhai Ambani University.",
};

export default function GalleryPage() {
  return (
    <>
      <Section className="pt-16 pb-10 md:pt-24">
        <Eyebrow>Gallery</Eyebrow>
        <PageTitle className="max-w-[20ch]">
          Where we were.
        </PageTitle>
        <p className="mt-6 max-w-[52ch] text-base leading-6 text-fg-muted text-pretty">
          Drag to look around, then open a photo to see which event it was, when
          it ran and where. Captions are placeholders until confirmed against
          club records.
        </p>
      </Section>

      <DomeGalleryStage tiles={galleryTiles} />
    </>
  );
}
