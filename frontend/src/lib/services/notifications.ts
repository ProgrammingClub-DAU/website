import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";

export interface UnsubscribeResult {
  kind: string; // "EVENTS" or "CONTESTS"
}

export const notificationsService = {
  /**
   * Unsubscribes from email notifications using a signed single-click or page token.
   */
  async unsubscribe(token: string): Promise<UnsubscribeResult> {
    const res = await apiClient.get<ApiResponse<UnsubscribeResult>>("/api/notifications/unsubscribe", {
      params: { token },
    });
    return res.data.data;
  },
};
