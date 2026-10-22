"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  FileText,
  PenSquare,
  Edit2,
  Trash2,
  Send,
  Eye,
  ArrowRight,
  ArrowLeft,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { useAuthStore } from "@/store/auth";
import type { BlogEditDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

export default function MyBlogsPage() {
  const { isAuthenticated } = useAuthStore();
  const [posts, setPosts] = useState<BlogEditDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  const loadPosts = async () => {
    try {
      const data = await blogService.mine();
      setPosts(data || []);
    } catch (err) {
      console.error("Failed to load your posts:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadPosts();
    } else {
      setLoading(false);
    }
  }, [isAuthenticated]);

  const handleSubmit = async (id: number) => {
    setActionLoadingId(id);
    try {
      await blogService.submit(id);
      await loadPosts();
    } catch (err) {
      console.error("Failed to submit post:", err);
      alert("Failed to submit post for review.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Permanently delete this post?")) return;
    try {
      await blogService.remove(id);
      setPosts((prev) => prev.filter((p) => p.id !== id));
    } catch (err) {
      console.error("Failed to delete post:", err);
      alert("Failed to delete post.");
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto max-w-md px-4 py-20 text-center space-y-4">
        <FileText className="size-10 text-primary mx-auto" />
        <h1 className="text-xl font-bold text-white">Sign In to View Your Articles</h1>
        <Link
          href="/login"
          className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-5 py-2 text-xs font-bold text-primary-foreground"
        >
          Sign In
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8 space-y-8">
      {/* Top Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
        <div>
          <Link
            href="/blog"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors mb-2"
          >
            <ArrowLeft className="size-3.5" />
            Back to Public Blog
          </Link>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white">
            My Articles ({posts.length})
          </h1>
        </div>

        <Link
          href="/blog/write"
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20"
        >
          <PenSquare className="size-4" />
          Write New Article
        </Link>
      </div>

      {loading ? (
        <div className="p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Loading your articles...
        </div>
      ) : posts.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center space-y-3">
          <FileText className="size-8 text-fg-subtle mx-auto" />
          <h3 className="font-semibold text-white text-base">You haven&apos;t written any posts yet</h3>
          <p className="text-xs text-fg-muted font-mono max-w-sm mx-auto">
            Share an editorial, algorithm explanation, or problem-solving guide with fellow club members.
          </p>
          <div className="pt-2">
            <Link
              href="/blog/write"
              className="inline-flex items-center gap-1.5 text-xs font-mono text-primary hover:underline"
            >
              Start Writing
              <ArrowRight className="size-3.5" />
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {posts.map((post) => {
            const isDraft = post.status === "DRAFT";
            const isPending = post.status === "PENDING_REVIEW";
            const isPublished = post.status === "PUBLISHED";
            const isRejected = post.status === "REJECTED";

            return (
              <div
                key={post.id}
                className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4 shadow-lg hover:border-white/20 transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      {isDraft && (
                        <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-white/10 text-fg-muted border border-white/15">
                          Draft
                        </span>
                      )}
                      {isPending && (
                        <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30">
                          Pending Review
                        </span>
                      )}
                      {isPublished && (
                        <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                          Published
                        </span>
                      )}
                      {isRejected && (
                        <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-rose-500/15 text-rose-300 border border-rose-500/30">
                          Changes Requested
                        </span>
                      )}

                      <span className="text-nano font-mono text-fg-subtle">
                        Updated {serverDate(post.updatedAt || post.createdAt).toLocaleDateString("en-IN")}
                      </span>
                    </div>

                    <h3 className="text-lg font-bold text-white tracking-tight">{post.title}</h3>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    {isPublished && (
                      <Link
                        href={`/blog/${post.slug}`}
                        className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-surface-2 px-3 py-1.5 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs"
                      >
                        <Eye className="size-3.5" />
                        View Live
                      </Link>
                    )}

                    {(isDraft || isRejected) && (
                      <>
                        <Link
                          href={`/blog/edit/${post.id}`}
                          className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-surface-2 px-3 py-1.5 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs"
                        >
                          <Edit2 className="size-3.5" />
                          Edit
                        </Link>

                        {isDraft && (
                          <button
                            type="button"
                            onClick={() => handleSubmit(post.id)}
                            disabled={actionLoadingId === post.id}
                            className="inline-flex items-center gap-1 rounded-xl bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground hover:bg-primary-hover transition-colors shadow-xs disabled:opacity-50"
                          >
                            <Send className="size-3.5" />
                            Submit
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => handleDelete(post.id)}
                          className="inline-flex items-center gap-1 rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-1.5 text-xs font-mono text-rose-400 hover:bg-rose-500/20 transition-colors shadow-xs"
                        >
                          <Trash2 className="size-3.5" />
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {isRejected && post.reviewNote && (
                  <div className="rounded-xl border border-rose-500/20 bg-rose-500/5 p-3 text-xs text-rose-300">
                    <strong className="font-mono uppercase tracking-caps text-nano block mb-0.5">
                      Reviewer Feedback:
                    </strong>
                    {post.reviewNote}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
