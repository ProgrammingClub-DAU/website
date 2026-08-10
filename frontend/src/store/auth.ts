/**
 * Auth Zustand State Store
 * Purpose: Centralized in-memory global state store managing authentication status and user details.
 * Auth Connection: Provides user, token, role, login(), logout(), and isAuthenticated state across components.
 * Deferred: Persistence layer (localStorage/cookies) and real backend JWT decoding (deferred to Stage 3).
 */

import { create } from "zustand";

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  role: string | null;
  isAuthenticated: boolean;
  login: (user: User, token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
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
}));
