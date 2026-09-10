/**
 * Event photo gallery content.
 *
 * Photos are plain files under `public/gallery/`, referenced by absolute path
 * (`/gallery/<folder>/<file>.jpg`). Nothing here reads from the backend: Phase 1
 * has no Event or Photo entity, so the club's own record of what happened lives
 * in this file until one exists.
 *
 * To add an album: drop the images in `public/gallery/<slug>/`, add an entry
 * below, and set `cover` to the photo that should represent it in the grid.
 *
 * Every file listed below is currently absent — no club photos have been
 * collected yet — so each tile falls back to `placeholderFor` at the bottom of
 * this file. That fallback is why an entry added before its photos arrive
 * degrades rather than breaks, and it is a real function rather than a claim in
 * a comment: an earlier version of this header promised the behaviour without
 * anything implementing it, and the page showed six identical broken images.
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
  /**
   * Human-readable, matching the style used in events.ts ("March 2025").
   *
   * Optional, along with {@link GalleryAlbum.venue}: an album whose date or
   * venue nobody has confirmed omits the field rather than carrying a
   * "[PLACEHOLDER] Month, Year" that renders on the page as though it were the
   * record. The dome joins whichever of the two exist and shows neither when
   * both are missing.
   */
  date?: string;
  venue?: string;
  /** One or two lines shown beside the photos when the album is open. */
  summary: string;
  /** Photo shown in the grid. Usually the first entry of `photos`. */
  cover: string;
  photos: GalleryPhoto[];
};

/**
 * Albums for events the club has actually run.
 *
 * No photographs have been collected yet, so every `src` below is absent and
 * each tile falls back to `placeholderFor`, which draws an "awaiting upload"
 * card in the album's own colour. That is a designed pending state rather than
 * a fabrication: it says plainly that the photo is not there.
 *
 * The copy is not. This file previously carried an invented third album — an
 * "[PLACEHOLDER] Intro to CP Workshop" that never happened — and marked every
 * venue, summary and alt string [PLACEHOLDER] while the page presented them as
 * the club's record of the event. Fields nobody has confirmed are now omitted
 * instead, and the invented album is gone.
 *
 * To add an album: drop the images in `public/gallery/<slug>/`, add an entry
 * here, and write real `alt` text describing each photo as you add it.
 */
export const galleryAlbums: GalleryAlbum[] = [
  {
    id: "spring-code-sprint-2025",
    title: "Spring Code Sprint",
    date: "March 2025",
    summary:
      "The club's annual spring contest. Five problems, two hours, open to all skill levels.",
    cover: "/gallery/spring-code-sprint-2025/01.jpg",
    photos: [
      { src: "/gallery/spring-code-sprint-2025/01.jpg", alt: "Spring Code Sprint — photo not yet uploaded" },
      { src: "/gallery/spring-code-sprint-2025/02.jpg", alt: "Spring Code Sprint — photo not yet uploaded" },
      { src: "/gallery/spring-code-sprint-2025/03.jpg", alt: "Spring Code Sprint — photo not yet uploaded" },
    ],
  },
  {
    id: "icpc-amritapuri-regionals",
    title: "ICPC Amritapuri Regionals",
    venue: "Amritapuri",
    summary: "Team DAUCoders at the regional round.",
    cover: "/gallery/icpc-amritapuri-regionals/01.jpg",
    photos: [
      { src: "/gallery/icpc-amritapuri-regionals/01.jpg", alt: "ICPC Amritapuri Regionals — photo not yet uploaded" },
      { src: "/gallery/icpc-amritapuri-regionals/02.jpg", alt: "ICPC Amritapuri Regionals — photo not yet uploaded" },
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
  date?: string;
  venue?: string;
  /** Shown when `src` fails to load. See `placeholderFor` below. */
  placeholder: string;
};

/**
 * A self-contained tile for a photo that is not there yet.
 *
 * An SVG data URI rather than a shared placeholder file, for one reason: it
 * carries the album's own name and colour, so the tiles are distinguishable
 * from one another. Six copies of the same grey rectangle look like a bug —
 * which is exactly how the missing files read before this existed.
 *
 * The colours are literal rather than tokens because an SVG in a data URI has
 * no document to resolve `var(--token)` against. They are the cool half of the
 * rank ladder, in the same order the club gradient uses them.
 */
const ALBUM_TINTS = ["#00c2c7", "#4c7dff", "#c066e0"];

function placeholderFor(albumIndex: number, title: string, photoNumber: number): string {
  const tint = ALBUM_TINTS[albumIndex % ALBUM_TINTS.length];
  // Escaped for an XML attribute: a title containing & or < would otherwise
  // produce an SVG the browser refuses to parse, and a silently blank tile.
  const label = title
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${tint}" stop-opacity="0.30"/>
      <stop offset="100%" stop-color="${tint}" stop-opacity="0.06"/>
    </linearGradient>
  </defs>
  <rect width="600" height="600" fill="#111113"/>
  <rect width="600" height="600" fill="url(#g)"/>
  <rect x="16" y="16" width="568" height="568" fill="none" stroke="${tint}" stroke-opacity="0.5" stroke-width="2" rx="14"/>
  <circle cx="300" cy="248" r="54" fill="none" stroke="${tint}" stroke-opacity="0.65" stroke-width="4"/>
  <circle cx="300" cy="248" r="20" fill="${tint}" fill-opacity="0.5"/>
  <text x="300" y="368" fill="#f7f8f8" font-family="system-ui,sans-serif" font-size="30" font-weight="600" text-anchor="middle">${label}</text>
  <text x="300" y="410" fill="${tint}" font-family="ui-monospace,monospace" font-size="21" letter-spacing="3" text-anchor="middle">PHOTO ${photoNumber}</text>
  <text x="300" y="452" fill="#8a8f98" font-family="ui-monospace,monospace" font-size="17" letter-spacing="2" text-anchor="middle">AWAITING UPLOAD</text>
</svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export const galleryTiles: GalleryTile[] = galleryAlbums.flatMap((album, albumIndex) =>
  album.photos.map((photo, photoIndex) => ({
    src: photo.src,
    alt: photo.alt,
    title: album.title,
    date: album.date,
    venue: album.venue,
    placeholder: placeholderFor(albumIndex, album.title, photoIndex + 1),
  }))
);
