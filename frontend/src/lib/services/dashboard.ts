import apiClient from "@/lib/axios";
import { Profile, LeaderboardEntry, PublicMember } from "@/types/api";
import type { ApiResponse } from "@/store/auth";
import { mockLeaderboardEntries, getMockProfile } from "@/lib/content/mock-dashboards";

// Mock mode disabled for Phase 1 completion
const IS_MOCK = false;

// ── Mappers: Transform backend UserResponseDto to Frontend Types ──

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
    clubRole: user.clubRole ?? null,
  };
}

interface UserProfileResponse {
  id: number;
  name: string;
  email: string;
  codeforcesHandle: string | null;
  rating: number | null;
  role: string;
  createdAt: string;
  avatarUrl?: string | null;
  phoneNumber?: string | null;
  leetcodeHandle?: string | null;
  leetcodeRating?: number | null;
  codechefUrl?: string | null;
  atcoderUrl?: string | null;
  githubUrl?: string | null;
  linkedinUrl?: string | null;
  clubRole?: Profile["clubRole"];
  batchYear?: number | null;
  academicYear?: Profile["academicYear"];
  profileComplete?: boolean;
}

export interface ProfileUpdateRequest {
  name: string;
  phoneNumber?: string | null;
  codeforcesHandle?: string | null;
  leetcodeHandle?: string | null;
  codechefUrl?: string | null;
  atcoderUrl?: string | null;
  githubUrl?: string | null;
  linkedinUrl?: string | null;
  avatarUrl?: string | null;
  academicYear?: Profile["academicYear"];
}

function mapUserToProfile(user: UserProfileResponse): Profile {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    codeforcesHandle: user.codeforcesHandle,
    rating: user.rating,
    role: user.role,
    createdAt: user.createdAt,
    avatarUrl: user.avatarUrl ?? null,
    phoneNumber: user.phoneNumber ?? null,
    leetcodeHandle: user.leetcodeHandle ?? null,
    leetcodeRating: user.leetcodeRating ?? null,
    codechefUrl: user.codechefUrl ?? null,
    atcoderUrl: user.atcoderUrl ?? null,
    githubUrl: user.githubUrl ?? null,
    linkedinUrl: user.linkedinUrl ?? null,
    clubRole: user.clubRole ?? null,
    batchYear: user.batchYear ?? null,
    academicYear: user.academicYear ?? null,
    profileComplete: user.profileComplete ?? false,
    maxRating: user.rating,
    eventParticipations: [], // Phase 2
    platformStats: [], // Phase 2
    ratingHistory: [], // Live fetch
    activityData: [], // Phase 2
  };
}

export const dashboardService = {
  // Members
  /**
   * The club's office bearers, already in hierarchy order.
   *
   * The order is the server's, not ours: it knows the club hierarchy and the
   * list is small enough to send whole. Re-sorting here would be a second place
   * for that order to be wrong.
   */
  getTeam: async (): Promise<PublicMember[]> => {
    const response = await apiClient.get<ApiResponse<PublicMember[]>>("/api/users/team");
    return response.data.data ?? [];
  },

  /**
   * The searchable membership, first page.
   *
   * Returns the total alongside, so the page can say honestly how many it is
   * showing rather than implying the list is everyone.
   */
  getDirectory: async (size = 100): Promise<{ members: PublicMember[]; total: number }> => {
    const response = await apiClient.get(`/api/users?size=${size}`);
    const paged = response.data?.data;
    return {
      members: (paged?.content ?? []) as PublicMember[],
      total: Number(paged?.totalElements ?? 0),
    };
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
  },

  updateProfile: async (data: ProfileUpdateRequest): Promise<Profile> => {
    const response = await apiClient.put<ApiResponse<UserProfileResponse>>(
      "/api/users/profile",
      data
    );
    return mapUserToProfile(response.data.data);
  }
};
