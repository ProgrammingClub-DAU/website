import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { GalleryPhoto, GallerySource, MemberGalleryPhoto } from "@/types/api";

export interface MemberGalleryPhotoRequest {
  batchYear: number;
  imageUrl: string;
  caption: string | null;
}

export const galleryService = {
  addMemberPhoto: async (data: MemberGalleryPhotoRequest): Promise<MemberGalleryPhoto> => {
    const response = await apiClient.post<ApiResponse<MemberGalleryPhoto>>(
      "/api/gallery/members",
      data
    );
    return response.data.data;
  },

  deleteMemberPhoto: async (id: number): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/gallery/members/${id}`);
    return response.data.data;
  },

  /**
   * One batch's photos. `serverRender` waits long enough for a sleeping backend
   * to wake, for the public page that renders on the server.
   */
  getPhotosByBatch: async (
    year: number,
    options?: { serverRender?: boolean }
  ): Promise<MemberGalleryPhoto[]> => {
    const response = await apiClient.get<ApiResponse<MemberGalleryPhoto[]>>(
      "/api/gallery/members",
      { params: { batch: year }, timeout: options?.serverRender ? 60_000 : undefined }
    );
    return response.data.data ?? [];
  },

  getAvailableBatchYears: async (options?: { serverRender?: boolean }): Promise<number[]> => {
    const response = await apiClient.get<ApiResponse<number[]>>("/api/gallery/members/batches", {
      timeout: options?.serverRender ? 60_000 : undefined,
    });
    return response.data.data ?? [];
  },

  /**
   * Every public photo, newest first, each knowing its event or achievement.
   *
   * Only the gallery page calls this, and it renders on the server, so it waits
   * long enough for a sleeping backend to wake.
   */
  listPhotos: async (): Promise<GalleryPhoto[]> => {
    const response = await apiClient.get<ApiResponse<GalleryPhoto[]>>("/api/gallery/photos", {
      timeout: 60_000,
    });
    return response.data.data ?? [];
  },
};

/** Where a gallery photo's "view" link goes. */
export function gallerySourceHref(source: GallerySource, sourceId: number): string {
  return source === "EVENT" ? `/events/${sourceId}` : `/hall-of-fame/${sourceId}`;
}

/** The words for that link. */
export function gallerySourceLabel(source: GallerySource): string {
  return source === "EVENT" ? "View event" : "View in Hall of Fame";
}
