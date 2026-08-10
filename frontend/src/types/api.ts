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

export interface Profile {
  id: string;
  email: string;
  name: string;
  handle: string;
  avatarUrl: string | null;
  cfRank: CfRankKey;
  rating: number;
  maxRating: number;
  joinedAt: string;
  totalSolved: number;
  platformStats: PlatformStats[];
  ratingHistory: RatingHistoryEntry[];
  activityData: ActivityDay[];
}

export interface LeaderboardEntry {
  id: string;
  rank: number;
  handle: string;
  cfRank: CfRankKey;
  rating: number;
  solvedCount: number;
}
