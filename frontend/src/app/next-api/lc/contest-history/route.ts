import { NextRequest, NextResponse } from "next/server";

/**
 * A member's LeetCode contest rating history.
 *
 * GET /next-api/lc/contest-history?handle=lee215
 *
 * The LeetCode chart on profiles used to be drawn from the site's own weekly
 * snapshots: one reading every Monday of whatever the member's rating was that
 * day. A new member, or anyone after the database was reset, therefore saw "not
 * enough data" for at least two weeks, and even then the line only began the
 * day they joined. LeetCode publishes the full history, so this reads it -- the
 * same way the Codeforces chart reads user.rating.
 *
 * Server-side for the same reasons as the Codeforces proxy: LeetCode's API does
 * not allow cross-origin browser requests, and it refuses requests without a
 * leetcode.com Referer, which a browser will not let a page set.
 */

const LEETCODE_GRAPHQL = "https://leetcode.com/graphql";

const HISTORY_QUERY = `
  query userContestRankingHistory($username: String!) {
    userContestRankingHistory(username: $username) {
      attended
      rating
      contest {
        title
        startTime
      }
    }
  }
`;

/**
 * LeetCode usernames are letters, digits, underscores, hyphens and dots.
 * Checked so this route cannot be used to send arbitrary strings to LeetCode
 * from our server.
 */
const HANDLE = /^[A-Za-z0-9_.-]{1,40}$/;

interface HistoryRow {
  attended: boolean;
  rating: number;
  contest: { title: string; startTime: number } | null;
}

export async function GET(request: NextRequest) {
  const handle = request.nextUrl.searchParams.get("handle")?.trim() ?? "";

  if (!HANDLE.test(handle)) {
    return NextResponse.json({ error: "A valid LeetCode handle is required" }, { status: 400 });
  }

  try {
    const res = await fetch(LEETCODE_GRAPHQL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // LeetCode answers 403 without this.
        Referer: "https://leetcode.com",
      },
      body: JSON.stringify({ query: HISTORY_QUERY, variables: { username: handle } }),
      // A rating only changes after a contest, so an hour is plenty.
      next: { revalidate: 3600 },
    });

    if (!res.ok) {
      return NextResponse.json({ error: "LeetCode returned an error" }, { status: 502 });
    }

    const body = (await res.json()) as {
      data?: { userContestRankingHistory?: HistoryRow[] | null };
    };
    const rows = body.data?.userContestRankingHistory ?? [];

    // LeetCode lists every contest since the account was created and marks the
    // ones the member actually entered. Only those move the rating; the rest
    // repeat the previous value and would draw a flat line through them.
    const history = rows
      .filter((row) => row.attended && row.contest)
      .map((row) => ({
        date: new Date(row.contest!.startTime * 1000).toISOString().slice(0, 10),
        rating: Math.round(row.rating),
        contestName: row.contest!.title,
      }));

    // An unknown handle and a member who has never entered a contest both come
    // back as an empty list, which is the correct answer for either.
    return NextResponse.json(
      { history },
      {
        // Also cached at the edge, so repeat profile views do not reach LeetCode.
        headers: { "Cache-Control": "public, s-maxage=3600, stale-while-revalidate=86400" },
      }
    );
  } catch {
    return NextResponse.json({ error: "Failed to reach LeetCode" }, { status: 502 });
  }
}
