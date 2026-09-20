import apiClient from "@/lib/axios";
import { Profile, PublicMember } from "@/types/api";
import type { ApiResponse } from "@/store/auth";

/**
 * How long a server-rendered page waits for the backend.
 *
 * Deliberately far longer than the client default. The backend sleeps on the
 * free tier and a cold JVM start runs 30-60 seconds, so the first visit after a
 * quiet spell would otherwise time out and render an empty page -- which looked
 * exactly like a club with no members.
 *
 * Nobody stares at a blank tab for this long in practice: it is the first
 * request after idle that pays it, and the instance stays warm afterwards.
 */
const SSR_TIMEOUT_MS = 60_000;

// ── Mappers: Transform backend UserResponseDto to Frontend Types ──

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
  maxRating?: number | null;
  equippedBannerId?: string | null;
  isPlatformCreator?: boolean;
  platformCreator?: boolean;
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
    maxRating: user.maxRating ?? user.rating,
    equippedBannerId: user.equippedBannerId ?? "rookie",
    isPlatformCreator: user.isPlatformCreator ?? user.platformCreator ?? false,
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
  getTeam: async (timeout: number = SSR_TIMEOUT_MS): Promise<PublicMember[]> => {
    const response = await apiClient.get<ApiResponse<PublicMember[]>>("/api/users/team", {
      timeout,
    });
    return response.data.data ?? [];
  },

  // Profile (requires auth)
  // Backend reads user ID securely from JWT via /api/users/profile
  getProfile: async (): Promise<Profile> => {
    const response = await apiClient.get("/api/users/profile");
    return mapUserToProfile(response.data?.data || response.data);
  },

  // Public Profile (by ID)
  getUserProfileById: async (userId: string): Promise<Profile> => {
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
