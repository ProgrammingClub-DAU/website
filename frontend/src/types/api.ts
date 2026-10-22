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
  equippedBannerId?: string | null;
  isPlatformCreator?: boolean;
  emailVerified?: boolean;
}

/** The club posts that appear on the members page, in the order they appear. */
export const CLUB_HIERARCHY: ClubRole[] = [
  "CONVENOR",
  "DEPUTY_CONVENOR",
  "CORE",
  "ASSOCIATE_CORE",
  "BATCH_REPRESENTATIVE",
];

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

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ── Club Activity & Event types ──

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
  equippedBannerId: string;
  isPlatformCreator: boolean;
  platformStats: PlatformStats[];
  ratingHistory: RatingHistoryEntry[];
  activityData: ActivityDay[];
  emailVerified?: boolean;
  notifyEvents?: boolean;
  notifyContests?: boolean;
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
  equippedBannerId?: string;
  rankBannerId?: string | null;
  activeBannerId?: string;
  maxRating?: number | null;
  /** New rating after each of the member's last rated contests, oldest first. Real data; may be empty. */
  contestHistory?: number[];
  /** Rating change in the member's latest rated contest; null when unknown. */
  ratingChange?: number | null;
  isPlatformCreator?: boolean;
  platformCreator?: boolean;
}

export type LeaderboardPlatform = "CODEFORCES" | "LEETCODE";

export type LeaderboardFilter = "ALL" | "CORE" | "BATCH_REP" | "STUDENTS";

/** What kind of event this is. Presentation only -- nothing branches on it. */
export type EventType =
  | "IPC"
  | "JUNIORS_CONTEST"
  | "INTER_WING"
  | "ROUND_ROBIN_RELAY"
  | "LECTURE"
  | "POST_CONTEST_DISCUSSION";

/** The words for each, so no screen hand-types them. */
export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  IPC: "IPC",
  JUNIORS_CONTEST: "Juniors' Contest",
  INTER_WING: "Inter-wing",
  ROUND_ROBIN_RELAY: "Round Robin Relay",
  LECTURE: "Lecture",
  POST_CONTEST_DISCUSSION: "Post-contest Discussion",
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
  capacity?: number | null;
  registrationCount?: number | null;
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

// ══════════════════════════════════════════════════════════════════════════════
// ── Phase 3: Stats, Scores, Contests, Practice, Awards, Blog & Calendar ───────
// ══════════════════════════════════════════════════════════════════════════════

// ── Stats & Leaderboard Types (Stage 1B / 2B) ────────────────────────────────

export type StatsPlatform = "CODEFORCES" | "LEETCODE" | "OVERALL";
export type StatsPeriod = "WEEK" | "MONTH" | "YEAR" | "ALL_TIME";
export type StatsMetric =
  | "SOLVED"
  | "CONTESTS"
  | "AVG_PROBLEM_RATING"
  | "ATTENDANCE"
  | "OVERALL_SCORE";

export interface StatsLeaderboardEntry {
  rank: number;
  userId: number;
  name: string;
  avatarUrl: string | null;
  clubRole: ClubRole | null;
  codeforcesHandle: string | null;
  leetcodeHandle: string | null;
  value: number | null;
  basis: string | number | null;
}

