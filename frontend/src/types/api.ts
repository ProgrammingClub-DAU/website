import type { CfRankKey } from "@/lib/cf-ranks";

export type ClubEventType = "Contest" | "Workshop" | "ICPC" | "Flagship" | "Other";

export type ClubRole =
  | "Convenor"
  | "Deputy Convenor"
  | "Core Member"
  | "Associate Core Member"
  | "Batch Representative"
  | "Club Participant";

export interface Member {
  id?: string;
  name: string;
  initials: string;
  batch: string;
  group: "Core team" | "Associate team" | "Batch representatives";
  role: string;
  cf: CfRankKey;
  about: string;
  codeforcesHandle?: string;
  rating?: number;
  solvedCount?: number;
  contestCount?: number;
  avatarUrl?: string | null;
  isActive?: boolean;
  degree?: string;
  gradYear?: string;
  clubRoleCategory?: "Leadership" | "Core" | "Associate Core" | "Batch Representative" | "Student Participant";
}

export interface RatingHistoryEntry {
  date: string;
  rating: number;
  contestName: string;
}

export interface ActivityDay {
  date: string;
  count: number;
  level: 0 | 1 | 2 | 3 | 4;
}

export interface PlatformStats {
  platform: string;
  solved: number;
}

export interface EventParticipation {
  eventId: string;
  eventName: string;
  eventType: ClubEventType;
  eventDate: string;
  rank: number | null;
  totalParticipants: number;
  status: "participated" | "registered" | "winner" | "runner-up" | "top-3";
  achievement: string | null;
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
