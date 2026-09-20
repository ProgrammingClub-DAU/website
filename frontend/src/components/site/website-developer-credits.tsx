import Link from "next/link";
import Image from "next/image";
import { ArrowUpRight, Globe, Shield } from "lucide-react";

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
  "from-indigo-600 via-purple-600 to-pink-500",
  "from-cyan-500 via-blue-600 to-indigo-700",
  "from-emerald-500 via-teal-600 to-cyan-700",
  "from-amber-500 via-orange-600 to-rose-600",
  "from-fuchsia-600 via-purple-600 to-blue-600",
  "from-blue-600 via-indigo-600 to-violet-700",
];

/** Hardcoded creator emails list as specified */
export const CREATOR_EMAILS = [
  "202401152@dau.ac.in",
  "202401474@dau.ac.in",
  "202401226@dau.ac.in",
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
          <div className="flex size-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-white/80">
            <Globe className="size-4" />
          </div>
          <div>
            <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Who Built This Website
            </h2>
          </div>
        </div>

        <div className="hidden sm:block h-px flex-1 mx-6 bg-gradient-to-r from-white/15 to-transparent" />

        <a
          href="https://github.com/ProgrammingClub-DAU/website"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 font-mono text-xs text-white/80 transition-all duration-200 hover:border-white/20 hover:bg-white/10 hover:text-white"
        >
          <GitHubMark className="size-4" />
          <span>View on GitHub</span>
          <ArrowUpRight className="size-3.5 opacity-60" />
        </a>
      </div>

      {/* ── Developer Cards Grid (Matching MemberCard style) ── */}
      <div className="flex flex-wrap justify-center gap-6">
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
                rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl
                p-4 sm:p-5 pb-5 min-h-[250px]
                w-full sm:w-[calc(50%-14px)] lg:w-[calc(25%-18px)]
                transition-all duration-300 cursor-pointer
                hover:-translate-y-1.5 hover:border-rose-500/30 hover:bg-white/[0.07]
                hover:shadow-[0_16px_32px_-10px_rgba(0,0,0,0.7)]
              `}
            >
              {/* Subtle top edge highlight on hover */}
              <div
                aria-hidden="true"
                className="absolute inset-x-0 top-0 h-px rounded-t-2xl bg-gradient-to-r from-transparent via-rose-500/40 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100"
              />

              {/* ── Centered Avatar (104px) ── */}
              <div className="mt-1 flex flex-col items-center gap-3 flex-1 w-full">
                <div
                  className="relative shrink-0 overflow-hidden rounded-full p-[2px] bg-white/10 transition-transform duration-300 group-hover:scale-105 group-hover:bg-rose-500/30 shadow-md"
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

                {/* Name */}
                <div className="text-center px-1">
                  <h3 className="font-heading text-base sm:text-lg font-bold tracking-tight text-white/90 transition-colors group-hover:text-white leading-tight line-clamp-2">
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
                        className="inline-flex size-7 items-center justify-center rounded-lg border border-white/10 bg-white/5
                          text-white/50 transition-all duration-200 hover:scale-110 hover:border-white/25 hover:text-white hover:bg-white/10"
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


