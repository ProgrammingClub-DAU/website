"use client";

import React, { useState, useEffect } from "react";
import {
  FileText,
  CheckCircle2,
  XCircle,
  Eye,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { blogService } from "@/lib/services/blog";
import { MarkdownRenderer } from "@/components/site/markdown-renderer";
import type { BlogEditDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

export function BlogReviewManager() {
  const [queue, setQueue] = useState<BlogEditDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPost, setSelectedPost] = useState<BlogEditDto | null>(null);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [rejectingPost, setRejectingPost] = useState<BlogEditDto | null>(null);
  const [rejectNote, setRejectNote] = useState("");
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const loadQueue = async () => {
    try {
      const res = await blogService.reviewQueue();
      setQueue(res || []);
    } catch (err) {
      console.error("Failed to load review queue:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQueue();
  }, []);

  const handleApprove = async (id: number) => {
    setActionLoadingId(id);
    setStatusMessage(null);
    try {
      await blogService.publish(id);
      setStatusMessage("Post approved and published successfully.");
      setSelectedPost(null);
      await loadQueue();
    } catch (err) {
      console.error("Failed to publish post:", err);
      alert("Failed to publish post.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingPost || !rejectNote.trim()) return;

    setActionLoadingId(rejectingPost.id);
    try {
      await blogService.reject(rejectingPost.id, rejectNote.trim());
      setStatusMessage("Post rejected with feedback note.");
      setRejectingPost(null);
      setRejectNote("");
      setSelectedPost(null);
      await loadQueue();
    } catch (err) {
      console.error("Failed to reject post:", err);
      alert("Failed to reject post.");
    } finally {
      setActionLoadingId(null);
    }
  };

  if (loading) {
    return (
      <div className="p-12 text-center font-mono text-xs text-fg-muted animate-pulse">
        Loading blog review queue...
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <FileText className="size-4 text-primary" />
            Editorial Review Queue
          </h3>
          <p className="text-xs text-fg-muted mt-0.5">
            Review member-submitted contest writeups, tutorials, and articles before publication.
          </p>
        </div>
        <span className="text-xs font-mono px-3 py-1 rounded-full bg-primary/10 text-primary border border-primary/30">
          {queue.length} pending review
        </span>
      </div>

      {statusMessage && (
        <div className="rounded-xl bg-white/5 border border-white/10 p-3 text-xs font-mono text-white flex items-center gap-2">
          <CheckCircle2 className="size-4 text-emerald-400 shrink-0" />
          {statusMessage}
        </div>
      )}

      {/* Review Queue List */}
      {queue.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-xs text-fg-muted font-mono">
          The review queue is completely clear! No articles waiting for review.
        </div>
      ) : (
        <div className="space-y-4">
          {queue.map((post) => {
            const isActing = actionLoadingId === post.id;
            const isPreviewing = selectedPost?.id === post.id;

            return (
              <div
                key={post.id}
                className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4 shadow-lg transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-3">
                  <div>
                    <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30">
                      Pending Review
                    </span>
                    <h4 className="text-base font-bold text-white mt-1.5">{post.title}</h4>
                    <div className="flex flex-wrap items-center gap-4 mt-1 text-micro font-mono text-fg-muted">
                      <span>Author: {post.authorName}</span>
                      <span>Tags: {post.tags || "None"}</span>
                      <span>
                        Submitted:{" "}
                        {post.submittedAt
                          ? serverDate(post.submittedAt).toLocaleDateString("en-IN")
                          : serverDate(post.createdAt).toLocaleDateString("en-IN")}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setSelectedPost(isPreviewing ? null : post)}
                      className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-surface-2 px-3 py-1.5 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs"
                    >
                      <Eye className="size-3.5" />
                      {isPreviewing ? "Close Preview" : "Preview"}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleApprove(post.id)}
                      disabled={isActing}
                      className="inline-flex items-center gap-1 rounded-xl bg-emerald-500 px-3.5 py-1.5 text-xs font-semibold text-black hover:bg-emerald-400 transition-colors shadow-xs disabled:opacity-50"
                    >
                      {isActing ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        <CheckCircle2 className="size-3.5" />
                      )}
                      Approve & Publish
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setRejectingPost(post);
                        setRejectNote("");
                      }}
                      disabled={isActing}
                      className="inline-flex items-center gap-1 rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-1.5 text-xs font-semibold text-rose-400 hover:bg-rose-500/20 transition-colors shadow-xs disabled:opacity-50"
                    >
                      <XCircle className="size-3.5" />
                      Reject
                    </button>
                  </div>
                </div>

                {/* Expanded Markdown Preview */}
                {isPreviewing && (
                  <div className="rounded-xl border border-white/10 bg-[#08080d] p-6 max-h-[500px] overflow-y-auto space-y-4">
                    <div className="text-xs font-mono text-fg-subtle border-b border-white/5 pb-2">
                      Full Article Markdown Preview:
                    </div>
                    <MarkdownRenderer content={post.content} />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Reject Note Dialog Modal */}
      {rejectingPost && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="w-full max-w-md rounded-2xl border border-white/10 bg-[#0d0d16] p-6 shadow-2xl space-y-4">
            <div>
              <h4 className="text-base font-bold text-white flex items-center gap-2">
                <AlertCircle className="size-4 text-rose-400" />
                Reject Post: &quot;{rejectingPost.title}&quot;
              </h4>
              <p className="text-xs text-fg-muted mt-1">
                Provide feedback to the author explaining what needs to be revised before publication.
              </p>
            </div>

            <form onSubmit={handleReject} className="space-y-4">
              <textarea
                value={rejectNote}
                onChange={(e) => setRejectNote(e.target.value)}
                placeholder="Explain the required revisions (e.g. Needs problem code explanation, formatting fixes...)"
                rows={4}
                required
                className="w-full rounded-xl border border-white/10 bg-surface-2 p-3 text-xs text-foreground placeholder:text-fg-subtle focus:border-rose-500/50 focus:outline-none"
              />

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectingPost(null)}
                  className="rounded-xl px-4 py-2 text-xs text-fg-muted hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoadingId === rejectingPost.id || !rejectNote.trim()}
                  className="rounded-xl bg-rose-500 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-600 disabled:opacity-50"
                >
                  {actionLoadingId === rejectingPost.id ? "Rejecting..." : "Send Rejection Note"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
