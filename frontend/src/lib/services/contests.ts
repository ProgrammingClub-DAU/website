import apiClient, { SLOW_REQUEST_TIMEOUT_MS } from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type {
  ClubContestSummaryDto,
  ClubContestDetailDto,
  ClubContestCreateRequest,
  UpsolveMatrixDto,
  MyUpsolvesDto,
} from "@/types/api";

export const contestService = {
  /**
   * Fetches all registered club contests, newest first.
   */
  async list(): Promise<ClubContestSummaryDto[]> {
    const res = await apiClient.get<ApiResponse<ClubContestSummaryDto[]>>("/api/club-contests");
    return res.data.data;
  },

  /**
   * Fetches ranklist details and problems for a single club contest.
   */
  async get(id: number | string): Promise<ClubContestDetailDto> {
    const res = await apiClient.get<ApiResponse<ClubContestDetailDto>>(`/api/club-contests/${id}`);
    return res.data.data;
  },

  /**
   * Admin: Add a Codeforces contest (gym/mashup/group/official) as a club contest.
   */
  async create(data: ClubContestCreateRequest): Promise<ClubContestSummaryDto> {
    // Reads the contest from Codeforces before saving it.
    const res = await apiClient.post<ApiResponse<ClubContestSummaryDto>>("/api/club-contests", data, {
      timeout: SLOW_REQUEST_TIMEOUT_MS,
    });
    return res.data.data;
  },

  /**
   * Admin: Trigger an immediate standings sync from Codeforces for this contest.
   */
  async sync(id: number | string): Promise<ClubContestDetailDto> {
    // Several rate-limited Codeforces requests; see SLOW_REQUEST_TIMEOUT_MS.
    const res = await apiClient.post<ApiResponse<ClubContestDetailDto>>(`/api/club-contests/${id}/sync`, undefined, {
      timeout: SLOW_REQUEST_TIMEOUT_MS,
    });
    return res.data.data;
  },

  /**
   * Admin: Remove a club contest.
   */
  async remove(id: number | string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/api/club-contests/${id}`);
  },

  /**
   * Fetches upsolve status matrix (members x problems) for a club contest.
   */
  async getUpsolve(id: number | string): Promise<UpsolveMatrixDto> {
    const res = await apiClient.get<ApiResponse<UpsolveMatrixDto>>(`/api/club-contests/${id}/upsolve`);
    return res.data.data;
  },

  /**
   * Fetches the authenticated member's pending upsolves across club contests.
   */
  async getMyUpsolves(): Promise<MyUpsolvesDto> {
    const res = await apiClient.get<ApiResponse<MyUpsolvesDto>>("/api/club-contests/upsolve/me");
    return res.data.data;
  },
};
