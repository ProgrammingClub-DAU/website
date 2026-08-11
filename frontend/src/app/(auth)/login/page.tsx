/**
 * Login Page Wrapper (Server Component)
 * Route Group: (auth) -> Ignored in URL; endpoint remains public /login.
 * Exports metadata statically and renders the LoginForm Client Component.
 */

import type { Metadata } from "next";
import LoginForm from "./login-form";

export const metadata: Metadata = {
  title: "Sign In",
  description: "Sign in to your Programming Club account.",
};

export default function LoginPage() {
  return <LoginForm />;
}
