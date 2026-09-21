"use client";

import React from "react";
import Link from "next/link";
import { MatchCreationForm } from "@/components/compete/match-creation-form";

export default function CreateMatchPage() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
      <header className="max-w-6xl mx-auto px-6 pt-12 pb-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-purple-600 to-pink-500 font-heading">
            Create Match
          </h1>
          <nav className="flex items-center gap-3">
            <Link
              href="/compete"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-2xl border border-border bg-card hover:shadow-md text-sm transition-all"
            >
              Back to Home
            </Link>
          </nav>
        </div>
        <p className="mt-3 text-sm text-muted-foreground">
          Set up a new Bingo CP match, invite teams, and start the timer.
        </p>
      </header>

      <main className="max-w-6xl mx-auto px-6 pb-16">
        <div className="bg-card border border-border rounded-2xl shadow-sm p-6 sm:p-8">
          <MatchCreationForm />
        </div>
      </main>
    </div>
  );
}
