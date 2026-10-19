import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type {
  StatsMetric,
  StatsPlatform,
  StatsPeriod,
  LeaderboardFilter,
  StatsLeaderboardResponse,
  MemberStats,
  RatingPointDto,
  CompareResult,
  TopicStrengthResponse,
} from "@/types/api";

export interface GetStatsLeaderboardParams {
  metric: StatsMetric;
  platform?: StatsPlatform;
  period?: StatsPeriod;
  filter?: LeaderboardFilter;
  page?: number;
  size?: number;
}

export const statsService = {
  /**
   * Fetches paginated stats leaderboard entries for a given metric, platform, and period.
   * Dynamic endpoint: /api/leaderboard/stats
   */
  getLeaderboard: async ({
    metric,
    platform = "OVERALL",
    period = "WEEK",
    filter = "ALL",
    page = 0,
    size = 50,
  }: GetStatsLeaderboardParams): Promise<StatsLeaderboardResponse> => {
    const params = new URLSearchParams({
      metric,
      period,
      filter,
      page: page.toString(),
      size: size.toString(),
    });

    if (platform) {
      params.set("platform", platform);
    }

    const res = await apiClient.get<ApiResponse<StatsLeaderboardResponse>>(
      `/api/leaderboard/stats?${params.toString()}`
    );
    return res.data.data;
  },

  /**
   * Fetches full member statistics for a specific period.
   * Endpoint: /api/stats/users/{id}?period={period}
   */
  getMemberStats: async (
    userId: number,
    period: StatsPeriod = "ALL_TIME"
  ): Promise<MemberStats> => {
    const res = await apiClient.get<ApiResponse<MemberStats>>(
      `/api/stats/users/${userId}?period=${period}`
    );
    return res.data.data;
  },

  /**
   * Fetches full contest-by-contest rating progression points for a user.
   * Endpoint: /api/stats/users/{id}/rating-history?platform={platform}
   */
  getRatingHistory: async (
    userId: number,
    platform?: StatsPlatform
  ): Promise<RatingPointDto[]> => {
    const query = platform ? `?platform=${platform}` : "";
    const res = await apiClient.get<ApiResponse<RatingPointDto[]>>(
      `/api/stats/users/${userId}/rating-history${query}`
    );
    return res.data.data ?? [];
  },

  /**
   * Fetches comparative head-to-head stats, rating history, and contest history between two members.
   * Endpoint: /api/stats/compare?a={aId}&b={bId}
   */
  compare: async (aId: number, bId: number): Promise<CompareResult> => {
    const res = await apiClient.get<ApiResponse<CompareResult>>(
      `/api/stats/compare?a=${aId}&b=${bId}`
    );
    return res.data.data;
  },

  /**
   * Fetches core CP topic strength and gap analysis for a member.
   * Endpoint: /api/stats/users/{id}/topics
   */
  getTopics: async (userId: number): Promise<TopicStrengthResponse> => {
    const res = await apiClient.get<ApiResponse<TopicStrengthResponse>>(
      `/api/stats/users/${userId}/topics`
    );
    return res.data.data;
  },
};
