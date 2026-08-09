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
  getProfile: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve({
        id: userId,
        email: "user@example.com",
        name: "Mock User",
        handle: "mock_coder",
        cfRank: "candidate",
        joinedAt: "2024-01-01T00:00:00Z"
      }), 500));
    }
    const response = await api.get<Profile>(`/users/${userId}`);
    return response.data;
  }
};

export default api;
