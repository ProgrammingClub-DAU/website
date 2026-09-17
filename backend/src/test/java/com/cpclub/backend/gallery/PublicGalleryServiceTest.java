package com.cpclub.backend.gallery;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.gallery.dto.GalleryPhotoDto;
import com.cpclub.backend.gallery.dto.GalleryPhotoDto.Source;
import com.cpclub.backend.gallery.service.PublicGalleryService;
import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import com.cpclub.backend.halloffame.entity.HallOfFamePhoto;
import com.cpclub.backend.halloffame.repository.HallOfFamePhotoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The public gallery, merged from events and the Hall of Fame.
 */
@ExtendWith(MockitoExtension.class)
class PublicGalleryServiceTest {

    @Mock
    private EventPhotoRepository eventPhotoRepository;

    @Mock
    private HallOfFamePhotoRepository hallOfFamePhotoRepository;

    @InjectMocks
    private PublicGalleryService service;

    @Test
    @DisplayName("Both sources arrive as one list, newest first")
    void mergesNewestFirst() {
        when(eventPhotoRepository.findAllWithEventExcludingStatus(EventStatus.CANCELLED)).thenReturn(List.of(
                eventPhoto(1L, event(10L, "Winter Sprint", LocalDateTime.of(2025, 12, 5, 15, 0))),
                eventPhoto(2L, event(11L, "Spring Sprint", LocalDateTime.of(2026, 3, 2, 15, 0)))));
        when(hallOfFamePhotoRepository.findAllWithEntry()).thenReturn(List.of(
                hofPhoto(7L, entry(20L, "ICPC Regional", LocalDate.of(2026, 1, 10)))));

        List<GalleryPhotoDto> photos = service.listPhotos();

        assertEquals(List.of("Spring Sprint", "ICPC Regional", "Winter Sprint"),
                photos.stream().map(GalleryPhotoDto::sourceTitle).toList());
    }

    @Test
    @DisplayName("Cancelled events are asked to be left out, so no tile links to a hidden event")
    void excludesCancelledEvents() {
        when(eventPhotoRepository.findAllWithEventExcludingStatus(EventStatus.CANCELLED)).thenReturn(List.of());
        when(hallOfFamePhotoRepository.findAllWithEntry()).thenReturn(List.of());

        service.listPhotos();

        verify(eventPhotoRepository).findAllWithEventExcludingStatus(EventStatus.CANCELLED);
    }

    @Test
    @DisplayName("Each photo says where it belongs, so the page can link back")
    void carriesTheSource() {
        when(eventPhotoRepository.findAllWithEventExcludingStatus(EventStatus.CANCELLED)).thenReturn(List.of(
                eventPhoto(1L, event(10L, "Spring Sprint", LocalDateTime.of(2026, 3, 2, 15, 0)))));
        when(hallOfFamePhotoRepository.findAllWithEntry()).thenReturn(List.of(
                hofPhoto(1L, entry(20L, "ICPC Regional", LocalDate.of(2026, 1, 10)))));

        List<GalleryPhotoDto> photos = service.listPhotos();

        GalleryPhotoDto fromEvent = photos.get(0);
        assertEquals(Source.EVENT, fromEvent.source());
        assertEquals(10L, fromEvent.sourceId());
        assertEquals("Lab 101", fromEvent.location());

        GalleryPhotoDto fromHof = photos.get(1);
        assertEquals(Source.HALL_OF_FAME, fromHof.source());
        assertEquals(20L, fromHof.sourceId());
        assertNull(fromHof.location());

        // Same row id in two tables must not collide as a React key.
        assertEquals("event-1", fromEvent.id());
        assertEquals("hof-1", fromHof.id());
    }

    private Event event(Long id, String title, LocalDateTime when) {
        return Event.builder().id(id).title(title).eventDate(when)
                .location("Lab 101").status(EventStatus.COMPLETED).build();
    }

    private EventPhoto eventPhoto(Long id, Event event) {
        EventPhoto photo = new EventPhoto();
        photo.setId(id);
        photo.setEvent(event);
        photo.setImageUrl("https://res.cloudinary.com/e" + id + ".jpg");
        return photo;
    }

    private HallOfFameEntry entry(Long id, String heading, LocalDate on) {
        HallOfFameEntry entry = new HallOfFameEntry();
        entry.setId(id);
        entry.setHeading(heading);
        entry.setAchievedOn(on);
        return entry;
    }

    private HallOfFamePhoto hofPhoto(Long id, HallOfFameEntry entry) {
        return HallOfFamePhoto.builder().id(id).entry(entry)
                .imageUrl("https://res.cloudinary.com/h" + id + ".jpg").position(0).build();
    }
}
