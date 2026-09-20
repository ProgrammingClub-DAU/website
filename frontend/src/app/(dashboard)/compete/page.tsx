"use client";

import React from "react";
import { MatchCreationForm } from "@/components/compete/match-creation-form";

export default function CompeteLobbyPage() {
  return (
    <div className="min-h-screen bg-[#111318] text-white">
      <header className="max-w-6xl mx-auto px-6 pt-12">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 font-heading">
            Bingo CP
          </h1>
          <nav className="flex items-center gap-3">
            <a
              href="https://github.com/hocln/bingo-cp"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 text-sm font-medium transition-colors"
            >
              Original Project
            </a>
          </nav>
        </div>

        <div className="mt-10 mb-12">
          <h2 className="text-2xl sm:text-3xl font-bold mb-3 font-heading">
            Competitive programming — turned into a social, party-ready game.
          </h2>
          <p className="text-white/70 max-w-3xl leading-relaxed">
            Pick a grid, invite friends or teammates, and race to claim squares by solving real
            competitive programming problems. First line wins — or in special modes, highest score
            claims the square. Fast, fair, and surprisingly addictive.
          </p>
        </div>
        
        <div className="mt-12">
          <MatchCreationForm />
        </div>
      </header>

      <footer className="border-t border-white/10 mt-20 py-8 text-center">
        <div className="text-sm text-white/50">
          © {new Date().getFullYear()} Bingo CP — Ported for the club.
        </div>
      </footer>
    </div>
  );
}
