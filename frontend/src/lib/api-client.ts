import axios from "axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";

// Initialize an Axios instance with base configuration
const api = axios.create({
  // Use env variable or default to localhost
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Mock implementations to allow frontend development while backend is pending
const IS_MOCK = true;

export const apiClient = {
  // Members
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
      // Returning static data as mock
      return new Promise((resolve) => setTimeout(() => resolve(mockMembers), 500));
    }
    const response = await api.get<Member[]>("/members");
    return response.data;
  },

  // Leaderboard
  getLeaderboard: async (): Promise<LeaderboardEntry[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve([
        { id: "1", rank: 1, handle: "Sumeet.Verma", cfRank: "grandmaster", rating: 2502, solvedCount: 5000 },
        { id: "2", rank: 2, handle: "Preet Sheth", cfRank: "master", rating: 2210, solvedCount: 4220 },
        { id: "3", rank: 3, handle: "Jalp Patel", cfRank: "candidate", rating: 1990, solvedCount: 3822},
        { id: "4", rank: 6, handle: "King-T", cfRank: "expert", rating: 1639, solvedCount: 2379},
        { id: "5", rank: 10, handle: "Alice", cfRank: "specialist", rating: 1447, solvedCount: 2009},
        { id: "6", rank: 15, handle: "XYZ", cfRank: "pupil", rating: 1218, solvedCount: 1210},
        { id: "7", rank: 90, handle: "Binod", cfRank: "newbie", rating: 818, solvedCount: 933}
      ]), 500));
    }
    const response = await api.get<LeaderboardEntry[]>("/leaderboard");
    return response.data;
  },

  // Profile (requires auth/userId)
  // Returns rich profile data including rating history, activity calendar, and platform stats
  getProfile: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      // Generate mock activity data for the last 365 days (GitHub-style green dots)
      const activityData = Array.from({ length: 365 }, (_, i) => {
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

      // Mock contest rating history over time
      const ratingHistory = [
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

      return new Promise((resolve) => setTimeout(() => resolve({
        id: userId,
        email: "sumeet.verma@dau.ac.in",
        name: "Sumeet Verma",
        handle: "Sumeet.Verma",
        avatarUrl: null,
        cfRank: "candidate",
        rating: 1990,
        maxRating: 1990,
        joinedAt: "2024-01-01T00:00:00Z",
        totalSolved: 847,
        platformStats: [
          { platform: "Codeforces", solved: 520 },
          { platform: "LeetCode", solved: 210 },
          { platform: "CodeChef", solved: 82 },
          { platform: "AtCoder", solved: 35 },
        ],
        ratingHistory,
        activityData,
      }), 500));
    }
    const response = await api.get<Profile>(`/users/${userId}`);
    return response.data;
  }
};

export default api;
