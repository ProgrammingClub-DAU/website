"use client";

import React, { use, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Eye,
  Edit3,
  Send,
  Save,
  Trash2,
  ArrowLeft,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { MarkdownRenderer } from "@/components/site/markdown-renderer";
import type { BlogEditDto } from "@/types/api";

interface BlogEditPageProps {
  params: Promise<{ id: string }>;
}

export default function BlogEditPage({ params }: BlogEditPageProps) {
  const { id } = use(params);
  const router = useRouter();

  const [post, setPost] = useState<BlogEditDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [tags, setTags] = useState("");
  const [content, setContent] = useState("");
  const [activeTab, setActiveTab] = useState<"write" | "preview">("write");

  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    blogService
      .getForEditing(id)
      .then((data) => {
        if (!ignore) {
          setPost(data);
          setTitle(data.title);
          setTags(data.tags || "");
          setContent(data.content);
        }
      })
      .catch((err: unknown) => {
        console.error("Failed to load post for editing:", err);
        if (!ignore) setError("Failed to load post. You may not have permission to edit it.");
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [id]);

  const handleSave = async () => {
    if (!title.trim() || !content.trim() || isSaving) return;
    setIsSaving(true);
    setError(null);
    try {
      await blogService.update(id, {
        title: title.trim(),
        content: content.trim(),
        tags: tags.trim() || undefined,
      });
      router.push("/blog/mine");
    } catch (err) {
      console.error("Failed to update post:", err);
      setError("Failed to update post.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleResubmit = async () => {
    if (!title.trim() || !content.trim() || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await blogService.update(id, {
        title: title.trim(),
        content: content.trim(),
        tags: tags.trim() || undefined,
      });
      await blogService.submit(id);
      router.push("/blog/mine");
    } catch (err) {
      console.error("Failed to re-submit post:", err);
      setError("Failed to re-submit post for review.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to permanently delete this post?")) return;
    setIsDeleting(true);
    try {
      await blogService.remove(id);
      router.push("/blog/mine");
    } catch (err) {
      console.error("Failed to delete post:", err);
      alert("Failed to delete post.");
      setIsDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto max-w-4xl px-4 py-16 text-center font-mono text-xs text-fg-muted animate-pulse">
        Loading post for editing...
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="container mx-auto max-w-4xl px-4 py-16 text-center space-y-4">
        <p className="text-rose-400 font-mono text-xs">{error || "Post not found."}</p>
        <Link href="/blog/mine" className="text-xs font-mono text-primary hover:underline">
          Return to My Articles
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8 space-y-8">
      {/* Top Bar */}
      <div className="flex items-center justify-between border-b border-white/10 pb-4">
        <Link
          href="/blog/mine"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
        >
          <ArrowLeft className="size-3.5" />
          Back to My Articles
        </Link>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleDelete}
            disabled={isDeleting}
            className="inline-flex items-center gap-1.5 rounded-xl border border-rose-500/30 bg-rose-500/10 px-3.5 py-2 text-xs font-mono text-rose-400 hover:bg-rose-500/20 transition-colors shadow-xs disabled:opacity-50"
          >
            <Trash2 className="size-3.5" />
            Delete
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={isSaving || isSubmitting}
            className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3.5 py-2 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs disabled:opacity-50"
          >
            {isSaving ? <Loader2 className="size-3.5 animate-spin" /> : <Save className="size-3.5" />}
            Save Changes
          </button>
          <button
            type="button"
            onClick={handleResubmit}
            disabled={isSaving || isSubmitting}
            className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20 disabled:opacity-50"
          >
            {isSubmitting ? <Loader2 className="size-3.5 animate-spin" /> : <Send className="size-3.5" />}
            Submit for Review
          </button>
        </div>
      </div>

      {/* Rejection Review Note Banner */}
      {post.status === "REJECTED" && post.reviewNote && (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5 space-y-2">
          <div className="flex items-center gap-2 text-rose-300 font-bold text-xs uppercase tracking-caps font-mono">
            <AlertCircle className="size-4 text-rose-400" />
            Reviewer Feedback (Changes Requested)
          </div>
          <p className="text-sm text-foreground leading-relaxed pl-6">
            {post.reviewNote}
          </p>
        </div>
      )}

      {error && (
        <div className="rounded-xl bg-rose-500/10 border border-rose-500/20 p-3 text-xs text-rose-400">
          {error}
        </div>
      )}

      {/* Inputs */}
      <div className="space-y-4">
        <div>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Article Title..."
            className="w-full bg-transparent text-2xl sm:text-3xl font-black text-white placeholder:text-white/20 focus:outline-none tracking-tight border-b border-white/10 pb-3"
          />
        </div>

        <div>
          <input
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="Tags (comma-separated, e.g. dp, graph, editorial)"
            className="w-full rounded-xl border border-white/10 bg-surface-2 px-4 py-2 text-xs font-mono text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
          />
        </div>
      </div>

      {/* Editor & Preview Tabs */}
      <div className="space-y-3">
        <div className="flex items-center justify-between border-b border-white/10 pb-2">
          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => setActiveTab("write")}
              className={`pb-2 text-xs font-mono font-semibold transition-colors flex items-center gap-1.5 ${
                activeTab === "write" ? "text-primary border-b-2 border-primary" : "text-fg-muted hover:text-white"
              }`}
            >
              <Edit3 className="size-3.5" />
              Write (Markdown)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("preview")}
              className={`pb-2 text-xs font-mono font-semibold transition-colors flex items-center gap-1.5 ${
                activeTab === "preview" ? "text-primary border-b-2 border-primary" : "text-fg-muted hover:text-white"
              }`}
            >
              <Eye className="size-3.5" />
              Preview
            </button>
          </div>
        </div>

        {activeTab === "write" ? (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4 backdrop-blur-md">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={22}
              className="w-full bg-transparent text-sm text-foreground placeholder:text-fg-subtle focus:outline-none resize-y font-mono leading-relaxed"
            />
          </div>
        ) : (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-6 sm:p-8 backdrop-blur-md min-h-[400px]">
            {content.trim() ? (
              <MarkdownRenderer content={content} />
            ) : (
              <p className="text-xs font-mono text-fg-subtle italic text-center py-12">
                Nothing to preview yet.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
