/**
 * Register Page Component
 * Purpose: Renders the member sign-up form with full name, email, password strength, and match validation.
 * Auth Connection: Validates registration payload, ensuring password complexity rules before submission.
 * Deferred: Wiring submit handler to `POST /api/auth/register` endpoint (deferred to Stage 3).
 */

"use client";

import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { registerSchema, type RegisterInput } from "@/lib/validations/auth";

export default function RegisterPage() {
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

  const onSubmit = (data: RegisterInput) => {
    // Stage 2 mock submit handler per specification
    console.log("Registration submitted:", data);
  };

  return (
    <div className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md shadow-panel border-hairline">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Create an Account
          </CardTitle>
          <CardDescription>
            Join the Programming Club to participate in rounds and track your rating
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="fullName">Full name</Label>
              <Input
                id="fullName"
                type="text"
                placeholder="Alex Turing"
                autoComplete="name"
                aria-invalid={!!errors.fullName}
                {...register("fullName")}
              />
              {errors.fullName && (
                <p className="font-mono text-xs text-destructive">
                  {errors.fullName.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">Email address</Label>
              <Input
                id="email"
                type="email"
                placeholder="member@university.edu"
                autoComplete="email"
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              {errors.email && (
                <p className="font-mono text-xs text-destructive">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="Min 8 chars, 1 uppercase & 1 number"
                autoComplete="new-password"
                aria-invalid={!!errors.password}
                {...register("password")}
              />
              {errors.password && (
                <p className="font-mono text-xs text-destructive">
                  {errors.password.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirmPassword">Confirm password</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="Re-enter password"
                autoComplete="new-password"
                aria-invalid={!!errors.confirmPassword}
                {...register("confirmPassword")}
              />
              {errors.confirmPassword && (
                <p className="font-mono text-xs text-destructive">
                  {errors.confirmPassword.message}
                </p>
              )}
            </div>
          </CardContent>

          <CardFooter className="flex flex-col gap-4">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full font-mono text-xs tracking-wider uppercase"
            >
              {isSubmitting ? "Creating Account..." : "Create Account"}
            </Button>
            <p className="text-center font-mono text-xs text-fg-muted">
              Already have an account?{" "}
              <Link
                href="/login"
                className="text-foreground underline underline-offset-4 hover:text-primary transition-colors"
              >
                Sign in here
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
