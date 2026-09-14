import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";

export interface SnapshotEntry {
  date: string;
  rating: number;
}

export const snapshotService = {
  getCodeforcesSnapshots: async (userId: number | string): Promise<SnapshotEntry[]> => {
    const response = await apiClient.get<ApiResponse<SnapshotEntry[]>>(
      `/api/snapshots/${userId}/codeforces`
    );
    return response.data?.data || [];
  },

  getLeetCodeSnapshots: async (userId: number | string): Promise<SnapshotEntry[]> => {
    const response = await apiClient.get<ApiResponse<SnapshotEntry[]>>(
      `/api/snapshots/${userId}/leetcode`
    );
    return response.data?.data || [];
  },
};
