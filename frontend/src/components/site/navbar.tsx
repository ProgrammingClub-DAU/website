"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Menu } from "lucide-react";

import { Button } from "@/components/ui/button";
import StarBorder from "@/components/site/star-border";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { ThemeToggle } from "@/components/site/theme-toggle";
import { GitHubMark } from "@/components/site/github-mark";
import { navItems, site, utilityLinks } from "@/lib/site";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";

/** Shared by the four star-bordered auth links, so the two pairs cannot drift. */
const NAV_STAR_INNER =
  "glass-control inline-flex h-8 items-center justify-center rounded-full text-xs font-medium tracking-wide whitespace-nowrap";

const SHEET_STAR_INNER =
  "glass-control flex h-10 w-full items-center justify-center rounded-full text-sm font-medium";

function Wordmark({ className }: { className?: string }) {
  return (
    <Link
      href="/"
      className={cn(
        "flex items-center gap-2.5 rounded-control text-sm font-semibold tracking-tight whitespace-nowrap transition-opacity hover:opacity-90",
        "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring",
        className
      )}
    >
      <Image
        src="/logo-mark.png"
        alt=""
        width={28}
        height={28}
        priority
        className="size-7 shrink-0 rounded-full ring-1 ring-border/40"
      />
      <span className="flex items-baseline gap-1.5">
        <span className="font-semibold text-foreground tracking-tight">{site.name}</span>
        <span className="rounded-md border border-hairline bg-surface-2/80 px-1.5 py-0.5 font-mono text-[11px] font-medium text-fg-muted">
          {site.suffix}
        </span>
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
        "sticky top-0 z-50 border-b backdrop-blur-md transition-colors duration-200",
        scrolled
          ? "border-border bg-background/85 shadow-xs"
          : "border-hairline bg-background/60"
      )}
      style={{ backgroundColor: "var(--nav-bg)" }}
    >
      <nav className="container-page flex h-16 items-center justify-between gap-4">
        <Wordmark />

        {/* Desktop navigation. Below lg the links move into the sheet. */}
        <div className="hidden items-center gap-1 lg:flex">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive(item.href) ? "page" : undefined}
              className={cn(
                "rounded-full px-3 py-1.5 text-[13px] font-medium transition-colors whitespace-nowrap",
                "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                isActive(item.href)
                  ? "bg-surface-2 text-foreground font-semibold shadow-xs"
                  : "text-fg-muted hover:bg-surface-2/60 hover:text-foreground"
              )}
            >
              {item.label}
            </Link>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <div className="hidden items-center gap-1 lg:flex">
            {utilityLinks.map((item) => (
              <a
                key={item.href}
                href={item.href}
                target="_blank"
                rel="noreferrer noopener"
                aria-label={item.label}
                title={item.label}
                className="inline-flex size-8 items-center justify-center rounded-full text-fg-muted transition-colors hover:bg-surface-2 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring"
              >
                <GitHubMark className="size-[18px]" />
              </a>
            ))}
          </div>

          <ThemeToggle />

          {/* Desktop auth controls: swap Login/Join for user name + Logout */}
          <div className="hidden lg:flex items-center gap-2">
            {!isMounted ? (
              <div className="h-8 w-24 animate-pulse rounded-full bg-surface-2" />
            ) : isAuthenticated ? (
              <>
                <Link
                  href={`/profile/${user?.id}`}
                  className="inline-flex items-center gap-2 rounded-full border border-hairline bg-surface-2/60 px-3 py-1 text-xs font-medium text-foreground transition-all hover:border-border hover:bg-surface-2"
                >
                  <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/20 text-[11px] font-bold text-primary">
                    {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "U"}
                  </span>
                  <span className="max-w-[130px] truncate">{user?.fullName || "Profile"}</span>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    logout();
                    router.push("/login");
                  }}
                  className="h-8 rounded-full px-2.5 text-xs font-medium text-fg-muted transition-colors hover:bg-destructive/10 hover:text-destructive"
                >
                  Logout
                </Button>
              </>
            ) : (
              <>
                <StarBorder
                  as={Link}
                  href="/login"
                  color="var(--cf-specialist)"
                  speed="7s"
                  className="rounded-full!"
                  innerClassName={NAV_STAR_INNER + " [--glass-fill:var(--glass-quiet)] px-3.5 text-fg-muted hover:text-foreground"}
                >
                  Login
                </StarBorder>
                <StarBorder
                  as={Link}
                  href="/register"
                  color="var(--cf-candidate)"
                  speed="6s"
                  className="rounded-full!"
                  innerClassName={NAV_STAR_INNER + " [--glass-fill:var(--glass-cta)] px-4 text-foreground"}
                >
                  Join
                </StarBorder>
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
                        "rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                        "hover:bg-surface-2 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                        isActive(item.href)
                          ? "bg-surface-2 text-foreground font-semibold"
                          : "text-fg-muted"
                      )}
                    >
                      {item.label}
                    </Link>
                  </SheetClose>
                ))}
                {utilityLinks.map((item) => (
                  <SheetClose asChild key={item.href}>
                    <a
                      href={item.href}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm font-medium text-fg-muted transition-colors hover:bg-surface-2 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring"
                    >
                      <GitHubMark className="size-4 shrink-0" />
                      {item.label}
                    </a>
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
                    <SheetClose asChild>
                      <Link
                        href={`/profile/${user?.id}`}
                        className="flex items-center justify-center gap-2 rounded-full border border-hairline bg-surface-2/60 px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-2"
                      >
                        <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/20 text-[11px] font-bold text-primary">
                          {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "U"}
                        </span>
                        <span>{user?.fullName}</span>
                      </Link>
                    </SheetClose>
                    <SheetClose asChild>
                      <Button
                        variant="outline"
                        className="h-10 rounded-full text-sm font-medium"
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
                      <StarBorder
                        as={Link}
                        href="/login"
                        color="var(--cf-specialist)"
                        speed="7s"
                        className="w-full rounded-full!"
                        innerClassName={SHEET_STAR_INNER + " [--glass-fill:var(--glass-quiet)] text-fg-muted"}
                      >
                        Login
                      </StarBorder>
                    </SheetClose>
                    <SheetClose asChild>
                      <StarBorder
                        as={Link}
                        href="/register"
                        color="var(--cf-candidate)"
                        speed="6s"
                        className="w-full rounded-full!"
                        innerClassName={SHEET_STAR_INNER + " [--glass-fill:var(--glass-cta)] text-foreground"}
                      >
                        Join the Club
                      </StarBorder>
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
