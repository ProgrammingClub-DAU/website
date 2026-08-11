/**
 * Auth Zustand State Store
 * Purpose: Centralized in-memory global state store managing authentication status and user details.
 * Auth Connection: Provides user, token, role, login(), logout(), and isAuthenticated state across components.
 *
 * The token is held in memory only, so a refresh or any full page load signs the
 * user out. That keeps it out of localStorage and away from XSS, at the cost of
 * session continuity — revisit together, not one without the other.
 */

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface User {
  id: number; // Persisted unique user identifier on the backend
  email: string;
  fullName: string;
  role: string;
  codeforcesHandle: string | null; // Optional synced Codeforces profile handle
}

// Standard API response envelope matching com.cpclub.backend.common.dto.ApiResponse
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// Successful credentials validation payload matching com.cpclub.backend.auth.dto.AuthResponse
export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: string;
  codeforcesHandle: string | null;
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
  };
}

interface AuthState {
  user: User | null;
  token: string | null;
  role: string | null;
  isAuthenticated: boolean;
  login: (user: User, token: string) => void;
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
    }
  )
);
