/**
 * One mark, and one accent colour, per platform a member can link.
 *
 * Extracted from the profile card so the members directory can show the same
 * glyphs. Two components drawing LinkedIn from two copies of the same path data
 * is how one of them ends up subtly wrong.
 */

import { GitHubMark } from "@/components/site/github-mark";
import { PlatformMark, PLATFORM_ACCENT, type PlatformId } from "@/components/site/platform-mark";
import type { ProfilePlatformId } from "@/lib/platform-profiles";

/** LinkedIn has no simple-icons entry here, and lucide dropped brand glyphs. */
function LinkedInMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden focusable="false">
      <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.46 10.9v8.37H9.2V10.9H6.46M7.83 6.7a1.6 1.6 0 0 0-1.6 1.6 1.6 1.6 0 0 0 1.6 1.6 1.6 1.6 0 0 0 1.6-1.6 1.6 1.6 0 0 0-1.6-1.6Z" />
    </svg>
  );
}

/**
 * The platform's own mark.
 *
 * @param platform which platform to draw
 * @param className sizing and colour, applied to the glyph
 */
export function PlatformGlyph({
  platform,
  className,
}: {
  platform: ProfilePlatformId;
  className?: string;
}) {
  if (platform === "github") return <GitHubMark className={className} />;
  if (platform === "linkedin") return <LinkedInMark className={className} />;
  return <PlatformMark platform={platform as PlatformId} className={className} />;
}

/** Accent hue per platform. The four CP platforms already have one. */
export const PROFILE_ACCENT: Record<ProfilePlatformId, string> = {
  codeforces: PLATFORM_ACCENT.codeforces,
  leetcode: PLATFORM_ACCENT.leetcode,
  codechef: PLATFORM_ACCENT.codechef,
  atcoder: PLATFORM_ACCENT.atcoder,
  github: "var(--fg-muted)",
  linkedin: "var(--cf-expert)",
};
