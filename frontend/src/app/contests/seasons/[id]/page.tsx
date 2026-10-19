"use client";

import React, { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { seasonService } from "@/lib/services/seasons";
import { ChampionshipTable } from "@/components/site/championship-table";
import type { ChampionshipDto } from "@/types/api";

interface SeasonChampionshipPageProps {
  params: Promise<{ id: string }>;
}

export default function SeasonChampionshipPage({ params }: SeasonChampionshipPageProps) {
  const { id } = use(params);
  const [data, setData] = useState<ChampionshipDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    seasonService
      .getChampionship(id)
      .then((res) => {
        if (!ignore) setData(res);
      })
      .catch((err: unknown) => {
        console.error("Failed to load championship standings:", err);
        if (!ignore) setError("Failed to load season championship standings.");
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [id]);

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-6">
      <Link
        href="/contests"
        className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
      >
        <ArrowLeft className="size-3.5" />
        Back to Contests & Seasons
      </Link>

      {loading ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Loading season championship standings...
        </div>
      ) : error || !data ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-rose-400">
          {error || "Season not found."}
        </div>
      ) : (
        <ChampionshipTable data={data} />
      )}
    </div>
  );
}
