import apiClient from "@/lib/axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";
import { mockLeaderboardEntries, getMockProfile } from "@/lib/content/mock-dashboards";

// Mock implementations to allow frontend development while backend is pending
const IS_MOCK = true;

export const dashboardService = {
  // Members
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
      // Returning static data as mock
      return new Promise((resolve) => setTimeout(() => resolve(mockMembers), 500));
    }
    const response = await apiClient.get<Member[]>("/users");
    return response.data;
  },

  // Leaderboard
  getLeaderboard: async (): Promise<LeaderboardEntry[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockLeaderboardEntries), 500));
    }
    const response = await apiClient.get<LeaderboardEntry[]>("/users/leaderboard");
    return response.data;
  },

  // Profile (requires auth/userId)
  // Returns rich profile data including rating history, activity calendar, and platform stats
  getProfile: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(getMockProfile(userId)), 500));
    }
    const response = await apiClient.get<Profile>(`/users/${userId}`);
    return response.data;
  }
};
