"use client";

/**
 * Profile Dashboard Page (Phase 2 Stage 2C)
 *
 * This client component renders:
 * - Profile header with avatar (Cloudinary upload support), name, club role badge, CF handle, and platform links
 * - Full owner-only Edit Profile panel (Name, Phone [required], CF, LeetCode, CodeChef, AtCoder, GitHub, LinkedIn)
 * - Contest rating graphs: Codeforces rating history + LeetCode snapshot rating history
 * - Account details with masked phone for visitors
 * - Club Activity & Event Performance summary
 */

import { useState, useMemo, useEffect, useCallback } from "react";
import Script from "next/script";
import Image from "next/image";
import { RatingGraph, type RatingPoint } from "@/components/site/rating-graph";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { ProfileLinksCard } from "@/components/site/profile-links-card";
import { dashboardService, type ProfileUpdateRequest } from "@/lib/services/dashboard";
import { PROFILE_PLATFORMS, profileUrl, usernameFrom } from "@/lib/platform-profiles";
import { snapshotService, type SnapshotEntry } from "@/lib/services/snapshots";
import { useAuthStore } from "@/store/auth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import { ACADEMIC_YEAR_LABELS, type Profile, type EventParticipation, type AcademicYear } from "@/types/api";
import {
  User,
  Trophy,
  Code,
  Award,
  Zap,
  Lock,
  Edit2,
  Camera,
  Phone,
  Mail,
  AlertCircle,
  CheckCircle2,
  Loader2,
} from "lucide-react";
import { codeforcesService, type CfUserInfo } from "@/lib/services/codeforces";
import type { RatingHistoryEntry as CfRatingHistoryEntry } from "@/types/api";


declare global {
  interface Window {
    cloudinary?: {
      createUploadWidget: (
        options: Record<string, unknown>,
        callback: (error: unknown, result: { event: string; info: { secure_url: string } }) => void
      ) => { open: () => void };
    };
  }
}

// ── Generate achievements from event participations ──
/**
 * Builds a complete profile payload.
 *
 * PUT /api/users/profile replaces every field it receives, and a field that is
 * absent is stored as empty. Sending only the avatar therefore wiped the phone
 * number and all four platform links, and saving the edit form wiped the avatar.
 * Every call sends the whole profile, with only the edited fields overridden.
 */
function profilePayload(profile: Profile, overrides: Partial<ProfileUpdateRequest>): ProfileUpdateRequest {
  return {
    name: profile.name,
    phoneNumber: profile.phoneNumber,
    codeforcesHandle: profile.codeforcesHandle,
    leetcodeHandle: profile.leetcodeHandle,
    codechefUrl: profile.codechefUrl,
    atcoderUrl: profile.atcoderUrl,
    githubUrl: profile.githubUrl,
    linkedinUrl: profile.linkedinUrl,
    avatarUrl: profile.avatarUrl,
    academicYear: profile.academicYear,
    ...overrides,
  };
}

/** Edit-form values for a member, used when the panel is opened. */
function formFromProfile(profile: Profile) {
  return {
    name: profile.name || "",
    phoneNumber: profile.phoneNumber || "",
    codeforcesHandle: profile.codeforcesHandle || "",
    leetcodeHandle: profile.leetcodeHandle || "",
    codechef: usernameFrom("codechef", profile.codechefUrl),
    atcoder: usernameFrom("atcoder", profile.atcoderUrl),
    github: usernameFrom("github", profile.githubUrl),
    linkedin: usernameFrom("linkedin", profile.linkedinUrl),
    academicYear: (profile.academicYear ?? "") as AcademicYear | "",
  };
}

function generateAchievements(events: EventParticipation[]): { icon: string; label: string }[] {
  const achievements: { icon: string; label: string }[] = [];

  events.forEach((e) => {
    if (e.achievement) {
      achievements.push({
        icon: e.achievement.split(" ")[0],
        label: `${e.eventName} — ${e.achievement.substring(e.achievement.indexOf(" ") + 1)}`,
      });
    }
  });

  const totalEvents = events.length;
  if (totalEvents >= 10) achievements.push({ icon: "🎯", label: "Participated in 10+ Club Events" });
  else if (totalEvents >= 5) achievements.push({ icon: "🎯", label: "Participated in 5+ Club Events" });

  const contests = events.filter(
    (e) => e.eventType === "Contest" || e.eventType === "Flagship" || e.eventType === "ICPC"
  );
  const top3Count = contests.filter((e) => e.rank !== null && e.rank <= 3).length;
  if (top3Count >= 3) achievements.push({ icon: "🏆", label: "Top 3 Finisher — 3+ Contests" });

  return achievements;
}

