/**
 * Cached reads for the public pages, which render on the server.
 *
 * Those pages used to call the backend on every visit. The backend sleeps on
 * Render's free tier, so the first visitor after a quiet spell watched a loading
 * skeleton for up to a minute, and every visitor paid a round trip to Render.
 *
 * Each read here is kept in Next's data cache for FRESH_FOR_SECONDS. After that the
 * next visitor is still served the cached copy at once, and the cache refreshes
 * in the background. Only the very first read of an empty cache waits for the
 * backend.
 *
 * Two rules keep it honest:
 * - A failed read throws, and a thrown error is never cached. If a background
 *   refresh fails, the last good copy stays: a sleeping backend shows slightly
 *   old data, not an empty page.
 * - "Not found" is an answer, not a failure, so the detail reads cache it as null.
 *   Otherwise a deleted event would keep its page forever, since every refresh
 *   would fail and leave the old copy in place.
 *
 * Server only: the pages render signed out (no token on the server), so every
 * cached value is the public view and safe to share between visitors.
 */

import { unstable_cache } from "next/cache";
import { isAxiosError } from "axios";

import { leaderboardService } from "@/lib/services/leaderboard";
import { dashboardService } from "@/lib/services/dashboard";
import { eventsService } from "@/lib/services/events";
import { galleryService } from "@/lib/services/gallery";
import { hallOfFameService } from "@/lib/services/hall-of-fame";
import { awardsService } from "@/lib/services/awards";

/**
 * How stale a public page may be. An admin's change shows up within this long,
 * and a page is never more than this out of date while the backend is reachable.
 */
const FRESH_FOR_SECONDS = 60;

const options = { revalidate: FRESH_FOR_SECONDS };

/** The backend's answer that something does not exist, or is not public. */
function isNotFound(error: unknown): boolean {
  if (!isAxiosError(error)) return false;
  const status = error.response?.status;
  return status === 400 || status === 403 || status === 404;
}

async function orNullIfMissing<T>(load: () => Promise<T>): Promise<T | null> {
  try {
    return await load();
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

export const getPublicLeaderboard = unstable_cache(
  () => leaderboardService.getLeaderboard("CODEFORCES", "ALL"),
  ["public", "leaderboard", "codeforces", "all"],
  options
);

export const getPublicTeam = unstable_cache(() => dashboardService.getTeam(), ["public", "team"], options);

export const getPublicPlatformCreators = unstable_cache(
  () => dashboardService.getPlatformCreators(),
  ["public", "platform-creators"],
  options
);

export const getPublicUpcomingEvents = unstable_cache(
  () => eventsService.listUpcoming(),
  ["public", "events", "upcoming"],
  options
);

export const getPublicCompletedEvents = unstable_cache(
  () => eventsService.listCompleted(),
  ["public", "events", "completed"],
  options
);

/** Null when there is no such public event. */
export const getPublicEventDetail = unstable_cache(
  (id: number) => orNullIfMissing(() => eventsService.getEventDetail(id)),
  ["public", "events", "detail"],
  options
);

export const getPublicGalleryPhotos = unstable_cache(
  () => galleryService.listPhotos(),
  ["public", "gallery", "photos"],
  options
);

export const getPublicBatchYears = unstable_cache(
  () => galleryService.getAvailableBatchYears({ serverRender: true }),
  ["public", "gallery", "batch-years"],
  options
);

export const getPublicBatchPhotos = unstable_cache(
  (year: number) => galleryService.getPhotosByBatch(year, { serverRender: true }),
  ["public", "gallery", "batch-photos"],
  options
);

export const getPublicHallOfFame = unstable_cache(
  () => hallOfFameService.list({ serverRender: true }),
  ["public", "hall-of-fame", "list"],
  options
);

/** The latest weekly awards, for the home page's Weekly Highlights. */
export const getPublicLatestAwards = unstable_cache(
  () => awardsService.getLatest(),
  ["public", "awards", "weekly-latest"],
  options
);

/** Null when there is no such public entry. */
export const getPublicHallOfFameEntry = unstable_cache(
  (id: number) => orNullIfMissing(() => hallOfFameService.get(id, { serverRender: true })),
  ["public", "hall-of-fame", "detail"],
  options
);
