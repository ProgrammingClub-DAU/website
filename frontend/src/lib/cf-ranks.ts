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