export interface StatsLeaderboardResponse {
  metric: StatsMetric;
  platform: StatsPlatform;
  period: StatsPeriod;
  trackingSince: string | null;
  computedAt: string | null;
  content: StatsLeaderboardEntry[];
  entries?: StatsLeaderboardEntry[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface LcDifficulty {
  easy: number | null;
  medium: number | null;
  hard: number | null;
}

export interface PlatformStatsDetail {
  platform: "CODEFORCES" | "LEETCODE";
  handle: string | null;
  solved: number | null;
  contests: number | null;
  avgProblemRating: number | null;
  ratedSolvesCount?: number | null;
  lcDifficulty: LcDifficulty | null;
  trackingSince: string | null;
  lastSyncedAt: string | null;
}

export interface OverallStats {
  solved: number | null;
  solvedPartial: boolean;
  contests: number | null;
}

export interface MemberStats {
  userId: number;
  period: StatsPeriod;
  codeforces: PlatformStatsDetail | null;
  leetcode: PlatformStatsDetail | null;
  overall: OverallStats;
  overallScore?: OverallScore | null;
}

export interface RatingPointDto {
  contestName: string;
  rating: number;
  date: string; // ISO date/time
  rank?: number | null;
}

export interface HeadToHeadContest {
  contestId: string;
  contestName: string;
  startedAt: string;
  aRank: number;
  bRank: number;
}

export interface HeadToHeadSummary {
  aAhead: number;
  bAhead: number;
  ties: number;
  sharedContests: HeadToHeadContest[];
}

export interface CompareMemberDetail {
  id: number;
  name: string;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
  leetcodeHandle: string | null;
  rating: number | null;
  leetcodeRating: number | null;
  allTime: MemberStats;
  year: MemberStats;
  ratingHistoryCf: RatingPointDto[];
  ratingHistoryLc: RatingPointDto[];
}

export interface CompareResult {
  memberA: CompareMemberDetail;
  memberB: CompareMemberDetail;
  headToHead: HeadToHeadSummary;
}

// ── Overall Score & Weekly Awards (Stage 3B / 4B) ─────────────────────────────

export interface OverallScore {
  overallScore: number;
  ratingScore: number;
  solvedScore: number;
  contestScore: number;
  computedAt: string;
}

export type AwardType = "CODER_OF_THE_WEEK" | "MOST_SOLVED" | "BIGGEST_RATING_JUMP";

export const AWARD_TYPE_LABELS: Record<AwardType, string> = {
  CODER_OF_THE_WEEK: "Coder of the Week",
  MOST_SOLVED: "Most Solved",
  BIGGEST_RATING_JUMP: "Biggest Rating Jump",
};

export interface WeeklyAward {
  id: number;
  weekStart: string; // YYYY-MM-DD
  awardType: AwardType;
  userId: number;
  userName: string;
  userAvatarUrl: string | null;
  metricValue: number;
  platform: "CODEFORCES" | "LEETCODE" | null;
  computedAt: string;
}

export interface WeeklyAwardsResponse {
  weekStart: string;
  computed: boolean;
  awards: WeeklyAward[];
}

export interface UserAwardHistoryItem {
  weekStart: string;
  metricValue: number;
  platform: string | null;
}

export interface UserAwardGroup {
  awardType: AwardType;
  count: number;
  latestWeekStart: string;
  history: UserAwardHistoryItem[];
}

// ── Topic Strength & Practice Suggestions (Stage 3B / 4B) ────────────────────

export interface TopicItem {
  tag: string;
  memberSolves: number;
  memberShare: number; // e.g. 0.15
  clubShare: number;   // e.g. 0.20
  gap: number;         // memberShare - clubShare
}

export interface TopicStrengthResponse {
  userId: number;
  totalCountedSolves: number;
  topics: TopicItem[];
  note?: string;
}

export interface PracticeSuggestion {
  problemId?: number;
  id?: number;
  problemKey: string;
  contestId: number | null;
  problemIndex: string;
  name: string;
  rating: number | null;
  solvedCount: number | null;
  tags: string[];
  url: string;
}

export interface PracticeSuggestionsResponseDto {
  ratingBandMin?: number;
  ratingBandMax?: number;
  weakTags: string[];
  reason?: string | null;
  suggestions: PracticeSuggestion[];
}

export interface PracticeSuggestionsResponse {
  userId?: number;
  targetBand?: string;
  ratingBandMin?: number;
  ratingBandMax?: number;
  weakTags: string[];
  suggestions: PracticeSuggestion[];
  reason?: string | null;
}

// ── Club Contests, Championship & Upsolve (Stage 3C / 4B) ─────────────────────

export type ClubContestKind = "GYM" | "MASHUP" | "GROUP" | "OFFICIAL";

export interface ClubSeason {
  id: number;
  name: string;
  startsOn: string;
  endsOn: string;
  countBest: number | null;
  createdAt?: string;
}

export interface ClubContest {
  id: number;
  cfContestId: number;
  name: string;
  kind: ClubContestKind;
  seasonId: number | null;
  seasonName: string | null;
  startsAt: string;
  durationSeconds: number;
  lastSyncedAt: string | null;
  createdAt: string;
}

export interface ClubContestProblem {
  id: number;
  problemIndex: string;
  name: string;
  rating: number | null;
}

export interface RanklistProblemResult {
  problemIndex: string;
  solved: boolean;
  points: number;
  penalty: number;
}

export interface RanklistRow {
  contestRank: number;
  userId: number;
  name: string;
  codeforcesHandle: string;
  avatarUrl: string | null;
  cfPoints: number;
  penalty: number;
  solvedCount: number;
  problemResults: Record<string, RanklistProblemResult>;
}

export interface ClubContestDetail extends ClubContest {
  problems: ClubContestProblem[];
  ranklist: RanklistRow[];
}

export type UpsolveStatus = "SOLVED_IN_CONTEST" | "UPSOLVED" | "UNSOLVED";

export interface UpsolveMemberRow {
  userId: number;
  name: string;
  codeforcesHandle: string;
  avatarUrl: string | null;
  results: Record<string, UpsolveStatus>;
  totalSolved: number;
  totalUpsolved: number;
}

export interface UpsolveProblemWithRate extends ClubContestProblem {
  upsolveRate: number; // percentage solved or upsolved
}

export interface UpsolveMatrix {
  contestId: number;
  contestName: string;
  problems: UpsolveProblemWithRate[];
  members: UpsolveMemberRow[];
}

export interface MyUpsolveItem {
  contestId: number;
  contestName: string;
  cfContestId: number;
  kind: ClubContestKind;
  problemIndex: string;
  problemName: string;
  rating: number | null;
  problemUrl: string;
}

export interface MyUpsolvesContestGroup {
  contestId: number;
  contestName: string;
  cfContestId: number;
  problems: MyUpsolveItem[];
}

export interface MyUpsolvesResponse {
  cfSyncedAt: string | null;
  contests: MyUpsolvesContestGroup[];
}

export interface ChampionshipContestScore {
  contestId: number;
  contestName: string;
  rank: number;
  points: number;
  counted: boolean;
}

export interface ChampionshipRow {
  rank: number;
  userId: number;
  name: string;
  avatarUrl: string | null;
  codeforcesHandle: string;
  totalPoints: number;
  contestsWithPoints: number;
  bestRank: number | null;
  contestScores: Record<number, ChampionshipContestScore>;
}

export interface ChampionshipResponse {
  season: ClubSeason;
  contests: ClubContest[];
  standings: ChampionshipRow[];
}

export interface WeeklyAwardDto {
  id: number;
  weekStart: string;
  awardType: AwardType;
  userId: number;
  userName: string;
  codeforcesHandle?: string | null;
  leetcodeHandle?: string | null;
  avatarUrl?: string | null;
  clubRole?: ClubRole | null;
  metricValue: number;
  platform?: "CODEFORCES" | "LEETCODE" | null;
  computedAt: string;
}

export interface LatestWeeklyAwardsResponseDto {
  weekStart: string;
  computed: boolean;
  awards: WeeklyAwardDto[];
}

export interface ClubContestSummaryDto {
  id: number;
  cfContestId: number;
  kind: ClubContestKind;
  name: string;
  startsAt: string;
  endsAt: string;
  durationSeconds: number;
  seasonId?: number | null;
  seasonName?: string | null;
  lastSyncedAt?: string | null;
  participantCount: number;
  url?: string;
}

export interface MemberRefDto {
  id: number;
  name: string;
  avatarUrl?: string | null;
  codeforcesHandle?: string | null;
}

export interface ContestProblemDto {
  id?: number;
  problemIndex: string;
  name: string;
  rating?: number | null;
}

export interface RanklistCellDto {
  problemIndex: string;
  solved: boolean;
  attempted: boolean;
}

export interface RanklistRowDto {
  rank: number;
  member: MemberRefDto;
  cfPoints: number;
  penalty: number;
  solvedCount: number;
  cells: RanklistCellDto[];
}

export interface ClubContestDetailDto {
  contest: ClubContestSummaryDto;
  problems: ContestProblemDto[];
  rows: RanklistRowDto[];
}

export interface UpsolveMatrixDto {
  clubContestId: number;
  contestName: string;
  endsAt: string;
  problems: {
    index: string;
    name: string;
    rating?: number | null;
    upsolveRate?: number | null;
  }[];
  rows: {
    member: MemberRefDto;
    cells: UpsolveStatus[];
  }[];
}

export interface MyUpsolvesContest {
  clubContestId: number;
  name: string;
  endsAt: string;
  pending: {
    index: string;
    name: string;
    rating?: number | null;
    url: string;
  }[];
}

export interface MyUpsolvesDto {
  cfSyncedAt: string | null;
  contests: MyUpsolvesContest[];
}

export interface SeasonDto {
  id: number;
  name: string;
  startsOn: string;
  endsOn: string;
  countBest: number | null;
  contestCount?: number;
}

export interface SeasonRequest {
  name: string;
  startsOn: string;
  endsOn: string;
  countBest?: number | null;
}

export interface ClubContestCreateRequest {
  cfContestId: number;
  kind: ClubContestKind;
  seasonId?: number | null;
}

export interface ChampionshipDto {
  season: SeasonDto;
  rows: {
    rank: number;
    member: MemberRefDto;
    total: number;
    contestsWithPoints: number;
    bestRank: number;
    breakdown: {
      clubContestId: number;
      contestName: string;
      clubRank: number;
      points: number;
      counted: boolean;
    }[];
  }[];
}

// ── Calendar (Stage 3A / 4B) ─────────────────────────────────────────────────

export type CalendarPlatform = "CODEFORCES" | "LEETCODE" | "CODECHEF" | "ATCODER" | "CLUB";

export interface CalendarItemDto {
  id: string;
  type: "CLUB_EVENT" | "EXTERNAL_CONTEST";
  platform?: string;
  title: string;
  url: string;
  startsAt: string;
  endsAt: string;
  eventType?: string | null;
}

export interface CalendarItem {
  id: string; // e.g. "event-42" or "cf-1843"
  kind: "EVENT" | "CONTEST";
  platform: CalendarPlatform;
  title: string;
  url: string;
  startsAt: string; // ISO UTC string
  durationSeconds: number;
  location?: string | null;
  description?: string | null;
  sourceId: number | string;
}

// ── Sync Health & Admin Sync (Stage 1A / 3A / 4B) ────────────────────────────

export type SyncJobType =
  | "CF_RATINGS"
  | "CF_SUBMISSIONS"
  | "CF_PROBLEMSET"
  | "CF_CONTESTS"
  | "LC_RATINGS"
  | "LC_CONTESTS"
  | "LC_TOTALS"
  | "EXTERNAL_CONTESTS"
  | "CLUB_CONTEST";

export type SyncHealthStatus =
  | "RUNNING"
  | "HEALTHY"
  | "DEGRADED"
  | "FAILING"
  | "STALE"
  | "NEVER_RUN";

/** Health of one job, as SyncHealthService reports it. Every timestamp is UTC. */
export interface SyncHealthDto {
  job: SyncJobType;
  status: SyncHealthStatus;
  startedAt: string | null;
  finishedAt: string | null;
  membersOk: number;
  membersFailed: number;
  errorSample: string | null;
  lastSuccessfulRun: string | null;
  stale: boolean;
  running: boolean;
}

export interface SyncRunRecord {
  id: number;
  job: SyncJobType;
  startedAt: string;
  finishedAt: string | null;
  membersOk: number;
  membersFailed: number;
  errorSample: string | null;
}

// ── Blog & Comments (Stage 5A / 6B) ──────────────────────────────────────────

export type BlogStatus = "DRAFT" | "PENDING_REVIEW" | "PUBLISHED" | "REJECTED";

export interface BlogResponseDto {
  id: number;
  title: string;
  slug: string;
  content: string;
  authorName: string;
  tags: string;
  published: boolean;
  createdAt: string;
  updatedAt: string | null;
  status: BlogStatus;
  authorId: number | null;
  publishedAt: string | null;
  commentCount: number;
  readMinutes: number;
}

export interface BlogEditDto extends BlogResponseDto {
  reviewNote: string | null;
  submittedAt: string | null;
}

export interface CommentAuthorDto {
  id: number;
  name: string;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
}

export interface CommentDto {
  id: number;
  parentId: number | null;
  author: CommentAuthorDto;
  body: string | null;
  createdAt: string;
  editedAt: string | null;
  deleted: boolean;
  canEdit: boolean;
  canDelete: boolean;
  replies?: CommentDto[];
}

// ── Event RSVP & Reminders (Stage 5B / 6B) ───────────────────────────────────

export interface RegistrationStatus {
  registered: boolean;
  count: number;
  capacity: number | null;
  open: boolean;
}

export interface EventRegistrationRow {
  id: number;
  userId: number;
  name: string;
  email: string;
  phoneNumber: string | null;
  clubRole: ClubRole | null;
  registeredAt: string;
  alreadyAttending: boolean;
}

