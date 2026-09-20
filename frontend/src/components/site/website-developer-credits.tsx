import Link from "next/link";
import Image from "next/image";
import { ArrowUpRight, Globe, Sparkles } from "lucide-react";

import { GitHubMark } from "@/components/site/github-mark";
import { PlatformGlyph } from "@/components/site/platform-glyph";
import { CONTRIBUTOR_PROFILES } from "@/lib/content/members";
import { profileUrl } from "@/lib/platform-profiles";
import type { PublicMember } from "@/types/api";

function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

/** Pre-computed subtle avatar gradient combinations */
const AVATAR_GRADIENTS = [
  "from-rose-500 via-purple-600 to-indigo-600",
  "from-indigo-500 via-blue-600 to-cyan-500",
  "from-violet-600 via-fuchsia-600 to-rose-500",
  "from-cyan-400 via-blue-600 to-indigo-700",
  "from-amber-500 via-rose-600 to-purple-600",
  "from-emerald-400 via-teal-600 to-indigo-700",
];

/** Hardcoded creator emails list as specified */
export const CREATOR_EMAILS = [
  "202401152@dau.ac.in",
  "202401226@dau.ac.in",
  "202401474@dau.ac.in",
  "202401041@dau.ac.in",
  "202401178@dau.ac.in",
  "202403019@dau.ac.in",
];

interface WebsiteDeveloperCreditsProps {
  creators?: PublicMember[];
}

export function WebsiteDeveloperCredits({ creators = [] }: WebsiteDeveloperCreditsProps) {
  return (
    <section id="credits">
      {/* ── Section Header (matching Core Team style) ── */}
      <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-9 items-center justify-center rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-400 shadow-[0_0_12px_rgba(244,63,94,0.2)]">
            <Globe className="size-4" />
          </div>
          <div>
            <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Who Built This Website
            </h2>
          </div>
        </div>

        <div className="hidden sm:block h-px flex-1 mx-6 bg-gradient-to-r from-rose-500/20 via-white/15 to-transparent" />

        <a
          href="https://github.com/ProgrammingClub-DAU/website"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 font-mono text-xs text-white/80 transition-all duration-200 hover:border-rose-500/30 hover:bg-rose-500/10 hover:text-white"
        >
          <GitHubMark className="size-4" />
          <span>View on GitHub</span>
          <ArrowUpRight className="size-3.5 opacity-60" />
        </a>
      </div>

      {/* ── Developer Cards Grid: 3 cards per row (6 cards across 2 rows) ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {CONTRIBUTOR_PROFILES.map((contributor, index) => {
          const avatarGradient = AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length];

          // Match by name or index from backend creators array
          const matchedCreator = creators.find(
            (c) =>
              c.name.toLowerCase().trim().includes(contributor.name.toLowerCase().trim()) ||
              contributor.name.toLowerCase().trim().includes(c.name.toLowerCase().trim()),
          ) ?? creators[index];

          const profileHref = matchedCreator?.id ? `/profile/${matchedCreator.id}` : undefined;
          const displayName = matchedCreator?.name || contributor.name;

          const PLATFORMS = [
            { platform: "codeforces" as const, value: matchedCreator?.codeforcesHandle, label: "Codeforces" },
            { platform: "linkedin" as const, value: matchedCreator?.linkedinUrl, label: "LinkedIn" },
            { platform: "github" as const, value: matchedCreator?.githubUrl, label: "GitHub" },
            { platform: "leetcode" as const, value: matchedCreator?.leetcodeHandle, label: "LeetCode" },
          ] as const;

          const links = PLATFORMS
            .map((l) => ({ ...l, href: profileUrl(l.platform, l.value ?? null) }))
            .filter((l) => l.href !== null);

          return (
            <article
              key={contributor.name}
              className={`
                group relative flex flex-col items-center
                rounded-2xl border border-white/[0.1] bg-white/[0.04] backdrop-blur-xl
                p-5 pb-5 min-h-[260px] w-full
                transition-all duration-300 cursor-pointer
                hover:-translate-y-1.5 hover:border-rose-500/40 hover:bg-white/[0.07]
                hover:shadow-[0_20px_40px_-10px_rgba(244,63,94,0.25)]
              `}
            >
              {/* Top gradient highlight on hover */}
              <div
                aria-hidden="true"
                className="absolute inset-x-0 top-0 h-0.5 rounded-t-2xl bg-gradient-to-r from-rose-500/50 via-purple-500/50 to-indigo-500/50 opacity-40 transition-opacity duration-300 group-hover:opacity-100"
              />

              {/* ── Centered Avatar (104px) with Subtle VIP Badge Overlay ── */}
              <div className="mt-1 flex flex-col items-center gap-3 flex-1 w-full">
                <div className="relative shrink-0">
                  <div
                    className="relative shrink-0 overflow-hidden rounded-full p-[2.5px] bg-gradient-to-br from-rose-500/50 via-purple-500/40 to-indigo-500/50 transition-all duration-300 group-hover:scale-105 group-hover:from-rose-500 group-hover:to-indigo-500 shadow-md"
                    style={{ width: "104px", height: "104px" }}
                  >
                    <div className="size-full overflow-hidden rounded-full bg-[#111318]">
                      {matchedCreator?.avatarUrl ? (
                        <Image
                          src={matchedCreator.avatarUrl}
                          alt={displayName}
                          width={104}
                          height={104}
                          className="size-full object-cover"
                        />
                      ) : (
                        <div
                          className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-bold text-xl text-white`}
                        >
                          {getInitials(displayName)}
                        </div>
                      )}
                    </div>
                  </div>
                  {/* Subtle VIP Creator Sparkle Icon */}
                  <span className="absolute -top-1 -right-1 flex size-5.5 items-center justify-center rounded-full bg-rose-500 text-white shadow-md ring-2 ring-[#111318]">
                    <Sparkles className="size-3" />
                  </span>
                </div>

                {/* Name */}
                <div className="text-center px-1">
                  <h3 className="font-heading text-base sm:text-lg font-bold tracking-tight text-white/95 transition-colors group-hover:text-rose-200 leading-tight line-clamp-2">
                    {profileHref ? (
                      <Link href={profileHref} className="after:absolute after:inset-0">
                        {displayName}
                      </Link>
                    ) : (
                      displayName
                    )}
                  </h3>
                </div>
              </div>

              {/* ── Bottom: Platform Links ── */}
              <div className="relative z-10 mt-auto pt-3 w-full border-t border-white/[0.08]">
                <div className="flex items-center justify-center gap-2">
                  {links.length > 0 ? (
                    links.slice(0, 4).map((link) => (
                      <a
                        key={link.platform}
                        href={link.href!}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`${displayName} on ${link.label}`}
                        title={link.label}
                        className="inline-flex size-7.5 items-center justify-center rounded-lg border border-white/10 bg-white/5
                          text-white/60 transition-all duration-200 hover:scale-110 hover:border-rose-400/50 hover:text-white hover:bg-rose-500/20"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <PlatformGlyph platform={link.platform} className="size-3.5" />
                      </a>
                    ))
                  ) : (
                    <span className="font-mono text-[9px] text-white/25 uppercase tracking-wider">
                      No profiles linked
                    </span>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}



