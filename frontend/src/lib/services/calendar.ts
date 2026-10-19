import apiClient from "@/lib/axios";
import type { CalendarItemDto } from "@/types/api";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const calendarService = {
  /**
   * Fetches club events and external contests within a date window.
   */
  async getRange(from: string, to: string, platforms?: string[]): Promise<CalendarItemDto[]> {
    const params: Record<string, string> = { from, to };
    if (platforms && platforms.length > 0) {
      params.platforms = platforms.join(",");
    }
    const res = await apiClient.get<CalendarItemDto[]>("/api/calendar", { params });
    return res.data;
  },

  /**
   * Returns the subscribe URL for the club's .ics live feed.
   */
  getClubIcsUrl(includeEvents = true, includeContests = true, platforms?: string[]): string {
    const parts: string[] = [];
    if (includeEvents) parts.push("events");
    if (includeContests) parts.push("contests");
    const qs = new URLSearchParams();
    if (parts.length > 0) qs.set("include", parts.join(","));
    if (platforms && platforms.length > 0) qs.set("platforms", platforms.join(","));
    const queryStr = qs.toString();
    return `${BASE_URL}/api/calendar/club.ics${queryStr ? `?${queryStr}` : ""}`;
  },

  /**
   * Returns the .ics download URL for a specific club event.
   */
  getEventIcsUrl(eventId: number | string): string {
    return `${BASE_URL}/api/events/${eventId}/ics`;
  },
};
