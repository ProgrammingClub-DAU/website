import apiClient from "@/lib/axios";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@/store/auth";
import type { Event, EventAttendee, EventDetail, EventPhoto, EventType, UserLookup } from "@/types/api";

export interface EventRequest {
  title: string;
  description: string | null;
  eventDate: string;
  location: string;
  coverImageUrl: string | null;
  /** The badge on the public timeline. Null simply means no badge. */
  eventType: EventType | null;
  /** Optional. No contest means no results section on the public page at all. */
  codeforcesContestUrl: string | null;
  /**
   * What the public may see. Sent on every save, including creation, because the
   * backend replaces all three -- omitting one would turn it off rather than
   * leave it alone.
   */
  showContestLink: boolean;
  showWinners: boolean;
  showAttendeeCount: boolean;
}

/** One placing, as the admin sets it. */
export interface WinnerRequest {
  position: number;
  userId: number;
}

export interface EventPhotoRequest {
  imageUrl: string;
  caption: string | null;
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

  /**
   * Replaces an event's podium.
   *
   * The whole podium goes at once: the rules the server enforces are between
   * the placings, so it needs the complete list. An empty array clears it.
   *
   * Recording winners does not announce them -- that is the showWinners switch.
   */
  setWinners: async (eventId: number, winners: WinnerRequest[]): Promise<EventDetail> => {
    const response = await apiClient.put<ApiResponse<EventDetail>>(
      `/api/events/${eventId}/winners`,
      { winners }
    );
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

  exportAttendees: (eventId: number): Promise<AxiosResponse<Blob>> =>
    apiClient.get<Blob>(`/api/events/${eventId}/attendees/export`, {
      responseType: "blob",
    }),

  /**
   * The attendance sheet as plain rows, header first.
   *
   * Comes from the server rather than being assembled here, even though the
   * browser already has the attendee list: the column rules -- student ID out of
   * the address, the Codeforces link, the year in words -- are defined once in
   * EventExportService and tested there. Rebuilding them in TypeScript would let
   * the Google Sheet and the .xlsx quietly disagree.
   */
  getAttendanceSheetRows: async (eventId: number): Promise<string[][]> => {
    const response = await apiClient.get<ApiResponse<string[][]>>(
      `/api/events/${eventId}/attendees/sheet`
    );
    return response.data.data;
  },

  addEventPhoto: async (eventId: number, data: EventPhotoRequest): Promise<EventPhoto> => {
    const response = await apiClient.post<ApiResponse<EventPhoto>>(
      `/api/events/${eventId}/photos`,
      data
    );
    return response.data.data;
  },

  deleteEventPhoto: async (photoId: number): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/events/photos/${photoId}`);
    return response.data.data;
  },

  getEventPhotos: async (eventId: number): Promise<EventPhoto[]> => {
    const response = await apiClient.get<ApiResponse<EventPhoto[]>>(
      `/api/events/${eventId}/photos`
    );
    return response.data.data;
  },

  /**
   * Finds members for the attendance panel.
   *
   * Takes what an admin can read off a student card: the student ID in front
   * of a DAU address, so 202401226 resolves 202401226@dau.ac.in. A full email,
   * a name or a Codeforces handle also match, for members who joined with
   * another address.
   */
  lookupMembers: async (query: string): Promise<UserLookup[]> => {
    const response = await apiClient.get<ApiResponse<UserLookup[]>>("/api/users/lookup", {
      params: { query },
    });
    return response.data?.data ?? [];
  },

  lookupUser: async (userId: number): Promise<UserLookup> => {
    const response = await apiClient.get<ApiResponse<UserLookup>>(`/api/users/${userId}/lookup`);
    return response.data.data;
  },
};
