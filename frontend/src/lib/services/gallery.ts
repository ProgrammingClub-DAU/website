import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/store/auth";
import type { MemberGalleryPhoto } from "@/types/api";

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

  getPhotosByBatch: async (year: number): Promise<MemberGalleryPhoto[]> => {
    const response = await apiClient.get<ApiResponse<MemberGalleryPhoto[]>>(
      "/api/gallery/members",
      { params: { batch: year } }
    );
    return response.data.data;
  },

  getAvailableBatchYears: async (): Promise<number[]> => {
    const response = await apiClient.get<ApiResponse<number[]>>("/api/gallery/members/batches");
    return response.data.data;
  },
};
