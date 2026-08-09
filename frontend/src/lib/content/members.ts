import type { CfRankKey } from "@/lib/cf-ranks";

export type Member = {
  name: string;
  initials: string;
  batch: string;
  group: "Core team" | "Associate team" | "Batch representatives";
  role: string;
  cf: CfRankKey;
  about: string;
};

export const memberGroups = [
  "Everyone",
  "Core team",
  "Associate team",
  "Batch representatives",
] as const;

const PLACEHOLDER_ABOUT = "[PLACEHOLDER] Short note about this member, written by them.";

/** Sample directory data, not real member data. */
export const members: Member[] = [
  { name: "[SAMPLE] A. Rao", initials: "AR", batch: "B.Tech ’27", group: "Core team", role: "Club lead", cf: "master", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] T. Nair", initials: "TN", batch: "B.Tech ’27", group: "Core team", role: "Contest ops", cf: "grandmaster", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] M. Patel", initials: "MP", batch: "B.Tech ’27", group: "Core team", role: "Problem setter", cf: "candidate", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] K. Verma", initials: "KV", batch: "B.Tech ’28", group: "Associate team", role: "Editorials", cf: "expert", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] P. Joshi", initials: "PJ", batch: "B.Tech ’28", group: "Associate team", role: "Events", cf: "expert", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] S. Iyer", initials: "SI", batch: "M.Tech ’26", group: "Associate team", role: "Outreach", cf: "pupil", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] N. Desai", initials: "ND", batch: "B.Tech ’28", group: "Batch representatives", role: "Batch of ’28 rep", cf: "specialist", about: PLACEHOLDER_ABOUT },
  { name: "[SAMPLE] R. Shah", initials: "RS", batch: "B.Tech ’29", group: "Batch representatives", role: "Batch of ’29 rep", cf: "newbie", about: PLACEHOLDER_ABOUT },
];

/** A permanent record: contributors stay listed after their term ends. */
export const credits = [
  { years: "[PLACEHOLDER] Year", name: "[SAMPLE] N. Desai", initials: "ND", work: "Design and front-end build of this site" },
  { years: "[PLACEHOLDER] Year", name: "[SAMPLE] R. Shah", initials: "RS", work: "Codeforces sync and leaderboard service" },
  { years: "[PLACEHOLDER] Year", name: "[SAMPLE] K. Verma", initials: "KV", work: "Auth, profiles, and member directory" },
];
