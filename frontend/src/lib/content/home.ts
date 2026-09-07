import type { PlatformId } from "@/components/site/platform-mark";
import type { CfRankKey } from "@/lib/cf-ranks";

/** Fake-but-plausible sample rows for the hero panel. NOT real member data. */
export const heroRows: {
  rank: string;
  handle: string;
  rating: number;
  delta: string;
  cf: CfRankKey;
}[] = [
  { rank: "01", handle: "arjun_dp", rating: 2114, delta: "+38", cf: "master" },
  { rank: "02", handle: "meher.solves", rating: 1902, delta: "+21", cf: "candidate" },
  { rank: "03", handle: "kx_bitset", rating: 1673, delta: "−12", cf: "expert" },
  { rank: "04", handle: "nidhi_ac", rating: 1408, delta: "+64", cf: "specialist" },
];

export const sparkline = [7, 10, 6, 12, 9, 14, 11, 16, 12, 18, 13, 15, 17, 18];

/**
 * `platforms` is optional and set on step 02 alone. Annotated explicitly rather
 * than inferred: with mixed shapes TypeScript widens the array to a union and
 * `step.platforms` then fails to compile on the steps that omit it.
 */
export const howItWorks: {
  n: string;
  title: string;
  body: string;
  platforms?: readonly PlatformId[];
}[] = [
  {
    n: "01",
    title: "Register",
    body: "Sign up with your DAU email. No prior contest experience needed.",
  },
  {
    n: "02",
    title: "Link your handles",
    body: "Add Codeforces, CodeChef, LeetCode, or AtCoder to your profile.",
    // Rendered as marks beneath the copy. Order matches the sentence above.
    platforms: ["codeforces", "codechef", "leetcode", "atcoder"] as const,
  },
  {
    n: "03",
    title: "Climb the leaderboard",
    body: "Solve, contest, and watch your rating and rank color update.",
  },
];
