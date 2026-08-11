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
import { apiClient } from "@/lib/axios";
import { useAuthStore } from "@/store/auth";

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
      // Connect to real backend endpoint
      const response = await apiClient.post("/api/auth/register", {
        name: data.fullName, // Backend key name is 'name'
        email: data.email,
        password: data.password,
        codeforcesHandle: null, // Fixed payload requirement
      });

      // API Response follows ApiResponse<AuthResponse> structure
      const authData = response.data.data;

      // Map backend 'name' to frontend store 'fullName'
      login(
        {
          id: authData.id,
          email: authData.email,
          fullName: authData.name,
          role: authData.role,
          codeforcesHandle: authData.codeforcesHandle,
        },
        authData.token
      );

      // Redirect home on success
      router.push("/");
    } catch (err: unknown) {
      // Safely access backend error response message or fallback
      let msg = "Failed to create account. Please try again.";
      if (axios.isAxiosError(err)) {
        const responseData = err.response?.data as { message?: string } | undefined;
        if (responseData?.message) {
          msg = responseData.message;
        }
      }
      setApiError(msg);
    }
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
                aria-describedby={errors.fullName ? "fullname-error" : undefined}
                {...register("fullName")}
              />
              {errors.fullName && (
                <p id="fullname-error" role="alert" className="font-mono text-xs text-destructive">
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
                aria-describedby={errors.email ? "email-error" : undefined}
                {...register("email")}
              />
              {errors.email && (
                <p id="email-error" role="alert" className="font-mono text-xs text-destructive">
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
                aria-describedby={errors.password ? "password-error" : undefined}
                {...register("password")}
              />
              {errors.password && (
                <p id="password-error" role="alert" className="font-mono text-xs text-destructive">
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
