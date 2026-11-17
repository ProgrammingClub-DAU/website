"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { dashboardService } from "@/lib/services/dashboard";
import { useAuthStore } from "@/store/auth";
import type { Profile } from "@/types/api";

type SetupFields = Pick<
  Profile,
  "codeforcesHandle" | "leetcodeHandle" | "academicYear" | "codeforcesHandleInvalid" | "leetcodeHandleInvalid"
>;

/**
 * What stops a member from showing up properly, in their own words.
 *
 * These used to fail silently: a handle that does not exist gave no rating, solves
 * or ranklist row, and a missing year left the member out of the year-wise prizes,
 * with nothing on screen to say why.
 */
export function setupIssues(p: SetupFields): string[] {
  const issues: string[] = [];
  if (!p.codeforcesHandle?.trim()) {
    issues.push("Add your Codeforces handle to appear on the leaderboards and contest ranklists.");
  } else if (p.codeforcesHandleInvalid) {
    issues.push(
      `Codeforces can't find the handle "${p.codeforcesHandle}". Check the spelling, or update it if you renamed your account.`
    );
  }
  if (p.leetcodeHandle?.trim() && p.leetcodeHandleInvalid) {
    issues.push(`LeetCode can't find the username "${p.leetcodeHandle}". Check the spelling.`);
  }
  if (!p.academicYear) {
    issues.push("Set your academic year. Without it you are not counted for the 1st-year or 2nd-year-onwards prizes.");
  }
  return issues;
}

interface ProfileSetupNoticeProps {
  profile: SetupFields;
  /** Opens the profile editor in place; without it the notice links to the profile page. */
  onFix?: () => void;
  className?: string;
}

/** The notice itself, for a profile already loaded. Renders nothing when all is well. */
export function ProfileSetupNotice({ profile, onFix, className }: ProfileSetupNoticeProps) {
  const issues = setupIssues(profile);
  if (issues.length === 0) return null;

  return (
    <div
      role="status"
      className={`flex flex-col gap-3 rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-100 sm:flex-row sm:items-start sm:justify-between ${className ?? ""}`}
    >
      <div className="flex gap-2.5">
        <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-400" />
        <div>
          <p className="font-semibold text-amber-200">Your profile needs attention</p>
          <ul className="mt-1 list-disc space-y-0.5 pl-4 text-xs text-amber-100/90">
            {issues.map((issue) => (
              <li key={issue}>{issue}</li>
            ))}
          </ul>
        </div>
      </div>
      {onFix ? (
        <button
          type="button"
          onClick={onFix}
          className="shrink-0 self-start rounded-lg border border-amber-400/50 px-3 py-1.5 text-xs font-semibold text-amber-100 hover:bg-amber-500/20"
        >
          Fix it now
        </button>
      ) : (
        <Link
          href="/profile"
          className="shrink-0 self-start rounded-lg border border-amber-400/50 px-3 py-1.5 text-xs font-semibold text-amber-100 hover:bg-amber-500/20"
        >
          Go to my profile
        </Link>
      )}
    </div>
  );
}

/**
 * The notice for the signed-in member, wherever they are. Loads their own profile;
 * shows nothing when signed out, while loading, or when there is nothing to fix.
 */
export function MyProfileSetupNotice({ className }: { className?: string }) {
  const { isAuthenticated } = useAuthStore();
  const [profile, setProfile] = useState<Profile | null>(null);

  useEffect(() => {
    if (!isAuthenticated) return;
    let ignore = false;
    dashboardService
      .getProfile()
      .then((p) => {
        if (!ignore) setProfile(p);
      })
      .catch(() => {
        // A notice is a nicety; a failed load just means no notice.
      });
    return () => {
      ignore = true;
    };
  }, [isAuthenticated]);

  if (!isAuthenticated || !profile) return null;
  return <ProfileSetupNotice profile={profile} className={className} />;
}
