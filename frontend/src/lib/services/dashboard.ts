import apiClient from "@/lib/axios";
import { Member, Profile, LeaderboardEntry } from "@/types/api";
import { members as mockMembers } from "@/lib/content/members";
import { mockLeaderboardEntries, getMockProfile } from "@/lib/content/mock-dashboards";

// Use real API integrations
const IS_MOCK = false;

interface ApiUserResponse {
  id?: number;
  userId?: number;
  name?: string;
  email?: string;
  codeforcesHandle?: string;
  rating?: number;
  role?: string;
  createdAt?: string;
}

const getInitials = (name: string) =>
  name
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

export const dashboardService = {
  // Members
  getMembers: async (): Promise<Member[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockMembers), 500));
    }
    try {
      const response = await apiClient.get("/api/users");
      const content = response.data?.data?.content;
      if (!Array.isArray(content)) return [];

      return content.map((item: ApiUserResponse) => ({
        id: item.id?.toString() ?? "",
        name: item.name ?? "",
        initials: getInitials(item.name ?? ""),
        batch: "",
        group: "" as Member["group"],
        role: item.role || "ROLE_USER",
        cf: item.rating ? "specialist" : "newbie",
        about: "",
        codeforcesHandle: item.codeforcesHandle ?? "",
        rating: item.rating ?? 0,
      }));
    } catch (error) {
      console.error("Error fetching members:", error);
      return [];
    }
  },

  // Leaderboard
  getLeaderboard: async (): Promise<LeaderboardEntry[]> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(mockLeaderboardEntries), 500));
    }
    try {
      const response = await apiClient.get("/api/leaderboard");
      const content = response.data?.data?.content;
      if (!Array.isArray(content)) return [];

      return content.map((item: ApiUserResponse) => ({
        id: item.userId ?? item.id ?? 0,
        name: item.name ?? "",
        email: item.email ?? "",
        codeforcesHandle: item.codeforcesHandle ?? "",
        rating: item.rating ?? 0,
        role: item.role || "ROLE_USER",
        createdAt: item.createdAt ?? "",
      }));
    } catch (error) {
      console.error("Error fetching leaderboard:", error);
      return [];
    }
  },

  // Profile
  getProfile: async (userId?: string): Promise<Profile | null> => {
    if (IS_MOCK) {
      return new Promise((resolve) => setTimeout(() => resolve(getMockProfile(userId || "1")), 500));
    }
    try {
      const response = await apiClient.get("/api/users/profile");
      const user = response.data?.data as ApiUserResponse | undefined;
      if (!user) return null;

      return {
        id: user.id ?? 0,
        name: user.name ?? "",
        email: user.email ?? "",
        codeforcesHandle: user.codeforcesHandle ?? "",
        rating: user.rating ?? 0,
        role: user.role ?? "ROLE_USER",
        createdAt: user.createdAt ?? "",
        avatarUrl: null,
        maxRating: user.rating ?? 0,
        clubRole: "Club Participant",
        eventParticipations: [],
        platformStats: [],
        ratingHistory: [],
        activityData: [],
      };
    } catch (error) {
      console.error("Error fetching profile:", error);
      return null;
    }
  }
};
