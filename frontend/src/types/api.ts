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
  /** The member's user id. The API calls it userId; leaderboardService renames it. */
  id: number;
  name: string;
  // Nullable: the backend returns members who have not linked a Codeforces
  // account. Typing this as `string` previously hid a crash in the search filter.
  codeforcesHandle: string | null;
  rating: number | null;
  rank: number;         // backend-computed rank (1-based)
  tier: string;         // backend-computed CF tier e.g. "Expert", "Newbie"
  clubRole: ClubRole | null;
  avatarUrl: string | null;
}

export type LeaderboardPlatform = "CODEFORCES" | "LEETCODE";

export type LeaderboardFilter = "ALL" | "CORE" | "BATCH_REP" | "STUDENTS";

/** What kind of event this is. Presentation only -- nothing branches on it. */
export type EventType = "FLAGSHIP" | "CONTEST" | "WORKSHOP" | "ICPC" | "TALK";

/** The words for each, so no screen hand-types them. */
export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  FLAGSHIP: "Flagship",
  CONTEST: "Contest",
  WORKSHOP: "Workshop",
  ICPC: "ICPC",
  TALK: "Talk",
};

export interface Event {
  id: number;
  title: string;
  description: string | null;
  eventDate: string;
  location: string;
  status: EventStatus;
  coverImageUrl: string | null;
  /** The badge on the timeline. Null for events created before types existed. */
  eventType: EventType | null;
  /**
   * The contest this event ran on.
   *
   * Null means either there is no contest -- a workshop, a talk -- or there is
   * one the club has not published yet. The public cannot tell those apart, and
   * should not be able to: that is what the switch is for.
   */
  codeforcesContestUrl: string | null;
  /** Publication state, not content. Sent to everyone; the admin panel's switches. */
  showContestLink: boolean;
  showWinners: boolean;
  showAttendeeCount: boolean;
  createdByName: string;
  createdAt: string;
}

/** One placing on an event's podium. */
export interface EventWinner {
  position: number;
  userId: number;
  name: string;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
  rating: number | null;
}

export interface EventDetail extends Event {
  photos: EventPhoto[];
  /** Empty when unpublished, or when no podium was recorded. */
  winners: EventWinner[];
  /** Null when the turnout has not been published -- not zero, which would be a claim. */
  attendeeCount: number | null;
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

// ── Hall of Fame ─────────────────────────────────────────────────────────────

export interface HallOfFameLink {
  label: string;
  url: string;
}

export interface HallOfFamePhoto {
  id: number;
  imageUrl: string;
  caption: string | null;
}

/** One achievement, as HallOfFameEntryDto returns it. */
export interface HallOfFameEntry {
  id: number;
  heading: string;
  subheading: string | null;
  details: string | null;
  /** ISO date, "YYYY-MM-DD" -- a day, not an instant. */
  achievedOn: string;
  links: HallOfFameLink[];
  photos: HallOfFamePhoto[];
  createdAt: string;
  updatedAt: string;
}

// ── Gallery ──────────────────────────────────────────────────────────────────

export type GallerySource = "EVENT" | "HALL_OF_FAME";

/** One photo on the public gallery, and where it belongs. */
export interface GalleryPhoto {
  /** Unique across both sources, e.g. "event-12" or "hof-12". */
  id: string;
  imageUrl: string;
  caption: string | null;
  source: GallerySource;
  sourceId: number;
  sourceTitle: string;
  /** ISO date, "YYYY-MM-DD". */
  date: string | null;
  /** The event's venue; null for Hall of Fame photos. */
  location: string | null;
}
