import apiClient from "@/lib/axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { ratingToRank } from "@/lib/cf-ranks";
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
    batch: "", // Phase 2 — no batch field on the backend yet
    group: "Associate team", // Phase 2
    role: user.role,
    // Derived from the rating rather than hardcoded, or every member renders
    // as a grey Newbie regardless of their actual standing.
    cf: ratingToRank(user.rating),
    about: "",
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
    // The directory buckets members by this field. Without it every bucket is
    // empty and the page renders no members at all. The backend has no club-role
    // concept yet (Phase 2), so everyone is a participant until it does.
    clubRoleCategory: "Student Participant",
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapUserToLeaderboard(user: any): LeaderboardEntry {
  // Backend LeaderboardResponseDto fields: rank, userId, name, codeforcesHandle, rating, tier
  // NOTE: field is "userId" not "id" — backend uses userId to distinguish from entity id
  return {
    id: user.userId,
    name: user.name,
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
    rank: user.rank,
    tier: user.tier,
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
