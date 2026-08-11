"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Menu, LogIn, User as UserIcon } from "lucide-react";

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
import { navItems } from "@/lib/site";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";

function BrandLogo({ className }: { className?: string }) {
  return (
    <Link
      href="/"
      className={cn(
        "flex items-center gap-2.5 rounded-control text-sm font-bold tracking-tight whitespace-nowrap transition-opacity hover:opacity-90",
        "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring",
        className
      )}
    >
      <Image
        src="/programming-club-logo.jpg"
        alt="Programming Club Logo"
        width={32}
        height={32}
        className="size-8 rounded-full object-cover ring-1 ring-border"
      />
      <span className="font-sans text-sm font-semibold tracking-tight text-foreground">
        Programming Club
      </span>
    </Link>
  );
}

export function Navbar() {
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
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
        "sticky top-0 z-50 border-b backdrop-blur-xl transition-all",
        scrolled ? "border-border bg-background/90 shadow-xs" : "border-hairline bg-background/75"
      )}
    >
      <nav className="mx-auto flex h-16 max-w-[1240px] items-center justify-between px-4 sm:px-6">
        {/* Left: Brand Logo & Title */}
        <div className="flex items-center gap-8">
          <BrandLogo />

          {/* Desktop Navigation Links */}
          <div className="hidden items-center gap-1 lg:flex">
            {navItems.map((item) => {
              const active = isActive(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "relative px-3 py-2 text-xs font-medium tracking-wide uppercase transition-colors rounded-md",
                    "hover:text-foreground hover:bg-surface-2/60",
                    "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                    active ? "text-foreground font-semibold" : "text-fg-muted"
                  )}
                >
                  {item.label}
                  {active && (
                    <span className="absolute inset-x-3 -bottom-3.5 h-0.5 rounded-full bg-primary" />
                  )}
                </Link>
              );
            })}
          </div>
        </div>

        {/* Right Actions: Theme Toggle, Login/Profile, Join */}
        <div className="flex items-center gap-3">
          <ThemeToggle />

          {/* Login or Profile button */}
          {isAuthenticated ? (
            <Button
              asChild
              variant="ghost"
              className={cn(
                "hidden h-9 px-3.5 text-xs font-semibold uppercase tracking-wider text-fg-muted hover:text-foreground lg:inline-flex",
                pathname.startsWith("/profile") && "text-foreground bg-surface-2"
              )}
            >
              <Link href="/profile" className="flex items-center gap-1.5">
                <UserIcon className="size-3.5" />
                Profile
              </Link>
            </Button>
          ) : (
            <Button
              asChild
              variant="ghost"
              className={cn(
                "hidden h-9 px-3.5 text-xs font-semibold uppercase tracking-wider text-fg-muted hover:text-foreground lg:inline-flex",
                pathname.startsWith("/login") && "text-foreground bg-surface-2"
              )}
            >
              <Link href="/login" className="flex items-center gap-1.5">
                <LogIn className="size-3.5" />
                Login
              </Link>
            </Button>
          )}

          {/* Join Button */}
          <Button
            asChild
            className="hidden h-9 rounded-full px-5 text-xs font-bold uppercase tracking-wider shadow-sm transition-all hover:shadow-primary/20 lg:inline-flex"
          >
            <Link href="/register">Join</Link>
          </Button>

          {/* Mobile Sheet Trigger */}
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild className="lg:hidden">
              <Button variant="ghost" size="icon" aria-label="Open menu" className="size-9">
                <Menu className="size-5" />
              </Button>
            </SheetTrigger>
            <SheetContent
              side="right"
              aria-describedby={undefined}
              className="flex flex-col gap-0 border-l border-border bg-surface-1 p-0 data-[side=right]:w-[min(20rem,85vw)]"
            >
              <SheetHeader className="border-b border-hairline px-5 py-4">
                <SheetTitle asChild>
                  <SheetClose asChild>
                    <BrandLogo />
                  </SheetClose>
                </SheetTitle>
              </SheetHeader>

              <div className="flex flex-1 flex-col gap-1 overflow-y-auto px-3 py-4">
                {navItems.map((item) => (
                  <SheetClose asChild key={item.href}>
                    <Link
                      href={item.href}
                      aria-current={isActive(item.href) ? "page" : undefined}
                      className={cn(
                        "flex items-center justify-between rounded-lg px-3 py-2.5 text-xs font-medium tracking-wider uppercase transition-colors",
                        "hover:bg-surface-2 focus-visible:outline-2 focus-visible:outline-ring",
                        isActive(item.href)
                          ? "bg-surface-2 font-bold text-primary"
                          : "text-fg-muted"
                      )}
                    >
                      {item.label}
                    </Link>
                  </SheetClose>
                ))}
              </div>

              <div className="flex flex-col gap-2.5 border-t border-hairline p-4 bg-surface-2/30">
                <SheetClose asChild>
                  {isAuthenticated ? (
                    <Button asChild variant="outline" className="h-10 w-full rounded-full text-xs uppercase tracking-wider">
                      <Link href="/profile">Profile</Link>
                    </Button>
                  ) : (
                    <Button asChild variant="outline" className="h-10 w-full rounded-full text-xs uppercase tracking-wider">
                      <Link href="/login">Login</Link>
                    </Button>
                  )}
                </SheetClose>
                <SheetClose asChild>
                  <Button asChild className="h-10 w-full rounded-full text-xs font-bold uppercase tracking-wider">
                    <Link href="/register">Join</Link>
                  </Button>
                </SheetClose>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </nav>
    </header>
  );
}
