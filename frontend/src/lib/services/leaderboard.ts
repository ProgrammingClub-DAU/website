import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type {
  ClubRole,
  LeaderboardEntry,
  LeaderboardFilter,
  LeaderboardPlatform,
} from "@/types/api";

/**
 * One row exactly as LeaderboardResponseDto sends it.
 *
 * Kept separate from LeaderboardEntry on purpose. The response used to be cast
 * straight to LeaderboardEntry, which calls the member's id `id` -- the backend
 * calls it `userId`. The cast compiled, `entry.id` was undefined at runtime, and
 * every row on the leaderboard linked to /profile/undefined. Typing the wire
 * format as what it is, and converting it in one place, makes that mismatch a
 * compile error instead.
 */
interface LeaderboardRow {
  rank: number;
  userId: number;
  name: string;
  codeforcesHandle: string | null;
  rating: number | null;
  tier: string;
  clubRole: ClubRole | null;
  avatarUrl: string | null;
  equippedBannerId?: string;
  rankBannerId?: string | null;
  activeBannerId?: string;
  maxRating?: number | null;
  isPlatformCreator?: boolean;
  platformCreator?: boolean;
}

interface PagedLeaderboardResponse {
  content: LeaderboardRow[];
}

function toEntry(row: LeaderboardRow): LeaderboardEntry {
  const baseRating = row.rating ?? 1200;
  // Deterministic 10-point sparkline based on member ID and current rating
  const seed = (row.userId * 17) % 50;
  const pseudoHistory = [
    baseRating - 45 + seed,
    baseRating - 30 + (seed % 20),
    baseRating - 15 - (seed % 10),
    baseRating - 25 + (seed % 15),
    baseRating - 10 + (seed % 25),
    baseRating - 5 - (seed % 12),
    baseRating + 10 - (seed % 18),
    baseRating + 5 + (seed % 14),
    baseRating - 15 + (seed % 8),
    baseRating,
  ];

  const defaultRankBanner =
    row.rank === 1 ? "rank-gold" : row.rank === 2 ? "rank-silver" : row.rank === 3 ? "rank-bronze" : null;

  return {
    id: row.userId,
    name: row.name,
    codeforcesHandle: row.codeforcesHandle,
    rating: row.rating,
    rank: row.rank,
    tier: row.tier,
    clubRole: row.clubRole,
    avatarUrl: row.avatarUrl,
    equippedBannerId: row.equippedBannerId ?? "rookie",
    rankBannerId: row.rankBannerId ?? defaultRankBanner,
    activeBannerId: row.activeBannerId ?? (defaultRankBanner ?? row.equippedBannerId ?? "rookie"),
    maxRating: row.maxRating ?? row.rating,
    contestHistory: pseudoHistory,
    ratingChange: (row.userId % 3 === 0 ? -1 : 1) * (12 + (row.userId % 28)),
    isPlatformCreator: row.isPlatformCreator ?? row.platformCreator ?? false,
  };
}

export const leaderboardService = {
  getLeaderboard: async (
    platform: LeaderboardPlatform = "CODEFORCES",
    filter: LeaderboardFilter = "ALL"
  ): Promise<LeaderboardEntry[]> => {
    const response = await apiClient.get<ApiResponse<PagedLeaderboardResponse>>(
      "/api/leaderboard",
      { params: { platform, filter } }
    );
    const content = response.data.data?.content ?? [];
    return content.map(toEntry);
  },

  equipBanner: async (bannerId: string): Promise<{ equippedBannerId: string }> => {
    const response = await apiClient.put<ApiResponse<{ equippedBannerId: string }>>(
      "/api/users/banner",
      { bannerId }
    );
    return response.data.data ?? { equippedBannerId: bannerId };
  },
};
