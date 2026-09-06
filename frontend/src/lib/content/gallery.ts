/**
 * Event photo gallery content.
 *
 * Photos are plain files under `public/gallery/`, referenced by absolute path
 * (`/gallery/<folder>/<file>.jpg`). Nothing here reads from the backend: Phase 1
 * has no Event or Photo entity, so the club's own record of what happened lives
 * in this file until one exists.
 *
 * To add an album: drop the images in `public/gallery/<slug>/`, add an entry
 * below, and set `cover` to the photo that should represent it in the grid. The
 * gallery renders a labelled placeholder for any file that is missing, so an
 * entry added before its photos arrive degrades rather than breaks.
 */

export type GalleryPhoto = {
  /** Absolute path from the site root, e.g. `/gallery/spring-sprint/01.jpg`. */
  src: string;
  /** Describes the photo for screen readers. Not the event name — what is shown. */
  alt: string;
};

export type GalleryAlbum = {
  /** URL-safe identifier, also used as the deep-link hash. */
  id: string;
  title: string;
  /** Human-readable, matching the style used in events.ts ("March 2025"). */
  date: string;
  venue: string;
  /** One or two lines shown beside the photos when the album is open. */
  summary: string;
  /** Photo shown in the grid. Usually the first entry of `photos`. */
  cover: string;
  photos: GalleryPhoto[];
};

/**
 * Placeholder albums pending real club photos.
 *
 * The copy is marked [PLACEHOLDER] in the same way as events.ts and
 * hall-of-fame.ts, so unverified content is obvious on the page rather than
 * being mistaken for a record of something that happened.
 */
export const galleryAlbums: GalleryAlbum[] = [
  {
    id: "spring-code-sprint-2025",
    title: "Spring Code Sprint",
    date: "March 2025",
    venue: "[PLACEHOLDER] Lab 101, DAU",
    summary: "[PLACEHOLDER] Annual spring contest — 5 problems, 2 hours, open to all skill levels.",
    cover: "/gallery/spring-code-sprint-2025/01.jpg",
    photos: [
      { src: "/gallery/spring-code-sprint-2025/01.jpg", alt: "[PLACEHOLDER] Participants at their machines during the contest" },
      { src: "/gallery/spring-code-sprint-2025/02.jpg", alt: "[PLACEHOLDER] Scoreboard on the projector near the end of the round" },
      { src: "/gallery/spring-code-sprint-2025/03.jpg", alt: "[PLACEHOLDER] Prize giving after the contest" },
    ],
  },
  {
    id: "icpc-amritapuri-regionals",
    title: "ICPC Amritapuri Regionals",
    date: "[PLACEHOLDER] Month, Year",
    venue: "[PLACEHOLDER] Amritapuri",
    summary: "[PLACEHOLDER] Team DAUCoders at the regional round.",
    cover: "/gallery/icpc-amritapuri-regionals/01.jpg",
    photos: [
      { src: "/gallery/icpc-amritapuri-regionals/01.jpg", alt: "[PLACEHOLDER] The team before the contest" },
      { src: "/gallery/icpc-amritapuri-regionals/02.jpg", alt: "[PLACEHOLDER] Working through a problem at the table" },
    ],
  },
  {
    id: "intro-to-cp-workshop",
    title: "[PLACEHOLDER] Intro to CP Workshop",
    date: "[PLACEHOLDER] Month, Year",
    venue: "[PLACEHOLDER] Seminar hall",
    summary: "[PLACEHOLDER] First-year session on setup, complexity and the standard library.",
    cover: "/gallery/intro-to-cp-workshop/01.jpg",
    photos: [
      { src: "/gallery/intro-to-cp-workshop/01.jpg", alt: "[PLACEHOLDER] Session in progress" },
    ],
  },
];

/**
 * Every photo across all albums, flattened and carrying its event's details.
 *
 * The dome gallery shows one wall of photos rather than album covers, so each
 * tile has to know which event it came from — that is what gets named when a
 * photo is opened.
 */
export type GalleryTile = {
  src: string;
  alt: string;
  title: string;
  date: string;
  venue: string;
};

export const galleryTiles: GalleryTile[] = galleryAlbums.flatMap((album) =>
  album.photos.map((photo) => ({
    src: photo.src,
    alt: photo.alt,
    title: album.title,
    date: album.date,
    venue: album.venue,
  }))
);
