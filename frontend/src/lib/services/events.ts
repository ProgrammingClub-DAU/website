import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { Event, EventDetail } from "@/types/api";

export const eventsService = {
  listUpcoming: async (): Promise<Event[]> => {
    const response = await apiClient.get<ApiResponse<Event[]>>("/api/events/upcoming");
    return response.data.data;
  },

  listCompleted: async (): Promise<Event[]> => {
    const response = await apiClient.get<ApiResponse<Event[]>>("/api/events/completed");
    return response.data.data;
  },

  getAllAdmin: async (): Promise<Event[]> => {
    const response = await apiClient.get<ApiResponse<Event[]>>("/api/events");
    return response.data.data;
  },

  getEventDetail: async (id: number): Promise<EventDetail> => {
    const response = await apiClient.get<ApiResponse<EventDetail>>(`/api/events/${id}`);
    return response.data.data;
  },
};
