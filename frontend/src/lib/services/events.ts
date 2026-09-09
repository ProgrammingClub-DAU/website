import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { Event, EventAttendee, EventDetail, UserLookup } from "@/types/api";

export interface EventRequest {
  title: string;
  description: string | null;
  eventDate: string;
  location: string;
  coverImageUrl: string | null;
}

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

  createEvent: async (data: EventRequest): Promise<Event> => {
    const response = await apiClient.post<ApiResponse<Event>>("/api/events", data);
    return response.data.data;
  },

  updateEvent: async (id: number, data: EventRequest): Promise<Event> => {
    const response = await apiClient.put<ApiResponse<Event>>(`/api/events/${id}`, data);
    return response.data.data;
  },

  markCompleted: async (id: number): Promise<Event> => {
    const response = await apiClient.put<ApiResponse<Event>>(`/api/events/${id}/complete`);
    return response.data.data;
  },

  cancel: async (id: number): Promise<Event> => {
    const response = await apiClient.put<ApiResponse<Event>>(`/api/events/${id}/cancel`);
    return response.data.data;
  },

  addAttendee: async (eventId: number, userId: number): Promise<EventAttendee> => {
    const response = await apiClient.post<ApiResponse<EventAttendee>>(
      `/api/events/${eventId}/attendees`,
      { userId }
    );
    return response.data.data;
  },

  removeAttendee: async (eventId: number, userId: number): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<void>>(
      `/api/events/${eventId}/attendees/${userId}`
    );
    return response.data.data;
  },

  getAttendees: async (eventId: number): Promise<EventAttendee[]> => {
    const response = await apiClient.get<ApiResponse<EventAttendee[]>>(
      `/api/events/${eventId}/attendees`
    );
    return response.data.data;
  },

  lookupUser: async (userId: number): Promise<UserLookup> => {
    const response = await apiClient.get<ApiResponse<UserLookup>>(`/api/users/${userId}/lookup`);
    return response.data.data;
  },
};
