/** Display helpers for skill-based practice: topic names, verdicts, and how long ago. */

const TOPIC_LABELS: Record<string, string> = {
  dp: "DP",
  "dfs and similar": "DFS & similar",
  "constructive algorithms": "Constructive",
  "data structures": "Data structures",
  "number theory": "Number theory",
  "binary search": "Binary search",
  "two pointers": "Two pointers",
  "brute force": "Brute force",
};

/** A Codeforces tag as a readable topic name: "dp" -> "DP", "graphs" -> "Graphs". */
export function topicLabel(tag: string): string {
  return TOPIC_LABELS[tag] ?? tag.charAt(0).toUpperCase() + tag.slice(1);
}

const VERDICT_LABELS: Record<string, string> = {
  WRONG_ANSWER: "Wrong answer",
  TIME_LIMIT_EXCEEDED: "Time limit",
  MEMORY_LIMIT_EXCEEDED: "Memory limit",
  RUNTIME_ERROR: "Runtime error",
  IDLENESS_LIMIT_EXCEEDED: "Idleness limit",
  PRESENTATION_ERROR: "Presentation error",
  CHALLENGED: "Hacked",
  PARTIAL: "Partial",
};

/** A Codeforces verdict as a short label: WRONG_ANSWER -> "Wrong answer". */
export function verdictLabel(verdict: string): string {
  if (VERDICT_LABELS[verdict]) return VERDICT_LABELS[verdict];
  const words = verdict.toLowerCase().replace(/_/g, " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** "today", "yesterday", "5 days ago", "2 months ago". */
export function timeAgo(date: Date, now: Date = new Date()): string {
  const days = Math.floor((now.getTime() - date.getTime()) / 86_400_000);
  if (days <= 0) return "today";
  if (days === 1) return "yesterday";
  if (days < 30) return `${days} days ago`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months} month${months === 1 ? "" : "s"} ago`;
  const years = Math.floor(days / 365);
  return `${years} year${years === 1 ? "" : "s"} ago`;
}

/** Focus topics are picked once a member has this many rated solves (matches the backend). */
export const MIN_SOLVES_FOR_FOCUS = 30;
