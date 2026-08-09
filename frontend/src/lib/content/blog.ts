export type Post = {
  title: string;
  date: string;
  read: string;
  author: string;
  tags: string[];
  excerpt: string;
};

export const featuredPost = {
  title: "[SAMPLE] Editorial: Weekly Round 13 — the DP nobody solved",
  excerpt:
    "A walk through problem E from the last club round: the state definition, why the greedy fails, and the O(n) rewrite.",
  date: "[DATE]",
  read: "10 min",
  author: "arjun_dp",
  initials: "AR",
};

/** Sample posts, illustrative of format. Replace with real content. */
export const posts: Post[] = [
  {
    title: "Editorial: Weekly Round 12, problems A–D",
    date: "[DATE]",
    read: "8 min",
    author: "arjun_dp",
    tags: ["dp", "greedy"],
    excerpt:
      "Full solutions with complexity notes, including the two approaches people tried for D.",
  },
  {
    title: "A practical order for learning graph algorithms",
    date: "[DATE]",
    read: "6 min",
    author: "meher.solves",
    tags: ["graphs"],
    excerpt:
      "What to learn first, what can wait, and which problems to solve after each topic.",
  },
  {
    title: "Binary search on the answer, without the off-by-ones",
    date: "[DATE]",
    read: "5 min",
    author: "kx_bitset",
    tags: ["binary search"],
    excerpt:
      "One template that works for both integer and floating-point predicates.",
  },
  {
    title: "Setting up a contest-ready C++ environment",
    date: "[DATE]",
    read: "4 min",
    author: "nidhi_ac",
    tags: ["setup"],
    excerpt:
      "Compile flags, a debug macro that prints containers, and a stress-testing script.",
  },
  {
    title: "Reading a problem statement under time pressure",
    date: "[DATE]",
    read: "5 min",
    author: "arjun_dp",
    tags: ["strategy"],
    excerpt: "Where constraints usually give away the intended complexity.",
  },
];

export const blogTags = [
  "All",
  ...Array.from(new Set(posts.flatMap((p) => p.tags))),
];
