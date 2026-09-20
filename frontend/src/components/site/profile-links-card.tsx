"use client";

/**
 * The member's profiles on every platform, as one row of capsules.
 *
 * Before this, four of the six links sat as small grey pills in the profile
 * header, and the two that matter most -- Codeforces and LeetCode -- were not
 * linked at all, even though the site knows both handles and shows both ratings.
 *
 * Each capsule carries the platform mark, the username and the rating where one
 * exists, so the card answers "who is this member, where" without a click, and
 * the whole capsule is the link.
 */

import { ExternalLink, Plus } from "lucide-react";
import { Line, LineChart, ResponsiveContainer, YAxis } from "recharts";

import { PlatformGlyph, PROFILE_ACCENT } from "@/components/site/platform-glyph";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PROFILE_PLATFORMS, profileUrl, usernameFrom, type ProfilePlatformId } from "@/lib/platform-profiles";
import { cn } from "@/lib/utils";

export interface ProfileLink {
  platform: ProfilePlatformId;
  /** Stored handle or URL, whichever that platform keeps. */
  value: string | null;
  /** Shown beside the username when the platform has a rating. */
  rating?: number | null;
  /** Rating history data points for sparkline */
  history?: { value: number }[] | null;
}

/**
 * @param links every platform in display order, linked or not
 * @param isOwner whether the viewer owns this profile, which decides what an
 *        unlinked platform looks like
 * @param onAddClick opens the edit panel, for the owner's unlinked capsules
 */
export function ProfileLinksCard({
  links,
  isOwner,
  onAddClick,
}: {
  links: ProfileLink[];
  isOwner: boolean;
  onAddClick?: () => void;
}) {
  const linked = links.filter((l) => usernameFrom(l.platform, l.value));
  const missing = links.filter((l) => !usernameFrom(l.platform, l.value));

  // A visitor looking at a profile with nothing linked gets a plain sentence
  // rather than six dead capsules.
  if (linked.length === 0 && !isOwner) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Coding profiles</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="py-2 text-sm text-fg-muted">This member has not linked any profiles yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Coding profiles</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {linked.map(({ platform, value, rating, history }) => {
            const username = usernameFrom(platform, value);
            const href = profileUrl(platform, value);
            const accent = PROFILE_ACCENT[platform];
            if (!href) return null;

            return (
              <a
                key={platform}
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                // The accent is decorative: it tints the mark and the hover
                // border only. Every word stays on a normal text colour.
                style={{ ["--accent" as string]: accent }}
                className={cn(
                  "group relative overflow-hidden flex items-center gap-3 rounded-panel border border-border bg-surface-2 px-3.5 py-3",
                  "transition-all hover:-translate-y-0.5 hover:border-[color-mix(in_srgb,var(--accent)_55%,transparent)]",
                  "hover:shadow-panel focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                )}
              >
                {/* ── Sparkline Background ── */}
                {history && history.length > 1 && (
                  <div className="pointer-events-none absolute bottom-0 left-0 right-0 h-1/2 opacity-[0.15] transition-opacity duration-300 group-hover:opacity-[0.25]">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={history}>
                        <YAxis domain={["dataMin", "dataMax"]} hide />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke={accent}
                          strokeWidth={2}
                          dot={false}
                          isAnimationActive={false}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                )}

                <span
                  className="relative z-10 flex size-9 shrink-0 items-center justify-center rounded-full border border-hairline"
                  style={{
                    color: accent,
                    background: "color-mix(in srgb, var(--accent) 12%, transparent)",
                  }}
                >
                  <PlatformGlyph platform={platform} className="size-4" />
                </span>

                <span className="relative z-10 min-w-0 flex-1">
                  <span className="flex items-center gap-1.5">
                    <span className="text-meta tracking-caps text-fg-subtle uppercase">
                      {PROFILE_PLATFORMS[platform].label}
                    </span>
                    {typeof rating === "number" && rating > 0 && (
                      <span className="rounded-full border border-hairline px-1.5 font-mono text-nano text-fg-muted shadow-sm backdrop-blur-sm">
                        {rating}
                      </span>
                    )}
                  </span>
                  <span className="block truncate text-body font-medium text-foreground drop-shadow-sm">
                    @{username}
                  </span>
                </span>

                <ExternalLink className="relative z-10 size-3.5 shrink-0 text-fg-subtle transition-colors group-hover:text-foreground" />
              </a>
            );
          })}

          {/* The owner sees what is still missing; a visitor does not, because
              another member's empty slots are not information they can act on. */}
          {isOwner &&
            missing.map(({ platform }) => (
              <button
                key={platform}
                type="button"
                onClick={onAddClick}
                className="group flex items-center gap-3 rounded-panel border border-dashed border-border px-3.5 py-3 text-left transition-colors hover:border-hairline-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-full border border-hairline text-fg-subtle">
                  <PlatformGlyph platform={platform} className="size-4" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-meta tracking-caps text-fg-subtle uppercase">
                    {PROFILE_PLATFORMS[platform].label}
                  </span>
                  <span className="block text-body text-fg-muted group-hover:text-foreground">
                    Not linked
                  </span>
                </span>
                <Plus className="size-3.5 shrink-0 text-fg-subtle transition-colors group-hover:text-foreground" />
              </button>
            ))}
        </div>
      </CardContent>
    </Card>
  );
}

export default ProfileLinksCard;
