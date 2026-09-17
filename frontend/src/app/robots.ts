import type { MetadataRoute } from "next";

import { siteUrl } from "@/lib/site-url";

/**
 * Crawlers may read every public page. Kept out: admin screens, the first
 * sign-in step, the "my profile" redirect and the server-side API proxies --
 * none of them has anything to show a visitor who is not signed in.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/admin", "/welcome", "/profile$", "/next-api/"],
    },
    sitemap: `${siteUrl}/sitemap.xml`,
  };
}
