import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { SeasonDto, SeasonRequest, ChampionshipDto } from "@/types/api";

export const seasonService = {
  /**
   * Fetches all seasons, newest first.
   */
  async list(): Promise<SeasonDto[]> {
    const res = await apiClient.get<ApiResponse<SeasonDto[]>>("/api/seasons");
    return res.data.data;
  },

  /**
   * Admin: Create a new championship season.
   */
  async create(data: SeasonRequest): Promise<SeasonDto> {
    const res = await apiClient.post<ApiResponse<SeasonDto>>("/api/seasons", data);
    return res.data.data;
  },

  /**
   * Admin: Update an existing season (dates, name, countBest).
   */
  async update(id: number | string, data: SeasonRequest): Promise<SeasonDto> {
    const res = await apiClient.put<ApiResponse<SeasonDto>>(`/api/seasons/${id}`, data);
    return res.data.data;
  },

  /**
   * Admin: Remove a season (contests are unlinked, not deleted).
   */
  async remove(id: number | string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/api/seasons/${id}`);
  },

  /**
   * Fetches championship standings and contest point breakdown for a season.
   */
  async getChampionship(id: number | string): Promise<ChampionshipDto> {
    const res = await apiClient.get<ApiResponse<ChampionshipDto>>(`/api/seasons/${id}/championship`);
    return res.data.data;
  },
};
