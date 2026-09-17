import type { RatingHistoryEntry } from "@/types/api";

export const leetcodeService = {
  /**
   * A member's full LeetCode contest rating history, oldest first.
   *
   * Read from LeetCode through /next-api/lc/contest-history, so it is complete
   * from the member's first contest and needs no sign-in -- like the Codeforces
   * chart beside it.
   *
   * @returns the rated contests, or an empty list on any failure
   */
  getContestHistory: async (handle: string): Promise<RatingHistoryEntry[]> => {
    if (!handle || handle.trim() === "") return [];

    try {
      const res = await fetch(`/next-api/lc/contest-history?handle=${encodeURIComponent(handle.trim())}`);
      if (!res.ok) return [];

      const data = (await res.json()) as { history?: RatingHistoryEntry[] };
      return data.history ?? [];
    } catch {
      // LeetCode unreachable: the chart shows its empty state.
      return [];
    }
  },
};
