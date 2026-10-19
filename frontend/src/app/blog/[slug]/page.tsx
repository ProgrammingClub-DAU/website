"use client";

import React, { use, useEffect, useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Calendar,
  Clock,
  User,
  Share2,
  Check,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { MarkdownRenderer } from "@/components/site/markdown-renderer";
import { CommentThread } from "@/components/site/comment-thread";
import type { BlogResponseDto } from "@/types/api";

interface BlogDetailPageProps {
  params: Promise<{ slug: string }>;
}

export default function BlogDetailPage({ params }: BlogDetailPageProps) {
  const { slug } = use(params);
  const [post, setPost] = useState<BlogResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);

  useEffect(() => {
    let ignore = false;
    blogService
      .getBlogBySlug(slug)
      .then((data) => {
        if (!ignore) setPost(data);
      })
      .catch((err: unknown) => {
        console.error("Failed to load blog post:", err);
        if (!ignore) setError("The requested article could not be found.");
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [slug]);

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  if (loading) {
    return (
      <div className="container mx-auto max-w-4xl px-4 py-16 text-center font-mono text-xs text-fg-muted animate-pulse">
        Loading article...
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="container mx-auto max-w-4xl px-4 py-16 text-center space-y-4">
        <p className="text-rose-400 font-mono text-xs">{error || "Article not found."}</p>
        <Link
          href="/blog"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-primary hover:underline"
        >
          <ArrowLeft className="size-3.5" />
          Back to Blog
        </Link>
      </div>
    );
  }

  const dateStr = post.publishedAt
    ? new Date(post.publishedAt).toLocaleDateString("en-IN", {
        month: "long",
        day: "numeric",
        year: "numeric",
      })
    : new Date(post.createdAt).toLocaleDateString("en-IN", {
        month: "long",
        day: "numeric",
        year: "numeric",
      });

  const tags = post.tags
    ? post.tags
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean)
    : [];

  return (
    <article className="container mx-auto max-w-4xl px-4 py-8 space-y-10">
      {/* Top Bar */}
      <div className="flex items-center justify-between border-b border-white/10 pb-4">
        <Link
          href="/blog"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
        >
          <ArrowLeft className="size-3.5" />
          Back to Articles
        </Link>

        <button
          type="button"
          onClick={handleShare}
          className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
        >
          {copiedLink ? <Check className="size-3.5 text-emerald-400" /> : <Share2 className="size-3.5" />}
          {copiedLink ? "Link Copied" : "Share"}
        </button>
      </div>

      {/* Article Header */}
      <header className="space-y-4">
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {tags.map((t) => (
              <span
                key={t}
                className="text-nano font-mono px-2.5 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20 capitalize"
              >
                {t}
              </span>
            ))}
          </div>
        )}

        <h1 className="text-3xl sm:text-4xl md:text-5xl font-black tracking-tight text-white leading-tight">
          {post.title}
        </h1>

        <div className="flex flex-wrap items-center gap-4 text-xs font-mono text-fg-muted pt-2 border-t border-white/5">
          <Link
            href={post.authorId ? `/profile/${post.authorId}` : "#"}
            className="flex items-center gap-1.5 text-white hover:text-primary transition-colors font-semibold"
          >
            <User className="size-3.5 text-primary" />
            {post.authorName}
          </Link>
          <span className="flex items-center gap-1">
            <Calendar className="size-3.5" />
            {dateStr}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="size-3.5" />
            {post.readMinutes} min read
          </span>
        </div>
      </header>

      {/* Article Body */}
      <div className="rounded-3xl border border-white/10 bg-[#0d0d16]/80 p-6 sm:p-10 backdrop-blur-md shadow-2xl">
        <MarkdownRenderer content={post.content} />
      </div>

      {/* Author Card */}
      <div className="rounded-2xl border border-white/10 bg-white/[0.02] p-5 flex items-center justify-between gap-4">
        <div className="space-y-0.5">
          <div className="text-nano font-mono uppercase tracking-caps text-fg-subtle">
            Published by
          </div>
          <div className="font-semibold text-white text-sm">{post.authorName}</div>
          <div className="text-nano text-fg-muted">Programming Club Member @ DAU</div>
        </div>
        {post.authorId && (
          <Link
            href={`/profile/${post.authorId}`}
            className="rounded-xl border border-white/10 bg-surface-2 px-3.5 py-1.5 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs"
          >
            View Profile
          </Link>
        )}
      </div>

      {/* Comment Thread */}
      <CommentThread postId={post.id} />
    </article>
  );
}
