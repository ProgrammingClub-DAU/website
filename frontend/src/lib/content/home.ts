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
 * Real figures about a real club, so nothing here may be invented: a visitor
 * reads these as fact. Replace each with a number confirmed against club
 * records before launch.
 */
export const stats = [
  { value: "[TBC]", label: "Active members" },
  { value: "[TBC]", label: "Problems solved" },
  { value: "[TBC]", label: "Contests run" },
  { value: "[TBC]", label: "ICPC teams sent" },
];

export const howItWorks = [
  {
    n: "01",
    title: "Register",
    body: "Sign up with your DAU email. No prior contest experience needed.",
  },
  {
    n: "02",
    title: "Link your handles",
    body: "Add Codeforces, CodeChef, LeetCode, or AtCoder to your profile.",
  },
  {
    n: "03",
    title: "Climb the leaderboard",
    body: "Solve, contest, and watch your rating and rank color update.",
  },
];
