/**
 * LoginForm Client Component
 * Renders the Google sign-in interface and connects to POST /api/auth/google.
 */

"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import axios from "axios";
import { AuthBackdrop } from "@/components/site/auth-backdrop";
import Link from "next/link";
import { GoogleSignInButton } from "@/components/site/google-sign-in-button";
import { apiClient } from "@/lib/axios";
import { useAuthStore, type ApiResponse, type AuthResponse, mapAuthResponseToUser } from "@/store/auth";

export default function LoginForm() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  /**
   * Exchanges Google's ID token for a session here.
   *
   * The token is not inspected in the browser. Everything that decides whether
   * this person may sign in -- that Google really issued the token, that it was
   * issued for this site, that the address is on the university domain -- is
   * checked on the server, where the answer cannot be edited.
   */
  const handleCredential = async (idToken: string) => {
    setApiError(null);
    setIsSigningIn(true);

    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/api/auth/google", {
        idToken,
      });

      const authData = response.data.data;
      login(mapAuthResponseToUser(authData), authData.token);

      // A member with no year on record has never been through the welcome
      // question -- either they just signed up, or their account predates it.
      // Signing in is the only moment we are certain to have their attention.
      router.push(authData.academicYear ? "/" : "/welcome");
    } catch (err: unknown) {
      let msg = "Failed to sign in. Please try again.";

      if (axios.isAxiosError(err)) {
        if (!err.response) {
          msg = "Cannot reach the server. Check your connection and try again.";
        } else {
          // The server's message is the useful one here: it names the actual
          // problem, which is almost always a personal account rather than the
          // university one.
          const responseData = err.response.data as { message?: string } | undefined;
          msg = responseData?.message ?? "Failed to sign in. Please try again.";
        }
      }

      setApiError(msg);
      setIsSigningIn(false);
    }
  };

  return (
    <div className="relative isolate flex min-h-[calc(100vh-3.5rem)] w-full flex-col items-center justify-center overflow-hidden px-6 py-14">
      {/* Covers the whole page. An earlier version cleared a large ellipse
          through the middle, which left the top of the page looking empty — the
          form sits on its own glass panel, so it does not need the backdrop
          held off it. What is left is a gentle relief directly behind the
          column, enough to keep the headline off the busiest pixels. */}
      <AuthBackdrop className="absolute inset-0 -z-10 [mask-image:radial-gradient(60%_46%_at_50%_46%,rgb(0_0_0/0.35),#000_72%)]" />

      <div className="w-full max-w-md">
        {/* The headline is the only text sitting on the raw backdrop — the panel
            below has its own glass. It carries a halo on the glyphs rather than
            a pool of page colour behind the block: the pool read as a cloud
            with an edge, while a text-shadow follows the letters and lets the
            field keep flowing right up to them. See .text-halo. */}
        <div className="space-y-3 text-center">
          <p className="text-halo font-mono text-label font-semibold tracking-[0.15em] text-primary uppercase">
            WEEKLY CONTESTS, LIVE RANK
          </p>
          <h1 className="text-halo font-heading text-[clamp(1.75rem,4vw,2.25rem)] leading-[1.1] font-medium tracking-tight text-balance text-foreground">
            Solve. Rank up. Climb the board.
          </h1>
        </div>

        <div className="glass-panel mt-8 rounded-panel p-6 sm:p-8">
          <div className="space-y-2 text-center">
            <p className="font-mono text-xs text-primary uppercase tracking-[0.12em] font-semibold">
              SIGN IN
            </p>
            <h2 className="text-3xl font-semibold tracking-tight text-foreground">
              Welcome back
            </h2>
            <p className="text-sm text-fg-muted">
              Use your university Google account to continue
            </p>
          </div>

          <div className="mt-7 space-y-4">
            <GoogleSignInButton onCredential={handleCredential} busy={isSigningIn} />

            {apiError && (
              <div
                id="login-api-error"
                role="alert"
                className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-center text-xs text-destructive font-mono"
              >
                {apiError}
              </div>
            )}

            <div className="space-y-2 border-t border-border pt-4 text-center">
              <p className="font-mono text-xs text-fg-muted">
                Only <span className="text-foreground">@dau.ac.in</span> accounts can sign in.
              </p>
              <p className="text-xs text-fg-subtle">
                No account needed — signing in for the first time creates one.
              </p>
              <p className="text-xs text-fg-subtle">
                <Link href="/privacy" className="underline underline-offset-4 hover:text-foreground">
                  Privacy policy
                </Link>
              </p>
            </div>
          </div>
        </div>

        {/* The rank ladder the particles are drawn from. */}
        <div className="mt-8 flex flex-col items-center gap-2.5" aria-hidden>
          <p className="font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
            RANK TRACK
          </p>
          <div className="flex gap-2.5">
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-newbie)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-pupil)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-specialist)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-expert)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-candidate)" }} />
          </div>
        </div>
      </div>
    </div>
  );
}
