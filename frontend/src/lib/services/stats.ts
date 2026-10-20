import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type {
  StatsMetric,
  StatsPlatform,
  StatsPeriod,
  LeaderboardFilter,
  StatsLeaderboardResponse,
  StatsLeaderboardEntry,
  MemberStats,
  PlatformStatsDetail,
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
    period = "ALL_TIME",
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

    const res = await apiClient.get<ApiResponse<StatsLeaderboardResponse & { entries?: StatsLeaderboardEntry[] }>>(
      `/api/leaderboard/stats?${params.toString()}`
    );
    const data = res.data.data;
    const list = data.content ?? data.entries ?? [];
    return {
      ...data,
      content: list,
      entries: list,
      pageNumber: data.pageNumber ?? (data as any).page ?? page,
      pageSize: data.pageSize ?? (data as any).size ?? size,
    };
  },

  /**
   * Fetches full member statistics for a specific period.
   * Endpoint: /api/stats/users/{id}?period={period}
   *
   * The backend serialises platform data as `platforms: PlatformStatsDetail[]`
   * (an array keyed by platform name). The frontend MemberStats type expects
   * top-level `codeforces` and `leetcode` named fields. We reshape the response
   * here so every component gets the expected structure without backend changes.
   */
  getMemberStats: async (
    userId: number,
    period: StatsPeriod = "ALL_TIME"
  ): Promise<MemberStats> => {
    const res = await apiClient.get<ApiResponse<MemberStats & { platforms?: PlatformStatsDetail[] }>>(
      `/api/stats/users/${userId}?period=${period}`
    );
    const raw = res.data.data;

    // Backend returns a `platforms` array — reshape into named top-level fields
    // that the MemberStats interface (and every component) expects.
    if (Array.isArray((raw as { platforms?: unknown }).platforms)) {
      const platforms = (raw as { platforms: PlatformStatsDetail[] }).platforms;
      return {
        ...raw,
        codeforces: platforms.find((p) => p.platform === "CODEFORCES") ?? null,
        leetcode:   platforms.find((p) => p.platform === "LEETCODE")   ?? null,
      };
    }

    // Already shaped correctly (future-proof if backend ever changes shape)
    return raw;
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
