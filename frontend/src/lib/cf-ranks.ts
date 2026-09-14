/**
 * The Codeforces rank ladder — the site's colour language in one place.
 *
 * `min` is the rating floor of each band, and it is the single source of truth:
 * {@link ratingToRank} derives its answer from this list rather than repeating
 * the numbers, so the ladder drawn on the home page and the rank assigned to a
 * member cannot disagree. The entries must stay ordered by ascending `min`.
 *
 * The bands collapse International Master (2300–2399) into Master, which is a
 * simplification this site has always made — seven colours is already the most
 * a legend can carry without becoming a table.
 */
export const CF_RANKS = [
  { key: "newbie", name: "Newbie", short: "Newbie", min: 0, color: "var(--cf-newbie)" },
  { key: "pupil", name: "Pupil", short: "Pupil", min: 1200, color: "var(--cf-pupil)" },
  { key: "specialist", name: "Specialist", short: "Spec.", min: 1400, color: "var(--cf-specialist)" },
  { key: "expert", name: "Expert", short: "Expert", min: 1600, color: "var(--cf-expert)" },
  { key: "candidate", name: "Candidate Master", short: "CM", min: 1900, color: "var(--cf-candidate)" },
  { key: "master", name: "Master", short: "Master", min: 2100, color: "var(--cf-master)" },
  { key: "grandmaster", name: "Grandmaster", short: "GM", min: 2400, color: "var(--cf-grandmaster)" },
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
 *
 * Walks {@link CF_RANKS} from the top down and takes the first band the rating
 * clears. Previously this was a hand-written ladder of `if` statements holding a
 * second copy of the thresholds; deriving it means adding or retuning a band is
 * a one-line edit above rather than two edits that have to agree.
 */
export function ratingToRank(rating: number | null | undefined): CfRankKey {
  if (rating == null) return "newbie";
  for (let i = CF_RANKS.length - 1; i > 0; i--) {
    if (rating >= CF_RANKS[i].min) return CF_RANKS[i].key;
  }
  return "newbie";
}
