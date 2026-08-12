"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";

export default function ProfileRedirectPage() {
  const { user, isAuthenticated } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    // Only redirect once we have a definitive auth state
    if (!isAuthenticated) {
      router.replace("/login");
    } else if (user?.id) {
      router.replace(`/profile/${user.id}`);
    }
  }, [isAuthenticated, user, router]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] text-fg-muted font-mono text-sm tracking-wider uppercase">
      Redirecting to profile...
    </div>
  );
}
