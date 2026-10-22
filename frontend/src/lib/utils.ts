import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Parses a timestamp the backend stored in UTC (decision D31) -- sync runs, club
 * and external contest times, comment and blog dates, `computedAt`, `lastSyncedAt`.
 *
 * Spring serialises those as a bare `2026-10-22T18:41:43`, with no zone. Handed to
 * `new Date()` as-is, a bare date-time is read as the *browser's* local time, so
 * every one of them showed five and a half hours early in India: a sync that ran
 * at 00:00 IST appeared as "6:30 pm" the previous day.
 *
 * Do NOT use this for `eventDate`: event times are stored as IST wall-clock.
 */
export function parseServerTimestamp(value: string | null | undefined): Date | null {
  if (!value) return null
  const hasZone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(value)
  // A date with no time part ("2026-10-22") is a calendar date, not an instant.
  const isDateOnly = /^\d{4}-\d{2}-\d{2}$/.test(value)
  const date = new Date(hasZone || isDateOnly ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * `parseServerTimestamp` as a `Date`, invalid when the value is missing -- a
 * drop-in for `new Date(value)` on UTC fields.
 */
export function serverDate(value: string | null | undefined): Date {
  return parseServerTimestamp(value) ?? new Date(NaN)
}

/**
 * Parses an event's `eventDate`, which is stored as IST wall-clock time rather
 * than UTC. Pinning the offset makes it right in any browser, not only in India.
 */
export function istDate(value: string | null | undefined): Date {
  if (!value) return new Date(NaN)
  const hasZone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(value)
  return new Date(hasZone ? value : `${value}+05:30`)
}

/** `parseServerTimestamp`, formatted in IST. Returns `fallback` for a missing or bad value. */
export function formatServerTimestamp(
  value: string | null | undefined,
  options: Intl.DateTimeFormatOptions = { dateStyle: "medium", timeStyle: "short" },
  fallback = "—"
): string {
  const date = parseServerTimestamp(value)
  return date ? date.toLocaleString("en-IN", { timeZone: "Asia/Kolkata", ...options }) : fallback
}

/** "3 min ago", "2 h ago", "yesterday" -- for a UTC timestamp from the server. */
export function timeAgo(value: string | null | undefined, now: Date = new Date()): string {
  const date = parseServerTimestamp(value)
  if (!date) return "never"
  const seconds = Math.round((now.getTime() - date.getTime()) / 1000)
  if (seconds < 45) return "just now"
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  if (days === 1) return "yesterday"
  return `${days} days ago`
}
