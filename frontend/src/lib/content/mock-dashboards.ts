import type { LeaderboardEntry, Profile } from "@/types/api";

export const mockLeaderboardEntries: LeaderboardEntry[] = [
  { id: 1, name: "Sumeet Verma", codeforcesHandle: "Sumeet.Verma", rating: 2502, rank: 1, tier: "Grandmaster", clubRole: "Convenor", solvedCount: 1289, yearlyActivityCount: 42, avatarUrl: null },
  { id: 2, name: "Preet Sheth", codeforcesHandle: "Preet Sheth", rating: 2210, rank: 2, tier: "Master", clubRole: "Core Member", solvedCount: 1042, yearlyActivityCount: 38, avatarUrl: null },
  { id: 3, name: "Jalp Patel", codeforcesHandle: "Jalp Patel", rating: 1990, rank: 3, tier: "Candidate Master", clubRole: "Core Member", solvedCount: 915, yearlyActivityCount: 29, avatarUrl: null },
  { id: 4, name: "King-T", codeforcesHandle: "King-T", rating: 1639, rank: 4, tier: "Expert", clubRole: "Associate Core Member", solvedCount: 654, yearlyActivityCount: 24, avatarUrl: null },
  { id: 5, name: "Alice Sharma", codeforcesHandle: "Alice", rating: 1447, rank: 5, tier: "Specialist", clubRole: "Batch Representative", solvedCount: 512, yearlyActivityCount: 18, avatarUrl: null },
  { id: 6, name: "XYZ Coder", codeforcesHandle: "XYZ", rating: 1218, rank: 6, tier: "Pupil", clubRole: "Club Participant", solvedCount: 380, yearlyActivityCount: 12, avatarUrl: null },
  { id: 7, name: "Binod Kumar", codeforcesHandle: "Binod", rating: 818, rank: 7, tier: "Newbie", clubRole: "Club Participant", solvedCount: 120, yearlyActivityCount: 0, avatarUrl: null },
  { id: 8, name: "Rohan Gupta", codeforcesHandle: "rohan_dp", rating: 1750, rank: 8, tier: "Expert", clubRole: "Associate Core Member", solvedCount: 780, yearlyActivityCount: 31, avatarUrl: null },
  { id: 9, name: "Meher Singh", codeforcesHandle: "meher.solves", rating: 1890, rank: 9, tier: "Candidate Master", clubRole: "Batch Representative", solvedCount: 810, yearlyActivityCount: 27, avatarUrl: null },
  { id: 10, name: "Arjun Mehta", codeforcesHandle: "arjun_bitset", rating: 2050, rank: 10, tier: "Master", clubRole: "Core Member", solvedCount: 610, yearlyActivityCount: 0, avatarUrl: null },
];


export const generateMockActivityData = () => {
  return Array.from({ length: 365 }, (_, i) => {
    const date = new Date();
    date.setDate(date.getDate() - (364 - i));
    const count = Math.random() > 0.3 ? Math.floor(Math.random() * 8) : 0;
    const level = count === 0 ? 0 : count <= 2 ? 1 : count <= 4 ? 2 : count <= 6 ? 3 : 4;
    return {
      date: date.toISOString().split("T")[0],
      count,
      level: level as 0 | 1 | 2 | 3 | 4,
    };
  });
};

export const mockRatingHistory = [
  { date: "2024-01-15", rating: 1200, contestName: "Codeforces Round #900" },
  { date: "2024-02-10", rating: 1350, contestName: "Codeforces Round #910" },
  { date: "2024-03-05", rating: 1280, contestName: "Codeforces Round #920" },
  { date: "2024-04-20", rating: 1450, contestName: "Codeforces Round #930" },
  { date: "2024-05-12", rating: 1520, contestName: "Codeforces Round #940" },
  { date: "2024-06-08", rating: 1490, contestName: "Educational CF Round #160" },
  { date: "2024-07-15", rating: 1620, contestName: "Codeforces Round #955" },
  { date: "2024-08-22", rating: 1580, contestName: "Codeforces Round #965" },
  { date: "2024-09-18", rating: 1700, contestName: "Codeforces Round #975" },
  { date: "2024-10-10", rating: 1750, contestName: "Educational CF Round #170" },
  { date: "2024-11-05", rating: 1820, contestName: "Codeforces Round #985" },
  { date: "2024-12-01", rating: 1900, contestName: "Codeforces Round #990" },
  { date: "2025-01-20", rating: 1950, contestName: "Codeforces Round #1000" },
  { date: "2025-03-10", rating: 1990, contestName: "Codeforces Round #1010" },
];

export const getMockProfile = (userId: string): Profile => {
  return {
    id: Number(userId) || 1,
    email: "sumeet.verma@dau.ac.in",
    name: "Sumeet Verma",
    codeforcesHandle: "Sumeet.Verma",
    role: "ROLE_USER",
    createdAt: "2024-01-01T00:00:00Z",
    avatarUrl: null,
    rating: 1990,
    maxRating: 1990,
    clubRole: "Core Member",
    eventParticipations: [
      {
        eventId: "evt-001",
        eventName: "Code Mutant 2026",
        eventType: "Flagship",
        eventDate: "2026-03-15T00:00:00Z",
        rank: 7,
        totalParticipants: 142,
        status: "top-3",
        achievement: null,
      },
      {
        eventId: "evt-002",
        eventName: "Weekly CP Contest #12",
        eventType: "Contest",
        eventDate: "2026-03-08T00:00:00Z",
        rank: 1,
        totalParticipants: 58,
        status: "winner",
        achievement: "🥇 1st Place",
      },
      {
        eventId: "evt-003",
        eventName: "DSA Workshop: Graph Algorithms",
        eventType: "Workshop",
        eventDate: "2026-02-28T00:00:00Z",
        rank: null,
        totalParticipants: 85,
        status: "participated",
        achievement: null,
      },
      {
        eventId: "evt-004",
        eventName: "Weekly CP Contest #11",
        eventType: "Contest",
        eventDate: "2026-02-22T00:00:00Z",
        rank: 3,
        totalParticipants: 62,
        status: "top-3",
        achievement: "🥉 3rd Place",
      },
      {
        eventId: "evt-005",
        eventName: "ICPC Prelims Practice Camp",
        eventType: "ICPC",
        eventDate: "2026-02-10T00:00:00Z",
        rank: 5,
        totalParticipants: 24,
        status: "participated",
        achievement: null,
      },
      {
        eventId: "evt-006",
        eventName: "DSA Workshop: Dynamic Programming",
        eventType: "Workshop",
        eventDate: "2026-01-20T00:00:00Z",
        rank: null,
        totalParticipants: 110,
        status: "participated",
        achievement: null,
      },
      {
        eventId: "evt-007",
        eventName: "Weekly CP Contest #8",
        eventType: "Contest",
        eventDate: "2026-01-11T00:00:00Z",
        rank: 2,
        totalParticipants: 55,
        status: "runner-up",
        achievement: "🥈 2nd Place",
      },
      {
        eventId: "evt-008",
        eventName: "Winter Long Contest 2025",
        eventType: "Contest",
        eventDate: "2025-12-20T00:00:00Z",
        rank: 12,
        totalParticipants: 98,
        status: "participated",
        achievement: null,
      },
    ],
    platformStats: [
      { platform: "Codeforces", solved: 520 },
      { platform: "LeetCode", solved: 210 },
      { platform: "CodeChef", solved: 82 },
      { platform: "AtCoder", solved: 35 },
    ],
    ratingHistory: mockRatingHistory,
    activityData: generateMockActivityData(),
  };
};
