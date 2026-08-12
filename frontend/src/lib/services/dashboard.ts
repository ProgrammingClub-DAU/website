import apiClient from "@/lib/axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";
import { mockLeaderboardEntries, getMockProfile } from "@/lib/content/mock-dashboards";

// Mock mode disabled for Phase 1 completion
const IS_MOCK = false;

// ── Mappers: Transform backend UserResponseDto to Frontend Types ──

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapUserToMember(user: any): Member {
  return {
    id: String(user.id),
    name: user.name,
    initials: user.name.split(" ").map((n: string) => n[0]).join("").substring(0, 2).toUpperCase(),
    batch: "", // Phase 2
    group: "Associate team", // Phase 2
    role: user.role,
    cf: "newbie", // Resolved dynamically by UI
    about: "",
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapUserToLeaderboard(user: any): LeaderboardEntry {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
    role: user.role,
    createdAt: user.createdAt,
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapUserToProfile(user: any): Profile {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
    role: user.role,
    createdAt: user.createdAt,
    avatarUrl: null,
    maxRating: user.rating,
    clubRole: "Club Participant",
    eventParticipations: [], // Phase 2
    platformStats: [], // Phase 2
    ratingHistory: [], // Live fetch
    activityData: [], // Phase 2
  };
}

export const dashboardService = {
  // Members
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockMembers), 500));
    }
    const response = await apiClient.get("/api/users");
    // Unwrap Spring Data PagedResponse
    const content = response.data?.data?.content || [];
    return content.map(mapUserToMember);
  },

  // Leaderboard
  getLeaderboard: async (): Promise<LeaderboardEntry[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockLeaderboardEntries), 500));
    }
    const response = await apiClient.get("/api/leaderboard");
    // Unwrap Spring Data PagedResponse
    const content = response.data?.data?.content || [];
    return content.map(mapUserToLeaderboard);
  },

  // Profile (requires auth)
  // Backend reads user ID securely from JWT via /api/users/profile
  getProfile: async (_userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(getMockProfile(_userId)), 500));
    }
    const response = await apiClient.get("/api/users/profile");
    return mapUserToProfile(response.data?.data || response.data);
  },

  // Public Profile (by ID)
  getUserProfileById: async (userId: string): Promise<Profile> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(getMockProfile(userId)), 500));
    }
    const response = await apiClient.get(`/api/users/${userId}`);
    return mapUserToProfile(response.data?.data || response.data);
  },

  // Update Codeforces Handle
  updateCodeforcesHandle: async (userId: string, handle: string): Promise<Profile> => {
    const response = await apiClient.put(`/api/users/${userId}/handle`, { handle });
    return mapUserToProfile(response.data?.data || response.data);
  }
};
