"use client";

import { useEffect } from "react";
import apiClient, { SLOW_REQUEST_TIMEOUT_MS } from "@/lib/axios";
import { tokenClaims, useAuthStore, type ApiResponse, type AuthResponse } from "@/store/auth";

/** A token younger than this is not worth swapping; it saves a request per page load. */
const RENEW_AFTER_MS = 60 * 60 * 1000;

/**
 * Keeps an active member signed in.
 *
 * Tokens expire (30 days, app.jwt.expiration-ms). On each full page load this swaps
 * the stored token for a fresh one, so the clock restarts whenever the member comes
 * back and only a month away signs them out. Renders nothing.
 *
 * A failed renewal changes nothing: the current token still works until it expires,
 * and the next visit tries again. That covers the slow first request after the
 * backend has been asleep on Render's free tier.
 */
export function SessionKeeper() {
  useEffect(() => {
    const { token } = useAuthStore.getState();
    if (!token) return;

    const issuedAt = tokenClaims(token)?.iat;
    if (issuedAt && Date.now() - issuedAt * 1000 < RENEW_AFTER_MS) return;

    apiClient
      .post<ApiResponse<AuthResponse>>("/api/auth/refresh", null, { timeout: SLOW_REQUEST_TIMEOUT_MS })
      .then((res) => {
        const fresh = res.data?.data?.token;
        // Only if nobody signed out or in while the request was out.
        if (fresh && useAuthStore.getState().token === token) {
          useAuthStore.getState().setToken(fresh);
        }
      })
      .catch((err) => {
        // 401: the token is no good (account deleted, secret rotated). Sign out
        // here, quietly; the page the member is on may not need an account.
        if (err?.response?.status === 401 && useAuthStore.getState().token === token) {
          useAuthStore.getState().logout();
        }
      });
  }, []);

  return null;
}
