/**
 * LoginForm Client Component
 * Renders sign-in interface and handles logic connecting to POST /api/auth/login.
 */

"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import axios from "axios";
import { ParticlesBackdrop } from "@/components/site/particles-backdrop";
import StarBorder from "@/components/site/star-border";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { loginSchema, type LoginInput } from "@/lib/validations/auth";
import { apiClient } from "@/lib/axios";
import { useAuthStore, type ApiResponse, type AuthResponse, mapAuthResponseToUser } from "@/store/auth";

export default function LoginForm() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginInput) => {
    setApiError(null);
    try {
      // Connect to backend login endpoint using strongly typed responses
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/api/auth/login", {
        email: data.email,
        password: data.password,
      });

      const authData = response.data.data;

      // Map backend AuthResponse to Zustand User object via shared mapper function
      login(mapAuthResponseToUser(authData), authData.token);

      // Redirect home on success
      router.push("/");
    } catch (err: unknown) {
      // Differentiate between network connection problems and server-side validation/cred failures
      let msg = "Failed to sign in. Please try again.";
      if (axios.isAxiosError(err)) {
        if (!err.response) {
          msg = "Cannot reach the server. Check your connection and try again.";
        } else {
          const responseData = err.response.data as { message?: string } | undefined;
          msg = responseData?.message ?? "Failed to sign in. Please check your credentials.";
        }
      }
      setApiError(msg);
    }
  };

  return (
    <div className="relative isolate flex min-h-[calc(100vh-3.5rem)] w-full flex-col items-center justify-center overflow-hidden px-6 py-14">
      {/* Full-bleed now, rather than confined to a left-hand panel. The mask
          thins the field through the middle so the centred column sits on the
          page's own background rather than on the particles, and the density
          stays at the edges where it reads as atmosphere. */}
      <ParticlesBackdrop className="pointer-events-none absolute inset-0 -z-10 [mask-image:radial-gradient(70%_62%_at_50%_50%,transparent_18%,#000_82%)]" />

      <div className="w-full max-w-md">
        {/* Was the left panel's branding. It keeps the page's h1 — dropping the
            panel outright would have left an auth page whose only heading was
            the form's h2. */}
        <div className="space-y-3 text-center">
          <p className="font-mono text-[11px] font-semibold tracking-[0.15em] text-primary uppercase">
            WEEKLY CONTESTS, LIVE RANK
          </p>
          <h1 className="text-[clamp(1.75rem,4vw,2.25rem)] leading-[1.1] font-semibold tracking-tight text-balance text-foreground">
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
              Enter your credentials to access your account
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
            <div className="space-y-1.5">
              <Label htmlFor="email" className="font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
                EMAIL ADDRESS
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="member@university.edu"
                autoComplete="email"
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? "email-error" : undefined}
                {...register("email")}
              />
              {errors.email && (
                <p id="email-error" role="alert" className="font-mono text-xs text-destructive">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password" className="font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
                PASSWORD
              </Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                autoComplete="current-password"
                aria-invalid={!!errors.password}
                aria-describedby={errors.password ? "password-error" : undefined}
                {...register("password")}
              />
              {errors.password && (
                <p id="password-error" role="alert" className="font-mono text-xs text-destructive">
                  {errors.password.message}
                </p>
              )}
            </div>

            {apiError && (
              <div
                id="login-api-error"
                role="alert"
                className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-center text-xs text-destructive font-mono"
              >
                {apiError}
              </div>
            )}

            <StarBorder
              type="submit"
              disabled={isSubmitting}
              className="mt-2 w-full disabled:opacity-60"
              innerClassName="h-10 w-full rounded-lg font-mono text-xs tracking-wider uppercase flex items-center justify-center"
            >
              {isSubmitting ? "Signing in..." : "Sign In"}
            </StarBorder>
          </form>

          <p className="text-center font-mono text-xs text-fg-muted">
            Don&apos;t have an account?{" "}
            <Link
              href="/register"
              className="text-foreground underline underline-offset-4 hover:text-primary transition-colors"
            >
              Register
            </Link>
          </p>
        </div>

        {/* The rank ladder the particles are drawn from. */}
        <div className="mt-8 flex flex-col items-center gap-2.5" aria-hidden>
          <p className="font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
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
