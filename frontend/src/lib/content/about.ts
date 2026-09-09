export const organisation = [
  {
    role: "Core team",
    color: "var(--cf-grandmaster)",
    body: "Elected student coordinators who guide the club's contests, workshops, cultural celebrations, and campus initiatives.",
  },
  {
    role: "Problem setters",
    color: "var(--cf-master)",
    body: "Members who write and test original problems for club rounds, curating challenges across all difficulty tiers.",
  },
  {
    role: "Web team",
    color: "var(--cf-expert)",
    body: "Maintains this platform, automated Codeforces sync, leaderboards, and member profiles.",
  },
  {
    role: "Mentors",
    color: "var(--cf-pupil)",
    body: "Seniors and ICPC regionalists who host doubt-solving sessions, editorial reviews, and 1-on-1 guidance.",
  },
];

export const calendar = [
  {
    when: "Semester start",
    title: "Onboarding",
    body: "Intro session, handle collection, and a beginner-friendly round in the first weeks.",
  },
  {
    when: "Through the term",
    title: "Weekly rounds",
    body: "A club contest every week, with an editorial published after it.",
  },
  {
    when: "Pre-ICPC",
    title: "Team practice",
    body: "Team formation, mock regionals, and past-set practice ahead of the prelims.",
  },
  {
    when: "Breaks",
    title: "Long contests",
    body: "Winter and summer long contests that stay open for several days.",
  },
];

export const whatWeDo = [
  {
    title: "Weekly rounds",
    body: "Club-hosted contests on a fixed weekly slot, with problems set by senior members and difficulty tiers for beginners.",
  },
  {
    title: "Editorials",
    body: "Written solutions after every round, plus deeper breakdowns of recurring topics like graphs, DP, and number theory.",
  },
  {
    title: "ICPC preparation",
    body: "Team formation, mock regionals, and practice on past problem sets through the second half of the year.",
  },
  {
    title: "Beginner track",
    body: "A structured start for first-year students: language basics, complexity, and a first hundred problems.",
  },
  {
    title: "Rating tracking",
    body: "Codeforces, CodeChef, LeetCode, and AtCoder handles collected in one directory so progress is visible over time.",
  },
];

/**
 * The platforms the club practises on.
 *
 * `syncs` is a factual claim about this site, not a ranking. Codeforces and
 * LeetCode expose stable public APIs, so their ratings are pulled into the
 * leaderboard automatically. CodeChef and AtCoder do not, so those are stored as
 * links only — a locked decision in Section 1 of the Phase 2 plan, taken because
 * scraping either one breaks whenever their markup changes.
 *
 * Saying so plainly is the point of the flag: a member who links a CodeChef
 * profile should not be left wondering why their rating never appears.
 */
export const platforms = [
  {
    id: "codeforces" as const,
    body: "Our main contest platform. Ratings sync into the club leaderboard automatically, every six hours.",
    syncs: true,
  },
  {
    id: "leetcode" as const,
    body: "Interview-style practice and weekly contests. Contest ratings sync to your profile when you link a handle.",
    syncs: true,
  },
  {
    id: "codechef" as const,
    body: "Long and short format rounds, with a gentle on-ramp for beginners. Link your profile from your dashboard.",
    syncs: false,
  },
  {
    id: "atcoder" as const,
    body: "Clean, carefully tested problems. The weekend Beginner Contests are a good first contest for anyone.",
    syncs: false,
  },
];

export const faq = [
  {
    q: "Do I need contest experience to join?",
    a: "No. The beginner track assumes you have written some code in any language and nothing more.",
  },
  {
    q: "Which language should I use?",
    a: "C++ is the most common in the club, but Java and Python are fine. Bring what you are fastest in.",
  },
  {
    q: "Is there a fee or a selection process?",
    a: "No fee and no selection. Sign in with your DAU email and you are a member.",
  },
  {
    q: "How much time does it take?",
    a: "Around 2 to 3 hours weekly for the contest and post-round editorial discussion. All other practice and workshops are completely self-paced.",
  },
];

export const joinSteps = [
  { n: "01", text: "Sign up with your DAU email." },
  { n: "02", text: "Add your Codeforces handle to your profile." },
  { n: "03", text: "Show up for the next weekly round." },
];
