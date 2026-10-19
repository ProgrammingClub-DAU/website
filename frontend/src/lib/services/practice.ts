import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { PracticeSuggestionsResponseDto } from "@/types/api";

export const practiceService = {
  /**
   * Fetches personalized practice suggestions based on weak tags and current CF rating band.
   */
  async getSuggestions(): Promise<PracticeSuggestionsResponseDto> {
    const res = await apiClient.get<ApiResponse<PracticeSuggestionsResponseDto>>("/api/practice/suggestions");
    return res.data.data;
  },

  /**
   * Permanently skip a problem suggestion for the current member.
   */
  async skipProblem(problemId: number | string): Promise<void> {
    await apiClient.post(`/api/practice/suggestions/${problemId}/skip`);
  },
};
