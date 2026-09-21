export interface Match {
  id: string;
  startTime: string;
  durationMinutes: number;
  mode: "classic" | "replace";
  replaceIncrement: number;
  gridSize: number;
  teams: Team[];
  problems: ProblemCell[];
  solveLog: SolveEntry[];
  timeoutMinutes?: number | null;
  showRatings?: boolean;
}

export interface Team {
  name: string;
  color: string;
  members: string[];
}

export interface ProblemCell {
  row: number;
  col: number;
  contestId: number;
  index: string;
  name: string;
  rating: number;
  link: string;
  claimedBy?: string;
  solvedBy?: string;
  active?: boolean;
  position: number;
}

export interface SolveEntry {
  handle: string;
  problem: {
    contestId: number;
    index: string;
  };
  team: string;
  timestamp: string;
}
