import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { LatestWeeklyAwardsResponseDto, WeeklyAwardDto } from "@/types/api";

export const awardsService = {
  /**
   * Fetches the latest computed weekly awards (Coder of the Week, Most Solved, Biggest Rating Jump).
   */
  async getLatest(): Promise<LatestWeeklyAwardsResponseDto> {
    const res = await apiClient.get<ApiResponse<LatestWeeklyAwardsResponseDto>>("/api/awards/weekly/latest");
    return res.data.data;
  },

  /**
   * Fetches weekly awards for a specific closed week (Monday IST: YYYY-MM-DD).
   */
  async getWeek(weekStart: string): Promise<WeeklyAwardDto[]> {
    const res = await apiClient.get<ApiResponse<WeeklyAwardDto[]>>(`/api/awards/weekly/${weekStart}`);
    return res.data.data;
  },

  /**
   * Fetches award history for a specific member.
   */
  async getForUser(userId: number | string): Promise<WeeklyAwardDto[]> {
    const res = await apiClient.get<ApiResponse<WeeklyAwardDto[]>>(`/api/awards/users/${userId}`);
    return res.data.data;
  },

  /**
   * Admin: Recompute awards for a closed week.
   */
  async recompute(weekStart: string): Promise<WeeklyAwardDto[]> {
    const res = await apiClient.post<ApiResponse<WeeklyAwardDto[]>>(`/api/admin/awards/weekly/${weekStart}/recompute`);
    return res.data.data;
  },
};
