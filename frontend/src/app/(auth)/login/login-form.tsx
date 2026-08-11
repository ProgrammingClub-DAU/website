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
import { loginSchema, type LoginInput } from "@/lib/validations/auth";
import { apiClient } from "@/lib/axios";
import { useAuthStore } from "@/store/auth";

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
      // Connect to real backend endpoint
      const response = await apiClient.post("/api/auth/login", {
        email: data.email,
        password: data.password,
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
      // A request that never reached the server has no response. Reporting that
      // as a credentials problem sends people hunting for a wrong password when
      // the backend simply is not running.
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
    <div className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md shadow-panel border-hairline">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Sign In
          </CardTitle>
          <CardDescription>
            Enter your credentials to access your Programming Club account
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <CardContent className="space-y-4">
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
          </CardContent>

          <CardFooter className="flex flex-col gap-4">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="w-full font-mono text-xs tracking-wider uppercase"
            >
              {isSubmitting ? "Signing in..." : "Sign In"}
            </Button>
            <p className="text-center font-mono text-xs text-fg-muted">
              Don&apos;t have an account?{" "}
              <Link
                href="/register"
                className="text-foreground underline underline-offset-4 hover:text-primary transition-colors"
              >
                Register here
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
