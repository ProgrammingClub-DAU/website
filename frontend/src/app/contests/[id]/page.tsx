"use client";

import React, { use, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { Trophy, ArrowLeft, RefreshCw, Layers } from "lucide-react";
import { contestService } from "@/lib/services/contests";
import { apiErrorMessage } from "@/lib/api-error";
import { RanklistTable } from "@/components/site/ranklist-table";
import { UpsolveMatrix } from "@/components/site/upsolve-matrix";
import { useAuthStore } from "@/store/auth";
import type { ClubContestDetailDto, UpsolveMatrixDto } from "@/types/api";

interface ContestDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ContestDetailPage({ params }: ContestDetailPageProps) {
  const { id } = use(params);
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeTab = searchParams.get("tab") === "upsolve" ? "upsolve" : "ranklist";

  const { user } = useAuthStore();
  const isAdmin = user?.role === "ROLE_ADMIN" || user?.role === "ADMIN";

  const [detail, setDetail] = useState<ClubContestDetailDto | null>(null);
  const [upsolveData, setUpsolveData] = useState<UpsolveMatrixDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [contestDetail, upsolveMatrix] = await Promise.all([
        contestService.get(id),
        contestService.getUpsolve(id),
      ]);
      setDetail(contestDetail);
      setUpsolveData(upsolveMatrix);
    } catch (err: unknown) {
      console.error("Failed to load contest detail:", err);
      setError(apiErrorMessage(err, "Failed to load contest standings. The contest may not exist."));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSync = async () => {
    if (syncing) return;
    setSyncing(true);
    try {
      const updated = await contestService.sync(id);
      setDetail(updated);
      const updatedUpsolve = await contestService.getUpsolve(id);
      setUpsolveData(updatedUpsolve);
    } catch (err: unknown) {
      console.error("Sync failed:", err);
      alert(`Failed to sync standings with Codeforces: ${apiErrorMessage(err, "Please try again.")}`);
    } finally {
      setSyncing(false);
    }
  };

  const handleTabChange = (tab: "ranklist" | "upsolve") => {
    const qs = tab === "upsolve" ? "?tab=upsolve" : "";
    router.replace(`/contests/${id}${qs}`, { scroll: false });
  };

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-6">
      {/* Top back nav & admin sync */}
      <div className="flex items-center justify-between">
        <Link
          href="/contests"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
        >
          <ArrowLeft className="size-3.5" />
          Back to Contests
        </Link>

        {isAdmin && (
          <button
            onClick={handleSync}
            disabled={syncing}
            className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3.5 py-1.5 text-xs font-mono text-primary hover:bg-primary/20 transition-all shadow-xs disabled:opacity-50"
          >
            <RefreshCw className={`size-3.5 ${syncing ? "animate-spin" : ""}`} />
            {syncing ? "Syncing CF..." : "Sync Standings Now"}
          </button>
        )}
      </div>

      {loading ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Loading contest standings...
        </div>
      ) : error || !detail ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-rose-400">
          {error || "Contest not found."}
        </div>
      ) : (
        <div className="space-y-6">
          {/* Tabs header */}
          <div className="flex border-b border-white/10 gap-6">
            <button
              onClick={() => handleTabChange("ranklist")}
              className={`pb-3 text-sm font-semibold tracking-tight transition-colors relative ${
                activeTab === "ranklist" ? "text-white" : "text-fg-muted hover:text-white"
              }`}
            >
              <span className="flex items-center gap-2">
                <Trophy className="size-4 text-primary" />
                Ranklist & Problems
              </span>
              {activeTab === "ranklist" && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
              )}
            </button>

            <button
              onClick={() => handleTabChange("upsolve")}
              className={`pb-3 text-sm font-semibold tracking-tight transition-colors relative ${
                activeTab === "upsolve" ? "text-white" : "text-fg-muted hover:text-white"
              }`}
            >
              <span className="flex items-center gap-2">
                <Layers className="size-4 text-cyan-400" />
                Upsolve Matrix
              </span>
              {activeTab === "upsolve" && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-cyan-400" />
              )}
            </button>
          </div>

          {/* Active Tab View */}
          {activeTab === "ranklist" ? (
            <RanklistTable data={detail} />
          ) : upsolveData ? (
            <UpsolveMatrix data={upsolveData} />
          ) : (
            <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center font-mono text-xs text-fg-muted">
              No upsolve matrix available.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
