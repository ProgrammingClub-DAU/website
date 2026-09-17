/**
 * The site's public origin, with no trailing slash.
 *
 * Share cards, robots.txt and the sitemap all need absolute URLs, and a wrong
 * origin in any of them is silent: the page still works, but search engines
 * and chat previews point at localhost.
 *
 * In order:
 * 1. NEXT_PUBLIC_SITE_URL -- set this on Vercel to the real domain.
 * 2. VERCEL_PROJECT_PRODUCTION_URL -- Vercel sets it on every deployment, so a
 *    production build without (1) still points somewhere real.
 * 3. localhost, for development.
 */
function resolveSiteUrl(): string {
  const explicit = process.env.NEXT_PUBLIC_SITE_URL;
  if (explicit) return explicit.replace(/\/+$/, "");
  const vercel = process.env.VERCEL_PROJECT_PRODUCTION_URL;
  if (vercel) return `https://${vercel}`;
  return "http://localhost:3000";
}

export const siteUrl = resolveSiteUrl();
