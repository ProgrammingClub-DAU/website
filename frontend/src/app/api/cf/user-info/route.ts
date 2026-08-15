import { NextRequest, NextResponse } from "next/server";

/**
 * Authenticated proxy route for the Codeforces user.info API.
 * Runs server-side to avoid CORS issues. Responses are cached for 1 hour.
 *
 * Security controls added (Finding 4 — CF Sync Audit, 14 Aug 2026):
 *
 * 1. **Authentication required.** The caller must supply a valid JWT in the
 *    Authorization header. Unauthenticated requests are rejected with 401.
 *    Without this, the route is an open relay — anyone can look up arbitrary
 *    handles, and every distinct handle is a fresh outbound Codeforces request.
 *    Enough distinct lookups trip the CF rate limit and break the club-wide sync.
 *
 * 2. **Handle must belong to a registered member.** The handle is validated
 *    against our own backend before being forwarded to Codeforces. This prevents
 *    the route being used to enumerate arbitrary CF accounts. The backend endpoint
 *    (/api/users/handle/{handle}/exists) is itself authenticated-only, so the
 *    user's token is forwarded for that check too.
 *
 * The `revalidate: 3600` cache only de-duplicates repeated lookups of the *same*
 * handle — it does not protect against handle enumeration, which rotates the
 * cache key every request.
 *
 * GET /api/cf/user-info?handle=tourist
 */
export async function GET(request: NextRequest) {
  // ── 1. Authentication ────────────────────────────────────────────────────────
  const authHeader = request.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return NextResponse.json(
      { error: "Authentication required to look up Codeforces profiles." },
      { status: 401 }
    );
  }

  // ── 2. Handle param ──────────────────────────────────────────────────────────
  const handle = request.nextUrl.searchParams.get("handle");
  if (!handle || handle.trim() === "") {
    return NextResponse.json(
      { error: "handle query param is required" },
      { status: 400 }
    );
  }
  const trimmedHandle = handle.trim();

  // ── 3. Validate handle belongs to a registered member ───────────────────────
  const backendUrl = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080")
    .replace("localhost", "127.0.0.1");
  try {
    const check = await fetch(
      `${backendUrl}/api/users/handle/${encodeURIComponent(trimmedHandle)}/exists`,
      {
        headers: { Authorization: authHeader },
        // Do not cache this check — membership can change.
        cache: "no-store",
      }
    );
    if (!check.ok) {
      // 404 = handle not registered; any other non-ok = treat as not allowed.
      return NextResponse.json(
        { error: "This Codeforces handle is not linked to any registered member." },
        { status: 403 }
      );
    }
  } catch {
    return NextResponse.json(
      { error: "Could not verify handle with the club backend." },
      { status: 502 }
    );
  }

  // ── 4. Forward to Codeforces ─────────────────────────────────────────────────
  try {
    const res = await fetch(
      `https://codeforces.com/api/user.info?handles=${encodeURIComponent(trimmedHandle)}`,
      {
        // Cache the response for 1 hour via Next.js Data Cache.
        next: { revalidate: 3600 },
      }
    );

    if (!res.ok) {
      return NextResponse.json(
        { error: "Codeforces API returned an error" },
        { status: 502 }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: "Failed to reach Codeforces API" },
      { status: 502 }
    );
  }
}
