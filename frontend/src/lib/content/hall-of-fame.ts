import type { CfRankKey } from "@/lib/cf-ranks";

export type HofEntry = {
  cat: string;
  title: string;
  people: string;
  note: string;
  cf: CfRankKey;
};

export type HofYear = {
  year: string;
  label: string;
  entries: HofEntry[];
};

/** Every entry is placeholder copy pending confirmation against club records. */
export const hallOfFame: HofYear[] = [
  {
    year: "2025–26",
    label: "Current session",
    entries: [
      {
        cat: "ICPC",
        title: "[PLACEHOLDER] Regional standing",
        people: "[PLACEHOLDER] Team + members",
        note: "[PLACEHOLDER] Round reached and rank. Confirm from official standings before publishing.",
        cf: "master",
      },
      {
        cat: "Contest",
        title: "[PLACEHOLDER] Intra-DAU winner",
        people: "[PLACEHOLDER] Winner handle",
        note: "[PLACEHOLDER] Contest name, date, and score.",
        cf: "expert",
      },
      {
        cat: "Milestone",
        title: "[PLACEHOLDER] Rating milestone",
        people: "[PLACEHOLDER] Member handle",
        note: "[PLACEHOLDER] Rank crossed and on which platform.",
        cf: "candidate",
      },
    ],
  },
  {
    year: "2024–25",
    label: "Archive",
    entries: [
      {
        cat: "Hackathon",
        title: "[PLACEHOLDER] Hackathon result",
        people: "[PLACEHOLDER] Team",
        note: "[PLACEHOLDER] Event, organiser, and placement.",
        cf: "specialist",
      },
      {
        cat: "Alumni",
        title: "[PLACEHOLDER] Alumni highlight",
        people: "[PLACEHOLDER] Name, batch",
        note: "[PLACEHOLDER] Where they are now, with their consent to be listed.",
        cf: "grandmaster",
      },
    ],
  },
  {
    year: "2023–24",
    label: "Archive",
    entries: [
      {
        cat: "ICPC",
        title: "[PLACEHOLDER] Qualification",
        people: "[PLACEHOLDER] Team",
        note: "[PLACEHOLDER] Prelims/regionals result.",
        cf: "candidate",
      },
      {
        cat: "Contest",
        title: "[PLACEHOLDER] Long contest winner",
        people: "[PLACEHOLDER] Winner handle",
        note: "[PLACEHOLDER] Contest format and duration.",
        cf: "pupil",
      },
    ],
  },
  {
    year: "Earlier",
    label: "Pre-2023",
    entries: [
      {
        cat: "Alumni",
        title: "[PLACEHOLDER] Notable alumni",
        people: "[PLACEHOLDER] Names",
        note: "[PLACEHOLDER] Club records from earlier batches, if available.",
        cf: "newbie",
      },
    ],
  },
];

/** First four entries across the two most recent years, for the Home/About teaser. */
export const hallOfFameTeaser = hallOfFame
  .flatMap((y) => y.entries.map((e) => ({ ...e, year: y.year })))
  .slice(0, 4);
