/**
 * Register Page Wrapper (Server Component)
 * Route Group: (auth) -> Ignored in URL; endpoint remains public /register.
 * Exports metadata statically and renders the RegisterForm Client Component.
 */

import type { Metadata } from "next";
import RegisterForm from "./register-form";

export const metadata: Metadata = {
  title: "Create Account",
  description: "Join the Programming Club to track ratings and participate in rounds.",
};

export default function RegisterPage() {
  return <RegisterForm />;
}
