import type { CfRankKey } from "@/lib/cf-ranks";

// These are the types for our backend API responses.
// They reflect the data we expect from the backend.

export interface Member {
  id?: string;
  name: string;
  initials: string;
  batch: string;
  group: "Core team" | "Associate team" | "Batch representatives";
  role: string;
  cf: CfRankKey;
  about: string;
}

export interface RatingHistoryEntry {
  date: string;
  rating: number;
  contestName: string;
}

export interface ActivityDay {
  date: string;   // YYYY-MM-DD
  count: number;  // number of problems solved that day
  level: 0 | 1 | 2 | 3 | 4; // intensity level for the heat map
}

export interface PlatformStats {
  platform: string;
  solved: number;
}

// ── Club Activity & Event types ──

export type ClubEventType = "Contest" | "Workshop" | "ICPC" | "Flagship" | "Other";

export type ClubRole =
  | "Convenor"
  | "Deputy Convenor"
  | "Core Member"
  | "Associate Core Member"
  | "Batch Representative"
  | "Club Participant";

export interface EventParticipation {
  eventId: string;
  eventName: string;
  eventType: ClubEventType;
  eventDate: string; // ISO date string
  rank: number | null; // null for non-ranked events (workshops, etc.)
  totalParticipants: number;
  status: "participated" | "registered" | "winner" | "runner-up" | "top-3";
  achievement: string | null; // e.g. "🥇 1st Place", null if none
}

export interface Profile {
  id: number;
  name: string;
  email: string;
  codeforcesHandle: string;
  rating: number;
  role: string;
  createdAt: string;
  avatarUrl: string | null;
  maxRating: number;
  clubRole: ClubRole;
  eventParticipations: EventParticipation[];
  platformStats: PlatformStats[];
  ratingHistory: RatingHistoryEntry[];
  activityData: ActivityDay[];
}

export interface LeaderboardEntry {
  id: number;
  name: string;
  email: string;
  codeforcesHandle: string;
  rating: number;
  role: string;
  createdAt: string;
  clubRole?: ClubRole;
  lastEventDate?: string;
  solvedCount?: number;
  yearlyActivityCount?: number;
  avatarUrl?: string | null;
}
