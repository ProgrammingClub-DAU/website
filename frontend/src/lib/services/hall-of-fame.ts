import { apiClient } from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { HallOfFameEntry } from "@/types/api";

/** What the admin form sends. Mirrors HallOfFameEntryRequest. */
export interface HallOfFameEntryRequest {
  heading: string;
  subheading: string | null;
  details: string | null;
  /** "YYYY-MM-DD" */
  achievedOn: string;
  links: { label: string; url: string }[];
  photos: { imageUrl: string; caption: string | null }[];
}

/**
 * Long enough for a sleeping free-tier backend to wake. Only the public pages,
 * which render on the server, pass it.
 */
const SSR_TIMEOUT_MS = 60_000;

export const hallOfFameService = {
  /** Every entry, newest first. The order is the server's. */
  list: async (options?: { serverRender?: boolean }): Promise<HallOfFameEntry[]> => {
    const response = await apiClient.get<ApiResponse<HallOfFameEntry[]>>("/api/hall-of-fame", {
      timeout: options?.serverRender ? SSR_TIMEOUT_MS : undefined,
    });
    return response.data.data ?? [];
  },

  get: async (id: number, options?: { serverRender?: boolean }): Promise<HallOfFameEntry> => {
    const response = await apiClient.get<ApiResponse<HallOfFameEntry>>(`/api/hall-of-fame/${id}`, {
      timeout: options?.serverRender ? SSR_TIMEOUT_MS : undefined,
    });
    return response.data.data;
  },

  create: async (data: HallOfFameEntryRequest): Promise<HallOfFameEntry> => {
    const response = await apiClient.post<ApiResponse<HallOfFameEntry>>("/api/hall-of-fame", data);
    return response.data.data;
  },

  /** Replaces the entry whole, links and photos included. */
  update: async (id: number, data: HallOfFameEntryRequest): Promise<HallOfFameEntry> => {
    const response = await apiClient.put<ApiResponse<HallOfFameEntry>>(`/api/hall-of-fame/${id}`, data);
    return response.data.data;
  },

  remove: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/hall-of-fame/${id}`);
  },
};
