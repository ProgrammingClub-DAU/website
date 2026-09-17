package com.cpclub.backend.halloffame.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryDto;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryRequest;
import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import com.cpclub.backend.halloffame.entity.HallOfFameLink;
import com.cpclub.backend.halloffame.entity.HallOfFamePhoto;
import com.cpclub.backend.halloffame.repository.HallOfFameEntryRepository;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The Hall of Fame: dated achievements, maintained by admins.
 *
 * <p>Reads are public and newest first. Writes replace an entry whole, links
 * and photos included -- see {@link HallOfFameEntryRequest} for why.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HallOfFameService {

    private final HallOfFameEntryRepository entryRepository;
    private final UserRepository userRepository;

    /** Every entry, newest first. Mapped inside the transaction: children are lazy. */
    @Transactional(readOnly = true)
    public List<HallOfFameEntryDto> listEntries() {
        return entryRepository.findAllByOrderByAchievedOnDescIdDesc().stream()
                .map(HallOfFameEntryDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public HallOfFameEntryDto getEntry(Long id) {
        return HallOfFameEntryDto.fromEntity(requireEntry(id));
    }

    /**
     * Records a new achievement.
     *
     * @param request the entry, with its links and photos
     * @param adminEmail the admin creating it; kept for the record, not shown
     * @return the saved entry
     */
    @Transactional
    public HallOfFameEntryDto createEntry(HallOfFameEntryRequest request, String adminEmail) {
        HallOfFameEntry entry = new HallOfFameEntry();
        // A missing account is not a reason to refuse the record -- the column
        // is nullable for exactly that reason -- so no 404 here.
        userRepository.findByEmail(adminEmail).ifPresent(entry::setCreatedBy);

        apply(entry, request);
        HallOfFameEntry saved = entryRepository.save(entry);

        log.info("Created Hall of Fame entry id {} by {}", saved.getId(), adminEmail);
        return HallOfFameEntryDto.fromEntity(saved);
    }

    /**
     * Replaces an entry, links and photos included.
     *
     * @throws ResourceNotFoundException if the entry does not exist
     */
    @Transactional
    public HallOfFameEntryDto updateEntry(Long id, HallOfFameEntryRequest request) {
        HallOfFameEntry entry = requireEntry(id);
        apply(entry, request);
        HallOfFameEntry saved = entryRepository.save(entry);

        log.info("Updated Hall of Fame entry id {}", id);
        return HallOfFameEntryDto.fromEntity(saved);
    }

    /**
     * Removes an entry. Its links and photo records go with it; the image files
     * stay on Cloudinary, which this server holds no credentials for.
     *
     * @throws ResourceNotFoundException if the entry does not exist
     */
    @Transactional
    public void deleteEntry(Long id) {
        entryRepository.delete(requireEntry(id));
        log.info("Deleted Hall of Fame entry id {}", id);
    }

    /**
     * Copies a request onto an entry.
     *
     * <p>The child lists are cleared and rebuilt rather than diffed. With
     * orphanRemoval that becomes the delete-then-insert it would be anyway, and
     * an entry has at most forty children. Positions are taken from list order,
     * so the order the admin arranged is the order the page shows.</p>
     */
    private void apply(HallOfFameEntry entry, HallOfFameEntryRequest request) {
        entry.setHeading(request.heading().trim());
        entry.setSubheading(trimToNull(request.subheading()));
        entry.setDetails(trimToNull(request.details()));
        entry.setAchievedOn(request.achievedOn());

        entry.getLinks().clear();
        List<HallOfFameEntryRequest.Link> links = request.linksOrEmpty();
        for (int i = 0; i < links.size(); i++) {
            entry.getLinks().add(HallOfFameLink.builder()
                    .entry(entry)
                    .label(links.get(i).label().trim())
                    .url(links.get(i).url().trim())
                    .position(i)
                    .build());
        }

        entry.getPhotos().clear();
        List<HallOfFameEntryRequest.Photo> photos = request.photosOrEmpty();
        for (int i = 0; i < photos.size(); i++) {
            entry.getPhotos().add(HallOfFamePhoto.builder()
                    .entry(entry)
                    .imageUrl(photos.get(i).imageUrl().trim())
                    .caption(trimToNull(photos.get(i).caption()))
                    .position(i)
                    .build());
        }
    }

    private HallOfFameEntry requireEntry(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall of Fame entry not found with id: " + id));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
