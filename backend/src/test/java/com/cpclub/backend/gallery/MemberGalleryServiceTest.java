package com.cpclub.backend.gallery;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.gallery.dto.AddMemberPhotoRequest;
import com.cpclub.backend.gallery.dto.MemberGalleryPhotoDto;
import com.cpclub.backend.gallery.entity.MemberGalleryPhoto;
import com.cpclub.backend.gallery.repository.MemberGalleryRepository;
import com.cpclub.backend.gallery.service.MemberGalleryService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for the member gallery service contract.
 */
@ExtendWith(MockitoExtension.class)
class MemberGalleryServiceTest {

    @Mock
    private MemberGalleryRepository memberGalleryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MemberGalleryService memberGalleryService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).name("Admin").email("admin@dau.ac.in").build();
    }

    @Test
    @DisplayName("Adding a member photo stores its batch metadata and admin audit record")
    void addPhoto_success() {
        AddMemberPhotoRequest request = new AddMemberPhotoRequest(2024, "https://cdn.example.com/batch.jpg", "Orientation");
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(memberGalleryRepository.save(any(MemberGalleryPhoto.class))).thenAnswer(invocation -> {
            MemberGalleryPhoto photo = invocation.getArgument(0);
            photo.setId(10L);
            photo.setUploadedAt(LocalDateTime.of(2026, 9, 11, 12, 0));
            return photo;
        });

        MemberGalleryPhotoDto result = memberGalleryService.addPhoto(request, "admin@dau.ac.in");

        ArgumentCaptor<MemberGalleryPhoto> savedPhoto = ArgumentCaptor.forClass(MemberGalleryPhoto.class);
        verify(memberGalleryRepository).save(savedPhoto.capture());
        assertEquals(2024, savedPhoto.getValue().getBatchYear());
        assertEquals("https://cdn.example.com/batch.jpg", savedPhoto.getValue().getImageUrl());
        assertEquals("Orientation", savedPhoto.getValue().getCaption());
        assertEquals(admin, savedPhoto.getValue().getUploadedBy());
        assertEquals(10L, result.id());
        assertEquals(2024, result.batchYear());
    }

    @Test
    @DisplayName("Deleting an existing member gallery photo removes that photo")
    void deletePhoto_success() {
        MemberGalleryPhoto photo = photo(10L, 2024, "Orientation", LocalDateTime.of(2026, 9, 11, 12, 0));
        when(memberGalleryRepository.findById(10L)).thenReturn(Optional.of(photo));

        memberGalleryService.deletePhoto(10L);

        verify(memberGalleryRepository).delete(photo);
    }

    @Test
    @DisplayName("Deleting a missing member gallery photo reports not found")
    void deletePhoto_notFound() {
        when(memberGalleryRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> memberGalleryService.deletePhoto(404L));

        assertTrue(error.getMessage().contains("Member gallery photo"));
    }

    @Test
    @DisplayName("Listing a batch maps gallery entities without exposing their uploader")
    void getPhotosByBatch_returnsMappedDtos() {
        LocalDateTime firstUpload = LocalDateTime.of(2026, 9, 1, 12, 0);
        LocalDateTime secondUpload = LocalDateTime.of(2026, 9, 2, 12, 0);
        when(memberGalleryRepository.findByBatchYearOrderByUploadedAtAsc(2024)).thenReturn(List.of(
                photo(10L, 2024, "First", firstUpload),
                photo(11L, 2024, "Second", secondUpload)
        ));

        List<MemberGalleryPhotoDto> result = memberGalleryService.getPhotosByBatch(2024);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals("First", result.get(0).caption());
        assertEquals(firstUpload, result.get(0).uploadedAt());
        assertEquals(11L, result.get(1).id());
    }

    @Test
    @DisplayName("A batch with no gallery photos returns an empty list")
    void getPhotosByBatch_empty() {
        when(memberGalleryRepository.findByBatchYearOrderByUploadedAtAsc(2020)).thenReturn(List.of());

        assertTrue(memberGalleryService.getPhotosByBatch(2020).isEmpty());
    }

    @Test
    @DisplayName("Available batch years retain the repository's distinct descending order")
    void getAvailableBatchYears_returnsDistinctDescendingYears() {
        when(memberGalleryRepository.findDistinctBatchYearsOrderByDesc()).thenReturn(List.of(2024, 2023, 2022));

        assertEquals(List.of(2024, 2023, 2022), memberGalleryService.getAvailableBatchYears());
    }

    private MemberGalleryPhoto photo(Long id, Integer batchYear, String caption, LocalDateTime uploadedAt) {
        return MemberGalleryPhoto.builder()
                .id(id)
                .batchYear(batchYear)
                .imageUrl("https://cdn.example.com/" + id + ".jpg")
                .caption(caption)
                .uploadedBy(admin)
                .uploadedAt(uploadedAt)
                .build();
    }
}
