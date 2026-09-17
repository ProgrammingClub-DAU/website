/**
 * Welcome Page Wrapper (Server Component)
 * Route Group: (auth) -> Ignored in URL; endpoint remains public /welcome.
 * Shown once, straight after a first Google sign-in.
 */

import type { Metadata } from "next";
import WelcomeForm from "./welcome-form";

export const metadata: Metadata = {
  title: "Welcome",
  description: "Finish setting up your Programming Club account.",
  robots: { index: false },
};

export default function WelcomePage() {
  return <WelcomeForm />;
}
