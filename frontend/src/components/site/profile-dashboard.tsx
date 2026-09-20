"use client";

/**
 * Profile Dashboard Page (Phase 2 Stage 2C)
 *
 * This client component renders:
 * - Profile header with avatar (Cloudinary upload support), name, club role badge, CF handle, and platform links
 * - Full owner-only Edit Profile panel (Name, Phone [required], CF, LeetCode, CodeChef, AtCoder, GitHub, LinkedIn)
 * - Contest rating graphs: Codeforces rating history + LeetCode snapshot rating history
 * - Account details with masked phone for visitors
 */

import { useState, useEffect, useCallback } from "react";
import Script from "next/script";
import Image from "next/image";
import { RatingGraph, type RatingPoint } from "@/components/site/rating-graph";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { ProfileLinksCard } from "@/components/site/profile-links-card";
import { InDevelopment } from "@/components/site/in-development";
import { dashboardService, type ProfileUpdateRequest } from "@/lib/services/dashboard";
import { PROFILE_PLATFORMS, profileUrl, usernameFrom } from "@/lib/platform-profiles";
import { leetcodeService } from "@/lib/services/leetcode";
import { leaderboardService } from "@/lib/services/leaderboard";
import {

  getBannerConfig,


} from "@/lib/banner-config";
import { BannerGraphic } from "@/components/site/leaderboard/banner-graphic";
import { BannerLockerDrawer } from "@/components/site/leaderboard/banner-locker-drawer";
import { useAuthStore } from "@/store/auth";
import { openUploadWidget } from "@/lib/cloudinary";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import { ACADEMIC_YEAR_LABELS, type Profile, type AcademicYear } from "@/types/api";
import {
  User,
  Trophy,
  Code,
  Edit2,
  Camera,
  Phone,
  Mail,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Sparkles,
} from "lucide-react";
import { codeforcesService, type CfUserInfo } from "@/lib/services/codeforces";
import type { RatingHistoryEntry as CfRatingHistoryEntry } from "@/types/api";


// window.cloudinary is declared once, in lib/cloudinary.ts.

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
    bannerId: profile.equippedBannerId || "rookie",
  };
}

