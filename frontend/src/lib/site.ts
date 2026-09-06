export const site = {
  name: "Programming Club",
  suffix: "@ DAU",
  fullName: "Programming Club @ DAU",
  university: "Dhirubhai Ambani University, Gandhinagar",
  tagline: "Competitive programming at Dhirubhai Ambani University, Gandhinagar.",
  github: "https://github.com/ProgrammingClub-DAU",
  /*
   * Unset on purpose. A Codeforces *group* lives at /group/<id>, not /profile/,
   * and pointing the public site at an unconfirmed account is a factual claim
   * about someone else's. Fill in once the club confirms the real URL, then
   * restore the link in footer.tsx.
   */
  codeforces: null,
} as const;

export const navItems = [
  { href: "/", label: "Home" },
  { href: "/about", label: "About" },
  { href: "/events", label: "Events" },
  { href: "/hall-of-fame", label: "Hall of Fame" },
  { href: "/gallery", label: "Gallery" },
  { href: "/blog", label: "Blog" },
  { href: "/members", label: "Members" },
  { href: "/leaderboard", label: "Leaderboard" },
] as const;

/**
 * Links that used to live only in the footer.
 *
 * The footer now renders on the home page alone. The GitHub org link was
 * previously reachable only from the footer, which would make it
 * unreachable from every other page. It sits apart from `navItems` because
 * it is a destination off the site rather than a section of it.
 *
 * Joining already has a route in the navbar itself (`/register`, rendered
 * as "Join" / "Join the Club"), so it isn't duplicated here.
 */
export const utilityLinks = [
  { href: site.github, label: "GitHub", external: true },
] as const;

