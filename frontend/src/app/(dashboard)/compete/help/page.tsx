"use client";

import React from "react";
import Link from "next/link";
import { ChevronDown, ArrowLeft } from "lucide-react";

const FAQItem = ({ question, answer }: { question: string; answer: React.ReactNode }) => {
  const [isOpen, setIsOpen] = React.useState(false);

  return (
    <div className="border-b border-border py-4">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-full justify-between items-center text-left text-lg font-semibold text-foreground focus:outline-none"
      >
        <span>{question}</span>
        <ChevronDown
          className={`w-5 h-5 text-muted-foreground transition-transform duration-300 ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>
      <div
        className={`overflow-hidden transition-all duration-300 ${
          isOpen ? "max-h-[500px] mt-4 opacity-100" : "max-h-0 opacity-0"
        }`}
      >
        <div className="text-muted-foreground text-sm leading-relaxed">{answer}</div>
      </div>
    </div>
  );
};

export default function HelpPage() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300 pb-20">
      <header className="w-full border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-4 py-4">
          <Link href="/compete" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-4 h-4" />
            <span className="font-semibold text-sm">Back</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/compete" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Home</Link>
            <Link href="/compete/create" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">ICPC Mode</Link>
            <Link href="/compete/ioi" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">IOI Mode</Link>
            <Link href="/compete/help" className="text-sm font-medium text-foreground">Help</Link>
          </nav>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 pt-12">
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight mb-8 font-heading">
          Help & Rules
        </h1>

        <section className="mb-12">
          <h2 className="text-2xl font-bold mb-4 border-b border-border pb-2">Game Modes</h2>
          
          <div className="bg-card border border-border rounded-xl p-6 shadow-sm mb-6">
            <h3 className="text-xl font-semibold mb-2 flex items-center gap-2">
              <span className="bg-indigo-500/10 text-indigo-500 p-1.5 rounded-lg">🎯</span>
              Classic Mode
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              The first team to solve a problem claims the square on the board. Once a square is claimed, it belongs to that team permanently. The first team to complete a full row, column, or diagonal wins the match.
            </p>
          </div>

          <div className="bg-card border border-border rounded-xl p-6 shadow-sm">
            <h3 className="text-xl font-semibold mb-2 flex items-center gap-2">
              <span className="bg-pink-500/10 text-pink-500 p-1.5 rounded-lg">⚔️</span>
              Replace Mode
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed mb-4">
              Squares can be stolen! If a team has already claimed a square, your team can steal it by solving the same problem, but you must accumulate more points (based on time or penalties, depending on the match settings) or simply be the most recent solver if points are not configured.
            </p>
            <div className="bg-muted p-4 rounded-lg text-sm text-muted-foreground">
              <strong>Note:</strong> Currently, the replace mechanic requires the stealing team to have a higher solver rating or a faster solve time depending on the match &quot;Replace Increment&quot; settings configured during match creation.
            </div>
          </div>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold mb-4 border-b border-border pb-2">Frequently Asked Questions</h2>
          <div className="bg-card border border-border rounded-xl px-6 py-2 shadow-sm">
            <FAQItem 
              question="How are problems chosen?" 
              answer="Problems are automatically fetched from Codeforces based on the minimum and maximum rating limits you set when creating the match. The system ensures that the problems are randomly selected and uniformly distributed across the grid."
            />
            <FAQItem 
              question="Do I need to refresh the page?" 
              answer="No! Bingo CP syncs directly with Codeforces in real-time. As soon as someone on your team (or an opposing team) gets an 'Accepted' verdict on Codeforces, the grid will automatically update and play a notification."
            />
            <FAQItem 
              question="Can I play solo?" 
              answer="Absolutely. You can create a match with just yourself on one team, and invite a friend to be on the other team for a 1v1 duel."
            />
            <FAQItem 
              question="What happens if the time runs out?" 
              answer="If the match duration expires before any team completes a Bingo line, the team with the most claimed squares wins. The match will automatically lock and display the final scoreboard."
            />
          </div>
        </section>
      </main>
    </div>
  );
}
