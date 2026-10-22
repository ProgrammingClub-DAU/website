import axios from "axios";

/**
 * The reason a request failed, in words a member can act on.
 *
 * Pages used to replace every failure with one fixed sentence ("Failed to submit
 * post for review."), so a too-short title, an expired session and a server
 * error all looked the same. The API already says what went wrong: the
 * `message` of its error envelope, and for a validation failure a field -> reason
 * map in `data`.
 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (!axios.isAxiosError(err)) return fallback;

  if (!err.response) {
    return err.code === "ECONNABORTED"
      ? "The server took too long to answer. It may be waking up after being idle; try again in a minute."
      : "Could not reach the server. Check your connection and try again.";
  }

  const body = err.response.data as { message?: unknown; data?: unknown } | undefined;
  if (body?.data && typeof body.data === "object" && !Array.isArray(body.data)) {
    const reasons = Object.values(body.data as Record<string, unknown>).filter(
      (v): v is string => typeof v === "string" && v.length > 0
    );
    if (reasons.length > 0) return reasons.join(" ");
  }
  if (typeof body?.message === "string" && body.message.length > 0) return body.message;

  switch (err.response.status) {
    case 401:
      return "Your session has expired. Please sign in again.";
    case 403:
      return "You are not allowed to do this.";
    case 404:
      return "That item no longer exists.";
    default:
      return fallback;
  }
}
