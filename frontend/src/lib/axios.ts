/**
 * Axios HTTP Client Instance
 * Purpose: Pre-configured Axios instance with a request interceptor for attaching bearer tokens.
 * Auth Connection: Automatically reads the token from `useAuthStore` and appends `Authorization: Bearer <token>`.
 * Deferred: Actual HTTP call execution until real backend endpoints exist in Stage 3.
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
    if (error.response?.status === 401 && !isAuthEndpoint) {
      useAuthStore.getState().logout();
      if (typeof window !== "undefined") {
        // eslint-disable-next-line @next/next/no-location-assign-relative-destination
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
