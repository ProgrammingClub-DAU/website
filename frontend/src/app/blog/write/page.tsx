"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  PenSquare,
  Eye,
  Edit3,
  Send,
  Save,
  ArrowLeft,
  Loader2,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { MarkdownRenderer } from "@/components/site/markdown-renderer";
import { useAuthStore } from "@/store/auth";
import { apiErrorMessage } from "@/lib/api-error";

/** Column limits on blog_posts; checked here so the member hears about them before saving. */
const TITLE_MIN = 3;
const TITLE_MAX = 255;
const TAGS_MAX = 255;

export default function BlogWritePage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  const [title, setTitle] = useState("");
  const [tags, setTags] = useState("");
  const [content, setContent] = useState("");
  const [activeTab, setActiveTab] = useState<"write" | "preview">("write");

  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Set once the post exists on the server. A retry after a failed submit used to
  // create the post again, leaving a duplicate draft behind for every attempt.
  const [draftId, setDraftId] = useState<number | null>(null);

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto max-w-2xl px-4 py-20 text-center space-y-4">
        <PenSquare className="size-12 text-primary mx-auto" />
        <h1 className="text-2xl font-bold text-white">Sign In to Write</h1>
        <p className="text-xs text-fg-muted font-mono">
          Only signed-in DAU club members can draft and submit articles for review.
        </p>
        <Link
          href="/login"
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover shadow-lg shadow-primary/20"
        >
          Sign In with DAU Account
        </Link>
      </div>
    );
  }

  const validate = () => {
    const t = title.trim();
    if (!t) {
      setError("Article title is required.");
      return false;
    }
    if (t.length < TITLE_MIN || t.length > TITLE_MAX) {
      setError(`The title must be between ${TITLE_MIN} and ${TITLE_MAX} characters.`);
      return false;
    }
    if (tags.trim().length > TAGS_MAX) {
      setError(`Tags can be at most ${TAGS_MAX} characters in total.`);
      return false;
    }
    if (!content.trim()) {
      setError("Article content cannot be empty.");
      return false;
    }
    return true;
  };

  /** Creates the draft the first time, updates it after that. Returns its id. */
  const saveDraft = async (): Promise<number> => {
    const fields = { title: title.trim(), content: content.trim(), tags: tags.trim() || undefined };
    if (draftId != null) {
      await blogService.update(draftId, fields);
      return draftId;
    }
    const created = await blogService.create({ ...fields, published: false });
    setDraftId(created.id);
    return created.id;
  };

  const handleSaveDraft = async () => {
    if (!validate() || isSaving) return;
    setIsSaving(true);
    setError(null);
    try {
      await saveDraft();
      router.push("/blog/mine");
    } catch (err) {
      console.error("Failed to save draft:", err);
      setError(apiErrorMessage(err, "Failed to save the draft. Please try again."));
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmitForReview = async () => {
    if (!validate() || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    let savedId: number | null = null;
    try {
      savedId = await saveDraft();
      await blogService.submit(savedId);
      router.push("/blog/mine");
    } catch (err) {
      console.error("Failed to submit for review:", err);
      const reason = apiErrorMessage(err, "Please try again.");
      setError(
        savedId != null
          ? `Your post is saved as a draft, but submitting it for review failed: ${reason}`
          : `Could not save your post: ${reason}`
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8 space-y-8">
      {/* Top Bar */}
      <div className="flex items-center justify-between border-b border-white/10 pb-4">
        <Link
          href="/blog"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-fg-muted hover:text-white transition-colors"
        >
          <ArrowLeft className="size-3.5" />
          Cancel & Exit
        </Link>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleSaveDraft}
            disabled={isSaving || isSubmitting}
            className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3.5 py-2 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs disabled:opacity-50"
          >
            {isSaving ? <Loader2 className="size-3.5 animate-spin" /> : <Save className="size-3.5" />}
            Save Draft
          </button>
          <button
            type="button"
            onClick={handleSubmitForReview}
            disabled={isSaving || isSubmitting}
            className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20 disabled:opacity-50"
          >
            {isSubmitting ? <Loader2 className="size-3.5 animate-spin" /> : <Send className="size-3.5" />}
            Submit for Review
          </button>
        </div>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-rose-500/10 border border-rose-500/20 p-3 text-xs text-rose-400">
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
            maxLength={TITLE_MAX}
            aria-label="Article title"
            placeholder="Article Title..."
            className="w-full bg-transparent text-2xl sm:text-3xl font-black text-white placeholder:text-white/20 focus:outline-none tracking-tight border-b border-white/10 pb-3"
          />
        </div>

        <div>
          <input
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            maxLength={TAGS_MAX}
            aria-label="Tags"
            placeholder="Tags (comma-separated, e.g. dp, graph, editorial, tutorial)"
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

          <div className="text-[11px] font-mono text-fg-subtle hidden sm:flex items-center gap-2">
            <span>Supports KaTeX: $O(N \log N)$ and $$...$$</span>
          </div>
        </div>

        {activeTab === "write" ? (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4 backdrop-blur-md">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Write your article in GitHub Flavored Markdown...&#10;&#10;## Problem Breakdown&#10;Explain the problem and observations here...&#10;&#10;$$\sum_{i=1}^n i = \frac{n(n+1)}{2}$$&#10;&#10;```cpp&#10;// Codeforces solution&#10;```"
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
                Nothing to preview yet. Switch to the Write tab and start drafting!
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
