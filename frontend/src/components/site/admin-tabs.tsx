"use client";

import React from "react";
import { Users, Calendar, Image as ImageIcon, Trophy } from "lucide-react";
import { cn } from "@/lib/utils";

export type AdminTab = "members" | "events" | "hallOfFame" | "galleries";

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
  ];

  return (
    <div className={cn("flex flex-wrap gap-2 border-b border-border pb-4", className)}>
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            type="button"
            onClick={() => onChange(tab.id)}
            className={cn(
              "inline-flex items-center gap-2 rounded-control px-4 py-2 text-sm font-medium transition-all",
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
