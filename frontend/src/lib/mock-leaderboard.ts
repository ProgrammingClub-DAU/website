import type { LeaderboardEntry, LeaderboardFilter, LeaderboardPlatform } from "@/types/api";

export const MOCK_LEADERBOARD_ENTRIES: LeaderboardEntry[] = [
  {
    id: 1,
    name: "Shane Christian",
    codeforcesHandle: "Raze07",
    rating: 1840,
    rank: 1,
    tier: "Candidate Master",
    clubRole: "CONVENOR",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: "rank-gold",
    activeBannerId: "rank-gold",
    maxRating: 1840,
    contestHistory: [1620, 1650, 1690, 1720, 1760, 1740, 1790, 1820, 1810, 1840],
    ratingChange: 30,
  },
  {
    id: 2,
    name: "Pratik Mistry",
    codeforcesHandle: "Pratik_10072008",
    rating: 1707,
    rank: 2,
    tier: "Expert",
    clubRole: "CORE",
    avatarUrl: null,
    equippedBannerId: "expert",
    rankBannerId: "rank-silver",
    activeBannerId: "rank-silver",
    maxRating: 1720,
    contestHistory: [1540, 1580, 1610, 1590, 1640, 1660, 1650, 1690, 1680, 1707],
    ratingChange: 27,
  },
  {
    id: 3,
    name: "Dev Gorasiya",
    codeforcesHandle: "Dev777",
    rating: 1662,
    rank: 3,
    tier: "Expert",
    clubRole: "CORE",
    avatarUrl: null,
    equippedBannerId: "expert",
    rankBannerId: "rank-bronze",
    activeBannerId: "rank-bronze",
    maxRating: 1685,
    contestHistory: [1490, 1530, 1550, 1580, 1620, 1600, 1640, 1630, 1650, 1662],
    ratingChange: 12,
  },
  {
    id: 4,
    name: "Tanishq Shah",
    codeforcesHandle: "King-T",
    rating: 1643,
    rank: 4,
    tier: "Expert",
    clubRole: "ASSOCIATE_CORE",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: null,
    activeBannerId: "creator-vip",
    maxRating: 1670,
    contestHistory: [1450, 1500, 1540, 1520, 1570, 1610, 1600, 1630, 1620, 1643],
    ratingChange: 23,
  },
  {
    id: 5,
    name: "Krish Patel",
    codeforcesHandle: "krish_patel20",
    rating: 1639,
    rank: 5,
    tier: "Expert",
    clubRole: "BATCH_REPRESENTATIVE",
    avatarUrl: null,
    equippedBannerId: "expert",
    rankBannerId: null,
    activeBannerId: "expert",
    maxRating: 1650,
    contestHistory: [1480, 1510, 1560, 1550, 1590, 1620, 1605, 1625, 1610, 1639],
    ratingChange: 29,
  },
  {
    id: 6,
    name: "Raj Patel",
    codeforcesHandle: "raj_patel",
    rating: 1588,
    rank: 6,
    tier: "Specialist",
    clubRole: "CORE",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: null,
    activeBannerId: "creator-vip",
    isPlatformCreator: true,
    maxRating: 1605,
    contestHistory: [1420, 1460, 1490, 1520, 1550, 1530, 1570, 1560, 1595, 1588],
    ratingChange: -7,
  },
  {
    id: 7,
    name: "Madhav Thesiya",
    codeforcesHandle: "madhav_t",
    rating: 1540,
    rank: 7,
    tier: "Specialist",
    clubRole: "CORE",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: null,
    activeBannerId: "creator-vip",
    isPlatformCreator: true,
    maxRating: 1560,
    contestHistory: [1380, 1410, 1440, 1470, 1500, 1490, 1520, 1510, 1530, 1540],
    ratingChange: 10,
  },
  {
    id: 8,
    name: "Gaurav Rathod",
    codeforcesHandle: "gaurav_r",
    rating: 1495,
    rank: 8,
    tier: "Specialist",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: null,
    activeBannerId: "creator-vip",
    isPlatformCreator: true,
    maxRating: 1510,
    contestHistory: [1320, 1360, 1390, 1420, 1450, 1440, 1470, 1460, 1480, 1495],
    ratingChange: 15,
  },
  {
    id: 9,
    name: "Mahek Kanani",
    codeforcesHandle: "mahek_k",
    rating: 1430,
    rank: 9,
    tier: "Specialist",
    clubRole: "CORE",
    avatarUrl: null,
    equippedBannerId: "creator-vip",
    rankBannerId: null,
    activeBannerId: "creator-vip",
    isPlatformCreator: true,
    maxRating: 1450,
    contestHistory: [1280, 1310, 1340, 1370, 1400, 1390, 1410, 1400, 1420, 1430],
    ratingChange: 10,
  },
  {
    id: 10,
    name: "Aarav Shah",
    codeforcesHandle: "aarav_cp",
    rating: 1380,
    rank: 10,
    tier: "Pupil",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "pupil",
    rankBannerId: null,
    activeBannerId: "pupil",
    maxRating: 1410,
    contestHistory: [1220, 1250, 1280, 1310, 1340, 1330, 1360, 1350, 1370, 1380],
    ratingChange: 10,
  },
  {
    id: 11,
    name: "Diya Joshi",
    codeforcesHandle: "diya_codes",
    rating: 1320,
    rank: 11,
    tier: "Pupil",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "pupil",
    rankBannerId: null,
    activeBannerId: "pupil",
    maxRating: 1340,
    contestHistory: [1180, 1210, 1240, 1260, 1290, 1280, 1300, 1290, 1310, 1320],
    ratingChange: 10,
  },
  {
    id: 12,
    name: "Harsh Mehta",
    codeforcesHandle: "harsh_m",
    rating: 1260,
    rank: 12,
    tier: "Pupil",
    clubRole: "BATCH_REPRESENTATIVE",
    avatarUrl: null,
    equippedBannerId: "pupil",
    rankBannerId: null,
    activeBannerId: "pupil",
    maxRating: 1280,
    contestHistory: [1140, 1170, 1190, 1210, 1230, 1220, 1240, 1230, 1250, 1260],
    ratingChange: 10,
  },
  {
    id: 13,
    name: "Riya Patel",
    codeforcesHandle: "riya_p",
    rating: 1190,
    rank: 13,
    tier: "Newbie",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "rookie",
    rankBannerId: null,
    activeBannerId: "rookie",
    maxRating: 1190,
    contestHistory: [1050, 1080, 1100, 1120, 1140, 1130, 1160, 1150, 1170, 1190],
    ratingChange: 20,
  },
  {
    id: 14,
    name: "Yash Sharma",
    codeforcesHandle: "yash_s",
    rating: 1120,
    rank: 14,
    tier: "Newbie",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "rookie",
    rankBannerId: null,
    activeBannerId: "rookie",
    maxRating: 1140,
    contestHistory: [1020, 1040, 1060, 1080, 1090, 1080, 1100, 1090, 1110, 1120],
    ratingChange: 10,
  },
  {
    id: 15,
    name: "Meet Trivedi",
    codeforcesHandle: "meet_t",
    rating: 1050,
    rank: 15,
    tier: "Newbie",
    clubRole: "STUDENT",
    avatarUrl: null,
    equippedBannerId: "rookie",
    rankBannerId: null,
    activeBannerId: "rookie",
    maxRating: 1060,
    contestHistory: [950, 970, 990, 1010, 1020, 1010, 1030, 1020, 1040, 1050],
    ratingChange: 10,
  },
];

