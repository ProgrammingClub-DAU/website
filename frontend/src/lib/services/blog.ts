import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { PagedResponse, BlogResponseDto, BlogEditDto } from "@/types/api";

export interface BlogCreateRequest {
  title: string;
  content: string;
  tags?: string;
  published?: boolean;
}

export interface BlogUpdateRequest {
  title: string;
  content: string;
  tags?: string;
}

export const blogService = {
  /**
   * Fetches published blog posts, newest first.
   */
  async getPublishedBlogs(page = 0, size = 10): Promise<PagedResponse<BlogResponseDto>> {
    const res = await apiClient.get<ApiResponse<PagedResponse<BlogResponseDto>>>("/api/blogs", {
      params: { page, size },
    });
    return res.data.data;
  },

  /**
   * Fetches a published blog post by its numeric ID.
   */
  async getBlogById(id: number | string): Promise<BlogResponseDto> {
    const res = await apiClient.get<ApiResponse<BlogResponseDto>>(`/api/blogs/${id}`);
    return res.data.data;
  },

  /**
   * Fetches a published blog post by its URL slug.
   */
  async getBlogBySlug(slug: string): Promise<BlogResponseDto> {
    const res = await apiClient.get<ApiResponse<BlogResponseDto>>(`/api/blogs/slug/${slug}`);
    return res.data.data;
  },

  /**
   * Fetches post details with review notes for the author or an admin.
   */
  async getForEditing(id: number | string): Promise<BlogEditDto> {
    const res = await apiClient.get<ApiResponse<BlogEditDto>>(`/api/blogs/${id}/edit`);
    return res.data.data;
  },

  /**
   * Fetches all posts belonging to the authenticated author (Draft, Pending, Published, Rejected).
   */
  async mine(): Promise<BlogEditDto[]> {
    const res = await apiClient.get<ApiResponse<BlogEditDto[]>>("/api/blogs/mine");
    return res.data.data;
  },

  /**
   * Admin: Fetches the queue of posts waiting for review, oldest first.
   */
  async reviewQueue(): Promise<BlogEditDto[]> {
    const res = await apiClient.get<ApiResponse<BlogEditDto[]>>("/api/blogs/review-queue");
    return res.data.data;
  },

  /**
   * Creates a new blog post as a DRAFT.
   */
  async create(data: BlogCreateRequest): Promise<BlogResponseDto> {
    const res = await apiClient.post<ApiResponse<BlogResponseDto>>("/api/blogs", data);
    return res.data.data;
  },

  /**
   * Updates an existing draft or rejected post.
   */
  async update(id: number | string, data: BlogUpdateRequest): Promise<BlogEditDto> {
    const res = await apiClient.put<ApiResponse<BlogEditDto>>(`/api/blogs/${id}`, data);
    return res.data.data;
  },

  /**
   * Deletes a draft, rejected post, or any post if admin.
   */
  async remove(id: number | string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/api/blogs/${id}`);
  },

  /**
   * Submits a draft post for core-team review.
   */
  async submit(id: number | string): Promise<BlogEditDto> {
    const res = await apiClient.post<ApiResponse<BlogEditDto>>(`/api/blogs/${id}/submit`);
    return res.data.data;
  },

  /**
   * Admin: Approves and publishes a reviewed post.
   */
  async publish(id: number | string): Promise<BlogEditDto> {
    const res = await apiClient.post<ApiResponse<BlogEditDto>>(`/api/blogs/${id}/publish`);
    return res.data.data;
  },

  /**
   * Admin: Rejects a submitted post with a required review explanation note.
   */
  async reject(id: number | string, note: string): Promise<BlogEditDto> {
    const res = await apiClient.post<ApiResponse<BlogEditDto>>(`/api/blogs/${id}/reject`, { note });
    return res.data.data;
  },
};
