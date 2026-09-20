import Link from "next/link";
import Image from "next/image";
import {
  ExternalLink,
} from "lucide-react";

import { GitHubMark } from "@/components/site/github-mark";
import { CONTRIBUTOR_PROFILES } from "@/lib/content/members";
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

/** Pre-computed unique vibrant gradient combinations for developer avatar placeholders */
const AVATAR_GRADIENTS = [
  "from-indigo-600 via-purple-600 to-pink-500",
  "from-cyan-500 via-blue-600 to-indigo-700",
  "from-emerald-500 via-teal-600 to-cyan-700",
  "from-amber-500 via-orange-600 to-rose-600",
  "from-fuchsia-600 via-purple-600 to-blue-600",
  "from-blue-600 via-indigo-600 to-violet-700",
];

interface WebsiteDeveloperCreditsProps {
  creators?: PublicMember[];
}

export function WebsiteDeveloperCredits({ creators = [] }: WebsiteDeveloperCreditsProps) {
  return (
    <section id="credits" className="relative overflow-hidden rounded-2xl border border-border/40 bg-surface/40 p-6 sm:p-10 backdrop-blur-xl shadow-2xl">
      {/* Header section */}
      <div className="relative z-10 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="max-w-2xl">
          <h2 className="mt-3 font-heading text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Who Built This Website
          </h2>
        </div>

        {/* GitHub repository link pill */}
        <div className="shrink-0">
          <a
            href="https://github.com/ProgrammingClub-DAU/website"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-xl border border-border/50 bg-surface/60 px-4 py-2.5 font-mono text-xs text-foreground/80 transition-all duration-200 hover:border-border hover:bg-surface hover:text-foreground hover:shadow-lg"
          >
            <GitHubMark className="size-4" />
            <span>Open Source on GitHub</span>
            <ExternalLink className="size-3.5 opacity-60" />
          </a>
        </div>
      </div>

      {/* Developers Grid — 3-3 division spanning full width */}
      <div className="relative z-10 mt-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8 pb-4">
        {CONTRIBUTOR_PROFILES.map((contributor, index) => {
          const avatarGradient = AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length];

          // Match contributor by name with backend creators list
          const matchedCreator = creators.find((c) =>
            c.name.toLowerCase().trim().includes(contributor.name.toLowerCase().trim()) ||
            contributor.name.toLowerCase().trim().includes(c.name.toLowerCase().trim())
          );

          const profileHref = matchedCreator?.id
            ? `/profile/${matchedCreator.id}`
            : undefined;

          const CardWrapper = ({ children }: { children: React.ReactNode }) =>
            profileHref ? (
              <Link href={profileHref} className="block w-full focus:outline-none">
                {children}
              </Link>
            ) : (
              <div className="w-full">{children}</div>
            );

          return (
            <div
              key={contributor.name}
              className="group relative flex flex-col items-center w-full focus:outline-none"
              style={{
                animation: "subtle-float 5s ease-in-out infinite",
                animationDelay: `${index * 0.5}s`,
              }}
            >
              <CardWrapper>
                <article className="relative w-full rounded-2xl p-[2px] overflow-hidden transition-all duration-300 group-hover:-translate-y-1.5 group-hover:shadow-[0_20px_40px_-12px_rgba(0,0,0,0.7)] cursor-pointer">
                  {/* ── Continuous Rotating Border Beam Animation with Ruby/Crimson Light ── */}
                  <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -inset-[150%] animate-[spin_5s_linear_infinite]"
                    style={{
                      background:
                        "conic-gradient(from 0deg at 50% 50%, transparent 0deg, transparent 270deg, rgba(255,30,66,0.85) 320deg, rgba(255,140,160,0.7) 345deg, transparent 360deg)",
                    }}
                  />

                  {/* ── Inner Dark Glass Card Body ── */}
                  <div className="relative z-10 flex flex-col items-center justify-center min-h-[180px] rounded-[16px] bg-card/95 p-5 pb-5 backdrop-blur-xl border border-border/40 group-hover:border-rose-500/50 transition-colors">
                    {/* Diagonal shimmer sweep on hover */}
                    <div
                      aria-hidden="true"
                      className="pointer-events-none absolute -inset-full -translate-x-full -skew-x-12 bg-gradient-to-r from-transparent via-white/[0.08] to-transparent transition-transform duration-1000 group-hover:translate-x-full"
                    />

                    {/* ── Centered Photo (80px) with Ruby Glow ── */}
                    <div
                      className="relative shrink-0 overflow-hidden rounded-full p-[2px] bg-rose-500/30 transition-all duration-300 group-hover:scale-105 group-hover:bg-rose-500/60 shadow-[0_0_18px_rgba(244,63,94,0.35)]"
                      style={{ width: "80px", height: "80px" }}
                    >
                      <div className="size-full overflow-hidden rounded-full bg-background">
                        {matchedCreator?.avatarUrl ? (
                          <Image
                            src={matchedCreator.avatarUrl}
                            alt={contributor.name}
                            width={80}
                            height={80}
                            className="size-full object-cover"
                          />
                        ) : (
                          <div
                            className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-bold text-xl text-white shadow-inner`}
                          >
                            {getInitials(contributor.name)}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* ── Name & Profile Link Indicator ── */}
                    <div className="mt-4 text-center px-1">
                      <h3 className="font-heading text-base sm:text-lg font-bold tracking-tight text-foreground transition-colors group-hover:text-rose-400 leading-tight flex items-center justify-center gap-1.5">
                        <span>{contributor.name}</span>
                        <ExternalLink className="size-3.5 opacity-0 group-hover:opacity-100 transition-opacity text-rose-400 shrink-0" />
                      </h3>

                      {/* ── Tag: Core Contributor for everyone ── */}
                      <div className="mt-2 flex items-center justify-center">
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-rose-500/30 bg-rose-500/[0.08] px-2.5 py-0.5 text-[10px] font-medium tracking-wide text-rose-400/95 shadow-[inset_0_1px_0_rgba(244,63,94,0.15)] backdrop-blur-md transition-colors group-hover:border-rose-500/45 group-hover:bg-rose-500/[0.15]">
                          <span className="size-1 rounded-full bg-rose-400 opacity-90 ring-2 ring-rose-400/30 shrink-0" aria-hidden="true" />
                          <span>Website Architect</span>
                        </span>
                      </div>
                    </div>
                  </div>
                </article>
              </CardWrapper>
            </div>
          );
        })}
      </div>
    </section>
  );
}



