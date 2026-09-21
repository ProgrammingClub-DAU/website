import type { MetadataRoute } from "next";

import { eventsService } from "@/lib/services/events";
import { hallOfFameService } from "@/lib/services/hall-of-fame";
import { siteUrl } from "@/lib/site-url";

// Rebuilt at most once an hour, so new events and Hall of Fame entries are
// listed without a redeploy.
export const revalidate = 3600;

/** Public pages with no parameters, most important first. */
const STATIC_ROUTES: { path: string; priority: number }[] = [
  { path: "", priority: 1 },
  { path: "/about", priority: 0.8 },
  { path: "/events", priority: 0.8 },
  { path: "/hall-of-fame", priority: 0.7 },
  { path: "/leaderboard", priority: 0.7 },
  { path: "/members", priority: 0.6 },
  { path: "/gallery", priority: 0.5 },
  { path: "/gallery/batches", priority: 0.4 },
  { path: "/blog", priority: 0.3 },
  { path: "/privacy", priority: 0.2 },
];

/**
 * Every public page, plus one entry per event and Hall of Fame entry.
 *
 * Member profiles are left out on purpose: they are reachable from the
 * leaderboard, but a list of every student's page is not something the club
 * needs to hand to search engines.
 *
 * If the backend cannot be reached, the static pages are still listed.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const entries: MetadataRoute.Sitemap = STATIC_ROUTES.map((route) => ({
    url: `${siteUrl}${route.path}`,
    priority: route.priority,
  }));

  const [upcoming, completed, hallOfFame] = await Promise.all([
    eventsService.listUpcoming().catch(() => []),
    eventsService.listCompleted().catch(() => []),
    hallOfFameService.list().catch(() => []),
  ]);

  for (const event of [...upcoming, ...completed]) {
    entries.push({ url: `${siteUrl}/events/${event.id}`, priority: 0.5 });
  }
  for (const entry of hallOfFame) {
    entries.push({
      url: `${siteUrl}/hall-of-fame/${entry.id}`,
      lastModified: entry.achievedOn,
      priority: 0.5,
    });
  }

  return entries;
}
