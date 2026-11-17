/**
 * Auth Zustand State Store
 * Purpose: Centralized global state store managing authentication status and user details.
 * Auth Connection: Provides user, token, role, login(), logout(), and isAuthenticated state across components.
 *
 * The store, token included, is persisted to localStorage under "cpclub-auth", so
 * a member stays signed in across refreshes, tabs and restarts until they sign out
 * or the JWT expires (30 days, app.jwt.expiration-ms). SessionKeeper renews the
 * token on each visit, so in practice only a month away signs a member out. The cost is that any script running on
 * the site could read the token, which is why third-party scripts are kept to Google
 * Sign-In and the Cloudinary upload widget, and a new one needs a good reason.
 * Moving to an httpOnly cookie would need the backend to issue it; change both together.
 */

import type { AcademicYear } from "@/types/api";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { ClubRole } from "@/types/api";

export interface User {
  id: number; // Persisted unique user identifier on the backend
  email: string;
  fullName: string;
  role: string;
  codeforcesHandle: string | null; // Optional synced Codeforces profile handle
  clubRole: ClubRole | null;
  batchYear: number | null;
  leetcodeHandle: string | null;
  phoneNumber: string | null;
  avatarUrl: string | null;
  emailVerified?: boolean;
}

// Standard API response envelope matching com.cpclub.backend.common.dto.ApiResponse
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// Successful sign-in payload matching com.cpclub.backend.auth.dto.AuthResponse
export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: string;
  codeforcesHandle: string | null;
  emailVerified?: boolean;
  /**
   * Null when the member has never been asked their year, which is how the
   * sign-in page knows to send them to /welcome instead of the site.
   */
  academicYear: AcademicYear | null;
}

/**
 * Shared mapper translating backend AuthResponse DTO into frontend User model.
 * Handles the name -> fullName field translation.
 */
export function mapAuthResponseToUser(authData: AuthResponse): User {
  return {
    id: authData.id,
    email: authData.email,
    fullName: authData.name,
    role: authData.role,
    codeforcesHandle: authData.codeforcesHandle,
    clubRole: null,
    batchYear: null,
    leetcodeHandle: null,
    phoneNumber: null,
    avatarUrl: null,
    emailVerified: authData.emailVerified ?? true,
  };
}

/** The parts of our JWT the browser reads. Seconds since the epoch, as in the spec. */
export interface TokenClaims {
  iat?: number;
  exp?: number;
}

/**
 * Reads a token's claims without verifying it. Only for deciding when to renew or
 * drop the token; the backend verifies the signature on every request.
 */
export function tokenClaims(token: string): TokenClaims | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    return JSON.parse(atob(padded)) as TokenClaims;
  } catch {
    return null;
  }
}

/** True when the token says it has expired. A token we cannot read is left to the backend. */
function isExpired(token: string): boolean {
  const exp = tokenClaims(token)?.exp;
  return typeof exp === "number" && exp * 1000 <= Date.now();
}

interface AuthState {
  user: User | null;
  token: string | null;
  role: string | null;
  isAuthenticated: boolean;
  login: (user: User, token: string) => void;
  /**
   * Merges freshly fetched profile fields into the stored user.
   *
   * The role is captured at sign-in, so a member promoted to admin since then
   * would keep seeing the member menu until their next login, and a demoted
   * admin would keep seeing admin links the API already refuses.
   */
  syncUser: (patch: Partial<User>) => void;
  /** Swaps in a renewed token for the same member; see SessionKeeper. */
  setToken: (token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      role: null,
      isAuthenticated: false,

      login: (user: User, token: string) =>
        set({
          user,
          token,
          role: user.role,
          isAuthenticated: true,
        }),

      syncUser: (patch: Partial<User>) =>
        set((state) =>
          state.user
            ? { user: { ...state.user, ...patch }, role: patch.role ?? state.role }
            : state
        ),

      setToken: (token: string) =>
        set((state) => (state.isAuthenticated ? { token } : state)),

      logout: () =>
        set({
          user: null,
          token: null,
          role: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: "cpclub-auth",
      storage: createJSONStorage(() => localStorage),
      version: 1,
      // A token that expired while the site was closed is dropped on load, before
      // anything renders. Otherwise the page showed the member as signed in, its
      // first request came back 401, and the interceptor bounced them to /login
      // from whatever public page they had opened.
      onRehydrateStorage: () => (state) => {
        if (state?.token && isExpired(state.token)) {
          state.logout();
        }
      },
      migrate: (persistedState: unknown) => {
        const state = persistedState as AuthState;

        return {
          ...state,
          user: state.user
          ? {
              ...state.user,
              clubRole: state.user.clubRole ?? null,
              batchYear: state.user.batchYear ?? null,
              leetcodeHandle: state.user.leetcodeHandle ?? null,
              phoneNumber: state.user.phoneNumber ?? null,
              avatarUrl: state.user.avatarUrl ?? null,
            }
          : null,
        };
      },
    }
  )
);
