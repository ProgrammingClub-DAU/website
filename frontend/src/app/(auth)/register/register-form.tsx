/**
 * RegisterForm Client Component
 * Renders sign-up interface and handles logic connecting to POST /api/auth/register.
 */

"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import axios from "axios";
import { AuthBackdrop } from "@/components/site/auth-backdrop";
import StarBorder from "@/components/site/star-border";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { registerSchema, type RegisterInput } from "@/lib/validations/auth";
import { apiClient } from "@/lib/axios";
import { useAuthStore, type ApiResponse, type AuthResponse, mapAuthResponseToUser } from "@/store/auth";

export default function RegisterForm() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (data: RegisterInput) => {
    setApiError(null);
    try {
      // Connect to backend register endpoint using strongly typed responses
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/api/auth/register", {
        name: data.fullName, // Backend key name is 'name'
        email: data.email,
        password: data.password,
        codeforcesHandle: null, // Fixed payload requirement
      });

      const authData = response.data.data;

      // Map backend AuthResponse to Zustand User object via shared mapper function
      login(mapAuthResponseToUser(authData), authData.token);

      // Redirect home on success
      router.push("/");
    } catch (err: unknown) {
      // Differentiate between network connection problems and server-side validation/cred failures
      let msg = "Failed to create account. Please try again.";
      if (axios.isAxiosError(err)) {
        if (!err.response) {
          msg = "Cannot reach the server. Check your connection and try again.";
        } else {
          const responseData = err.response.data as { message?: string } | undefined;
          msg = responseData?.message ?? msg;
        }
      }
      setApiError(msg);
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
        {/* Was the left panel's branding. It keeps the page's h1 — dropping the
            panel outright would have left an auth page whose only heading was
            the form's h2. */}
        {/* The headline is the only text sitting on the raw backdrop — the form
            below has its own glass panel. Rather than punch a hole in the
            background, which left the top of the page looking empty, it gets a
            local scrim: the page colour at full strength directly behind the
            words, fading to nothing well before the block's edge. Without it
            the field's brightest pixels land under white text at 1.9:1. */}
        <div className="relative space-y-3 text-center">
          <div
            aria-hidden
            className="pointer-events-none absolute -inset-x-16 -inset-y-12 -z-10 bg-[radial-gradient(farthest-side_at_50%_50%,var(--background)_62%,transparent_100%)]"
          />
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
              REGISTER
            </p>
            <h2 className="text-3xl font-semibold tracking-tight text-foreground">
              Create an Account
            </h2>
            <p className="text-sm text-fg-muted">
              Join the Programming Club to track ratings and participate in rounds
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-1.5">
              <Label htmlFor="fullName" className="font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
                FULL NAME
              </Label>
              <Input
                id="fullName"
                type="text"
                placeholder="Alex Turing"
                autoComplete="name"
                aria-invalid={!!errors.fullName}
                aria-describedby={errors.fullName ? "fullname-error" : undefined}
                {...register("fullName")}
              />
              {errors.fullName && (
                <p id="fullname-error" role="alert" className="font-mono text-xs text-destructive">
                  {errors.fullName.message}
                </p>
              )}
            </div>

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
                placeholder="Min 8 chars, 1 uppercase & 1 number"
                autoComplete="new-password"
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

            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword" className="font-mono text-[10px] tracking-[0.1em] text-fg-subtle uppercase">
                CONFIRM PASSWORD
              </Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="Re-enter password"
                autoComplete="new-password"
                aria-invalid={!!errors.confirmPassword}
                aria-describedby={errors.confirmPassword ? "confirmpassword-error" : undefined}
                {...register("confirmPassword")}
              />
              {errors.confirmPassword && (
                <p id="confirmpassword-error" role="alert" className="font-mono text-xs text-destructive">
                  {errors.confirmPassword.message}
                </p>
              )}
            </div>

            {apiError && (
              <div
                id="register-api-error"
                role="alert"
                className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-center text-xs text-destructive font-mono"
              >
                {apiError}
              </div>
            )}

            <StarBorder
              type="submit"
              backgroundColor="var(--primary)"
              textColor="var(--primary-foreground)"
              disabled={isSubmitting}
              className="mt-2 w-full disabled:opacity-60"
              innerClassName="h-10 w-full rounded-lg font-mono text-xs tracking-wider uppercase flex items-center justify-center"
            >
              {isSubmitting ? "Creating Account..." : "Create Account"}
            </StarBorder>
          </form>

          <p className="text-center font-mono text-xs text-fg-muted">
            Already have an account?{" "}
            <Link
              href="/login"
              className="text-foreground underline underline-offset-4 hover:text-primary transition-colors"
            >
              Login
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
