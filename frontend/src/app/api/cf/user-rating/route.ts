import { NextRequest, NextResponse } from "next/server";

/**
 * Proxy route for Codeforces user.rating API.
 * Runs server-side to avoid CORS issues. Responses are cached for 1 hour.
 *
 * GET /api/cf/user-rating?handle=tourist
 */
export async function GET(request: NextRequest) {
  const handle = request.nextUrl.searchParams.get("handle");

  if (!handle || handle.trim() === "") {
    return NextResponse.json({ error: "handle query param is required" }, { status: 400 });
  }

  try {
    const res = await fetch(
      `https://codeforces.com/api/user.rating?handle=${encodeURIComponent(handle.trim())}`,
      {
        // Cache the response for 1 hour via Next.js ISR (revalidate on the server)
        next: { revalidate: 3600 },
      }
    );

    if (!res.ok) {
      return NextResponse.json({ error: "Codeforces API returned an error" }, { status: 502 });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json({ error: "Failed to reach Codeforces API" }, { status: 502 });
  }
}
