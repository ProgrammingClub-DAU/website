// Route Group: (dashboard) — groups data-driven user pages without affecting the URL.
// Public route remains /members.

// Never statically pre-render — this page fetches live member data from the backend.
export const dynamic = "force-dynamic";

import type { Metadata } from "next";

import { PageTitle, Section } from "@/components/site/primitives";
import { MembersDirectory } from "@/components/site/members-directory";
import { WebsiteDeveloperCredits } from "@/components/site/website-developer-credits";
import { dashboardService } from "@/lib/services/dashboard";

export const metadata: Metadata = {
  title: "Members & Credits",
  description:
    "The committee that runs the Programming Club @ DAU this term, and everyone who engineered this web platform.",
};

export default async function MembersPage() {
  let team: Awaited<ReturnType<typeof dashboardService.getTeam>> = [];
  let unreachable = false;

  try {
    team = await dashboardService.getTeam();
  } catch (error) {
    console.error("Members page: could not load the committee:", error);
    unreachable = true;
  }

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-[#090b10]">

      {/* ── 3D Perspective Grid Floor ── */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 bottom-0 -z-10 h-[65vh] overflow-hidden">
        {/* Grid plane */}
        <div
          style={{
            position: "absolute",
            inset: 0,
            backgroundImage: `
              linear-gradient(rgba(255,255,255,0.055) 1px, transparent 1px),
              linear-gradient(90deg, rgba(255,255,255,0.055) 1px, transparent 1px)
            `,
            backgroundSize: "72px 72px",
            transform: "perspective(700px) rotateX(72deg) scaleX(1.6)",
            transformOrigin: "bottom center",
            WebkitMaskImage: "linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.5) 40%, transparent 75%)",
            maskImage: "linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.5) 40%, transparent 75%)",
          }}
        />
        {/* Soft horizon glow — very muted, no color */}
        <div
          style={{
            position: "absolute",
            bottom: "42%",
            left: "50%",
            transform: "translateX(-50%)",
            width: "80%",
            height: "1px",
            background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.07) 30%, rgba(255,255,255,0.12) 50%, rgba(255,255,255,0.07) 70%, transparent)",
            filter: "blur(3px)",
          }}
        />
      </div>

      {/* Very subtle top luminance — no color, just depth */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-32 left-1/2 -z-10 h-[500px] w-[700px] -translate-x-1/2 rounded-full bg-[radial-gradient(ellipse,rgba(255,255,255,0.04)_0%,transparent_70%)]"
      />

      {/* Hero Section */}
      <Section className="!max-w-[94rem] pt-10 pb-4 md:pt-16 md:pb-8">
        <div className="max-w-3xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3.5 py-1 font-mono text-micro uppercase tracking-caps text-primary">
            <span className="size-1.5 rounded-full bg-primary animate-ping" />
            <span>2026 - 2027 Term • Programming Club</span>
          </div>

          <PageTitle className="mt-4">
            Who runs &amp; builds the club.
          </PageTitle>

          <p className="mt-4 max-w-[65ch] text-base leading-relaxed text-fg-muted text-pretty">
            Meet the committee steering the club&apos;s competitive programming activities,
            events, and student mentorship.
          </p>
        </div>
      </Section>

      {/* Committee Directory: Core Team & Batch Representatives */}
      <Section className="!max-w-[94rem] pb-16">
        <MembersDirectory team={team} unreachable={unreachable} />
      </Section>

      {/* Website Developer Credits Showcase: Who Built This Website */}
      <Section className="!max-w-[94rem] pb-24">
        <WebsiteDeveloperCredits />
      </Section>
    </div>
  );
}

