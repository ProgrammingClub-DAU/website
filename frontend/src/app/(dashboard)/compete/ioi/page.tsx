"use client";

import React from "react";
import Link from "next/link";
import { ArrowLeft, Clock } from "lucide-react";

export default function IOIModePage() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
      <header className="w-full border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-4 py-4">
          <Link href="/compete" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-4 h-4" />
            <span className="font-semibold text-sm">Back</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/compete" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Home</Link>
            <Link href="/compete/create" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">ICPC Mode</Link>
            <Link href="/compete/ioi" className="text-sm font-medium text-foreground">IOI Mode</Link>
            <Link href="/compete/help" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Help</Link>
          </nav>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 pt-32 flex flex-col items-center text-center">
        <div className="w-24 h-24 bg-indigo-500/10 text-indigo-500 rounded-3xl flex items-center justify-center mb-8 shadow-inner border border-indigo-500/20">
          <Clock className="w-12 h-12" />
        </div>
        
        <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight mb-6 font-heading">
          IOI Mode
        </h1>
        
        <div className="inline-block px-4 py-1.5 rounded-full bg-gradient-to-r from-purple-600 to-pink-500 text-white text-sm font-bold uppercase tracking-wider mb-8">
          Coming Soon
        </div>
        
        <p className="text-muted-foreground text-lg max-w-2xl leading-relaxed">
          We are actively working on IOI Mode! This mode will introduce partial scoring based on subtasks. 
          Instead of simply claiming a square with an "Accepted" verdict, teams will battle for the highest 
          score on each square up to 100 points. 
        </p>
        
        <div className="mt-12">
          <Link 
            href="/compete/create"
            className="inline-flex items-center justify-center px-8 py-3 text-sm font-semibold rounded-2xl bg-foreground text-background hover:bg-foreground/90 transition-all shadow-md hover:shadow-lg"
          >
            Play Classic ICPC Mode
          </Link>
        </div>
      </main>
    </div>
  );
}
