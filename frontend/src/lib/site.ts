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
  { href: "/contests", label: "Contests" },
  { href: "/practice", label: "Practice" },
  { href: "/calendar", label: "Calendar" },
  { href: "/events", label: "Events" },
  { href: "/leaderboard", label: "Leaderboard" },
  { href: "/hall-of-fame", label: "Hall of Fame" },
  { href: "/blog", label: "Blog" },
  { href: "/compete", label: "Compete" },
  { href: "/members", label: "Members" },
  { href: "/about", label: "About" },
  { href: "/gallery", label: "Gallery" },
] as const;

/**
 * Links that used to live only in the footer.
 *
 * The footer now renders on the home page alone. The GitHub org link was
 * previously reachable only from the footer, which would make it
 * unreachable from every other page. It sits apart from `navItems` because
 * it is a destination off the site rather than a section of it.
 *
 * Joining already has a route in the navbar itself (`/login`, rendered as
 * "Sign in"), so it isn't duplicated here. Signing in with a university
 * Google account is what creates an account, so there is no separate
 * registration route to link to.
 */
export const utilityLinks = [
  { href: site.github, label: "GitHub", external: true },
] as const;

