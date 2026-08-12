"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Menu } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { ThemeToggle } from "@/components/site/theme-toggle";
import { navItems, site } from "@/lib/site";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";

function Wordmark({ className }: { className?: string }) {
  return (
    <Link
      href="/"
      className={cn(
        "flex items-baseline gap-2 rounded-control text-sm font-semibold tracking-tight whitespace-nowrap",
        "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring",
        className
      )}
    >
      <span>{site.name}</span>
      <span className="font-mono text-xs font-medium tracking-wide text-fg-muted">
        {site.suffix}
      </span>
    </Link>
  );
}

export function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { isAuthenticated, user, logout } = useAuthStore();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsMounted(true);
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const isActive = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  return (
    <header
      className={cn(
        "sticky top-0 z-50 border-b backdrop-blur-xl",
        scrolled ? "border-border" : "border-hairline"
      )}
      style={{ backgroundColor: "var(--nav-bg)" }}
    >
      <nav className="mx-auto flex min-h-14 max-w-[1240px] items-center gap-4 px-6 py-2">
        <Wordmark />

        {/* Desktop navigation. Below lg the links move into the sheet. */}
        <div className="hidden flex-1 items-center gap-0.5 lg:flex">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive(item.href) ? "page" : undefined}
              className={cn(
                "relative rounded-control px-2.5 py-2 font-mono text-[13px] tracking-[0.06em] uppercase whitespace-nowrap transition-colors",
                "hover:bg-surface-2 hover:text-foreground",
                "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                isActive(item.href) ? "text-foreground" : "text-fg-muted"
              )}
            >
              {item.label}
              {isActive(item.href) && (
                <span className="absolute inset-x-2.5 bottom-0.5 h-px bg-foreground" />
              )}
            </Link>
          ))}
        </div>

        <div className="ml-auto flex items-center gap-2">
          <ThemeToggle />

          {/* Desktop auth controls: swap Login/Join for user name + Logout */}
          <div className="hidden lg:flex items-center gap-2">
            {!isMounted ? (
              <div className="h-8 w-24 animate-pulse rounded-full bg-surface-2" />
            ) : isAuthenticated ? (
              <>
                <Link href={`/profile/${user?.id}`} className="font-mono text-[13px] tracking-[0.06em] text-fg-muted uppercase hover:text-foreground hover:underline transition-colors">
                  {user?.fullName}
                </Link>
                <Button
                  variant="ghost"
                  onClick={() => {
                    logout();
                    router.push("/login");
                  }}
                  className="h-8 rounded-full px-3 font-mono text-[13px] tracking-[0.06em] text-fg-muted uppercase inline-flex"
                >
                  Logout
                </Button>
              </>
            ) : (
              <>
                <Button
                  asChild
                  variant="ghost"
                  className="h-8 rounded-full px-3 font-mono text-[13px] tracking-[0.06em] text-fg-muted uppercase inline-flex"
                >
                  <Link href="/login">Login</Link>
                </Button>
                <Button
                  asChild
                  className="h-8 rounded-full px-4 font-mono text-[13px] tracking-[0.06em] uppercase inline-flex"
                >
                  <Link href="/register">Join</Link>
                </Button>
              </>
            )}
          </div>

          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild className="lg:hidden">
              <Button variant="outline" size="icon" aria-label="Open menu">
                <Menu />
              </Button>
            </SheetTrigger>
            {/* Needs the data-[side] prefix: sheet.tsx sets w-3/4 at that
                specificity, which a bare w-* class loses to. */}
            <SheetContent
              side="right"
              aria-describedby={undefined}
              className="gap-0 data-[side=right]:w-[min(20rem,85vw)]"
            >
              <SheetHeader className="border-b border-hairline px-6 py-4">
                <SheetTitle asChild>
                  {/* Also a link, so it must close the sheet like the rest —
                      the sheet lives in the layout and survives navigation. */}
                  <SheetClose asChild>
                    <Wordmark className="text-base" />
                  </SheetClose>
                </SheetTitle>
              </SheetHeader>

              <div className="flex flex-1 flex-col gap-1 overflow-y-auto p-4">
                {navItems.map((item) => (
                  <SheetClose asChild key={item.href}>
                    <Link
                      href={item.href}
                      aria-current={isActive(item.href) ? "page" : undefined}
                      className={cn(
                        "rounded-control px-3 py-3 font-mono text-sm tracking-[0.06em] uppercase transition-colors",
                        "hover:bg-surface-2 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                        isActive(item.href)
                          ? "bg-surface-2 text-foreground"
                          : "text-fg-muted"
                      )}
                    >
                      {item.label}
                    </Link>
                  </SheetClose>
                ))}
              </div>

              {/* Mobile auth controls: swap Login/Join for user name + Logout */}
              <div className="flex flex-col gap-2 border-t border-hairline p-4">
                {!isMounted ? (
                  <div className="flex flex-col gap-2">
                    <div className="h-10 w-full animate-pulse rounded-full bg-surface-2" />
                    <div className="h-10 w-full animate-pulse rounded-full bg-surface-2" />
                  </div>
                ) : isAuthenticated ? (
                  <>
                    <Link href={`/profile/${user?.id}`} className="px-3 py-2 text-center font-mono text-xs tracking-[0.06em] text-fg-muted uppercase hover:text-foreground hover:underline transition-colors block">
                      {user?.fullName}
                    </Link>
                    <SheetClose asChild>
                      <Button
                        variant="outline"
                        className="h-10 rounded-full"
                        onClick={() => {
                          logout();
                          router.push("/login");
                        }}
                      >
                        Logout
                      </Button>
                    </SheetClose>
                  </>
                ) : (
                  <>
                    <SheetClose asChild>
                      <Button asChild variant="outline" className="h-10 rounded-full">
                        <Link href="/login">Login</Link>
                      </Button>
                    </SheetClose>
                    <SheetClose asChild>
                      <Button asChild className="h-10 rounded-full">
                        <Link href="/register">Join the Club</Link>
                      </Button>
                    </SheetClose>
                  </>
                )}
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </nav>
    </header>
  );
}
