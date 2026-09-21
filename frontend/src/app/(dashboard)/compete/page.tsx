"use client";

import React, from "react";
import Link from "next/link";
import { ArrowRight, Trophy, Zap, Users } from "lucide-react";

export default function CompeteLobbyPage() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
      <header className="max-w-7xl mx-auto px-6 pt-12 md:pt-20 lg:pt-24 pb-16">
        <div className="flex flex-col lg:flex-row items-center gap-12 lg:gap-20">
          <div className="flex-1 text-center lg:text-left">
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight mb-6">
              <span className="bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 font-heading">
                Bingo CP
              </span>
            </h1>
            <h2 className="text-2xl sm:text-3xl font-bold mb-4 font-heading text-foreground/90">
              Competitive programming — turned into a social, party-ready game.
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto lg:mx-0 text-lg leading-relaxed mb-8">
              Pick a grid, invite friends or teammates, and race to claim squares by solving real
              Codeforces problems. First line wins. Fast, fair, and surprisingly addictive.
            </p>
            
            <div className="flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
              <Link 
                href="/compete/create"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 text-base font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-2xl shadow-lg hover:shadow-xl transition-all w-full sm:w-auto hover:-translate-y-1"
              >
                Create a Match <ArrowRight className="w-5 h-5" />
              </Link>
              <a
                href="https://github.com/hocln/bingo-cp"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 px-8 py-4 text-base font-semibold rounded-2xl border border-border bg-card hover:bg-accent text-foreground hover:text-accent-foreground transition-all w-full sm:w-auto"
              >
                Original Project
              </a>
            </div>
          </div>
          
          {/* Decorative 3x3 Grid */}
          <div className="flex-1 w-full max-w-md">
            <div className="aspect-square grid grid-cols-3 gap-3 p-4 rounded-3xl border border-border bg-card/50 backdrop-blur-sm shadow-2xl rotate-3 hover:rotate-0 transition-transform duration-500">
              {[...Array(9)].map((_, i) => (
                <div 
                  key={i} 
                  className={`rounded-xl border flex items-center justify-center transition-colors
                    ${i === 0 || i === 4 || i === 8 ? 'bg-indigo-500 border-indigo-600 text-white' : 
                      i === 2 ? 'bg-pink-500 border-pink-600 text-white' : 
                      'bg-card border-border'
                    }`}
                >
                  {(i === 0 || i === 4 || i === 8 || i === 2) && <Trophy className="w-8 h-8 opacity-50" />}
                </div>
              ))}
            </div>
          </div>
        </div>
      </header>

      <section className="max-w-7xl mx-auto px-6 py-20 border-t border-border">
        <div className="grid md:grid-cols-3 gap-10">
          <div className="flex flex-col items-center text-center">
            <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 text-indigo-500 flex items-center justify-center mb-6">
              <Zap className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold mb-3">Live Sync</h3>
            <p className="text-muted-foreground">
              Directly syncs with Codeforces. Submit your code, and the grid updates instantly for everyone.
            </p>
          </div>
          <div className="flex flex-col items-center text-center">
            <div className="w-16 h-16 rounded-2xl bg-purple-500/10 text-purple-500 flex items-center justify-center mb-6">
              <Users className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold mb-3">Team Play</h3>
            <p className="text-muted-foreground">
              Play solo or form teams of up to 5 players. Compete against up to 16 teams simultaneously.
            </p>
          </div>
          <div className="flex flex-col items-center text-center">
            <div className="w-16 h-16 rounded-2xl bg-pink-500/10 text-pink-500 flex items-center justify-center mb-6">
              <Trophy className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold mb-3">Dynamic Grid</h3>
            <p className="text-muted-foreground">
              Choose your grid size and rating range. The backend automatically fetches random Codeforces problems.
            </p>
          </div>
        </div>
      </section>

      <footer className="border-t border-border mt-10 py-8 text-center bg-card">
        <div className="text-sm text-muted-foreground font-medium">
          © {new Date().getFullYear()} Bingo CP — Ported for the club.
        </div>
      </footer>
    </div>
  );
}
