"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Command } from "cmdk";
import { Search, Trophy, Users, User, Code, LogIn, Swords } from "lucide-react";

export function CommandPalette() {
  const [open, setOpen] = React.useState(false);
  const router = useRouter();

  React.useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        setOpen((open) => !open);
      }
    };

    document.addEventListener("keydown", down);
    return () => document.removeEventListener("keydown", down);
  }, []);

  const runCommand = React.useCallback((command: () => void) => {
    setOpen(false);
    command();
  }, []);

  return (
    <Command.Dialog
      open={open}
      onOpenChange={setOpen}
      label="Global Command Menu"
      className="fixed inset-0 z-50 flex items-start justify-center pt-[15vh] sm:pt-[20vh] bg-background/80 backdrop-blur-sm"
    >
      <div className="relative w-full max-w-lg overflow-hidden rounded-xl border border-border bg-surface-2 shadow-2xl">
        <div className="flex items-center border-b border-border px-3">
          <Search className="mr-2 size-4 shrink-0 text-fg-muted" />
          <Command.Input
            placeholder="Type a command or search..."
            className="flex h-11 w-full rounded-md bg-transparent py-3 text-sm outline-none placeholder:text-fg-muted disabled:cursor-not-allowed disabled:opacity-50 text-foreground"
          />
        </div>
        <Command.List className="max-h-[300px] overflow-y-auto overflow-x-hidden p-1">
          <Command.Empty className="py-6 text-center text-sm text-fg-muted">
            No results found.
          </Command.Empty>
          <Command.Group heading="Navigation" className="p-2 text-xs font-medium text-fg-muted">
            <Command.Item
              onSelect={() => runCommand(() => router.push("/leaderboard"))}
              className="relative flex cursor-pointer select-none items-center rounded-sm px-2 py-2 text-sm outline-none data-[selected=true]:bg-surface-3 data-[selected=true]:text-foreground text-fg-subtle hover:bg-surface-3 hover:text-foreground"
            >
              <Trophy className="mr-2 size-4" />
              Leaderboard
            </Command.Item>
            <Command.Item
              onSelect={() => runCommand(() => router.push("/members"))}
              className="relative flex cursor-pointer select-none items-center rounded-sm px-2 py-2 text-sm outline-none data-[selected=true]:bg-surface-3 data-[selected=true]:text-foreground text-fg-subtle hover:bg-surface-3 hover:text-foreground"
            >
              <Users className="mr-2 size-4" />
              Members Directory
            </Command.Item>
            <Command.Item
              onSelect={() => runCommand(() => router.push("/compete"))}
              className="relative flex cursor-pointer select-none items-center rounded-sm px-2 py-2 text-sm outline-none data-[selected=true]:bg-surface-3 data-[selected=true]:text-foreground text-fg-subtle hover:bg-surface-3 hover:text-foreground"
            >
              <Swords className="mr-2 size-4" />
              Compete Matchmaking
            </Command.Item>
          </Command.Group>
          <Command.Group heading="Account" className="p-2 text-xs font-medium text-fg-muted border-t border-border mt-1 pt-2">
            <Command.Item
              onSelect={() => runCommand(() => router.push("/profile"))}
              className="relative flex cursor-pointer select-none items-center rounded-sm px-2 py-2 text-sm outline-none data-[selected=true]:bg-surface-3 data-[selected=true]:text-foreground text-fg-subtle hover:bg-surface-3 hover:text-foreground"
            >
              <User className="mr-2 size-4" />
              My Profile
            </Command.Item>
            <Command.Item
              onSelect={() => runCommand(() => router.push("/login"))}
              className="relative flex cursor-pointer select-none items-center rounded-sm px-2 py-2 text-sm outline-none data-[selected=true]:bg-surface-3 data-[selected=true]:text-foreground text-fg-subtle hover:bg-surface-3 hover:text-foreground"
            >
              <LogIn className="mr-2 size-4" />
              Sign In
            </Command.Item>
          </Command.Group>
        </Command.List>
      </div>
    </Command.Dialog>
  );
}

