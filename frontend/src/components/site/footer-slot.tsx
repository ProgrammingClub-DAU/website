"use client";

import { usePathname } from "next/navigation";

import { Footer } from "@/components/site/footer";

/**
 * Renders the footer on the home page only.
 *
 * The root layout is a server component and cannot read the current route, so
 * this small client component holds the rule. Keeping it here means the footer
 * stays mounted in one place rather than being repeated per page.
 *
 * The links the footer used to carry alone — GitHub and joining — moved into the
 * navbar first, so they remain reachable everywhere.
 */
export function FooterSlot() {
  const pathname = usePathname();
  return pathname === "/" ? <Footer /> : null;
}
