"use client";

import React from "react";
import Link from "next/link";
import { ArrowRight, Trophy, Zap, Users } from "lucide-react";

export default function CompeteLobbyPage() {
  const [activeCell, setActiveCell] = React.useState(-1);
  const [claimed, setClaimed] = React.useState<Record<number, string>>({});
  
  React.useEffect(() => {
    const interval = setInterval(() => {
      const nextCell = Math.floor(Math.random() * 25);
      setActiveCell(nextCell);
      
      if (Math.random() > 0.6) {
        setClaimed(prev => ({
          ...prev,
          [nextCell]: Math.random() > 0.5 ? 'bg-indigo-500 text-white border-indigo-600' : 'bg-pink-500 text-white border-pink-600'
        }));
      }
    }, 1500);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
      
      {/* Header Navigation */}
      <header className="w-full border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-6 py-4">
          <Link href="/compete" className="flex items-center gap-2">
            <span className="font-extrabold text-xl bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 to-pink-500 font-heading">
              Bingo CP
            </span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/compete" className="text-sm font-medium text-foreground">Home</Link>
            <Link href="/compete/create" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">ICPC Mode</Link>
            <Link href="/compete/ioi" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">IOI Mode</Link>
            <Link href="/compete/help" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Help</Link>
          </nav>
        </div>
      </header>

      <section className="max-w-7xl mx-auto px-6 pt-12 md:pt-20 lg:pt-24 pb-16">
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
          
          {/* Animated 5x5 Grid */}
          <div className="flex-1 w-full max-w-lg">
            <div className="aspect-square grid grid-cols-5 gap-2 p-3 rounded-2xl border border-border bg-card shadow-2xl">
              {[...Array(25)].map((_, i) => (
                <div 
                  key={i} 
                  className={`rounded-lg border flex flex-col items-center justify-center transition-all duration-500 text-center p-1
                    ${claimed[i] ? claimed[i] : 'bg-background border-border text-foreground'}
                    ${activeCell === i && !claimed[i] ? 'ring-2 ring-indigo-500 scale-105 shadow-lg' : ''}
                  `}
                >
                  <span className="text-[9px] opacity-70 mb-0.5">{800 + Math.floor(Math.random()*4)*100}</span>
                  <span className="text-[10px] sm:text-xs font-semibold truncate w-full px-1">Problem {String.fromCharCode(65 + Math.floor(Math.random()*5))}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-6 py-20 border-t border-border bg-card/30">
        <h2 className="text-3xl font-bold text-center mb-16 font-heading">How to Play</h2>
        <div className="grid md:grid-cols-4 gap-8">
          {[
            { step: 1, title: "Create Match", desc: "Choose grid size (3x3 to 6x6), rating range, and game mode (Classic or Replace)." },
            { step: 2, title: "Add Teams", desc: "Add up to 16 teams. Each team can have up to 16 Codeforces handles." },
            { step: 3, title: "Start Solving", desc: "The grid is populated with random CF problems. Submit solutions on Codeforces." },
            { step: 4, title: "Bingo!", desc: "The system syncs your accepted submissions. Claim a full row, column, or diagonal to win!" }
          ].map(s => (
            <div key={s.step} className="flex flex-col items-center text-center relative">
              <div className="w-12 h-12 rounded-full bg-indigo-500 text-white flex items-center justify-center font-bold text-xl mb-4 z-10 ring-4 ring-background">
                {s.step}
              </div>
              <h3 className="text-lg font-bold mb-2">{s.title}</h3>
              <p className="text-sm text-muted-foreground">{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

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

      <footer className="border-t border-border py-8 text-center bg-card">
        <div className="text-sm text-muted-foreground font-medium">
          © {new Date().getFullYear()} Bingo CP — Ported for the club.
        </div>
      </footer>
    </div>
  );
}
