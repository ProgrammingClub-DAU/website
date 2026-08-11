import apiClient from "@/lib/axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";
import { mockLeaderboardEntries, getMockProfile } from "@/lib/content/mock-dashboards";

// Mock implementations to allow frontend development while backend is pending
const IS_MOCK = true;

// In-memory profile cache to persist edits during mock mode
const profileStore = new Map<string, Profile>();

export const dashboardService = {
  // Members
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
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
  getProfile: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      if (!profileStore.has(userId)) {
        profileStore.set(userId, getMockProfile(userId));
      }
      return new Promise((resolve) => setTimeout(() => resolve({ ...profileStore.get(userId)! }), 300));
    }
    const response = await apiClient.get<Profile>(`/users/${userId}`);
    return response.data;
  },

  // Update Profile (Name, Social Links, CP Handles, Manual Achievements)
  updateProfile: async (userId: string, updates: Partial<Profile>): Promise<Profile> => {
    if (IS_MOCK) {
      const current = profileStore.get(userId) || getMockProfile(userId);
      const updated: Profile = {
        ...current,
        ...updates,
        name: updates.name ?? current.name,
        codeforcesHandle: updates.cpHandles?.codeforces ?? updates.codeforcesHandle ?? current.codeforcesHandle,
        socialLinks: {
          ...current.socialLinks,
          ...updates.socialLinks,
        },
        cpHandles: {
          ...current.cpHandles,
          ...updates.cpHandles,
        },
        manualAchievements: updates.manualAchievements ?? current.manualAchievements,
      };

      // Sync platformStats handles with new cpHandles
      if (updated.cpHandles) {
        const stats = [...(updated.platformStats || [])];
        const platforms = [
          { name: "Codeforces", handle: updated.cpHandles.codeforces },
          { name: "LeetCode", handle: updated.cpHandles.leetcode },
          { name: "CodeChef", handle: updated.cpHandles.codechef },
          { name: "AtCoder", handle: updated.cpHandles.atcoder },
        ];

        platforms.forEach(({ name, handle }) => {
          const idx = stats.findIndex((s) => s.platform === name);
          if (idx >= 0) {
            if (handle) {
              stats[idx] = { ...stats[idx], handle };
            } else {
              stats[idx] = { ...stats[idx], handle: undefined };
            }
          } else if (handle) {
            stats.push({ platform: name, handle });
          }
        });

        updated.platformStats = stats;
      }

      profileStore.set(userId, updated);
      return new Promise((resolve) => setTimeout(() => resolve({ ...updated }), 400));
    }

    const response = await apiClient.put<Profile>(`/users/${userId}`, updates);
    return response.data;
  },
};
