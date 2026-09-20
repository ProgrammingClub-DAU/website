export type BannerRarity = "Common" | "Rare" | "Epic" | "Legendary" | "Exclusive" | "Special";

export interface BannerColors {
  primary: string;       // Primary background tint
  accent: string;        // Accent highlight / glow
  border: string;        // Card / banner border
  glow: string;          // Box shadow / ambient glow
  text: string;          // Light high-contrast text color
  gradient: string;      // CSS gradient string
  scrim: string;         // Left scrim for dark text-shadow readability
}

export interface BannerConfig {
  id: string;
  name: string;
  minRating: number;
  rarity: BannerRarity;
  colors: BannerColors;
  animation: "geometric" | "waves" | "circuits" | "particles" | "flames" | "sunburst" | "shimmer" | "sparks";
  isRankBanner?: boolean;
  rank?: 1 | 2 | 3;
  description: string;
  iconName?: "crown" | "medal" | "trophy" | "sparkles" | "flame" | "shield";
}

// ── LAYER 1: RANK BANNERS (TOP 3 EXCLUSIVE) ──
export const RANK_BANNERS: Record<string, BannerConfig> = {
  "rank-gold": {
    id: "rank-gold",
    name: "Apex Sovereign (Gold #1)",
    minRating: 0,
    rarity: "Exclusive",
    isRankBanner: true,
    rank: 1,
    iconName: "crown",
    description: "Exclusive to Rank #1. Features an animated crown, sunburst rays, diagonal shimmer, and celestial star dust.",
    animation: "sunburst",
    colors: {
      primary: "#3a2a0a",
      accent: "#f5b83d",
      border: "#f5b83d",
      glow: "rgba(245, 184, 61, 0.45)",
      text: "#fff3cf",
      gradient: "linear-gradient(135deg, #2b1f06 0%, #3d2d0b 50%, #1a1304 100%)",
      scrim: "linear-gradient(90deg, rgba(15, 10, 2, 0.92) 0%, rgba(15, 10, 2, 0.6) 60%, transparent 100%)",
    },
  },
  "rank-silver": {
    id: "rank-silver",
    name: "Vanguard Titan (Silver #2)",
    minRating: 0,
    rarity: "Exclusive",
    isRankBanner: true,
    rank: 2,
    iconName: "medal",
    description: "Exclusive to Rank #2. Cool metallic platinum-silver with slow shimmer sweep and star glints.",
    animation: "shimmer",
    colors: {
      primary: "#262c37",
      accent: "#cfd5e2",
      border: "#cfd5e2",
      glow: "rgba(207, 213, 226, 0.35)",
      text: "#f4f6fb",
      gradient: "linear-gradient(135deg, #1f232c 0%, #2f3644 50%, #171a21 100%)",
      scrim: "linear-gradient(90deg, rgba(12, 15, 20, 0.92) 0%, rgba(12, 15, 20, 0.6) 60%, transparent 100%)",
    },
  },
  "rank-bronze": {
    id: "rank-bronze",
    name: "Valiant Sentinel (Bronze #3)",
    minRating: 0,
    rarity: "Exclusive",
    isRankBanner: true,
    rank: 3,
    iconName: "medal",
    description: "Exclusive to Rank #3. Warm copper banner with ember glow and gentle metallic shimmer.",
    animation: "shimmer",
    colors: {
      primary: "#3a2416",
      accent: "#d08a5a",
      border: "#d08a5a",
      glow: "rgba(208, 138, 90, 0.35)",
      text: "#ffd9bd",
      gradient: "linear-gradient(135deg, #29170c 0%, #3e2617 50%, #1b0f07 100%)",
      scrim: "linear-gradient(90deg, rgba(18, 10, 5, 0.92) 0%, rgba(18, 10, 5, 0.6) 60%, transparent 100%)",
    },
  },
};

