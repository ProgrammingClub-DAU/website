package com.cpclub.backend.halloffame.repository;

import com.cpclub.backend.halloffame.entity.HallOfFamePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HallOfFamePhotoRepository extends JpaRepository<HallOfFamePhoto, Long> {

    /**
     * Every Hall of Fame photo, with its entry, for the public gallery.
     *
     * <p>The entry is fetch-joined because every gallery tile names it and links to
     * it; loading it lazily would be one query per photo.</p>
     */
    @Query("SELECT p FROM HallOfFamePhoto p JOIN FETCH p.entry")
    List<HallOfFamePhoto> findAllWithEntry();
}
