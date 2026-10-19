import apiClient from "@/lib/axios";
import type { SyncHealthDto, SyncJobType, SyncRunRecord } from "@/types/api";

export const syncService = {
  /**
   * Admin: Fetches health/staleness status of all background sync jobs.
   */
  async getHealth(): Promise<SyncHealthDto[]> {
    const res = await apiClient.get<SyncHealthDto[]>("/api/admin/sync/health");
    return res.data;
  },

  /**
   * Admin: Fetches recent execution history of sync runs.
   */
  async getRuns(job?: SyncJobType, limit = 20): Promise<SyncRunRecord[]> {
    const params: Record<string, string | number> = { limit };
    if (job) params.job = job;
    const res = await apiClient.get<SyncRunRecord[]>("/api/admin/sync/runs", { params });
    return res.data;
  },

  /**
   * Admin: Triggers an immediate execution of a specific sync job.
   */
  async trigger(job: SyncJobType): Promise<number> {
    const res = await apiClient.post<number>(`/api/admin/sync/${job}`);
    return res.data;
  },
};
