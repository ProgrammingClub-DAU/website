package com.cpclub.backend.gallery.repository;

import com.cpclub.backend.gallery.entity.MemberGalleryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for the public, batch-wise member gallery.
 */
@Repository
public interface MemberGalleryRepository extends JpaRepository<MemberGalleryPhoto, Long> {

    /**
     * Returns one batch's photos in the order they were uploaded.
     *
     * @param batchYear admission batch year
     * @return photos, earliest upload first
     */
    List<MemberGalleryPhoto> findByBatchYearOrderByUploadedAtAsc(Integer batchYear);

    /**
     * Lists batch years with at least one gallery photo, newest batch first.
     *
     * <p>This is a scalar projection so the database performs the distinct and
     * ordering work without loading gallery entities just to populate a filter.</p>
     *
     * @return distinct available batch years, descending
     */
    @Query("SELECT DISTINCT photo.batchYear FROM MemberGalleryPhoto photo ORDER BY photo.batchYear DESC")
    List<Integer> findDistinctBatchYearsOrderByDesc();
}
