import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { CpGroupMemberDetail, CpGroupMemberRow, CpGroupMyMembership } from "@/types/api";

/**
 * The CP Practice Group. Everything under /api/admin/cp-group is admin-only on the
 * backend; myMembership is the one call a student makes.
 */
export const cpGroupService = {
  async list(): Promise<CpGroupMemberRow[]> {
    const res = await apiClient.get<ApiResponse<CpGroupMemberRow[]>>("/api/admin/cp-group/members");
    return res.data.data;
  },

  async detail(userId: number | string): Promise<CpGroupMemberDetail> {
    const res = await apiClient.get<ApiResponse<CpGroupMemberDetail>>(`/api/admin/cp-group/members/${userId}`);
    return res.data.data;
  },

  /** Adds students to a batch; a student already in the group moves to it. */
  async add(userIds: number[], batchYear: number): Promise<{ added: number; moved: number }> {
    const res = await apiClient.post<ApiResponse<{ added: number; moved: number }>>(
      "/api/admin/cp-group/members",
      { userIds, batchYear }
    );
    return res.data.data;
  },

  /** Changes the batch and/or notes. Empty notes clear them. */
  async update(userId: number, patch: { batchYear?: number; notes?: string }): Promise<void> {
    await apiClient.patch(`/api/admin/cp-group/members/${userId}`, patch);
  },

  async remove(userId: number): Promise<void> {
    await apiClient.delete(`/api/admin/cp-group/members/${userId}`);
  },

  async myMembership(): Promise<CpGroupMyMembership> {
    const res = await apiClient.get<ApiResponse<CpGroupMyMembership>>("/api/cp-group/me");
    return res.data.data;
  },
};
