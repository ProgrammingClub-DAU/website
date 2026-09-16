import type { CfRankKey } from "@/lib/cf-ranks";

// These are the types for our backend API responses.
// They reflect the data we expect from the backend.

/**
 * A member exactly as the public API returns them.
 *
 * Mirrors PublicUserResponseDto field for field, on purpose. The older `Member`
 * below is a view model with invented fields -- a batch, a degree, an "about"
 * line -- that the backend has never supplied; this one has nothing in it the
 * server did not say.
 *
 * `phoneNumber` is null unless the viewer is allowed it: office bearers publish
 * theirs, everyone else's is admin-only, and that is decided server-side.
 */
export interface PublicMember {
  id: number;
  name: string;
  avatarUrl: string | null;
  clubRole: ClubRole | null;
  academicYear: AcademicYear | null;
  codeforcesHandle: string | null;
  rating: number | null;
  leetcodeHandle: string | null;
  leetcodeRating: number | null;
  codechefUrl: string | null;
  atcoderUrl: string | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  phoneNumber: string | null;
  createdAt: string;
}

/** The club posts that appear on the members page, in the order they appear. */
export const CLUB_HIERARCHY: ClubRole[] = [
  "CONVENOR",
  "DEPUTY_CONVENOR",
  "CORE",
  "ASSOCIATE_CORE",
  "BATCH_REPRESENTATIVE",
];

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
  | "CONVENOR"
  | "DEPUTY_CONVENOR"
  | "CORE"
  | "ASSOCIATE_CORE"
  | "BATCH_REPRESENTATIVE"
  | "EX_PC_MEMBER"
  | "EX_CORE"
  | "EX_CDC"
  | "STUDENT";

export type EventStatus = "UPCOMING" | "COMPLETED" | "CANCELLED";

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

/** How far into the course a member is. Chosen once, at registration. */
export type AcademicYear = "FIRST_YEAR" | "SECOND_YEAR_ONWARDS";

/** Words for each year, so no screen hand-types them. */
export const ACADEMIC_YEAR_LABELS: Record<AcademicYear, string> = {
  FIRST_YEAR: "1st year",
  SECOND_YEAR_ONWARDS: "2nd year onwards",
};

export interface Profile {
  id: number;
  name: string;
  email: string;
  codeforcesHandle: string | null;
  rating: number | null;
  role: string;
  createdAt: string;
  avatarUrl: string | null;
  phoneNumber: string | null;
  leetcodeHandle: string | null;
  leetcodeRating: number | null;
  codechefUrl: string | null;
  atcoderUrl: string | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  clubRole: ClubRole | null;
  batchYear: number | null;
  academicYear: AcademicYear | null;
  /** Name, Codeforces handle, phone number and year all filled in. */
  profileComplete: boolean;
  maxRating: number | null;
  eventParticipations: EventParticipation[];
  platformStats: PlatformStats[];
  ratingHistory: RatingHistoryEntry[];
  activityData: ActivityDay[];
}

export interface LeaderboardEntry {
  id: number;           // maps to backend userId
  name: string;
  // Nullable: the backend returns members who have not linked a Codeforces
  // account. Typing this as `string` previously hid a crash in the search filter.
  codeforcesHandle: string | null;
  rating: number | null;
  rank: number;         // backend-computed rank (1-based)
  tier: string;         // backend-computed CF tier e.g. "Expert", "Newbie"
  clubRole: ClubRole | null;
  solvedCount?: number;
  yearlyActivityCount?: number;
  avatarUrl?: string | null;
}

export type LeaderboardPlatform = "CODEFORCES" | "LEETCODE";

export type LeaderboardFilter = "ALL" | "CORE" | "BATCH_REP" | "STUDENTS";

export interface Event {
  id: number;
  title: string;
  description: string | null;
  eventDate: string;
  location: string;
  status: EventStatus;
  coverImageUrl: string | null;
  createdByName: string;
  createdAt: string;
}

export interface EventDetail extends Event {
  photos: EventPhoto[];
  attendeeCount: number;
}

export interface EventPhoto {
  id: number;
  imageUrl: string;
  caption: string | null;
  uploadedAt: string;
}

export interface EventAttendee {
  userId: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  hasPhone: boolean;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
  cfRating: number | null;
  leetcodeHandle: string | null;
  leetcodeRating: number | null;
  codechefUrl: string | null;
  atcoderUrl: string | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  addedAt: string;
  clubRole: ClubRole | null;
}

export interface UserLookup {
  id: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
  cfRating: number | null;
  leetcodeHandle: string | null;
  leetcodeRating: number | null;
  codechefUrl: string | null;
  atcoderUrl: string | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  clubRole: ClubRole | null;
  batchYear: number | null;
}

export interface MemberGalleryPhoto {
  id: number;
  batchYear: number;
  imageUrl: string;
  caption: string | null;
  uploadedAt: string;
}
