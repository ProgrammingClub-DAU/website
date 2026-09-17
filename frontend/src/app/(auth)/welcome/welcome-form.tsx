/**
 * WelcomeForm Client Component
 * The one question a Google sign-in cannot answer, asked once, on first visit.
 */

"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import axios from "axios";
import { AuthBackdrop } from "@/components/site/auth-backdrop";
import StarBorder from "@/components/site/star-border";
import { dashboardService } from "@/lib/services/dashboard";
import { useAuthStore } from "@/store/auth";
import { ACADEMIC_YEAR_LABELS, type AcademicYear, type Profile } from "@/types/api";

const YEAR_OPTIONS = [
  { value: "FIRST_YEAR", label: ACADEMIC_YEAR_LABELS.FIRST_YEAR, hint: "Just joined DAU" },
  {
    value: "SECOND_YEAR_ONWARDS",
    label: ACADEMIC_YEAR_LABELS.SECOND_YEAR_ONWARDS,
    hint: "Second year or later",
  },
] as const;

export default function WelcomeForm() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();

  const [profile, setProfile] = useState<Profile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [academicYear, setAcademicYear] = useState<AcademicYear | "">("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [codeforcesHandle, setCodeforcesHandle] = useState("");

  useEffect(() => {
    // Nothing here is reachable without an account, and arriving signed out
    // means a stale link or an expired session rather than a new member.
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }

    let cancelled = false;

    dashboardService
      .getProfile()
      .then((loaded) => {
        if (cancelled) return;

        // Someone who already answered does not get asked again, however they
        // reached this URL.
        if (loaded.academicYear) {
          router.replace("/profile");
          return;
        }

        setProfile(loaded);
        // Prefilled with whatever Google supplied, which is a starting point and
        // not an answer: Workspace accounts often carry a name in capitals, or
        // with the roll number in front of it, and an account with no name claim
        // at all falls back to the student ID -- so some members arrive here
        // called "202401226".
        setName(loaded.name ?? "");
        setPhoneNumber(loaded.phoneNumber ?? "");
        setCodeforcesHandle(loaded.codeforcesHandle ?? "");
        setIsLoading(false);
      })
      .catch(() => {
        if (!cancelled) {
          setError("Could not load your account. Please reload the page.");
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, user?.id, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (name.trim().length < 2) {
      setError("Please enter your full name.");
      return;
    }
    if (!academicYear) {
      setError("Please choose your year to continue.");
      return;
    }
    if (!profile) return;

    setIsSaving(true);
    try {
      // The profile endpoint replaces every field it receives, so the whole
      // profile goes back, not just what this screen asked about.
      await dashboardService.updateProfile({
        name: name.trim(),
        phoneNumber: phoneNumber.trim() || null,
        codeforcesHandle: codeforcesHandle.trim() || null,
        leetcodeHandle: profile.leetcodeHandle,
        codechefUrl: profile.codechefUrl,
        atcoderUrl: profile.atcoderUrl,
        githubUrl: profile.githubUrl,
        linkedinUrl: profile.linkedinUrl,
        avatarUrl: profile.avatarUrl,
        academicYear,
      });

      router.push("/profile");
    } catch (err: unknown) {
      let msg = "Could not save. Please try again.";
      if (axios.isAxiosError(err) && err.response) {
        const data = err.response.data as { message?: string } | undefined;
        msg = data?.message ?? msg;
      }
      setError(msg);
      setIsSaving(false);
    }
  };

  return (
    <div className="relative isolate flex min-h-[calc(100vh-3.5rem)] w-full flex-col items-center justify-center overflow-hidden px-6 py-14">
      <AuthBackdrop className="absolute inset-0 -z-10 [mask-image:radial-gradient(60%_46%_at_50%_46%,rgb(0_0_0/0.35),#000_72%)]" />

      <div className="w-full max-w-md">
        <div className="space-y-3 text-center">
          <p className="text-halo font-mono text-label font-semibold tracking-[0.15em] text-primary uppercase">
            ONE LAST THING
          </p>
          <h1 className="text-halo font-heading text-[clamp(1.75rem,4vw,2.25rem)] leading-[1.1] font-medium tracking-tight text-balance text-foreground">
            {name.trim() ? `Welcome, ${name.trim().split(" ")[0]}.` : "Welcome."}
          </h1>
        </div>

        <div className="glass-panel mt-8 rounded-panel p-6 sm:p-8">
          {isLoading ? (
            <p className="py-8 text-center font-mono text-xs text-fg-muted">Loading your account...</p>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label htmlFor="welcome-name" className="mb-1 block text-xs font-medium text-fg-muted">
                  Your name <span className="text-red-400">*</span>
                </label>
                <input
                  id="welcome-name"
                  type="text"
                  required
                  autoComplete="name"
                  maxLength={100}
                  placeholder="Ravi Patel"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                />
                <p className="mt-1 text-nano text-fg-subtle">
                  This is how you appear on the leaderboard and the members page.
                </p>
              </div>

              <fieldset className="space-y-2">
                <legend
                  id="year-legend"
                  className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase"
                >
                  YEAR OF STUDY <span className="text-red-400">*</span>
                </legend>
                <p className="text-xs text-fg-subtle">
                  First-years get the beginner sessions; everyone else gets the contest track.
                </p>

                <div
                  role="radiogroup"
                  aria-labelledby="year-legend"
                  className="grid gap-2 pt-1 sm:grid-cols-2"
                >
                  {YEAR_OPTIONS.map((option) => (
                    <label key={option.value} className="cursor-pointer">
                      <input
                        type="radio"
                        name="academicYear"
                        value={option.value}
                        checked={academicYear === option.value}
                        onChange={() => setAcademicYear(option.value)}
                        className="peer sr-only"
                      />
                      <span className="flex h-full flex-col gap-0.5 rounded-control border border-border bg-surface-2 px-3 py-2.5 text-xs text-fg-muted transition-colors hover:text-foreground peer-checked:border-primary peer-checked:bg-primary/10 peer-checked:text-foreground peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-ring">
                        <span className="font-medium">{option.label}</span>
                        <span className="text-nano text-fg-subtle">{option.hint}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              {/* Optional here on purpose. A new member may not have a Codeforces
                  account yet, and blocking their first sign-in on one would be a
                  strange way to welcome them. The profile badge keeps asking. */}
              <div className="space-y-4 border-t border-border pt-5">
                <p className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                  WHILE YOU ARE HERE (OPTIONAL)
                </p>

                <div>
                  <label
                    htmlFor="welcome-cf"
                    className="mb-1 block text-xs font-medium text-fg-muted"
                  >
                    Codeforces handle
                  </label>
                  <input
                    id="welcome-cf"
                    type="text"
                    placeholder="tourist"
                    value={codeforcesHandle}
                    onChange={(e) => setCodeforcesHandle(e.target.value)}
                    className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  />
                  <p className="mt-1 text-nano text-fg-subtle">
                    Puts you on the club leaderboard. You can add it later.
                  </p>
                </div>

                <div>
                  <label
                    htmlFor="welcome-phone"
                    className="mb-1 block text-xs font-medium text-fg-muted"
                  >
                    Phone number
                  </label>
                  <input
                    id="welcome-phone"
                    type="tel"
                    placeholder="+91 9876543210"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  />
                  <p className="mt-1 text-nano text-fg-subtle">
                    How an organiser reaches you at an event.
                  </p>
                </div>
              </div>

              {error && (
                <div
                  role="alert"
                  className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-center font-mono text-xs text-destructive"
                >
                  {error}
                </div>
              )}

              <StarBorder
                type="submit"
                backgroundColor="var(--primary)"
                textColor="var(--primary-foreground)"
                disabled={isSaving}
                className="w-full disabled:opacity-60"
                innerClassName="h-10 w-full rounded-lg font-mono text-xs tracking-wider uppercase flex items-center justify-center"
              >
                {isSaving ? "Saving..." : "Get started"}
              </StarBorder>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
