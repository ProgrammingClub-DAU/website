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
  { href: "/events", label: "Events" },
  { href: "/blog", label: "Blogs" },
  { href: "/hall-of-fame", label: "Hall of Fame" },
  { href: "/members", label: "Members" },
  { href: "/leaderboard", label: "Leaderboard" },
  { href: "/about", label: "About" },
] as const;

