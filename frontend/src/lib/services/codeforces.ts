/**
 * Codeforces Public API Service
 *
 * Fetches live user data (avatar, maxRating) and full rating history directly
 * from the Codeforces API via Next.js server-side proxy routes (to avoid CORS).
 *
 * All functions return null / empty arrays on failure so the profile page
 * degrades gracefully if Codeforces is unreachable.
 */

import type { RatingHistoryEntry } from "@/types/api";

// ── Types matching the Codeforces API response shape ──

export interface CfUserInfo {
  handle: string;
  /** Full-size profile photo URL */
  titlePhoto: string;
  /** Thumbnail avatar URL */
  avatar: string;
  rating: number;
  maxRating: number;
  rank: string;
  maxRank: string;
  country?: string;
  city?: string;
  organization?: string;
}

interface CfRatingChange {
  contestId: number;
  contestName: string;
  handle: string;
  rank: number;
  ratingUpdateTimeSeconds: number;
  oldRating: number;
  newRating: number;
}

// ── Service ──

export const codeforcesService = {
  /**
   * Fetches live profile info (avatar, maxRating, rank) for a given handle.
   * Calls our server-side proxy at /api/cf/user-info to avoid browser CORS limits.
   *
   * @returns CfUserInfo on success, null on any failure
   */
  getUserInfo: async (handle: string): Promise<CfUserInfo | null> => {
    if (!handle || handle.trim() === "") return null;

    try {
      const res = await fetch(`/api/cf/user-info?handle=${encodeURIComponent(handle.trim())}`);
      if (!res.ok) return null;

      const data = await res.json();
      if (data.status === "OK" && Array.isArray(data.result) && data.result.length > 0) {
        return data.result[0] as CfUserInfo;
      }
      return null;
    } catch {
      // Codeforces is unreachable — fail silently
      return null;
    }
  },

  /**
   * Fetches the full contest rating history for a given handle.
   * Maps the CF response to our frontend RatingHistoryEntry shape.
   * Calls our server-side proxy at /api/cf/user-rating.
   *
   * @returns Array of RatingHistoryEntry (may be empty on failure or unrated user)
   */
  getRatingHistory: async (handle: string): Promise<RatingHistoryEntry[]> => {
    if (!handle || handle.trim() === "") return [];

    try {
      const res = await fetch(`/api/cf/user-rating?handle=${encodeURIComponent(handle.trim())}`);
      if (!res.ok) return [];

      const data = await res.json();
      if (data.status === "OK" && Array.isArray(data.result)) {
        return (data.result as CfRatingChange[]).map((entry) => ({
          // Convert Unix timestamp (seconds) → ISO date string (YYYY-MM-DD)
          date: new Date(entry.ratingUpdateTimeSeconds * 1000).toISOString().split("T")[0],
          rating: entry.newRating,
          contestName: entry.contestName,
        }));
      }
      return [];
    } catch {
      // Codeforces is unreachable — fail silently
      return [];
    }
  },
};
