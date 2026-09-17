package com.cpclub.backend.halloffame.repository;

import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallOfFameEntryRepository extends JpaRepository<HallOfFameEntry, Long> {

    /**
     * Every entry, newest achievement first.
     *
     * <p>The id tie-break keeps two entries dated the same day in a stable order,
     * so a page does not reshuffle between loads. Backed by
     * {@code idx_hof_achieved_on}.</p>
     */
    List<HallOfFameEntry> findAllByOrderByAchievedOnDescIdDesc();
}
