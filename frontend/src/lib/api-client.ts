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
        { id: "1", rank: 1, handle: "tourist", cfRank: "legendary grandmaster", rating: 3800, solvedCount: 5000 },
        { id: "2", rank: 2, handle: "benq", cfRank: "legendary grandmaster", rating: 3750, solvedCount: 4500 }
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
