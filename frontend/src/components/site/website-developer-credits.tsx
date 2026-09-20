import Image from "next/image";
import {
  ExternalLink,
  Sparkles,
} from "lucide-react";

import { GitHubMark } from "@/components/site/github-mark";
import { CONTRIBUTOR_PROFILES } from "@/lib/content/members";

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

export function WebsiteDeveloperCredits() {

  return (
    <section className="relative overflow-hidden rounded-2xl border border-white/[0.08] bg-white/[0.02] p-6 sm:p-10 backdrop-blur-xl shadow-2xl">
      {/* Header section */}
      <div className="relative z-10 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3.5 py-1 font-mono text-micro font-medium uppercase tracking-caps text-white/75">
            <Sparkles className="size-3 text-amber-300 animate-pulse" />
            <span>Engineered by Students • 6 Contributors</span>
          </div>

          <h2 className="mt-3 font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
            Who Built This Website
          </h2>

          <p className="mt-2 text-base leading-relaxed text-fg-muted text-pretty">
            Meet the engineering team who designed and built this platform from scratch.
          </p>
        </div>

        {/* GitHub repository link pill */}
        <div className="shrink-0">
          <a
            href="https://github.com/ProgrammingClub-DAU/website"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 font-mono text-xs text-white/70 transition-all duration-200 hover:border-white/25 hover:bg-white/10 hover:text-white hover:shadow-lg"
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

          return (
            <div
              key={contributor.name}
              className="group relative flex flex-col items-center w-full focus:outline-none"
              style={{
                animation: "subtle-float 5s ease-in-out infinite",
                animationDelay: `${index * 0.5}s`,
              }}
            >
              <article className="relative w-full rounded-2xl p-[2px] overflow-hidden transition-all duration-300 group-hover:-translate-y-1.5 group-hover:shadow-[0_20px_40px_-12px_rgba(0,0,0,0.7)]">
                {/* ── Continuous Rotating Border Beam Animation ── */}
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute -inset-[150%] animate-[spin_5s_linear_infinite]"
                  style={{
                    background:
                      "conic-gradient(from 0deg at 50% 50%, transparent 0deg, transparent 270deg, rgba(255,255,255,0.7) 320deg, rgba(165,180,252,0.6) 345deg, transparent 360deg)",
                  }}
                />

                {/* ── Inner Dark Glass Card Body ── */}
                <div className="relative z-10 flex flex-col items-center justify-center min-h-[180px] rounded-[16px] bg-[#0c0f1a]/95 p-5 pb-5 backdrop-blur-xl border border-white/[0.05]">
                  {/* Diagonal shimmer sweep on hover */}
                  <div
                    aria-hidden="true"
                    className="pointer-events-none absolute -inset-full -translate-x-full -skew-x-12 bg-gradient-to-r from-transparent via-white/[0.08] to-transparent transition-transform duration-1000 group-hover:translate-x-full"
                  />

                  {/* ── Centered Photo (80px) ── */}
                  <div
                    className="relative shrink-0 overflow-hidden rounded-full p-[2px] bg-white/15 transition-all duration-300 group-hover:scale-105 group-hover:bg-white/30 shadow-xl"
                    style={{ width: "80px", height: "80px" }}
                  >
                    <div className="size-full overflow-hidden rounded-full bg-[#111318]">
                        <div
                          className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-bold text-xl text-white shadow-inner`}
                        >
                          {getInitials(contributor.name)}
                        </div>
                    </div>
                  </div>

                  {/* ── Name ── */}
                  <div className="mt-4 text-center px-1">
                    <h3 className="font-heading text-base sm:text-lg font-bold tracking-tight text-white transition-colors group-hover:text-white leading-tight">
                      {contributor.name}
                    </h3>

                    {/* ── Tag: Core Contributor for everyone ── */}
                    <div className="mt-2 flex items-center justify-center">
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-violet-500/25 bg-violet-500/[0.08] px-2.5 py-0.5 text-[10px] font-medium tracking-wide text-violet-200/95 shadow-[inset_0_1px_0_rgba(167,139,250,0.12)] backdrop-blur-md transition-colors group-hover:border-violet-500/35 group-hover:bg-violet-500/[0.12]">
                        <span className="size-1 rounded-full bg-violet-400 opacity-85 ring-2 ring-violet-400/25 shrink-0" aria-hidden="true" />
                        <span>Core Contributor</span>
                      </span>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          );
        })}
      </div>
    </section>
  );
}