export function getMockLeaderboard(
  platform: LeaderboardPlatform = "CODEFORCES",
  filter: LeaderboardFilter = "ALL"
): LeaderboardEntry[] {
  let list = [...MOCK_LEADERBOARD_ENTRIES];

  // Adjust for LeetCode platform if selected
  if (platform === "LEETCODE") {
    list = list.map((item, idx) => {
      const lcRating = Math.round(item.rating! * 1.15 + (idx % 2 === 0 ? 40 : -30));
      return {
        ...item,
        rating: lcRating,
        maxRating: lcRating + 20,
        tier: lcRating >= 2100 ? "Guardian" : lcRating >= 1850 ? "Knight" : "Unrated",
        contestHistory: item.contestHistory?.map((h) => Math.round(h * 1.15)) ?? [],
      };
    });
  }

  // Filter by role
  if (filter === "CORE") {
    list = list.filter(
      (m) =>
        m.clubRole === "CONVENOR" ||
        m.clubRole === "DEPUTY_CONVENOR" ||
        m.clubRole === "CORE" ||
        m.clubRole === "ASSOCIATE_CORE"
    );
  } else if (filter === "BATCH_REP") {
    list = list.filter((m) => m.clubRole === "BATCH_REPRESENTATIVE");
  } else if (filter === "STUDENTS") {
    list = list.filter((m) => m.clubRole === "STUDENT" || m.clubRole === null);
  }

  // Recalculate ranks and rank banners after filtering
  list.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
  return list.map((item, idx) => {
    const rank = idx + 1;
    const rankBanner =
      rank === 1 ? "rank-gold" : rank === 2 ? "rank-silver" : rank === 3 ? "rank-bronze" : null;
    return {
      ...item,
      rank,
      rankBannerId: rankBanner,
      activeBannerId: rankBanner ?? item.equippedBannerId ?? "rookie",
    };
  });
}
