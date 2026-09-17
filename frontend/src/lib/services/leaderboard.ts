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
}

interface PagedLeaderboardResponse {
  content: LeaderboardRow[];
}

function toEntry(row: LeaderboardRow): LeaderboardEntry {
  return {
    id: row.userId,
    name: row.name,
    codeforcesHandle: row.codeforcesHandle,
    rating: row.rating,
    rank: row.rank,
    tier: row.tier,
    clubRole: row.clubRole,
    avatarUrl: row.avatarUrl,
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
    return (response.data.data?.content ?? []).map(toEntry);
  },
};
