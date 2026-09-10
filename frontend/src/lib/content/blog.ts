export type Post = {
  title: string;
  date: string;
  read: string;
  author: string;
  tags: string[];
  excerpt: string;
};

export type FeaturedPost = {
  title: string;
  excerpt: string;
  date: string;
  read: string;
  /** The author's own handle, as it appears on their profile. */
  author: string;
  /** Initials for the avatar chip. */
  initials: string;
  /**
   * An optional snippet lifted from the post, shown beside it.
   *
   * Part of the post's own data rather than markup in the page, which is where
   * it used to live: the page hard-coded a DP loop that was presented as coming
   * from the featured editorial and would have stayed on screen unchanged
   * underneath any real post that replaced it.
   */
  snippet?: string;
};

/**
 * Editorials published by club members.
 *
 * Empty, because the club has not published any yet.
 *
 * This file previously held five posts under invented member handles —
 * arjun_dp, meher.solves, kx_bitset, nidhi_ac — every one dated "[DATE]", which
 * the blog page rendered in the same layout a real post would get. Sample copy
 * that is shaped like real content is worse than no content at all: a visitor
 * has no way to tell which of the two they are reading, and the handles named
 * members who do not exist.
 *
 * The page renders a designed empty state off these, so publishing the first
 * real editorial means adding an entry here and nothing else.
 */
export const posts: Post[] = [];

/** The post pinned above the list, or `null` when there is nothing to pin. */
export const featuredPost: FeaturedPost | null = null;

export const blogTags = [
  "All",
  ...Array.from(new Set(posts.flatMap((p) => p.tags))),
];