export default function ProfileDashboard({ userId }: { userId: string }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [cfInfo, setCfInfo] = useState<CfUserInfo | null>(null);
  const [cfHistory, setCfHistory] = useState<CfRatingHistoryEntry[]>([]);
  const [lcHistory, setLcHistory] = useState<CfRatingHistoryEntry[]>([]);
  const [lcLoading, setLcLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const { user, isAuthenticated } = useAuthStore();

  const isOwner = isAuthenticated && String(user?.id) === userId;

  const loadProfile = useCallback(async () => {
    try {
      const data = isOwner
        ? await dashboardService.getProfile()
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

      // The member's real contest history, straight from LeetCode, for every
      // visitor. It used to come from the site's weekly snapshots, which needed
      // two Mondays of data and a signed-in viewer before anything showed.
      if (data.leetcodeHandle) {
        setLcLoading(true);
        try {
          setLcHistory(await leetcodeService.getContestHistory(data.leetcodeHandle));
        } finally {
          setLcLoading(false);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [userId, isOwner]);

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
  lcHistory: CfRatingHistoryEntry[];
  lcLoading: boolean;
  onUpdate: () => void;
  isOwner: boolean;
}) {
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isBannerLockerOpen, setIsBannerLockerOpen] = useState(false);

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
    bannerId: profile.equippedBannerId || "rookie",
  });

  const openEditor = () => {
    setFormData(formFromProfile(profile));
    setSaveError(null);
    setIsEditingProfile(true);
  };



  /** Saves a new avatar URL, whether it came from an upload or was pasted. */
  const saveAvatar = (avatarUrl: string) => {
    setIsUploadingAvatar(true);
    dashboardService
      .updateProfile(profilePayload(profile, { avatarUrl }))
      .then(() => onUpdate())
      .catch((err) => console.error("Failed to save avatar URL:", err))
      .finally(() => setIsUploadingAvatar(false));
  };

  const handleOpenCloudinary = () => {
    const opened = openUploadWidget({ folder: "cpclub/avatars", onUpload: saveAvatar });
    if (!opened) {
      const manualUrl = window.prompt("Photo upload is unavailable. Paste an image URL for your avatar:");
      if (manualUrl && manualUrl.trim()) saveAvatar(manualUrl.trim());
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
      if (formData.bannerId !== profile.equippedBannerId) {
        await leaderboardService.equipBanner(formData.bannerId);
      }
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

  const lcPoints: RatingPoint[] = lcHistory;

  /** Posts the club publishes a contact number for. */
  const isOfficeBearer =
    profile.clubRole !== null &&
    ["CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE", "BATCH_REPRESENTATIVE"].includes(
      profile.clubRole,
    );

  /** The four fields the club needs, named the way the form names them. */
  const missingProfileFields = [
    !profile.name?.trim() && "name",
    !profile.codeforcesHandle?.trim() && "Codeforces handle",
    !profile.phoneNumber?.trim() && "phone number",
    !profile.academicYear && "year of study",
  ].filter(Boolean) as string[];

  const displayAvatar = profile.avatarUrl || cfInfo?.titlePhoto || cfInfo?.avatar;
  const equippedBanner = getBannerConfig(profile.equippedBannerId || "rookie");
  const selectedBanner = getBannerConfig(formData.bannerId);
  const effectiveMaxRating = profile.maxRating ?? profile.rating ?? 0;

  const leaderboardMember = {
    id: profile.id,
    name: profile.name,
    codeforcesHandle: profile.codeforcesHandle,
    rating: profile.rating ?? 0,
    maxRating: profile.maxRating ?? profile.rating ?? 0,
    equippedBannerId: profile.equippedBannerId || "rookie",
    isPlatformCreator: profile.isPlatformCreator,
    avatarUrl: displayAvatar ?? null,
    rank: 0,
    tier: rankName,
    clubRole: profile.clubRole,
  };

  return (
    <div className="space-y-8">
      {/* ── Profile Header with Active Banner Showcase ── */}
      <Card
        className="relative overflow-hidden transition-colors shadow-lg"
        style={{ borderColor: equippedBanner.colors.border }}
      >
        {/* Colorful Banner Graphic Header */}
        <div className="relative h-32 sm:h-44 w-full overflow-hidden">
          <BannerGraphic banner={equippedBanner} showEffects={true} withScrim={true} />
          <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/40 to-transparent" />

          {/* Active Banner Badge Tag */}
          <div className="absolute top-3 right-3 flex items-center gap-2 z-10">
            <span
              className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold backdrop-blur-md shadow-lg"
              style={{
                backgroundColor: `${equippedBanner.colors.primary}cc`,
                borderColor: equippedBanner.colors.border,
                color: equippedBanner.colors.text,
              }}
            >
              <Sparkles className="size-3.5" />
              {equippedBanner.name} Banner
            </span>
          </div>
        </div>

        <CardContent className="relative pt-0 pb-6 px-6">
          {/* Avatar + Actions Bar */}
          <div className="-mt-14 mb-4 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
            {/* Avatar with Cloudinary Upload Option */}
            <div className="relative group">
              <div
                className="flex size-24 shrink-0 items-center justify-center rounded-full border-4 bg-surface shadow-xl overflow-hidden"
                style={{ borderColor: equippedBanner.colors.border }}
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

            {/* Action Buttons (Owner Only) */}
            {isOwner && (
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsBannerLockerOpen(true)}
                  className="inline-flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-3.5 py-1.5 text-xs font-semibold text-amber-300 transition-all hover:bg-amber-500/20 shadow-xs"
                >
                  <Sparkles className="size-3.5 text-amber-400" />
                  Change Banner
                </button>

                <button
                  type="button"
                  onClick={() => (isEditingProfile ? setIsEditingProfile(false) : openEditor())}
                  className="inline-flex items-center gap-1.5 rounded-full border border-border bg-surface-2 px-3.5 py-1.5 text-xs font-medium text-foreground transition-all hover:border-hairline-strong hover:bg-surface-3 shadow-xs"
                >
                  <Edit2 className="size-3.5" />
                  {isEditingProfile ? "Cancel Editing" : "Edit Profile"}
                </button>
              </div>
            )}
          </div>

          {/* Name + handle + rank + club role */}
          <div className="space-y-3">
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

            {/* Badges row */}
            <div className="flex flex-wrap items-center gap-2.5">
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

                <div className="rounded-panel border border-border bg-surface-2/50 p-4">
                  <div className="mb-3 flex items-start justify-between gap-3">
                    <div>
                      <label htmlFor="profile-banner" className="block text-xs font-medium text-foreground">
                        Profile banner
                      </label>
                      <p className="mt-1 text-[11px] text-fg-muted">
                        Choose from banners unlocked by your maximum Codeforces rating or special achievements.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setIsBannerLockerOpen(true)}
                      className="inline-flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-xs font-semibold text-amber-300 transition-all hover:bg-amber-500/20 shrink-0 shadow-xs"
                    >
                      <Sparkles className="size-3 text-amber-400" />
                      Open Banner Locker
                    </button>
                  </div>

                  <div className="relative mb-3 h-20 overflow-hidden rounded-control border border-border p-3">
                    <BannerGraphic banner={selectedBanner} showEffects={false} withScrim />
                    <div className="relative z-10 flex h-full items-center justify-between">
                      <span className="text-sm font-semibold" style={{ color: selectedBanner.colors.text }}>
                        {selectedBanner.name}
                      </span>
                      <span
                        className={`rounded-full border px-2 py-0.5 text-[10px] ${getRarityBadgeStyle(selectedBanner.rarity, selectedBanner.id).text} ${getRarityBadgeStyle(selectedBanner.rarity, selectedBanner.id).border}`}
                      >
                        {selectedBanner.rarity}
                      </span>
                    </div>
                  </div>

                  <select
                    id="profile-banner"
                    value={formData.bannerId}
                    onChange={(e) => setFormData({ ...formData, bannerId: e.target.value })}
                    className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  >
                    {RATING_BANNERS.map((banner) => {
                      const unlocked = isBannerUnlocked(banner, effectiveMaxRating, profile);
                      return (
                        <option key={banner.id} value={banner.id} disabled={!unlocked}>
                          {unlocked ? banner.name : `${banner.name} - locked (${banner.minRating} rating)`}
                        </option>
                      );
                    })}
                  </select>
                  <p className="mt-2 text-[11px] text-fg-muted">
                    Special banners are only selectable when your account has been granted access.
                  </p>
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
          { 
            platform: "codeforces", 
            value: profile.codeforcesHandle, 
            rating: currentRating,
            history: cfHistory.length > 0 ? cfHistory.map(p => ({ value: p.rating })) : null 
          },
          { 
            platform: "leetcode", 
            value: profile.leetcodeHandle, 
            rating: profile.leetcodeRating,
            history: lcPoints.length > 0 ? lcPoints.map(p => ({ value: p.rating })) : null 
          },
          { platform: "codechef", value: profile.codechefUrl },
          { platform: "atcoder", value: profile.atcoderUrl },
          { platform: "github", value: profile.githubUrl },
          { platform: "linkedin", value: profile.linkedinUrl },
        ]}
      />

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

      {/* ── LeetCode Rating History ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Code className="size-4 text-amber-500" />
            LeetCode Rating History
          </CardTitle>
        </CardHeader>
        <CardContent>
          {lcLoading ? (
            <div className="flex h-[200px] items-center justify-center text-xs text-fg-muted">
              <Loader2 className="mr-2 size-4 animate-spin text-primary" />
              Loading LeetCode history...
            </div>
          ) : lcPoints.length > 0 ? (
            // One contest is enough to draw, as it is for Codeforces.
            <RatingGraph data={lcPoints} />
          ) : (
            <div className="flex h-[200px] flex-col items-center justify-center gap-2 text-sm text-fg-muted">
              <Code className="size-6 text-fg-subtle" />
              <span>
                {profile.leetcodeHandle
                  ? "No rated LeetCode contests yet."
                  : "No LeetCode handle linked."}
              </span>
            </div>
          )}
        </CardContent>
      </Card>

      <InDevelopment
        headingLevel="h3"
        title="Practice stats"
        body="A breakdown of what this member practises, built from their Codeforces submissions."
        items={[
          "Problems solved",
          "Average problem rating",
          "Topic strength",
          "Practice suggestions",
        ]}
      />

      {/* ── Account Details ── */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {/* The address is never in the public projection, so this is owner-only
              by construction as well as by this check. */}
          {isOwner && (
            <>
              {/* gap and min-w-0 so a long university address wraps instead of
                  crushing its label or pushing the card off a phone screen. */}
              <div className="flex items-start justify-between gap-3">
                <span className="flex shrink-0 items-center gap-2 text-fg-muted">
                  <Mail className="size-3.5" /> Email
                </span>
                <span className="min-w-0 font-mono text-xs break-all">{profile.email}</span>
              </div>
              <Separator />
            </>
          )}

          {/*
            Whether this number arrived at all is the server's decision: office
            bearers publish theirs because the post is a point of contact, and
            everyone else's reaches admins only. So the rule is not repeated
            here -- a number in hand is a number this viewer may see.
          */}
          {(isOwner || profile.phoneNumber) && (
            <>
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-fg-muted">
                  <Phone className="size-3.5" /> Phone Number
                </span>
                <span>
                  {profile.phoneNumber ? (
                    <a href={`tel:${profile.phoneNumber}`} className="hover:text-primary">
                      {profile.phoneNumber}
                    </a>
                  ) : (
                    <span className="text-fg-subtle">Not provided</span>
                  )}
                </span>
              </div>
              {!isOwner && isOfficeBearer && (
                <p className="text-nano text-fg-subtle">
                  Listed publicly because this is a club post.
                </p>
              )}
              <Separator />
            </>
          )}
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

      {/* ── Banner Locker Drawer – strictly owner-only for equipping ── */}
      {isOwner && (
        <BannerLockerDrawer
          member={leaderboardMember}
          isOpen={isBannerLockerOpen}
          isOwnProfile={true}
          onClose={() => setIsBannerLockerOpen(false)}
          onBannerEquipped={(newBannerId) => {
            setFormData((prev) => ({ ...prev, bannerId: newBannerId }));
            setIsBannerLockerOpen(false);
            onUpdate();
          }}
        />
      )}
    </div>
  );
}
