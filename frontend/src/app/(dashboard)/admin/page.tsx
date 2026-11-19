"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Script from "next/script";
import { useAuthStore } from "@/store/auth";
import { AdminTabs, isAdminTab, type AdminTab } from "@/components/site/admin-tabs";
import { Shield, ShieldAlert, Loader2 } from "lucide-react";
import dynamic from "next/dynamic";
import { MembersTab } from "@/components/admin/members-tab";

// Each tab is its own chunk, fetched the first time it is opened, so the admin page
// no longer downloads every manager up front. Members is the default tab and is
// imported directly, so the first screen has no loading step.
function TabLoading() {
  return (
    <div className="flex items-center justify-center gap-2 py-20 text-sm text-fg-muted">
      <Loader2 className="size-4 animate-spin" aria-hidden />
      Loading…
    </div>
  );
}

const EventsTab = dynamic(() => import("@/components/admin/events-tab").then((m) => m.EventsTab), { loading: TabLoading });
const GalleriesTab = dynamic(() => import("@/components/admin/galleries-tab").then((m) => m.GalleriesTab), { loading: TabLoading });
const HallOfFameManager = dynamic(() => import("@/components/admin/hall-of-fame-manager").then((m) => m.HallOfFameManager), { loading: TabLoading });
const ContestsManager = dynamic(() => import("@/components/admin/contests-manager").then((m) => m.ContestsManager), { loading: TabLoading });
const AwardsManager = dynamic(() => import("@/components/admin/awards-manager").then((m) => m.AwardsManager), { loading: TabLoading });
const SyncManager = dynamic(() => import("@/components/admin/sync-manager").then((m) => m.SyncManager), { loading: TabLoading });
const BlogReviewManager = dynamic(() => import("@/components/admin/blog-review-manager").then((m) => m.BlogReviewManager), { loading: TabLoading });
const CpGroupTab = dynamic(() => import("@/components/admin/cp-group-tab").then((m) => m.CpGroupTab), { loading: TabLoading });

export default function AdminDashboardPage() {
  const { user, isAuthenticated } = useAuthStore();
  const [activeTab, setActiveTab] = useState<AdminTab>("members");

  // ?tab=... opens that tab, so a link (or "back" from a CP Practice student's
  // page) lands on the right one. Switching tabs keeps the URL in step.
  useEffect(() => {
    const tab = new URLSearchParams(window.location.search).get("tab");
    if (tab && isAdminTab(tab)) setActiveTab(tab);
  }, []);
  const changeTab = (tab: AdminTab) => {
    setActiveTab(tab);
    window.history.replaceState(null, "", tab === "members" ? "/admin" : `/admin?tab=${tab}`);
  };

  // Authentication & authorization guard
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-amber-500" />
        <h2 className="text-lg font-bold">Authentication Required</h2>
        <p className="text-xs text-fg-muted max-w-sm">
          Please log in with an administrator account to access this dashboard.
        </p>
        <Link
          href="/login"
          className="mt-2 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow"
        >
          Sign In
        </Link>
      </div>
    );
  }

  if (user?.role !== "ROLE_ADMIN") {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-red-500" />
        <h2 className="text-lg font-bold">Access Denied</h2>
        <p className="text-xs text-fg-muted max-w-sm">
          You do not have administrative privileges to view this page.
        </p>
        <Link
          href="/leaderboard"
          className="mt-2 rounded-control border border-border bg-surface-2 px-4 py-2 text-xs font-semibold hover:text-foreground"
        >
          Return to Leaderboard
        </Link>
      </div>
    );
  }

  return (
    <>
      <Script src="https://upload-widget.cloudinary.com/global/all.js" strategy="lazyOnload" />
      {/* Horizontal padding, which the admin pages never had. The public pages
          get theirs from Section/container-page; these build their own container,
          so on a phone every panel ran edge to edge with text touching the glass. */}
      <div className="mx-auto max-w-7xl space-y-6 px-4 pb-16 pt-6 sm:px-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Admin Dashboard
            </h1>
            <p className="text-xs text-fg-muted">
              Manage club members, schedule events, record attendance, and curate photo galleries.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <Shield className="size-3.5" /> Administrator Mode
            </span>
          </div>
        </div>

        <AdminTabs activeTab={activeTab} onChange={changeTab} />

        {activeTab === "members" && <MembersTab />}
        {activeTab === "events" && <EventsTab />}
        {activeTab === "hallOfFame" && <HallOfFameManager />}
        {activeTab === "galleries" && <GalleriesTab />}
        {activeTab === "contests" && <ContestsManager />}
        {activeTab === "awards" && <AwardsManager />}
        {activeTab === "sync" && <SyncManager />}
        {activeTab === "blogs" && <BlogReviewManager />}
        {activeTab === "cpGroup" && <CpGroupTab />}
      </div>
    </>
  );
}