// ── LAYER 2: RATING BANNERS (EARNED BY MAX RATING) ──
export const RATING_BANNERS: BannerConfig[] = [
  {
    id: "rookie",
    name: "Rookie",
    minRating: 0,
    rarity: "Common",
    animation: "geometric",
    description: "Graphite geometric grid. Standard issue for all club initiate coders.",
    colors: {
      primary: "#222533",
      accent: "#4a516d",
      border: "#3a3f55",
      glow: "rgba(74, 81, 109, 0.25)",
      text: "#e1e4ee",
      gradient: "linear-gradient(135deg, #181a24 0%, #232738 60%, #151720 100%)",
      scrim: "linear-gradient(90deg, rgba(11, 12, 18, 0.92) 0%, rgba(11, 12, 18, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "pupil",
    name: "Pupil",
    minRating: 1200,
    rarity: "Common",
    animation: "waves",
    description: "Emerald matrix signal. Unlocked upon reaching 1200 rating.",
    colors: {
      primary: "#143323",
      accent: "#2f9d64",
      border: "#288052",
      glow: "rgba(47, 157, 100, 0.3)",
      text: "#d2f7e3",
      gradient: "linear-gradient(135deg, #0e2419 0%, #1a422e 60%, #0b1a12 100%)",
      scrim: "linear-gradient(90deg, rgba(7, 18, 12, 0.92) 0%, rgba(7, 18, 12, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "specialist",
    name: "Specialist",
    minRating: 1400,
    rarity: "Rare",
    animation: "waves",
    description: "Cybernetic cyan waves. Unlocked upon reaching 1400 rating.",
    colors: {
      primary: "#0f343e",
      accent: "#18b5c9",
      border: "#18a5b8",
      glow: "rgba(24, 181, 201, 0.35)",
      text: "#cbf7fc",
      gradient: "linear-gradient(135deg, #09222a 0%, #124452 60%, #06191f 100%)",
      scrim: "linear-gradient(90deg, rgba(5, 18, 22, 0.92) 0%, rgba(5, 18, 22, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "expert",
    name: "Expert",
    minRating: 1600,
    rarity: "Rare",
    animation: "circuits",
    description: "Electric sapphire circuits. Unlocked upon reaching 1600 rating.",
    colors: {
      primary: "#142854",
      accent: "#3d7eff",
      border: "#336de0",
      glow: "rgba(61, 126, 255, 0.38)",
      text: "#d9e6ff",
      gradient: "linear-gradient(135deg, #0d1a38 0%, #1a3674 60%, #09132a 100%)",
      scrim: "linear-gradient(90deg, rgba(7, 14, 30, 0.92) 0%, rgba(7, 14, 30, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "candidate-master",
    name: "Candidate Master",
    minRating: 1900,
    rarity: "Epic",
    animation: "particles",
    description: "Cosmic violet nebula with floating stardust. Unlocked upon reaching 1900 rating.",
    colors: {
      primary: "#321d58",
      accent: "#a966ff",
      border: "#964ef5",
      glow: "rgba(169, 102, 255, 0.45)",
      text: "#f1e3ff",
      gradient: "linear-gradient(135deg, #22123d 0%, #412674 60%, #190c2e 100%)",
      scrim: "linear-gradient(90deg, rgba(16, 8, 29, 0.92) 0%, rgba(16, 8, 29, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "master",
    name: "Master",
    minRating: 2100,
    rarity: "Legendary",
    animation: "flames",
    description: "Solar flare ignition with incandescent embers. Unlocked upon reaching 2100 rating.",
    colors: {
      primary: "#4a2406",
      accent: "#ff9426",
      border: "#e67812",
      glow: "rgba(255, 148, 38, 0.48)",
      text: "#ffeacc",
      gradient: "linear-gradient(135deg, #331702 0%, #612f07 60%, #261101 100%)",
      scrim: "linear-gradient(90deg, rgba(23, 10, 1, 0.92) 0%, rgba(23, 10, 1, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "club-founder",
    name: "Club Founder",
    minRating: 0,
    rarity: "Special",
    animation: "shimmer",
    description: "Honorary badge reserved for founding members and executive convenors.",
    colors: {
      primary: "#40132b",
      accent: "#ff3d94",
      border: "#e02677",
      glow: "rgba(255, 61, 148, 0.45)",
      text: "#ffe0ee",
      gradient: "linear-gradient(135deg, #2a0b1c 0%, #521937 60%, #1e0613 100%)",
      scrim: "linear-gradient(90deg, rgba(20, 4, 13, 0.92) 0%, rgba(20, 4, 13, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "contest-winner",
    name: "Contest Winner",
    minRating: 0,
    rarity: "Special",
    animation: "sparks",
    description: "Conferred to champions of official college programming tournaments.",
    colors: {
      primary: "#332244",
      accent: "#f4a261",
      border: "#e76f51",
      glow: "rgba(244, 162, 97, 0.42)",
      text: "#fff0e6",
      gradient: "linear-gradient(135deg, #22162e 0%, #442d5c 60%, #180e22 100%)",
      scrim: "linear-gradient(90deg, rgba(16, 9, 23, 0.92) 0%, rgba(16, 9, 23, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "creator-vip",
    name: "Website Architect (VIP)",
    minRating: 0,
    rarity: "Exclusive",
    animation: "flames",
    iconName: "crown",
    description: "Ultra-exclusive Red VIP Banner reserved for the 6 engineers who designed and built this website platform.",
    colors: {
      primary: "#2e0508",
      accent: "#ff1e42",
      border: "#ff2e51",
      glow: "rgba(255, 30, 66, 0.55)",
      text: "#fff0f2",
      gradient: "linear-gradient(135deg, #1f0305 0%, #460910 50%, #150204 100%)",
      scrim: "linear-gradient(90deg, rgba(20, 2, 4, 0.94) 0%, rgba(20, 2, 4, 0.65) 60%, transparent 100%)",
    },
  },
];

// ── LAYER 3: LEETCODE TIER BANNERS ──
// LeetCode only has two named titles: Knight (≥ 1850) and Guardian (≥ 2100).
// Everything below 1850 is unranked/unrated.
export const LC_BANNERS: BannerConfig[] = [
  {
    id: "lc-unrated",
    name: "LC Unrated",
    minRating: 0,
    rarity: "Common",
    animation: "geometric",
    description: "LeetCode unrated — the first step on the grinding path.",
    colors: {
      primary: "#1a1f2e",
      accent: "#6b7a99",
      border: "#4e5a7a",
      glow: "rgba(107, 122, 153, 0.25)",
      text: "#d8ddf0",
      gradient: "linear-gradient(135deg, #12151f 0%, #1f2438 60%, #0e1018 100%)",
      scrim: "linear-gradient(90deg, rgba(10, 12, 18, 0.92) 0%, rgba(10, 12, 18, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "lc-knight",
    name: "LC Knight",
    minRating: 1850,
    rarity: "Epic",
    animation: "circuits",
    description: "LeetCode Knight — elite solver who dominates contests and hard problems alike.",
    colors: {
      primary: "#1c2a50",
      accent: "#7c9eff",
      border: "#5c82ff",
      glow: "rgba(124, 158, 255, 0.42)",
      text: "#e0e8ff",
      gradient: "linear-gradient(135deg, #111a36 0%, #243468 60%, #0c1228 100%)",
      scrim: "linear-gradient(90deg, rgba(8, 12, 26, 0.92) 0%, rgba(8, 12, 26, 0.6) 60%, transparent 100%)",
    },
  },
  {
    id: "lc-guardian",
    name: "LC Guardian",
    minRating: 2100,
    rarity: "Legendary",
    animation: "particles",
    description: "LeetCode Guardian — the top 0.1%. A mythic banner wreathed in violet starfire.",
    colors: {
      primary: "#2e1a5c",
      accent: "#c084fc",
      border: "#a855f7",
      glow: "rgba(192, 132, 252, 0.50)",
      text: "#f5e6ff",
      gradient: "linear-gradient(135deg, #1e0f40 0%, #3b2080 60%, #160b30 100%)",
      scrim: "linear-gradient(90deg, rgba(14, 7, 30, 0.92) 0%, rgba(14, 7, 30, 0.6) 60%, transparent 100%)",
    },
  },
];

export function isWebsiteCreator(
  member?: { isPlatformCreator?: boolean; platformCreator?: boolean } | null
): boolean {
  if (!member) return false;
  return !!(member.isPlatformCreator ?? member.platformCreator);
}

export const ALL_BANNERS: Record<string, BannerConfig> = {
  ...RANK_BANNERS,
  ...Object.fromEntries(RATING_BANNERS.map((b) => [b.id, b])),
  ...Object.fromEntries(LC_BANNERS.map((b) => [b.id, b])),
};

export function getBannerConfig(bannerId?: string | null): BannerConfig {
  if (!bannerId) return ALL_BANNERS["rookie"];
  return ALL_BANNERS[bannerId.toLowerCase().trim()] ?? ALL_BANNERS["rookie"];
}

/** Maps a LeetCode rating to the correct LC banner id. */
export function ratingToLcBannerId(rating: number | null | undefined): string {
  const r = rating ?? 0;
  if (r >= 2100) return "lc-guardian";
  if (r >= 1850) return "lc-knight";
  return "lc-unrated";
}

/**
 * Resolves the effective banner for a member.
 * - Top-3 always get the rank banner (gold/silver/bronze) regardless of platform.
 * - For rank ≥ 4 on CODEFORCES → use their equipped banner.
 * - For rank ≥ 4 on LEETCODE  → derive from their rating automatically.
 */
export function getActiveBannerForMember(
  rank: number,
  equippedBannerId?: string | null,
  platform: "CODEFORCES" | "LEETCODE" = "CODEFORCES",
  rating?: number | null
): BannerConfig {
  if (rank === 1) return RANK_BANNERS["rank-gold"];
  if (rank === 2) return RANK_BANNERS["rank-silver"];
  if (rank === 3) return RANK_BANNERS["rank-bronze"];
  if (platform === "LEETCODE") return getBannerConfig(ratingToLcBannerId(rating));
  return getBannerConfig(equippedBannerId);
}

export function isBannerUnlocked(
  banner: BannerConfig,
  currentOrMaxRating: number | null | undefined,
  member?: { isPlatformCreator?: boolean } | null
): boolean {
  if (banner.isRankBanner) return false; // Rank banners cannot be equipped manually
  if (banner.id === "creator-vip") {
    return isWebsiteCreator(member);
  }
  const rating = currentOrMaxRating ?? 0;
  return rating >= banner.minRating;
}

export function getRarityBadgeStyle(
  rarity: BannerRarity,
  bannerId?: string
): {
  bg: string;
  text: string;
  border: string;
} {
  if (bannerId === "creator-vip") {
    return {
      bg: "bg-rose-500/20",
      text: "text-rose-300 font-black tracking-wide",
      border: "border-rose-500/50 shadow-[0_0_12px_rgba(244,63,94,0.3)]",
    };
  }

  switch (rarity) {
    case "Exclusive":
      return {
        bg: "bg-amber-400/15",
        text: "text-amber-300",
        border: "border-amber-400/40",
      };
    case "Legendary":
      return {
        bg: "bg-orange-500/15",
        text: "text-orange-400",
        border: "border-orange-500/40",
      };
    case "Epic":
      return {
        bg: "bg-purple-500/15",
        text: "text-purple-300",
        border: "border-purple-500/40",
      };
    case "Rare":
      return {
        bg: "bg-cyan-500/15",
        text: "text-cyan-300",
        border: "border-cyan-500/40",
      };
    case "Special":
      return {
        bg: "bg-pink-500/15",
        text: "text-pink-300",
        border: "border-pink-500/40",
      };
    case "Common":
    default:
      return {
        bg: "bg-slate-500/15",
        text: "text-slate-300",
        border: "border-slate-500/30",
      };
  }
}

