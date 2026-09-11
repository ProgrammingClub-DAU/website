package com.cpclub.backend.gallery.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.gallery.dto.AddMemberPhotoRequest;
import com.cpclub.backend.gallery.dto.MemberGalleryPhotoDto;
import com.cpclub.backend.gallery.entity.MemberGalleryPhoto;
import com.cpclub.backend.gallery.repository.MemberGalleryRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the batch-wise public member gallery.
 *
 * <p>Cloudinary owns file storage. This service records only the returned image
 * URL and its metadata, along with the admin audit relationship.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberGalleryService {

    private final MemberGalleryRepository memberGalleryRepository;
    private final UserRepository userRepository;

    /**
     * Stores a member-gallery image uploaded by an administrator.
     *
     * @param request image URL and batch metadata
     * @param adminEmail authenticated administrator's email
     * @return the stored photo's public representation
     * @throws ResourceNotFoundException if the authenticated account no longer exists
     */
    @Transactional
    public MemberGalleryPhotoDto addPhoto(AddMemberPhotoRequest request, String adminEmail) {
        User admin = requireUserByEmail(adminEmail);

        MemberGalleryPhoto photo = MemberGalleryPhoto.builder()
                .batchYear(request.batchYear())
                .imageUrl(request.imageUrl())
                .caption(request.caption())
                .uploadedBy(admin)
                .build();

        MemberGalleryPhoto saved = memberGalleryRepository.save(photo);
        log.info("Added member gallery photo {} for batch {}", saved.getId(), saved.getBatchYear());
        return MemberGalleryPhotoDto.fromEntity(saved);
    }

    /**
     * Removes one member-gallery photo.
     *
     * @param id photo identifier
     * @throws ResourceNotFoundException if the photo does not exist
     */
    @Transactional
    public void deletePhoto(Long id) {
        MemberGalleryPhoto photo = memberGalleryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member gallery photo not found with id: " + id));
        memberGalleryRepository.delete(photo);
        log.info("Deleted member gallery photo {}", id);
    }

    /**
     * Retrieves one batch's public gallery in upload order.
     *
     * @param batchYear admission batch year
     * @return photos for the requested batch, or an empty list when none exist
     */
    @Transactional(readOnly = true)
    public List<MemberGalleryPhotoDto> getPhotosByBatch(Integer batchYear) {
        return memberGalleryRepository.findByBatchYearOrderByUploadedAtAsc(batchYear)
                .stream()
                .map(MemberGalleryPhotoDto::fromEntity)
                .toList();
    }

    /**
     * Lists all gallery batch filters in descending year order.
     *
     * @return unique available batch years, newest first
     */
    @Transactional(readOnly = true)
    public List<Integer> getAvailableBatchYears() {
        return memberGalleryRepository.findDistinctBatchYearsOrderByDesc();
    }

    private User requireUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
