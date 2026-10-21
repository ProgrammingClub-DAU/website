"use client";

import { Suspense, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search, ArrowRightLeft, Users, AlertCircle, Loader2 } from "lucide-react";
import { statsService } from "@/lib/services/stats";
import { dashboardService } from "@/lib/services/dashboard";
import { CompareView } from "@/components/site/compare-view";
import { PageTitle, Eyebrow } from "@/components/site/primitives";
import { useAuthStore } from "@/store/auth";
import type { CompareResult, PublicMember } from "@/types/api";

function CompareContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const currentUser = useAuthStore((s) => s.user);

  const paramA = searchParams.get("a");
  const paramB = searchParams.get("b");

  const [memberAId, setMemberAId] = useState<number | null>(
    paramA ? parseInt(paramA, 10) : currentUser ? currentUser.id : null
  );
  const [memberBId, setMemberBId] = useState<number | null>(
    paramB ? parseInt(paramB, 10) : null
  );

  const [searchA, setSearchA] = useState("");
  const [searchB, setSearchB] = useState("");
  const [resultsA, setResultsA] = useState<PublicMember[]>([]);
  const [resultsB, setResultsB] = useState<PublicMember[]>([]);
  const [isSearchingA, setIsSearchingA] = useState(false);
  const [isSearchingB, setIsSearchingB] = useState(false);

  const [compareData, setCompareData] = useState<CompareResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync URL with selected member IDs
  useEffect(() => {
    const params = new URLSearchParams();
    if (memberAId) params.set("a", memberAId.toString());
    if (memberBId) params.set("b", memberBId.toString());
    const newQuery = params.toString();
    const currentQuery = searchParams.toString();
    if (newQuery !== currentQuery) {
      router.replace(`/compare?${newQuery}`, { scroll: false });
    }
  }, [memberAId, memberBId, router, searchParams]);

  // Load comparison whenever both A and B are selected
  useEffect(() => {
    if (!memberAId || !memberBId) {
      setCompareData(null);
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setError(null);

    statsService
      .compare(memberAId, memberBId)
      .then((data) => {
        if (isMounted) setCompareData(data);
      })
      .catch((err) => {
        if (isMounted) {
          setError(
            err.response?.data?.message || "Failed to load comparison data. Please check member selections."
          );
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [memberAId, memberBId]);

  // Search debouncing for Member A
  useEffect(() => {
    if (!searchA.trim() || searchA.length < 2) {
      setResultsA([]);
      return;
    }
    const timer = setTimeout(async () => {
      setIsSearchingA(true);
      try {
        const res = await dashboardService.getMembers(searchA.trim(), 0, 10);
        setResultsA(res.content);
      } catch {
        setResultsA([]);
      } finally {
        setIsSearchingA(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [searchA]);

  // Search debouncing for Member B
  useEffect(() => {
    if (!searchB.trim() || searchB.length < 2) {
      setResultsB([]);
      return;
    }
    const timer = setTimeout(async () => {
      setIsSearchingB(true);
      try {
        const res = await dashboardService.getMembers(searchB.trim(), 0, 10);
        setResultsB(res.content);
      } catch {
        setResultsB([]);
      } finally {
        setIsSearchingB(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [searchB]);

  const swapMembers = () => {
    const temp = memberAId;
    setMemberAId(memberBId);
    setMemberBId(temp);
  };

  return (
    <div className="container-page py-10 max-w-4xl mx-auto space-y-8">
      {/* ── Page Header ── */}
      <div>
        <Eyebrow>Head-to-Head Analytics</Eyebrow>
        <PageTitle>Compare Members</PageTitle>
        <p className="mt-2 text-body text-fg-muted max-w-xl">
          Side-by-side performance comparison across Codeforces, LeetCode, solves, contest history, and direct rated head-to-head records.
        </p>
      </div>

      {/* ── Member Selectors Bar ── */}
      <div className="grid grid-cols-1 md:grid-cols-[1fr,auto,1fr] items-center gap-3 rounded-panel border border-hairline bg-surface/70 p-4 backdrop-blur-md">
        {/* Selector A */}
        <div className="relative">
          <label className="block text-nano font-mono text-fg-muted uppercase tracking-caps mb-1.5">
            Member 1 {compareData && `(${compareData.memberA.name})`}
          </label>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 size-4 text-fg-subtle" />
            <input
              type="text"
              placeholder={compareData ? compareData.memberA.name : "Search name, ID or handle..."}
              value={searchA}
              onChange={(e) => setSearchA(e.target.value)}
              className="w-full rounded-control border border-hairline bg-surface-elevated/60 pl-9 pr-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-2 focus-visible:outline-ring font-body"
            />
            {isSearchingA && (
              <Loader2 className="absolute right-3 top-2.5 size-4 animate-spin text-fg-subtle" />
            )}
          </div>
          {resultsA.length > 0 && (
            <div className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-control border border-border bg-surface-elevated shadow-xl">
              {resultsA.map((m) => (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => {
                    setMemberAId(m.id);
                    setSearchA("");
                    setResultsA([]);
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-surface-elevated/80 flex items-center justify-between text-sm transition-colors border-b border-hairline last:border-0"
                >
                  <span className="font-medium text-foreground">{m.name}</span>
                  <span className="font-mono text-nano text-fg-muted">
                    {m.codeforcesHandle ? `@${m.codeforcesHandle}` : m.clubRole ? m.clubRole : "Member"}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Swap Button */}
        <div className="flex justify-center md:pt-5">
          <button
            type="button"
            onClick={swapMembers}
            disabled={!memberAId || !memberBId}
            title="Swap positions"
            className="flex size-9 items-center justify-center rounded-full border border-hairline bg-surface-elevated text-fg-muted hover:text-foreground hover:bg-surface transition-all disabled:opacity-30 disabled:pointer-events-none"
          >
            <ArrowRightLeft className="size-4" />
          </button>
        </div>

        {/* Selector B */}
        <div className="relative">
          <label className="block text-nano font-mono text-fg-muted uppercase tracking-caps mb-1.5">
            Member 2 {compareData && `(${compareData.memberB.name})`}
          </label>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 size-4 text-fg-subtle" />
            <input
              type="text"
              placeholder={compareData ? compareData.memberB.name : "Search name, ID or handle..."}
              value={searchB}
              onChange={(e) => setSearchB(e.target.value)}
              className="w-full rounded-control border border-hairline bg-surface-elevated/60 pl-9 pr-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-2 focus-visible:outline-ring font-body"
            />
            {isSearchingB && (
              <Loader2 className="absolute right-3 top-2.5 size-4 animate-spin text-fg-subtle" />
            )}
          </div>
          {resultsB.length > 0 && (
            <div className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-control border border-border bg-surface-elevated shadow-xl">
              {resultsB.map((m) => (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => {
                    setMemberBId(m.id);
                    setSearchB("");
                    setResultsB([]);
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-surface-elevated/80 flex items-center justify-between text-sm transition-colors border-b border-hairline last:border-0"
                >
                  <span className="font-medium text-foreground">{m.name}</span>
                  <span className="font-mono text-nano text-fg-muted">
                    {m.codeforcesHandle ? `@${m.codeforcesHandle}` : m.clubRole ? m.clubRole : "Member"}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ── Main Display Body ── */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-24 space-y-3">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="font-mono text-label text-fg-muted">Loading head-to-head metrics...</p>
        </div>
      ) : error ? (
        <div className="rounded-card border border-rose-500/30 bg-rose-500/10 p-4 text-rose-300 flex items-center gap-3">
          <AlertCircle className="size-5 shrink-0" />
          <p className="text-sm font-medium">{error}</p>
        </div>
      ) : compareData ? (
        <CompareView data={compareData} />
      ) : (
        <div className="rounded-panel border border-dashed border-hairline py-20 px-4 text-center">
          <Users className="mx-auto size-10 text-fg-subtle mb-3 opacity-60" />
          <h3 className="font-heading text-lg font-medium text-foreground">
            Select two members to compare
          </h3>
          <p className="mt-1 text-label text-fg-muted max-w-sm mx-auto">
            Search by student name, ID, or Codeforces handle to view detailed comparative stats.
          </p>
        </div>
      )}
    </div>
  );
}

export default function ComparePage() {
  return (
    <Suspense
      fallback={
        <div className="container-page py-20 text-center font-mono text-label text-fg-muted">
          Loading compare page...
        </div>
      }
    >
      <CompareContent />
    </Suspense>
  );
}
