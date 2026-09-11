package com.cpclub.backend.gallery.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.gallery.dto.AddMemberPhotoRequest;
import com.cpclub.backend.gallery.dto.MemberGalleryPhotoDto;
import com.cpclub.backend.gallery.service.MemberGalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public member-gallery reads and administrative gallery management.
 *
 * <p>URL authorization in {@code SecurityConfig} grants public access to the
 * GET routes and restricts every other gallery route to administrators. The
 * method-level checks on writes provide a second layer next to the operations
 * that change gallery content.</p>
 */
@RestController
@RequestMapping("/api/gallery/members")
@RequiredArgsConstructor
@Tag(name = "Member Gallery", description = "Batch-wise club member gallery")
public class GalleryController {

    private final MemberGalleryService memberGalleryService;

    /**
     * Adds a Cloudinary-hosted photo to a batch gallery.
     *
     * @param request image URL and batch metadata
     * @param userDetails authenticated administrator
     * @return newly stored public photo representation
     */
    @PostMapping
    @Operation(summary = "Add a member gallery photo (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberGalleryPhotoDto>> addPhoto(
            @Valid @RequestBody AddMemberPhotoRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MemberGalleryPhotoDto photo = memberGalleryService.addPhoto(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(photo, "Member gallery photo added successfully"));
    }

    /**
     * Deletes a member-gallery photo.
     *
     * @param id photo identifier
     * @return empty success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a member gallery photo (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long id) {
        memberGalleryService.deletePhoto(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Member gallery photo deleted successfully"));
    }

    /**
     * Retrieves every photo for one admission batch.
     *
     * @param batch admission batch year
     * @return photos in upload order, or an empty list when the batch has none
     */
    @GetMapping
    @Operation(summary = "Get member gallery photos for a batch")
    public ResponseEntity<ApiResponse<List<MemberGalleryPhotoDto>>> getPhotosByBatch(
            @RequestParam Integer batch
    ) {
        List<MemberGalleryPhotoDto> photos = memberGalleryService.getPhotosByBatch(batch);
        return ResponseEntity.ok(ApiResponse.success(photos, "Fetched member gallery photos successfully"));
    }

    /**
     * Lists the admission years represented in the member gallery.
     *
     * @return distinct batch years, newest first
     */
    @GetMapping("/batches")
    @Operation(summary = "Get available member gallery batch years")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailableBatchYears() {
        List<Integer> batchYears = memberGalleryService.getAvailableBatchYears();
        return ResponseEntity.ok(ApiResponse.success(batchYears, "Fetched available batch years successfully"));
    }
}
