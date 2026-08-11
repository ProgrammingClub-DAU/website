/**
 * Axios HTTP Client Instance
 * Purpose: Pre-configured Axios instance with a request interceptor for attaching bearer tokens.
 * Auth Connection: Automatically reads the token from `useAuthStore` and appends `Authorization: Bearer <token>`.
 *
 * This module is reachable from Server Components — `lib/services/dashboard.ts`
 * imports it at module scope — so `useAuthStore` instantiates in the Node
 * process as a singleton shared across every request. Reads are harmless there
 * (no token is ever set server-side), but writes must stay browser-only or one
 * user's session state would follow the next request on the same process.
 */

import axios from "axios";
import { useAuthStore } from "@/store/auth";

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    // Inject Bearer token from Zustand store if present
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Session expiration handler: 401 on protected requests triggers local logout and login redirect
    const url = error.config?.url || "";
    const isAuthEndpoint = url.includes("/api/auth/login") || url.includes("/api/auth/register");
    // Browser-only: the store is a shared singleton on the server, so clearing
    // it there would sign out whoever the process serves next.
    if (
      error.response?.status === 401 &&
      !isAuthEndpoint &&
      typeof window !== "undefined"
    ) {
      useAuthStore.getState().logout();
      // A full load rather than router.push: this runs at module scope where no
      // router exists, and it clears any other client state along with it.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default apiClient;
