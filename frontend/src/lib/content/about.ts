/**
 * Copy for the About page.
 *
 * Everything here is either the club's own description of itself (the
 * orientation deck) or a plain fact about this site. Nothing is estimated: a
 * member count, a meeting slot or a founding year belongs here only once the
 * club confirms it.
 */

/** What the club runs, in the club's own words (orientation deck, "What do we do"). */
export const whatWeDo = [
  { title: "Intra College Programming Contests", short: "IPC" },
  { title: "Juniors' Contests" },
  { title: "Inter-wing Competition" },
  { title: "Round Robin Relay" },
  { title: "Lecture sessions on competitive programming" },
  { title: "Post-contest discussions and editorials" },
  { title: "Guiding juniors in their CP journey" },
];

/** "New to competitive programming?" -- the orientation deck's introduction. */
export const cpIntro = {
  what: [
    "Solving well-defined problems by writing code, within given constraints.",
    "Built on algorithms, data structures, mathematics and logic.",
    "A solution has to be correct and efficient in both time and memory.",
  ],
  why: [
    "Sharpens analytical and problem-solving skills.",
    "Unlimited practice material online.",
    "The platforms are free to use.",
    "A large community to learn from.",
    "Coding rounds and technical interviews test the same skills.",
  ],
  benefits: [
    "Exercise for the brain.",
    "Practice at working under pressure.",
    "Thinking out of the box.",
    "Evolving from a working solution to an efficient one.",
    "Knowing your language's features well.",
    "Competitive and career opportunities.",
  ],
};

/**
 * Where to practise.
 *
 * `syncs` is a factual claim about this site, not a ranking. Codeforces and
 * LeetCode expose stable public APIs, so their ratings are pulled into the
 * leaderboard automatically. The others do not, so they are not tracked here --
 * a locked decision in Section 1 of the Phase 2 plan, taken because scraping
 * breaks whenever a site's markup changes.
 */
export const practicePlatforms = [
  { id: "codeforces", label: "Codeforces", url: "https://codeforces.com", syncs: true },
  { id: "atcoder", label: "AtCoder", url: "https://atcoder.jp", syncs: false },
  { id: "leetcode", label: "LeetCode", url: "https://leetcode.com", syncs: true },
  { id: "codechef", label: "CodeChef", url: "https://www.codechef.com", syncs: false },
  { id: "hackerrank", label: "HackerRank", url: "https://www.hackerrank.com", syncs: false },
] as const;

export const faq = [
  {
    q: "Do I need contest experience to join?",
    a: "No. It helps to have written some code in any language, and that is all.",
  },
  {
    q: "Which language should I use?",
    a: "Whichever you are fastest in. C++ is the most common choice in competitive programming, and Java and Python are accepted on every platform above.",
  },
  {
    q: "How do I sign in?",
    a: "With your DAU Google account (@dau.ac.in). Other accounts are not accepted.",
  },
  {
    q: "Which ratings appear on the leaderboard?",
    a: "Codeforces and LeetCode, read from the handles on your profile. CodeChef and AtCoder profiles show as links on your profile.",
  },
];

export const joinSteps = [
  { n: "01", text: "Sign in with your DAU Google account." },
  { n: "02", text: "Add your Codeforces and LeetCode handles to your profile." },
  { n: "03", text: "Check the events page for what is next." },
];
