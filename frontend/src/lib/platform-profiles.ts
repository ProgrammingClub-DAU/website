/**
 * Turning a username into a profile link, and back again.
 *
 * Members type a username, never a URL: "tourist", not
 * "https://codeforces.com/profile/tourist". Typing a link is the step people get
 * wrong, and a wrong link is invisible until someone clicks it.
 *
 * The database is unchanged. Codeforces and LeetCode already store a bare handle
 * (their sync jobs look members up by it), while CodeChef, AtCoder, GitHub and
 * LinkedIn are stored as full URLs and are read as links by the attendance
 * export. So the username the member types is expanded to a URL on the way in,
 * and reduced back to a username for display. {@link usernameFrom} accepts both
 * shapes, so a pasted profile URL still works.
 */

export type ProfilePlatformId =
  | "codeforces"
  | "leetcode"
  | "codechef"
  | "atcoder"
  | "github"
  | "linkedin";

interface PlatformProfileSpec {
  label: string;
  /** How the site addresses one member's page on that platform. */
  buildUrl: (username: string) => string;
  /** Path prefixes a pasted URL may carry before the username. */
  pathPrefixes: string[];
  placeholder: string;
  /** What the platform itself calls this identifier. */
  noun: string;
}

export const PROFILE_PLATFORMS: Record<ProfilePlatformId, PlatformProfileSpec> = {
  codeforces: {
    label: "Codeforces",
    buildUrl: (u) => `https://codeforces.com/profile/${u}`,
    pathPrefixes: ["profile"],
    placeholder: "tourist",
    noun: "handle",
  },
  leetcode: {
    label: "LeetCode",
    buildUrl: (u) => `https://leetcode.com/u/${u}`,
    pathPrefixes: ["u"],
    placeholder: "lee215",
    noun: "username",
  },
  codechef: {
    label: "CodeChef",
    buildUrl: (u) => `https://www.codechef.com/users/${u}`,
    pathPrefixes: ["users"],
    placeholder: "gennady",
    noun: "username",
  },
  atcoder: {
    label: "AtCoder",
    buildUrl: (u) => `https://atcoder.jp/users/${u}`,
    pathPrefixes: ["users"],
    placeholder: "tourist",
    noun: "username",
  },
  github: {
    label: "GitHub",
    buildUrl: (u) => `https://github.com/${u}`,
    pathPrefixes: [],
    placeholder: "octocat",
    noun: "username",
  },
  linkedin: {
    label: "LinkedIn",
    // LinkedIn public profiles live under /in/, and the part after it is what a
    // member sees in their own address bar.
    buildUrl: (u) => `https://www.linkedin.com/in/${u}`,
    pathPrefixes: ["in", "pub"],
    placeholder: "your-name-1a2b3c",
    noun: "profile name",
  },
};

/**
 * Reads the username out of whatever is stored, or whatever was typed.
 *
 * Accepts a bare username, a full profile URL, a URL with no scheme, a leading
 * "@", and trailing slashes or query strings. Returns an empty string when there
 * is nothing usable, so callers can treat "not linked" as one case.
 *
 * @param platform which platform the value belongs to
 * @param stored the stored URL, or the raw text a member typed
 * @returns the bare username
 */
export function usernameFrom(platform: ProfilePlatformId, stored: string | null | undefined): string {
  const raw = (stored ?? "").trim();
  if (!raw) return "";

  // A bare username: no slashes and no dots that would make it a hostname.
  if (!raw.includes("/") && !raw.includes(".")) {
    return raw.replace(/^@+/, "");
  }

  const withoutScheme = raw.replace(/^[a-z][a-z0-9+.-]*:\/\//i, "");
  const [pathAndQuery] = withoutScheme.split(/[?#]/);
  const segments = pathAndQuery.split("/").filter(Boolean);

  // Drop the hostname, then any prefix the platform puts before the username.
  if (segments.length && segments[0].includes(".")) segments.shift();
  const { pathPrefixes } = PROFILE_PLATFORMS[platform];
  while (segments.length > 1 && pathPrefixes.includes(segments[0].toLowerCase())) {
    segments.shift();
  }

  return (segments[0] ?? "").replace(/^@+/, "");
}

/**
 * Builds the profile link for a username.
 *
 * @param platform which platform to link to
 * @param value a username, or an already-stored URL
 * @returns the profile URL, or null when there is no username
 */
export function profileUrl(platform: ProfilePlatformId, value: string | null | undefined): string | null {
  const username = usernameFrom(platform, value);
  if (!username) return null;
  return PROFILE_PLATFORMS[platform].buildUrl(encodeURIComponent(username));
}
