"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";

export default function CompeteLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsMounted(true);
  }, []);

  useEffect(() => {
    if (isMounted && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isMounted, isAuthenticated, router]);

  // Prevent flashing the compete UI before redirecting unauthenticated users
  if (!isMounted || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-fg-muted font-mono text-sm tracking-wider uppercase">
        Checking authentication...
      </div>
    );
  }

  return <>{children}</>;
}
