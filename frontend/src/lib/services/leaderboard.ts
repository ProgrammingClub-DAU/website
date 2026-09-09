import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type {
  LeaderboardEntry,
  LeaderboardFilter,
  LeaderboardPlatform,
} from "@/types/api";

interface PagedLeaderboardResponse {
  content: LeaderboardEntry[];
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
    return response.data.data.content;
  },
};
