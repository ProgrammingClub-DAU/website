export const CF_RANKS = [
  { key: "newbie", name: "Newbie", color: "var(--cf-newbie)" },
  { key: "pupil", name: "Pupil", color: "var(--cf-pupil)" },
  { key: "specialist", name: "Specialist", color: "var(--cf-specialist)" },
  { key: "expert", name: "Expert", color: "var(--cf-expert)" },
  { key: "candidate", name: "Candidate Master", color: "var(--cf-candidate)" },
  { key: "master", name: "Master", color: "var(--cf-master)" },
  { key: "grandmaster", name: "Grandmaster", color: "var(--cf-grandmaster)" },
] as const;

export type CfRankKey = (typeof CF_RANKS)[number]["key"];

export function rankColor(key: CfRankKey) {
  return CF_RANKS.find((r) => r.key === key)!.color;
}

/**
 * Maps a Codeforces rating onto a rank key, using Codeforces' own thresholds.
 *
 * `null` covers members who have not linked an account or have never competed;
 * they fall to Newbie rather than being excluded, so the directory still shows
 * them. Lives here rather than in a component so the leaderboard and the member
 * directory cannot drift apart.
 */
export function ratingToRank(rating: number | null | undefined): CfRankKey {
  if (rating == null) return "newbie";
  if (rating >= 2400) return "grandmaster";
  if (rating >= 2100) return "master";
  if (rating >= 1900) return "candidate";
  if (rating >= 1600) return "expert";
  if (rating >= 1400) return "specialist";
  if (rating >= 1200) return "pupil";
  return "newbie";
}
