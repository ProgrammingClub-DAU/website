import type { CfRankKey } from "@/lib/cf-ranks";

export type ClubEvent = {
  type: "Flagship" | "Contests" | "Workshops" | "ICPC";
  date: string;
  title: string;
  body: string;
  meta: string;
  dots: CfRankKey[];
};

export type NextEvent = {
  title: string;
  summary: string;
  /** Rows of the detail table beside the copy: date, time, venue, format. */
  meta: { k: string; v: string }[];
};

/**
 * The event pinned to the top of the events page, or `null` when nothing is
 * scheduled.
 *
 * It used to be a fixed block of copy reading "[PLACEHOLDER] Event name" under
 * a live "Next up" pulse, beside a detail table whose every row said
 * [PLACEHOLDER] — an announcement, in the styling of a real one, for an event
 * that did not exist. The page now renders an honest panel instead when this is
 * null, so announcing a real round is a matter of filling this in.
 */
export const nextEvent: NextEvent | null = null;

/**
 * Events the club has actually held.
 *
 * Six of the seven entries here were invented — a winter long contest, a
 * beginner C series, an ICPC practice camp, a weekly round series, a guest
 * session and an intra-DAU contest — each marked [PLACEHOLDER] in its own title
 * and body, and each rendered on the page in exactly the layout a real event
 * gets. They have been removed rather than relabelled: an archive that lists
 * events that never happened is not an archive.
 *
 * Add real events here as they are confirmed against club records.
 */
export const events: ClubEvent[] = [
  {
    type: "Contests",
    date: "March 2025",
    title: "Spring Code Sprint",
    body: "Our annual spring competitive programming contest. 5 problems, 2 hours. Great for all skill levels.",
    meta: "120 participants",
    dots: ["master", "candidate", "expert", "specialist"],
  },
];

/**
 * Filter chips for the timeline, derived from the events that exist.
 *
 * Previously a fixed list of all four categories, which meant three of the four
 * chips filtered down to an empty state — a control that only ever reports
 * having found nothing. Deriving it means a category appears once the club has
 * run something in it.
 */
export const eventTypes: string[] = [
  "All events",
  ...Array.from(new Set(events.map((e) => e.type))),
];
