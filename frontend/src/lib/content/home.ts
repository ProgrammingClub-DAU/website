import type { PlatformId } from "@/components/site/platform-mark";

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
    title: "Sign in",
    body: "Use your DAU Google account. No prior contest experience needed.",
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
