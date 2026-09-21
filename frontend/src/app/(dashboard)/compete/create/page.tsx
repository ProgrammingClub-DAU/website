"use client";

import React from "react";
import Link from "next/link";
import { MatchCreationForm } from "@/components/compete/match-creation-form";

export default function CreateMatchPage() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
      <header className="w-full border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-4 py-4">
          <Link href="/compete" className="flex items-center gap-2">
            <span className="font-extrabold text-xl bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 to-pink-500 font-heading">
              Bingo CP
            </span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/compete" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Home</Link>
            <Link href="/compete/create" className="text-sm font-medium text-foreground">ICPC Mode</Link>
            <Link href="/compete/ioi" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">IOI Mode</Link>
            <Link href="/compete/help" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Help</Link>
          </nav>
        </div>
      </header>
      
      <div className="max-w-6xl mx-auto px-6 pt-12 pb-6">
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-purple-600 to-pink-500 font-heading">
          Create Match
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">
          Set up a new Bingo CP match, invite teams, and start the timer.
        </p>
      </div>

      <main className="max-w-6xl mx-auto px-6 pb-16">
        <div className="bg-card border border-border rounded-2xl shadow-sm p-6 sm:p-8">
          <MatchCreationForm />
        </div>
      </main>
    </div>
  );
}
