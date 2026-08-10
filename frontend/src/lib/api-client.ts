import axios from "axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

const IS_MOCK = true;

export const apiClient = {
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockMembers), 500));
    }
    const response = await api.get<Member[]>("/users");
    return response.data;
  },

  getLeaderboard: async (): Promise<LeaderboardEntry[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve([
        { id: 1, name: "Sumeet Verma", email: "sumeet@example.com", codeforcesHandle: "Sumeet.Verma", rating: 2502, role: "ROLE_USER", clubRole: "Convenor", createdAt: "2024-01-01T00:00:00Z", lastEventDate: "2026-03-15T00:00:00Z", solvedCount: 1289, yearlyActivityCount: 42, avatarUrl: null },
        { id: 2, name: "Preet Sheth", email: "preet@example.com", codeforcesHandle: "Preet Sheth", rating: 2210, role: "ROLE_USER", clubRole: "Core Member", createdAt: "2024-01-02T00:00:00Z", lastEventDate: "2026-03-08T00:00:00Z", solvedCount: 1042, yearlyActivityCount: 38, avatarUrl: null },
      ]), 500));
    }
    const response = await api.get<LeaderboardEntry[]>("/users/leaderboard");
    return response.data;
  },

  getProfile: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve({
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
        eventParticipations: [],
        platformStats: [],
        ratingHistory: [],
        activityData: [],
      }), 500));
    }
    const response = await api.get<Profile>(`/users/${userId}`);
    return response.data;
  }
};

export default api;
