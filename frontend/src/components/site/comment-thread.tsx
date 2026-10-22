"use client";

import React, { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  MessageSquare,
  Send,
  Reply,
  Edit2,
  Trash2,
  CornerDownRight,
  Loader2,
} from "lucide-react";
import { commentsService } from "@/lib/services/comments";
import { apiErrorMessage } from "@/lib/api-error";
import { useAuthStore } from "@/store/auth";
import type { CommentDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

interface CommentThreadProps {
  postId: number | string;
}

export function CommentThread({ postId }: CommentThreadProps) {
  const { isAuthenticated } = useAuthStore();
  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(true);

  // Forms State
  const [newComment, setNewComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [replyingToId, setReplyingToId] = useState<number | null>(null);
  const [replyText, setReplyText] = useState("");
  const [isReplying, setIsReplying] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editText, setEditText] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const loadComments = useCallback(async () => {
    try {
      const res = await commentsService.list(postId);
      setComments(res || []);
    } catch (err) {
      console.error("Failed to load comments:", err);
    } finally {
      setLoading(false);
    }
  }, [postId]);

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  // Rate limit countdown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const handlePostTopComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim() || isSubmitting || cooldown > 0) return;

    setIsSubmitting(true);
    try {
      await commentsService.create(postId, { body: newComment.trim() });
      setNewComment("");
      setCooldown(20); // 20s rate-limit
      await loadComments();
    } catch (err) {
      console.error("Failed to post comment:", err);
      alert(`Failed to post comment: ${apiErrorMessage(err, "Please try again.")}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePostReply = async (parentId: number) => {
    if (!replyText.trim() || isReplying || cooldown > 0) return;

    setIsReplying(true);
    try {
      await commentsService.create(postId, { body: replyText.trim(), parentId });
      setReplyText("");
      setReplyingToId(null);
      setCooldown(20);
      await loadComments();
    } catch (err) {
      console.error("Failed to post reply:", err);
      alert(`Failed to post reply: ${apiErrorMessage(err, "Please try again.")}`);
    } finally {
      setIsReplying(false);
    }
  };

  const handleSaveEdit = async (commentId: number) => {
    if (!editText.trim() || isEditing) return;

    setIsEditing(true);
    try {
      await commentsService.update(commentId, editText.trim());
      setEditingId(null);
      setEditText("");
      await loadComments();
    } catch (err) {
      console.error("Failed to edit comment:", err);
      alert("Edit window (15 minutes) may have expired.");
    } finally {
      setIsEditing(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    if (!window.confirm("Delete this comment?")) return;
    try {
      await commentsService.remove(commentId);
      await loadComments();
    } catch (err) {
      console.error("Failed to delete comment:", err);
      alert(`Failed to delete comment: ${apiErrorMessage(err, "Please try again.")}`);
    }
  };

  return (
    <div className="space-y-8 pt-8 border-t border-white/10">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-white flex items-center gap-2">
          <MessageSquare className="size-5 text-primary" />
          Discussion ({comments.length})
        </h3>
      </div>

      {/* New Top-Level Comment Form */}
      {isAuthenticated ? (
        <form onSubmit={handlePostTopComment} className="space-y-3">
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4 backdrop-blur-md">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Join the discussion... (Markdown supported)"
              rows={3}
              className="w-full bg-transparent text-sm text-foreground placeholder:text-fg-subtle focus:outline-none resize-none"
            />
            <div className="flex items-center justify-between pt-3 border-t border-white/5">
              <span className="text-nano font-mono text-fg-subtle">
                {cooldown > 0 ? `Rate limit: wait ${cooldown}s` : "Plain text or Markdown"}
              </span>
              <button
                type="submit"
                disabled={!newComment.trim() || isSubmitting || cooldown > 0}
                className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-1.5 text-xs font-semibold text-primary-foreground hover:bg-primary-hover transition-colors shadow-xs disabled:opacity-50"
              >
                {isSubmitting ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <Send className="size-3.5" />
                )}
                {isSubmitting ? "Posting..." : "Comment"}
              </button>
            </div>
          </div>
        </form>
      ) : (
        <div className="rounded-2xl border border-white/10 bg-white/[0.02] p-6 text-center text-xs text-fg-muted">
          <Link href="/login" className="text-primary hover:underline font-semibold">
            Sign in with your DAU account
          </Link>{" "}
          to post comments and join the discussion.
        </div>
      )}

      {/* Comments List */}
      {loading ? (
        <div className="p-8 text-center font-mono text-xs text-fg-muted animate-pulse">
          Loading comments...
        </div>
      ) : comments.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/50 p-8 text-center text-xs text-fg-muted font-mono">
          No comments yet. Be the first to start the discussion!
        </div>
      ) : (
        <div className="space-y-6">
          {comments.map((comment) => (
            <CommentCard
              key={comment.id}
              comment={comment}
              onReply={(parentId) => {
                setReplyingToId(parentId);
                setReplyText("");
              }}
              onStartEdit={(c) => {
                setEditingId(c.id);
                setEditText(c.body || "");
              }}
              onDelete={handleDelete}
              replyingToId={replyingToId}
              replyText={replyText}
              setReplyText={setReplyText}
              onPostReply={handlePostReply}
              onCancelReply={() => setReplyingToId(null)}
              isReplying={isReplying}
              editingId={editingId}
              editText={editText}
              setEditText={setEditText}
              onSaveEdit={handleSaveEdit}
              onCancelEdit={() => setEditingId(null)}
              isEditing={isEditing}
              cooldown={cooldown}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface CommentCardProps {
  comment: CommentDto;
  onReply: (id: number) => void;
  onStartEdit: (c: CommentDto) => void;
  onDelete: (id: number) => void;
  replyingToId: number | null;
  replyText: string;
  setReplyText: (text: string) => void;
  onPostReply: (parentId: number) => void;
  onCancelReply: () => void;
  isReplying: boolean;
  editingId: number | null;
  editText: string;
  setEditText: (text: string) => void;
  onSaveEdit: (id: number) => void;
  onCancelEdit: () => void;
  isEditing: boolean;
  cooldown: number;
}

function CommentCard({
  comment,
  onReply,
  onStartEdit,
  onDelete,
  replyingToId,
  replyText,
  setReplyText,
  onPostReply,
  onCancelReply,
  isReplying,
  editingId,
  editText,
  setEditText,
  onSaveEdit,
  onCancelEdit,
  isEditing,
  cooldown,
}: CommentCardProps) {
  const isCurrentlyEditing = editingId === comment.id;
  const isReplyingToThis = replyingToId === comment.id;

  return (
    <div className="space-y-4">
      {/* Top Comment Body */}
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4 backdrop-blur-md space-y-3">
        <div className="flex items-center justify-between">
          <Link href={`/profile/${comment.author.id}`} className="flex items-center gap-2.5 group">
            <div className="size-8 rounded-full overflow-hidden bg-surface-3 border border-white/10 shrink-0">
              {comment.author.avatarUrl ? (
                <Image
                  src={comment.author.avatarUrl}
                  alt={comment.author.name}
                  width={32}
                  height={32}
                  className="size-full object-cover"
                />
              ) : (
                <div className="size-full flex items-center justify-center text-[10px] font-bold text-fg-muted">
                  {comment.author.name.slice(0, 2).toUpperCase()}
                </div>
              )}
            </div>
            <div>
              <div className="font-semibold text-white group-hover:text-primary transition-colors text-xs">
                {comment.author.name}
              </div>
              <div className="text-nano font-mono text-fg-muted">
                {comment.author.codeforcesHandle ? `@${comment.author.codeforcesHandle}` : "Member"}
              </div>
            </div>
          </Link>

          <span className="text-nano font-mono text-fg-subtle">
            {serverDate(comment.createdAt).toLocaleDateString("en-IN", {
              month: "short",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
            {comment.editedAt && " (edited)"}
          </span>
        </div>

        {/* Content or Edit Form */}
        {isCurrentlyEditing ? (
          <div className="space-y-2 pt-1">
            <textarea
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              rows={3}
              className="w-full rounded-xl border border-white/10 bg-surface-2 p-3 text-xs text-foreground focus:border-primary/50 focus:outline-none"
            />
            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={onCancelEdit}
                className="rounded-lg px-3 py-1 text-xs text-fg-muted hover:text-white"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => onSaveEdit(comment.id)}
                disabled={isEditing || !editText.trim()}
                className="rounded-lg bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground hover:bg-primary-hover disabled:opacity-50"
              >
                {isEditing ? "Saving..." : "Save"}
              </button>
            </div>
          </div>
        ) : comment.deleted ? (
          <p className="text-xs font-mono text-fg-subtle italic">[This comment has been deleted]</p>
        ) : (
          <p className="text-xs text-foreground leading-relaxed whitespace-pre-wrap">{comment.body}</p>
        )}

        {/* Actions bar */}
        {!comment.deleted && !isCurrentlyEditing && (
          <div className="flex items-center gap-4 pt-2 border-t border-white/5 text-[11px] font-mono">
            <button
              type="button"
              onClick={() => onReply(comment.id)}
              className="text-fg-muted hover:text-primary transition-colors flex items-center gap-1"
            >
              <Reply className="size-3" />
              Reply
            </button>

            {comment.canEdit && (
              <button
                type="button"
                onClick={() => onStartEdit(comment)}
                className="text-fg-muted hover:text-amber-400 transition-colors flex items-center gap-1"
              >
                <Edit2 className="size-3" />
                Edit (15m window)
              </button>
            )}

            {comment.canDelete && (
              <button
                type="button"
                onClick={() => onDelete(comment.id)}
                className="text-fg-muted hover:text-rose-400 transition-colors flex items-center gap-1"
              >
                <Trash2 className="size-3" />
                Delete
              </button>
            )}
          </div>
        )}
      </div>

      {/* Reply input box */}
      {isReplyingToThis && (
        <div className="ml-8 rounded-2xl border border-primary/30 bg-[#0d0d16]/90 p-3 space-y-2">
          <div className="flex items-center gap-1.5 text-nano font-mono text-primary">
            <CornerDownRight className="size-3" />
            Replying to {comment.author.name}
          </div>
          <textarea
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder="Write your reply..."
            rows={2}
            className="w-full rounded-xl border border-white/10 bg-surface-2 p-2.5 text-xs text-foreground focus:border-primary/50 focus:outline-none"
          />
          <div className="flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onCancelReply}
              className="rounded-lg px-3 py-1 text-xs text-fg-muted hover:text-white"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => onPostReply(comment.id)}
              disabled={isReplying || !replyText.trim() || cooldown > 0}
              className="rounded-lg bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground hover:bg-primary-hover disabled:opacity-50"
            >
              {isReplying ? "Replying..." : "Post Reply"}
            </button>
          </div>
        </div>
      )}

      {/* Nested Replies */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="ml-6 sm:ml-8 pl-4 border-l-2 border-white/10 space-y-3">
          {comment.replies.map((reply) => {
            const isEditingReply = editingId === reply.id;
            return (
              <div
                key={reply.id}
                className="rounded-xl border border-white/5 bg-white/[0.02] p-3.5 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <Link href={`/profile/${reply.author.id}`} className="flex items-center gap-2 group">
                    <div className="size-6 rounded-full overflow-hidden bg-surface-3 border border-white/10 shrink-0">
                      {reply.author.avatarUrl ? (
                        <Image
                          src={reply.author.avatarUrl}
                          alt={reply.author.name}
                          width={24}
                          height={24}
                          className="size-full object-cover"
                        />
                      ) : (
                        <div className="size-full flex items-center justify-center text-[9px] font-bold text-fg-muted">
                          {reply.author.name.slice(0, 2).toUpperCase()}
                        </div>
                      )}
                    </div>
                    <span className="font-semibold text-white group-hover:text-primary transition-colors text-xs">
                      {reply.author.name}
                    </span>
                  </Link>
                  <span className="text-[10px] font-mono text-fg-subtle">
                    {serverDate(reply.createdAt).toLocaleDateString("en-IN", {
                      month: "short",
                      day: "numeric",
                    })}
                  </span>
                </div>

                {isEditingReply ? (
                  <div className="space-y-2 pt-1">
                    <textarea
                      value={editText}
                      onChange={(e) => setEditText(e.target.value)}
                      rows={2}
                      className="w-full rounded-lg border border-white/10 bg-surface-2 p-2 text-xs text-foreground focus:outline-none"
                    />
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={onCancelEdit}
                        className="text-xs text-fg-muted hover:text-white"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={() => onSaveEdit(reply.id)}
                        disabled={isEditing || !editText.trim()}
                        className="rounded bg-primary px-2.5 py-0.5 text-xs font-semibold text-primary-foreground"
                      >
                        Save
                      </button>
                    </div>
                  </div>
                ) : reply.deleted ? (
                  <p className="text-xs font-mono text-fg-subtle italic">[Deleted]</p>
                ) : (
                  <p className="text-xs text-foreground leading-relaxed whitespace-pre-wrap">{reply.body}</p>
                )}

                {!reply.deleted && !isEditingReply && (
                  <div className="flex items-center gap-3 pt-1 text-[10px] font-mono">
                    {reply.canEdit && (
                      <button
                        type="button"
                        onClick={() => onStartEdit(reply)}
                        className="text-fg-muted hover:text-amber-400"
                      >
                        Edit
                      </button>
                    )}
                    {reply.canDelete && (
                      <button
                        type="button"
                        onClick={() => onDelete(reply.id)}
                        className="text-fg-muted hover:text-rose-400"
                      >
                        Delete
                      </button>
                    )}
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