export default function ProfileDashboard({ userId }: { userId: string }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [cfInfo, setCfInfo] = useState<CfUserInfo | null>(null);
  const [cfHistory, setCfHistory] = useState<CfRatingHistoryEntry[]>([]);
  const [lcHistory, setLcHistory] = useState<SnapshotEntry[]>([]);
  const [lcLoading, setLcLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const { user, isAuthenticated } = useAuthStore();

  const isOwner = isAuthenticated && String(user?.id) === userId;

  const loadProfile = useCallback(async () => {
    try {
      const data = isOwner
        ? await dashboardService.getProfile(userId)
        : await dashboardService.getUserProfileById(userId);
      setProfile(data);

      if (data.codeforcesHandle) {
        try {
          const [info, history] = await Promise.all([
            codeforcesService.getUserInfo(data.codeforcesHandle),
            codeforcesService.getRatingHistory(data.codeforcesHandle),
          ]);
          setCfInfo(info);
          setCfHistory(history);
        } catch (err) {
          console.error("Failed to load Codeforces data:", err);
        }
      }

      if (isAuthenticated && data.id) {
        setLcLoading(true);
        try {
          const snapshots = await snapshotService.getLeetCodeSnapshots(data.id);
          setLcHistory(snapshots);
        } catch (err) {
          console.error("Failed to load LeetCode snapshots:", err);
        } finally {
          setLcLoading(false);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [userId, isOwner, isAuthenticated]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
    loadProfile();
  }, [loadProfile]);

  if (loading) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center font-mono text-sm tracking-wider text-fg-muted uppercase animate-pulse">
        <Loader2 className="mb-2 size-6 animate-spin text-primary" />
        Loading profile data...
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center font-mono text-sm tracking-wider text-fg-muted uppercase">
        Profile not found.
      </div>
    );
  }

  return (
    <>
      <Script src="https://upload-widget.cloudinary.com/global/all.js" strategy="lazyOnload" />
      <ProfileDashboardContent
        profile={profile}
        cfInfo={cfInfo}
        cfHistory={cfHistory}
        lcHistory={lcHistory}
        lcLoading={lcLoading}
        onUpdate={loadProfile}
        isOwner={isOwner}
      />
    </>
  );
}

function ProfileDashboardContent({
  profile,
  cfInfo,
  cfHistory,
  lcHistory,
  lcLoading,
  onUpdate,
  isOwner,
}: {
  profile: Profile;
  cfInfo: CfUserInfo | null;
  cfHistory: CfRatingHistoryEntry[];
  lcHistory: SnapshotEntry[];
  lcLoading: boolean;
  onUpdate: () => void;
  isOwner: boolean;
}) {
  const { isAuthenticated } = useAuthStore();
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    name: profile.name || "",
    phoneNumber: profile.phoneNumber || "",
    codeforcesHandle: profile.codeforcesHandle || "",
    leetcodeHandle: profile.leetcodeHandle || "",
    codechef: usernameFrom("codechef", profile.codechefUrl),
    atcoder: usernameFrom("atcoder", profile.atcoderUrl),
    github: usernameFrom("github", profile.githubUrl),
    linkedin: usernameFrom("linkedin", profile.linkedinUrl),
    academicYear: (profile.academicYear ?? "") as AcademicYear | "",
  });

  const openEditor = () => {
    setFormData(formFromProfile(profile));
    setSaveError(null);
    setIsEditingProfile(true);
  };



  const handleOpenCloudinary = () => {
    const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || "stdcydx1";
    const uploadPreset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "cpclub_unsigned";

    if (window.cloudinary) {
      const widget = window.cloudinary.createUploadWidget(
        {
          cloudName,
          uploadPreset,
          folder: "cpclub",
          maxFiles: 1,
          clientAllowedFormats: ["jpg", "png", "webp", "jpeg"],
          maxFileSize: 5000000,
        },
        async (error, result) => {
          if (!error && result && result.event === "success") {
            setIsUploadingAvatar(true);
            try {
              await dashboardService.updateProfile(
                profilePayload(profile, { avatarUrl: result.info.secure_url })
              );
              onUpdate();
            } catch (err) {
              console.error("Failed to save avatar URL:", err);
            } finally {
              setIsUploadingAvatar(false);
            }
          }
        }
      );
      widget.open();
    } else {
      const manualUrl = window.prompt("Enter direct image URL for your avatar:");
      if (manualUrl && manualUrl.trim()) {
        setIsUploadingAvatar(true);
        dashboardService
          .updateProfile(profilePayload(profile, { avatarUrl: manualUrl.trim() }))
          .then(() => onUpdate())
          .catch((err) => console.error("Failed to update avatar:", err))
          .finally(() => setIsUploadingAvatar(false));
      }
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaveError(null);

    if (!formData.name.trim()) {
      setSaveError("Name is required.");
      return;
    }
    if (!formData.phoneNumber.trim()) {
      setSaveError("Phone number is required.");
      return;
    }
    if (!formData.academicYear) {
      setSaveError("Select your year of study.");
      return;
    }

    setIsSaving(true);
    try {
      const payload: ProfileUpdateRequest = {
        name: formData.name.trim(),
        phoneNumber: formData.phoneNumber.trim(),
        codeforcesHandle: formData.codeforcesHandle.trim() || null,
        leetcodeHandle: formData.leetcodeHandle.trim() || null,
        // Members type a username; the column keeps the full profile link,
        // which is also what the attendance export writes out.
        codechefUrl: profileUrl("codechef", formData.codechef),
        atcoderUrl: profileUrl("atcoder", formData.atcoder),
        githubUrl: profileUrl("github", formData.github),
        linkedinUrl: profileUrl("linkedin", formData.linkedin),
        academicYear: formData.academicYear || null,
        // Carried through so that saving the form does not clear the avatar.
        avatarUrl: profile.avatarUrl,
      };
      await dashboardService.updateProfile(payload);
      setIsEditingProfile(false);
      onUpdate();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Failed to update profile. Please check your inputs.";
      setSaveError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const getCfRank = (rating: number): import("@/lib/cf-ranks").CfRankKey => {
    if (rating >= 2400) return "grandmaster";
    if (rating >= 2100) return "master";
    if (rating >= 1900) return "candidate";
    if (rating >= 1600) return "expert";
    if (rating >= 1400) return "specialist";
    if (rating >= 1200) return "pupil";
    return "newbie";
  };

  const currentRating = cfInfo?.rating ?? profile.rating;
  const actualCfRank = getCfRank(currentRating ?? 0);
  const nameColor = rankColor(actualCfRank);
  const rankName = CF_RANKS.find((r) => r.key === actualCfRank)?.name ?? actualCfRank;

  const eventParticipations = useMemo(() => profile.eventParticipations ?? [], [profile.eventParticipations]);
  const clubStats = useMemo(() => {
    const contests = eventParticipations.filter(
      (e) => e.eventType === "Contest" || e.eventType === "Flagship" || e.eventType === "ICPC"
    );
    const workshops = eventParticipations.filter((e) => e.eventType === "Workshop");
    const rankedEvents = eventParticipations.filter((e) => e.rank !== null);
    const bestRank = rankedEvents.length > 0 ? Math.min(...rankedEvents.map((e) => e.rank!)) : null;
    const achievements = generateAchievements(eventParticipations);
    return {
      totalEvents: eventParticipations.length,
      totalContests: contests.length,
      totalWorkshops: workshops.length,
      bestRank,
      achievementCount: achievements.length,
      achievements,
    };
  }, [eventParticipations]);

  const lcPoints: RatingPoint[] = useMemo(() => {
    return lcHistory.map((h) => ({
      date: h.date,
      rating: h.rating,
      contestName: "LeetCode Weekly Snapshot",
    }));
  }, [lcHistory]);

  /** The four fields the club needs, named the way the form names them. */
  const missingProfileFields = [
    !profile.name?.trim() && "name",
    !profile.codeforcesHandle?.trim() && "Codeforces handle",
    !profile.phoneNumber?.trim() && "phone number",
    !profile.academicYear && "year of study",
  ].filter(Boolean) as string[];

  const displayAvatar = profile.avatarUrl || cfInfo?.titlePhoto || cfInfo?.avatar;


  return (
    <div className="space-y-8">
      {/* ── Profile Header ── */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
            {/* Avatar with Cloudinary Upload Option */}
            <div className="relative group">
              <div
                className="flex size-24 shrink-0 items-center justify-center rounded-full border-2 bg-surface-2 overflow-hidden"
                style={{ borderColor: nameColor }}
              >
                {displayAvatar ? (
                  <Image
                    src={displayAvatar}
                    alt={profile.name}
                    width={96}
                    height={96}
                    className="size-full rounded-full object-cover"
                  />
                ) : (
                  <User className="size-10 text-fg-muted" />
                )}
              </div>
              {isOwner && (
                <button
                  onClick={handleOpenCloudinary}
                  disabled={isUploadingAvatar}
                  title="Upload profile photo via Cloudinary"
                  className="absolute bottom-0 right-0 flex size-8 items-center justify-center rounded-full border border-border bg-surface text-fg-muted shadow-md transition-all hover:bg-primary hover:text-primary-foreground"
                >
                  {isUploadingAvatar ? (
                    <Loader2 className="size-4 animate-spin" />
                  ) : (
                    <Camera className="size-4" />
                  )}
                </button>
              )}
            </div>

            {/* Name + handle + rank + club role */}
            <div className="flex-1 text-center sm:text-left">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <div>
                  <h2 className="text-2xl font-bold tracking-tight" style={{ color: nameColor }}>
                    {profile.name}
                  </h2>
                  <p className="mt-0.5 text-sm text-fg-muted">
                    {profile.codeforcesHandle ? `@${profile.codeforcesHandle}` : "No Codeforces handle linked"}
                    {profile.leetcodeHandle && (
                      <span className="ml-3 text-fg-subtle">
                        LC: <strong className="text-foreground">@{profile.leetcodeHandle}</strong>
                      </span>
                    )}
                  </p>
                </div>

                {isOwner && (
                  <button
                    onClick={() => (isEditingProfile ? setIsEditingProfile(false) : openEditor())}
                    className="inline-flex items-center self-center sm:self-start gap-1.5 rounded-full border border-border bg-surface-2 px-3 py-1.5 text-xs font-medium text-foreground transition-all hover:border-hairline-strong hover:bg-surface-3"
                  >
                    <Edit2 className="size-3.5" />
                    {isEditingProfile ? "Cancel Editing" : "Edit Profile"}
                  </button>
                )}
              </div>

              {/* Badges row */}
              <div className="mt-3 flex flex-wrap items-center justify-center gap-2.5 sm:justify-start">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full border px-3 py-0.5 text-xs font-medium capitalize"
                  style={{ color: nameColor, borderColor: nameColor }}
                >
                  <Trophy className="size-3" />
                  {rankName}
                </span>

                <ClubRoleBadge clubRole={profile.clubRole} />

                {profile.profileComplete ? (
                  <span
                    className="inline-flex items-center gap-1 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-400"
                    title="Name, Codeforces handle, phone number and year are all filled in"
                  >
                    <CheckCircle2 className="size-3" />
                    Profile complete
                  </span>
                ) : isOwner ? (
                  // Only the owner can fix it, so only the owner is told.
                  <button
                    type="button"
                    onClick={openEditor}
                    title={`Still missing: ${missingProfileFields.join(", ")}`}
                    className="inline-flex items-center gap-1 rounded-full border border-amber-500/40 bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-400 transition-colors hover:bg-amber-500/20"
                  >
                    <AlertCircle className="size-3" />
                    Finish your profile ({missingProfileFields.length} left)
                  </button>
                ) : null}

                {profile.batchYear && (
                  <span className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-2 px-2.5 py-0.5 text-xs font-medium text-fg-muted">
                    Batch {profile.batchYear}
                  </span>
                )}

                <span className="text-xs text-fg-muted">
                  CF Rating: <strong className="text-foreground">{currentRating ?? "Unrated"}</strong>
                  {cfInfo?.maxRating && currentRating !== cfInfo.maxRating && (
                    <span className="text-fg-subtle"> (max {cfInfo.maxRating})</span>
                  )}
                </span>
                {profile.leetcodeRating !== null && profile.leetcodeRating > 0 && (
                  <span className="text-xs text-fg-muted">
                    LeetCode: <strong className="text-foreground">{profile.leetcodeRating}</strong>
                  </span>
                )}
              </div>

            </div>

            {/* Quick stats cards */}
            <div className="grid grid-cols-2 gap-3 text-center">
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{profile.leetcodeRating ?? "—"}</div>
                <div className="text-label text-fg-muted">LeetCode</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{cfHistory.length}</div>
                <div className="text-label text-fg-muted">CF Contests</div>
              </div>
            </div>
          </div>

          {/* ── Edit Profile Form (Owner Only) ── */}
          {isOwner && isEditingProfile && (
            <div className="mt-6 border-t border-border pt-6">
              <h3 className="text-base font-semibold text-foreground mb-4 flex items-center gap-2">
                <Edit2 className="size-4 text-primary" />
                Edit Profile
              </h3>

              {saveError && (
                <div className="mb-4 flex items-center gap-2 rounded-panel border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-400">
                  <AlertCircle className="size-4 shrink-0" />
                  <span>{saveError}</span>
                </div>
              )}

              <form onSubmit={handleSaveProfile} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      Full Name <span className="text-red-400">*</span>
                    </label>
                    <input
                      type="text"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      Phone Number <span className="text-red-400">*</span>
                    </label>
                    <input
                      type="tel"
                      required
                      placeholder="+91 9876543210"
                      value={formData.phoneNumber}
                      onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      Year of study <span className="text-red-400">*</span>
                    </label>
                    <select
                      required
                      value={formData.academicYear}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          academicYear: e.target.value as AcademicYear | "",
                        })
                      }
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    >
                      <option value="">Select your year</option>
                      <option value="FIRST_YEAR">{ACADEMIC_YEAR_LABELS.FIRST_YEAR}</option>
                      <option value="SECOND_YEAR_ONWARDS">
                        {ACADEMIC_YEAR_LABELS.SECOND_YEAR_ONWARDS}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">Codeforces Handle</label>
                    <input
                      type="text"
                      placeholder="tourist"
                      value={formData.codeforcesHandle}
                      onChange={(e) => setFormData({ ...formData, codeforcesHandle: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">LeetCode Handle</label>
                    <input
                      type="text"
                      placeholder="leetcode_ninja"
                      value={formData.leetcodeHandle}
                      onChange={(e) => setFormData({ ...formData, leetcodeHandle: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      CodeChef username
                    </label>
                    <input
                      type="text"
                      placeholder={PROFILE_PLATFORMS.codechef.placeholder}
                      value={formData.codechef}
                      onChange={(e) => setFormData({ ...formData, codechef: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      AtCoder username
                    </label>
                    <input
                      type="text"
                      placeholder={PROFILE_PLATFORMS.atcoder.placeholder}
                      value={formData.atcoder}
                      onChange={(e) => setFormData({ ...formData, atcoder: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      GitHub username
                    </label>
                    <input
                      type="text"
                      placeholder={PROFILE_PLATFORMS.github.placeholder}
                      value={formData.github}
                      onChange={(e) => setFormData({ ...formData, github: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-fg-muted mb-1">
                      LinkedIn profile name
                    </label>
                    <input
                      type="text"
                      placeholder={PROFILE_PLATFORMS.linkedin.placeholder}
                      value={formData.linkedin}
                      onChange={(e) => setFormData({ ...formData, linkedin: e.target.value })}
                      className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsEditingProfile(false)}
                    className="rounded-control border border-border px-4 py-1.5 text-xs text-fg-muted hover:text-foreground"
                    disabled={isSaving}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSaving}
                    className="inline-flex items-center gap-1.5 rounded-control bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow-sm hover:bg-primary/90 disabled:opacity-50"
                  >
                    {isSaving && <Loader2 className="size-3 animate-spin" />}
                    Save Changes
                  </button>
                </div>
              </form>
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Club Stats Summary ── */}
      {/* Every platform this member is on, each capsule linking straight to
          the profile. Ratings come from the sync jobs where we have them. */}
      <ProfileLinksCard
        isOwner={isOwner}
        onAddClick={openEditor}
        links={[
          { platform: "codeforces", value: profile.codeforcesHandle, rating: currentRating },
          { platform: "leetcode", value: profile.leetcodeHandle, rating: profile.leetcodeRating },
          { platform: "codechef", value: profile.codechefUrl },
          { platform: "atcoder", value: profile.atcoderUrl },
          { platform: "github", value: profile.githubUrl },
          { platform: "linkedin", value: profile.linkedinUrl },
        ]}
      />

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Zap className="size-4 text-primary" />
            Club Stats
          </CardTitle>
        </CardHeader>
        <CardContent>
          {eventParticipations.length > 0 ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center">
                <div className="text-2xl font-bold">{clubStats.totalEvents}</div>
                <div className="mt-1 text-xs text-fg-muted">Events Participated</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center">
                <div className="text-2xl font-bold">{clubStats.totalContests}</div>
                <div className="mt-1 text-xs text-fg-muted">Contests</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center">
                <div className="text-2xl font-bold">{clubStats.totalWorkshops}</div>
                <div className="mt-1 text-xs text-fg-muted">Workshops</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center">
                <div className="text-2xl font-bold">{clubStats.bestRank !== null ? `#${clubStats.bestRank}` : "—"}</div>
                <div className="mt-1 text-xs text-fg-muted">Best Rank</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center">
                <div className="text-2xl font-bold">{clubStats.achievementCount}</div>
                <div className="mt-1 text-xs text-fg-muted">Achievements</div>
              </div>
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-fg-muted">
              No club activities recorded yet.
            </p>
          )}
        </CardContent>
      </Card>

      {/* ── Achievements ── */}
      {clubStats.achievements.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Award className="size-4 text-amber-400" />
              Achievements
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-3 sm:grid-cols-2">
              {clubStats.achievements.map((a, i) => (
                <div
                  key={i}
                  className="flex items-center gap-3 rounded-panel border border-border bg-surface-2 px-4 py-3"
                >
                  <span className="text-xl">{a.icon}</span>
                  <span className="text-sm font-medium">{a.label}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* ── Codeforces Contest Rating History ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Trophy className="size-4 text-primary" />
            Codeforces Contest Rating History
          </CardTitle>
        </CardHeader>
        <CardContent>
          {cfHistory.length > 0 ? (
            <RatingGraph data={cfHistory} />
          ) : (
            <div className="flex h-[240px] flex-col items-center justify-center gap-2 text-sm text-fg-muted">
              <Trophy className="size-8 opacity-20" />
              {profile.codeforcesHandle
                ? "No contest history found on Codeforces."
                : "No Codeforces handle linked."}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── LeetCode Rating History (Snapshots) ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Code className="size-4 text-amber-500" />
            LeetCode Rating History (Weekly Snapshots)
          </CardTitle>
        </CardHeader>
        <CardContent>
          {!isAuthenticated ? (
            <div className="flex h-[200px] flex-col items-center justify-center gap-2 text-sm text-fg-muted">
              <Lock className="size-6 text-fg-subtle" />
              <span>Sign in to see LeetCode rating history</span>
            </div>
          ) : lcLoading ? (
            <div className="flex h-[200px] items-center justify-center text-xs text-fg-muted">
              <Loader2 className="mr-2 size-4 animate-spin text-primary" />
              Loading LeetCode history...
            </div>
          ) : lcPoints.length >= 2 ? (
            <RatingGraph data={lcPoints} />
          ) : (
            <div className="flex h-[200px] flex-col items-center justify-center gap-2 text-sm text-fg-muted">
              <Code className="size-6 text-fg-subtle" />
              <span>
                {profile.leetcodeHandle
                  ? "Not enough weekly snapshot data yet. Snapshots record every Monday."
                  : "No LeetCode handle linked."}
              </span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Account Details ── */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {isOwner && (
            <>
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-fg-muted">
                  <Mail className="size-3.5" /> Email
                </span>
                <span className="font-mono text-xs">{profile.email}</span>
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-fg-muted">
                  <Phone className="size-3.5" /> Phone Number
                </span>
                <span>
                  {profile.phoneNumber || <span className="text-fg-subtle">Not provided</span>}
                </span>
              </div>
              <Separator />
            </>
          )}
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Codeforces Handle</span>
            <span style={{ color: nameColor }}>
              {profile.codeforcesHandle ? `@${profile.codeforcesHandle}` : "None"}
            </span>
          </div>
          <Separator />
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">LeetCode Handle</span>
            <span>{profile.leetcodeHandle ? `@${profile.leetcodeHandle}` : "None"}</span>
          </div>
          <Separator />
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Member Since</span>
            <span>
              {profile.createdAt
                ? new Date(profile.createdAt).toLocaleDateString("en-US", {
                    month: "long",
                    year: "numeric",
                  })
                : "—"}
            </span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
