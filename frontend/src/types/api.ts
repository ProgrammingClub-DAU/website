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

export interface Profile {
  id: string;
  email: string;
  name: string;
  handle: string;
  cfRank: CfRankKey;
  joinedAt: string;
}

export interface LeaderboardEntry {
  id: string;
  rank: number;
  handle: string;
  cfRank: CfRankKey;
  rating: number;
  solvedCount: number;
}
