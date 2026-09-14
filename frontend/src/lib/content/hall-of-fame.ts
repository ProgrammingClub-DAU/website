import type { CfRankKey } from "@/lib/cf-ranks";

export type HofEntry = {
  cat: string;
  title: string;
  people: string;
  note: string;
  cf: CfRankKey;
  /**
   * Path under /public to a photograph of this specific achievement.
   *
   * Optional, and currently unset on every entry, so no polaroid renders. The
   * timeline previously showed one shared placeholder image on all of them,
   * captioned "Event photo for {title}" — which claimed to be a photo of each
   * distinct event while being the same graphic every time. Better to show
   * nothing until real photographs exist; set this on an entry and its polaroid
   * appears on its own.
   */
  photo?: string;
};

export type HofYear = {
  year: string;
  label: string;
  entries: HofEntry[];
};

/**
 * The club's record of results.
 *
 * Eight of the nine entries this file used to hold were invented and marked
 * [PLACEHOLDER] in every field — a regional standing, an intra-DAU winner, a
 * rating milestone, a hackathon result, two alumni highlights, an ICPC
 * qualification and a long-contest winner — spread across four academic years
 * to make the archive look established. They were rendered in the same cards,
 * with the same rank colours, that a real achievement gets, and four of them
 * were on the home page.
 *
 * A hall of fame that lists achievements nobody earned is the one page on a
 * club site that must not be padded, so they are gone rather than relabelled.
 * What is left is the single entry the file did not mark as invented, and it
 * still needs confirming against official standings before it is relied on.
 *
 * Add real results here as club records confirm them.
 */
export const hallOfFame: HofYear[] = [
  {
    year: "2025–26",
    label: "Current session",
    entries: [
      {
        cat: "ICPC",
        title: "ICPC Amritapuri Regionals",
        people: "Team DAUCoders (Sumeet Verma, Jalp Patel, King-T)",
        note: "Qualified for regionals and placed top 50 in India.",
        cf: "master",
      },
    ],
  },
];

/** First four entries across the two most recent years, for the Home/About teaser. */
export const hallOfFameTeaser = hallOfFame
  .flatMap((y) => y.entries.map((e) => ({ ...e, year: y.year })))
  .slice(0, 4);
