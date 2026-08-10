import type { CfRankKey } from "@/lib/cf-ranks";

export type ClubEvent = {
  type: "Flagship" | "Contests" | "Workshops" | "ICPC";
  date: string;
  title: string;
  body: string;
  meta: string;
  dots: CfRankKey[];
};

export const eventTypes = ["All events", "Flagship", "Contests", "Workshops", "ICPC"] as const;

export const nextEventMeta = [
  { k: "Date", v: "[PLACEHOLDER]" },
  { k: "Time", v: "[PLACEHOLDER] IST" },
  { k: "Venue", v: "[PLACEHOLDER] Lab / online" },
  { k: "Format", v: "[PLACEHOLDER] problems, duration" },
];

export const events: ClubEvent[] = [
  {
    type: "Contests",
    date: "March 2025",
    title: "Spring Code Sprint",
    body: "Our annual spring competitive programming contest. 5 problems, 2 hours. Great for all skill levels.",
    meta: "120 participants",
    dots: ["master", "candidate", "expert", "specialist"],
  },
  {
    type: "Flagship",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] Intra-DAU Programming Contest",
    body: "[PLACEHOLDER] The club-wide campus contest. Add format, rounds, and prizes.",
    meta: "[TBC] participants",
    dots: ["grandmaster", "candidate", "expert"],
  },
  {
    type: "Contests",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] Winter long contest",
    body: "[PLACEHOLDER] Multi-day contest over the break. Add duration and problem count.",
    meta: "[TBC] participants",
    dots: ["expert", "specialist"],
  },
  {
    type: "Workshops",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] Beginner C and logic-building series",
    body: "[PLACEHOLDER] Sessions for first-years: syntax, complexity, and a first set of problems.",
    meta: "[TBC] attendees",
    dots: ["newbie", "pupil"],
  },
  {
    type: "ICPC",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] ICPC prelims practice camp",
    body: "[PLACEHOLDER] Team practice on past regional sets, with a debrief per session.",
    meta: "[TBC] teams",
    dots: ["master", "candidate", "expert"],
  },
  {
    type: "Contests",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] Weekly round series",
    body: "[PLACEHOLDER] The regular weekly contest. Add the season and number of rounds held.",
    meta: "[TBC] rounds",
    dots: ["specialist", "expert"],
  },
  {
    type: "Workshops",
    date: "[PLACEHOLDER] Month, Year",
    title: "[PLACEHOLDER] Guest session",
    body: "[PLACEHOLDER] Alumni or invited speaker session — topic and speaker, with their consent.",
    meta: "[TBC] attendees",
    dots: ["candidate"],
  },
];
