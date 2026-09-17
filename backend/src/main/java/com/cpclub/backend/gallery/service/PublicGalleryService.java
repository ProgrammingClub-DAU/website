package com.cpclub.backend.gallery.service;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.gallery.dto.GalleryPhotoDto;
import com.cpclub.backend.gallery.dto.GalleryPhotoDto.Source;
import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import com.cpclub.backend.halloffame.entity.HallOfFamePhoto;
import com.cpclub.backend.halloffame.repository.HallOfFamePhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The public gallery: every event photo and every Hall of Fame photo, as one
 * list, newest first.
 *
 * <p>There is no gallery table. A photo belongs to the event or achievement it
 * was taken at, and is added there; the gallery is a view over both. That keeps
 * a single place to add or remove a photo, and means the gallery cannot hold a
 * picture whose event has been deleted.</p>
 *
 * <p>Merged in memory. That is safe at the club's scale -- hundreds of rows of
 * short strings -- and the alternative, a SQL UNION across two differently
 * shaped tables, would be harder to read for no benefit here.</p>
 */
@Service
@RequiredArgsConstructor
public class PublicGalleryService {

    /** Newest first; then events before Hall of Fame on the same day; then newest upload. */
    private static final Comparator<Ranked> NEWEST_FIRST = Comparator
            .comparing((Ranked r) -> r.photo().date(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(r -> r.photo().source())
            .thenComparing(Ranked::rowId, Comparator.reverseOrder());

    private final EventPhotoRepository eventPhotoRepository;
    private final HallOfFamePhotoRepository hallOfFamePhotoRepository;

    @Transactional(readOnly = true)
    public List<GalleryPhotoDto> listPhotos() {
        Stream<Ranked> fromEvents = eventPhotoRepository
                .findAllWithEventExcludingStatus(EventStatus.CANCELLED).stream()
                .map(PublicGalleryService::fromEvent);

        Stream<Ranked> fromHallOfFame = hallOfFamePhotoRepository.findAllWithEntry().stream()
                .map(PublicGalleryService::fromHallOfFame);

        return Stream.concat(fromEvents, fromHallOfFame)
                .sorted(NEWEST_FIRST)
                .map(Ranked::photo)
                .toList();
    }

    private static Ranked fromEvent(EventPhoto photo) {
        Event event = photo.getEvent();
        return new Ranked(photo.getId(), new GalleryPhotoDto(
                "event-" + photo.getId(),
                photo.getImageUrl(),
                photo.getCaption(),
                Source.EVENT,
                event.getId(),
                event.getTitle(),
                event.getEventDate() != null ? event.getEventDate().toLocalDate() : null,
                event.getLocation()
        ));
    }

    private static Ranked fromHallOfFame(HallOfFamePhoto photo) {
        HallOfFameEntry entry = photo.getEntry();
        return new Ranked(photo.getId(), new GalleryPhotoDto(
                "hof-" + photo.getId(),
                photo.getImageUrl(),
                photo.getCaption(),
                Source.HALL_OF_FAME,
                entry.getId(),
                entry.getHeading(),
                entry.getAchievedOn(),
                null
        ));
    }

    /** A photo plus its row id, which the sort needs and the public DTO does not expose as a number. */
    private record Ranked(Long rowId, GalleryPhotoDto photo) {
    }
}
