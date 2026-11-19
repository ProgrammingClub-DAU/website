/** Display helpers for the admin CP Practice Group pages. */

/** A student with no activity for this many days is flagged as needing a nudge. */
export const INACTIVE_DAYS = 7;

/** Today's date in IST as yyyy-mm-dd, the calendar the backend counts days in. */
export function todayIst(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata" }).format(new Date());
}

/** Whole days from an IST date (yyyy-mm-dd) to today; null when there is no date. */
export function daysSinceIst(date: string | null): number | null {
  if (!date) return null;
  return Math.max(0, Math.round((Date.parse(todayIst()) - Date.parse(date)) / 86_400_000));
}

/** "Active today", "Yesterday", "5 days ago", "No activity yet". */
export function lastActiveText(days: number | null): string {
  if (days == null) return "No activity yet";
  if (days === 0) return "Today";
  if (days === 1) return "Yesterday";
  return `${days} days ago`;
}

/** True when the student has done nothing for INACTIVE_DAYS days, or ever. */
export function needsNudge(days: number | null): boolean {
  return days == null || days >= INACTIVE_DAYS;
}

/** "+40", "-15", "0". */
export function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

/** Up to two initials, for avatars without a photo. */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}
