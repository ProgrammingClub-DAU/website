"use client";

import React from "react";
import { Users, Calendar, Image as ImageIcon, Trophy, Award, RefreshCw, FileText } from "lucide-react";
import { cn } from "@/lib/utils";

export type AdminTab =
  | "members"
  | "events"
  | "hallOfFame"
  | "galleries"
  | "contests"
  | "awards"
  | "sync"
  | "blogs";

interface AdminTabsProps {
  activeTab: AdminTab;
  onChange: (tab: AdminTab) => void;
  className?: string;
}

export function AdminTabs({ activeTab, onChange, className }: AdminTabsProps) {
  const tabs: { id: AdminTab; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { id: "members", label: "Members", icon: Users },
    { id: "events", label: "Events", icon: Calendar },
    { id: "hallOfFame", label: "Hall of Fame", icon: Trophy },
    { id: "galleries", label: "Galleries", icon: ImageIcon },
    { id: "contests", label: "Contests & Seasons", icon: Trophy },
    { id: "awards", label: "Awards", icon: Award },
    { id: "sync", label: "Sync Health", icon: RefreshCw },
    { id: "blogs", label: "Blog Reviews", icon: FileText },
  ];

  // One scrolling row on phones -- eight wrapped tabs filled the first screen
  // before any admin content -- and wrapped rows from md up.
  return (
    <div
      role="tablist"
      aria-label="Admin sections"
      className={cn(
        "-mx-4 flex gap-2 overflow-x-auto border-b border-border px-4 pb-4 no-scrollbar md:mx-0 md:flex-wrap md:overflow-visible md:px-0",
        className
      )}
    >
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(tab.id)}
            className={cn(
              "inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-control px-4 py-2 text-sm font-medium transition-all",
              isActive
                ? "bg-primary text-primary-foreground shadow-sm"
                : "border border-border bg-surface-2 text-fg-muted hover:border-hairline-strong hover:text-foreground"
            )}
          >
            <Icon className="size-4" />
            <span>{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
}

export default AdminTabs;
