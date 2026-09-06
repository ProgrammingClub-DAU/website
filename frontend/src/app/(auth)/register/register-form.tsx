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
import { ParticlesBackdrop } from "@/components/site/particles-backdrop";
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
    <div className="grid min-h-[calc(100vh-3.5rem)] w-full lg:grid-cols-2">
      {/* Left Column: Branding and Ranks (hidden on mobile) */}
      <div className="relative isolate hidden flex-col justify-between overflow-hidden border-r border-hairline bg-surface-3/30 p-12 select-none lg:flex lg:p-16">
        {/* The panel already ends on a row of rank-coloured dots; the particles
            are the same colours at the scale of the whole panel. It costs
            nothing on mobile: the panel is display:none there, so the observer
            never starts the loop and no WebGL context is opened. */}
        <ParticlesBackdrop className="pointer-events-none absolute inset-0 -z-10 [mask-image:radial-gradient(125%_95%_at_20%_40%,#000_30%,transparent_85%)]" />

        {/* No wordmark here: the global navbar already renders one directly above
            this panel, so repeating it showed "Programming Club @ DAU" twice on
            the same screen, at two different left offsets. The spacer keeps the
            three-part justify-between layout intact. */}
        <div aria-hidden="true" />

        {/* Hero headline */}
        <div className="space-y-4">
          <p className="font-mono text-xs text-primary uppercase tracking-[0.15em] font-semibold">
            WEEKLY CONTESTS, LIVE RANK
          </p>
          <h1 className="text-4xl font-semibold tracking-tight text-foreground leading-[1.1] text-balance">
            Solve. Rank up.<br />Climb the board.
          </h1>
        </div>

        {/* Indicators */}
        <div className="space-y-3">
          <p className="font-mono text-[10px] text-fg-subtle tracking-[0.1em] uppercase">
            RANK TRACK
          </p>
          <div className="flex gap-2.5">
            {/* Rank colors from globals.css variables */}
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-newbie)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-pupil)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-specialist)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-expert)" }} />
            <span className="size-2 rounded-full" style={{ backgroundColor: "var(--cf-candidate)" }} />
          </div>
        </div>
      </div>

      {/* Right Column: Interactive Form */}
      <div className="flex items-center justify-center p-6 sm:p-12 lg:p-16 bg-background">
        <div className="w-full max-w-sm space-y-8">
          <div className="space-y-2">
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
      </div>
    </div>
  );
}
