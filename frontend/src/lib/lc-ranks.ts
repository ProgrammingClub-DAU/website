/**
 * LeetCode rank ladder
 * 
 * LeetCode only has two official named titles based on rating:
 * Guardian (top 5%, typically >= 2100 in community approximation)
 * Knight (top 25%, typically >= 1850 in community approximation)
 * Unrated/Participant for the rest.
 */
export const LC_RANKS = [
  { key: "unrated", name: "Unrated", short: "Unrated", min: 0, color: "var(--lc-unrated, #6b7a99)" },
  { key: "knight", name: "Knight", short: "Knight", min: 1850, color: "var(--lc-knight, #7c9eff)" },
  { key: "guardian", name: "Guardian", short: "Guardian", min: 2100, color: "var(--lc-guardian, #c084fc)" },
] as const;

export type LcRankKey = (typeof LC_RANKS)[number]["key"];

export function lcRankColor(key: LcRankKey) {
  return LC_RANKS.find((r) => r.key === key)!.color;
}

export function ratingToLcRank(rating: number | null | undefined): LcRankKey {
  if (rating == null) return "unrated";
  for (let i = LC_RANKS.length - 1; i > 0; i--) {
    if (rating >= LC_RANKS[i].min) return LC_RANKS[i].key;
  }
  return "unrated";
}

export function lcRankName(rating: number | null | undefined): string {
  if (rating == null || rating === 0) return "Unrated";
  return LC_RANKS.find((r) => r.key === ratingToLcRank(rating))?.name ?? "Unrated";
}
