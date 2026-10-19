import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { CommentDto } from "@/types/api";

export interface CommentRequest {
  body: string;
  parentId?: number | null;
}

export const commentsService = {
  /**
   * Fetches the full comment tree for a published post.
   */
  async list(postId: number | string): Promise<CommentDto[]> {
    const res = await apiClient.get<ApiResponse<CommentDto[]>>(`/api/blogs/${postId}/comments`);
    return res.data.data;
  },

  /**
   * Posts a new comment or a reply to an existing comment.
   */
  async create(postId: number | string, data: CommentRequest): Promise<CommentDto> {
    const res = await apiClient.post<ApiResponse<CommentDto>>(`/api/blogs/${postId}/comments`, data);
    return res.data.data;
  },

  /**
   * Edits a comment within its 15-minute creation window.
   */
  async update(commentId: number | string, body: string): Promise<CommentDto> {
    const res = await apiClient.put<ApiResponse<CommentDto>>(`/api/comments/${commentId}`, { body });
    return res.data.data;
  },

  /**
   * Deletes a comment (soft delete if replies exist; hard delete if leaf).
   */
  async remove(commentId: number | string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/api/comments/${commentId}`);
  },
};
