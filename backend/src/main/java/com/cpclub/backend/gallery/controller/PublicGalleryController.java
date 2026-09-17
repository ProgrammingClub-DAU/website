package com.cpclub.backend.gallery.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.gallery.dto.GalleryPhotoDto;
import com.cpclub.backend.gallery.service.PublicGalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The public photo gallery.
 *
 * <p>Read-only. Photos are added to an event or a Hall of Fame entry, not to the
 * gallery; this only lists them.</p>
 */
@RestController
@RequestMapping("/api/gallery/photos")
@RequiredArgsConstructor
@Tag(name = "Gallery", description = "Event and Hall of Fame photos, newest first")
public class PublicGalleryController {

    private final PublicGalleryService publicGalleryService;

    @GetMapping
    @Operation(summary = "List every public photo with its source, newest first")
    public ResponseEntity<ApiResponse<List<GalleryPhotoDto>>> listPhotos() {
        return ResponseEntity.ok(ApiResponse.success(
                publicGalleryService.listPhotos(), "Fetched gallery successfully"));
    }
}
