"use client";

import React, { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import {
  FileText,
  PenSquare,
  BookOpen,
  MessageSquare,
  Clock,
  Search,
  User,
  ArrowRight,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { useAuthStore } from "@/store/auth";
import type { BlogResponseDto } from "@/types/api";

export default function BlogPage() {
  const { isAuthenticated } = useAuthStore();
  const [blogs, setBlogs] = useState<BlogResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [selectedTag, setSelectedTag] = useState<string>("ALL");

  useEffect(() => {
    let ignore = false;
    blogService
      .getPublishedBlogs(0, 50)
      .then((res) => {
        if (!ignore) setBlogs(res?.content || []);
      })
      .catch((err) => {
        console.error("Failed to load blog posts:", err);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  // Extract all distinct tags
  const allTags = useMemo(() => {
    const set = new Set<string>();
    blogs.forEach((b) => {
      if (b.tags) {
        b.tags
          .split(",")
          .map((t) => t.trim().toLowerCase())
          .filter(Boolean)
          .forEach((t) => set.add(t));
      }
    });
    return Array.from(set);
  }, [blogs]);

  const filteredBlogs = useMemo(() => {
    return blogs.filter((b) => {
      const matchesSearch =
        !search.trim() ||
        b.title.toLowerCase().includes(search.toLowerCase()) ||
        b.authorName.toLowerCase().includes(search.toLowerCase()) ||
        b.content.toLowerCase().includes(search.toLowerCase());

      const matchesTag =
        selectedTag === "ALL" ||
        (b.tags &&
          b.tags
            .toLowerCase()
            .split(",")
            .map((t) => t.trim())
            .includes(selectedTag));

      return matchesSearch && matchesTag;
    });
  }, [blogs, search, selectedTag]);

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-10">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-3">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono uppercase tracking-caps text-primary">
            <BookOpen className="size-3.5" />
            Club Publications
          </div>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
            Editorials & Blog
          </h1>
          <p className="text-sm text-fg-muted max-w-2xl">
            Contest solution walk-throughs, algorithm breakdowns, and technical articles authored by DAU club members.
          </p>
        </div>

        {/* Action buttons */}
        <div className="flex flex-wrap items-center gap-2">
          {isAuthenticated && (
            <Link
              href="/blog/mine"
              className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-4 py-2.5 text-xs font-mono font-medium text-foreground hover:bg-surface-3 transition-colors shadow-xs"
            >
              My Articles
            </Link>
          )}
          <Link
            href="/blog/write"
            className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20"
          >
            <PenSquare className="size-4" />
            Write an Article
          </Link>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
        {/* Tag pills */}
        <div className="flex flex-wrap items-center gap-1.5">
          <button
            onClick={() => setSelectedTag("ALL")}
            className={`rounded-xl px-3 py-1 text-xs font-mono transition-all ${
              selectedTag === "ALL"
                ? "bg-primary text-primary-foreground font-semibold"
                : "border border-white/10 bg-white/[0.02] text-fg-muted hover:text-white"
            }`}
          >
            All
          </button>
          {allTags.map((tag) => (
            <button
              key={tag}
              onClick={() => setSelectedTag(tag)}
              className={`rounded-xl px-3 py-1 text-xs font-mono transition-all capitalize ${
                selectedTag === tag
                  ? "bg-primary text-primary-foreground font-semibold"
                  : "border border-white/10 bg-white/[0.02] text-fg-muted hover:text-white"
              }`}
            >
              {tag}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-fg-muted" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search articles..."
            className="w-full rounded-xl border border-white/10 bg-surface-2 pl-9 pr-3 py-1.5 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
          />
        </div>
      </div>

      {/* Articles Grid */}
      {loading ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Loading published articles...
        </div>
      ) : filteredBlogs.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center space-y-3">
          <FileText className="size-8 text-fg-subtle mx-auto" />
          <p className="text-sm font-semibold text-white">
            {search || selectedTag !== "ALL"
              ? "No articles match your search or filter."
              : "No blog posts published yet."}
          </p>
          <p className="text-xs text-fg-muted font-mono max-w-sm mx-auto">
            Be the first to share an editorial or tutorial with the club community.
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredBlogs.map((post) => {
            // Strip markdown formatting for a clean excerpt
            const plainText = post.content
              .replace(/#+\s/g, "")
              .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
              .replace(/[*_`]/g, "")
              .trim();
            const excerpt = plainText.slice(0, 140) + (plainText.length > 140 ? "..." : "");

            const dateStr = post.publishedAt
              ? new Date(post.publishedAt).toLocaleDateString("en-IN", {
                  month: "short",
                  day: "numeric",
                  year: "numeric",
                })
              : new Date(post.createdAt).toLocaleDateString("en-IN", {
                  month: "short",
                  day: "numeric",
                  year: "numeric",
                });

            return (
              <Link
                key={post.id}
                href={`/blog/${post.slug}`}
                className="group relative rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md hover:border-white/20 hover:bg-[#0d0d16] transition-all flex flex-col justify-between shadow-lg"
              >
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-1.5">
                    {post.tags &&
                      post.tags.split(",").slice(0, 2).map((tag) => (
                        <span
                          key={tag}
                          className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20 capitalize"
                        >
                          {tag.trim()}
                        </span>
                      ))}
                  </div>

                  <h3 className="text-lg font-bold text-white group-hover:text-primary transition-colors tracking-tight line-clamp-2">
                    {post.title}
                  </h3>

                  <p className="text-xs text-fg-muted line-clamp-3 leading-relaxed">
                    {excerpt}
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-white/5 flex items-center justify-between text-nano font-mono text-fg-subtle">
                  <div className="flex items-center gap-1.5 text-fg-muted">
                    <User className="size-3" />
                    <span>{post.authorName}</span>
                    <span>·</span>
                    <span>{dateStr}</span>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1">
                      <Clock className="size-3" />
                      {post.readMinutes}m read
                    </span>
                    <span className="flex items-center gap-1">
                      <MessageSquare className="size-3" />
                      {post.commentCount}
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
