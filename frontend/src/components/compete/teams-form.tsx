import { useState } from "react";

export interface TeamInput {
  name: string;
  color: string;
  members: string[];
}

interface TeamsFormProps {
  onTeamsChange: (teams: TeamInput[]) => void;
}

export function TeamsForm({ onTeamsChange }: TeamsFormProps) {
  type ColorOption = "red" | "blue" | "green" | "purple" | "orange" | "pink" | "yellow" | "teal";

  const COLOR_OPTIONS: ColorOption[] = [
    "red", "blue", "green", "purple", "orange", "pink", "yellow", "teal"
  ];

  const COLOR_CLASSES: Record<ColorOption, { bg: string; text: string }> = {
    red: { bg: "bg-red-500/20", text: "text-red-300" },
    blue: { bg: "bg-blue-500/20", text: "text-blue-300" },
    green: { bg: "bg-green-500/20", text: "text-green-300" },
    purple: { bg: "bg-purple-500/20", text: "text-purple-300" },
    orange: { bg: "bg-orange-500/20", text: "text-orange-300" },
    pink: { bg: "bg-pink-500/20", text: "text-pink-300" },
    yellow: { bg: "bg-yellow-500/20", text: "text-yellow-300" },
    teal: { bg: "bg-teal-500/20", text: "text-teal-300" },
  };

  const [teams, setTeams] = useState<TeamInput[]>([
    { name: "", color: "", members: [""] },
  ]);

  const removeTeam = (index: number) => {
    const newTeams = [...teams];
    newTeams.splice(index, 1);
    updateTeams(newTeams);
  };

  const updateTeam = <K extends keyof TeamInput>(
    index: number,
    key: K,
    value: TeamInput[K]
  ) => {
    const newTeams = [...teams];
    newTeams[index][key] = value;
    updateTeams(newTeams);
  };

  const removeMember = (teamIndex: number, memberIndex: number) => {
    const newTeams = [...teams];
    newTeams[teamIndex].members.splice(memberIndex, 1);
    updateTeams(newTeams);
  };

  const updateTeams = (updated: TeamInput[]) => {
    setTeams(updated);
    onTeamsChange(updated);
  };

  const addTeam = () => {
    if (teams.length >= 16) {
      alert("You can't add more than 16 teams.");
      return;
    }
    const newTeams = [...teams, { name: "", color: "", members: [""] }];
    updateTeams(newTeams);
  };

  const updateMember = (teamIndex: number, memberIndex: number, value: string) => {
    const newTeams = [...teams];
    newTeams[teamIndex].members[memberIndex] = value;
    updateTeams(newTeams);
  };

  const addMember = (teamIndex: number) => {
    const newTeams = [...teams];
    if (newTeams[teamIndex].members.length < 16) {
      newTeams[teamIndex].members.push("");
      updateTeams(newTeams);
    }
  };

  return (
    <div className="space-y-4">
      {teams.map((team, i) => (
        <div key={i} className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 mb-4">
          <div className="flex justify-between items-center mb-4">
            <h4 className="font-semibold text-white">Team {i + 1}</h4>
            <button
              type="button"
              onClick={() => removeTeam(i)}
              className="text-red-400 hover:text-red-300 text-sm font-medium transition-colors"
            >
              Remove
            </button>
          </div>

          <input
            type="text"
            className="flex h-10 w-full rounded-md border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder:text-white/50 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-transparent mb-3"
            placeholder="Team name"
            value={team.name}
            onChange={(e) => updateTeam(i, "name", e.target.value)}
          />

          <select
            className="flex h-10 w-full rounded-md border border-white/10 bg-[#1a1d24] px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-transparent mb-4"
            value={team.color}
            onChange={(e) => updateTeam(i, "color", e.target.value)}
          >
            <option value="" className="bg-[#1a1d24] text-white/50">Select Team Color</option>
            {COLOR_OPTIONS.filter((c) => 
              !teams.some((t, idx) => t.color === c && idx !== i)
            ).map((color) => (
              <option
                key={color}
                value={color}
                className={`${COLOR_CLASSES[color].bg} ${COLOR_CLASSES[color].text}`}
              >
                {color.charAt(0).toUpperCase() + color.slice(1)}
              </option>
            ))}
          </select>
          
          <label className="block text-sm font-medium text-white/80 mb-2">
            Codeforces Handles (case-sensitive)
          </label>

          <div className="space-y-2 mb-3">
            {team.members.map((member, j) => (
              <div key={j} className="flex gap-2">
                <input
                  type="text"
                  className="flex h-10 flex-1 rounded-md border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder:text-white/50 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-transparent"
                  placeholder={`Handle ${j + 1}`}
                  value={member}
                  onChange={(e) => updateMember(i, j, e.target.value)}
                />
                {team.members.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeMember(i, j)}
                    className="text-red-400 hover:text-red-300 text-sm font-medium px-2"
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>

          {team.members.length < 16 && (
            <button
              type="button"
              onClick={() => addMember(i)}
              className="text-blue-400 hover:text-blue-300 text-sm font-medium transition-colors"
            >
              + Add Member
            </button>
          )}
        </div>
      ))}

      {teams.length < 8 && (
        <button
          type="button"
          onClick={addTeam}
          className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-white hover:bg-white/10 hover:border-white/20 transition-all shadow-sm"
        >
          + Add Team
        </button>
      )}
    </div>
  );
}
