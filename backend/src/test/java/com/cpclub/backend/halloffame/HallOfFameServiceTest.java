package com.cpclub.backend.halloffame;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryDto;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryRequest;
import com.cpclub.backend.halloffame.entity.HallOfFameEntry;
import com.cpclub.backend.halloffame.entity.HallOfFameLink;
import com.cpclub.backend.halloffame.repository.HallOfFameEntryRepository;
import com.cpclub.backend.halloffame.service.HallOfFameService;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recording and editing Hall of Fame entries.
 *
 * <p>The interesting behaviour is in the children: links and photos are ordered
 * lists the admin arranges, replaced whole on every save.</p>
 */
@ExtendWith(MockitoExtension.class)
class HallOfFameServiceTest {

    @Mock
    private HallOfFameEntryRepository entryRepository;

    @Mock
    private UserRepository userRepository;

    private HallOfFameService service;

    @BeforeEach
    void setUp() {
        service = new HallOfFameService(entryRepository, userRepository);
    }

    @Test
    @DisplayName("A new entry keeps its links and photos in the order the admin gave them")
    void createEntry_keepsTheAdminsOrder() {
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin()));
        when(entryRepository.save(any(HallOfFameEntry.class))).thenAnswer(i -> i.getArgument(0));

        HallOfFameEntryDto created = service.createEntry(request(
                List.of(link("Ranklist", "https://icpc.global/r"), link("Editorial", "https://cf.com/e")),
                List.of(photo("https://res.cloudinary.com/a.jpg", "Team"), photo("https://res.cloudinary.com/b.jpg", null))
        ), "admin@dau.ac.in");

        assertEquals(List.of("Ranklist", "Editorial"),
                created.links().stream().map(HallOfFameEntryDto.LinkDto::label).toList());
        assertEquals(List.of("https://res.cloudinary.com/a.jpg", "https://res.cloudinary.com/b.jpg"),
                created.photos().stream().map(HallOfFameEntryDto.PhotoDto::imageUrl).toList());

        ArgumentCaptor<HallOfFameEntry> saved = ArgumentCaptor.forClass(HallOfFameEntry.class);
        verify(entryRepository).save(saved.capture());
        assertEquals(List.of(0, 1),
                saved.getValue().getLinks().stream().map(HallOfFameLink::getPosition).toList(),
                "positions come from list order");
        assertSame(saved.getValue(), saved.getValue().getLinks().get(0).getEntry(),
                "each child points back at its entry, or the foreign key is null on insert");
    }

    @Test
    @DisplayName("Blank optional text is stored as absent, not as an empty string")
    void createEntry_storesBlankTextAsNull() {
        when(entryRepository.save(any(HallOfFameEntry.class))).thenAnswer(i -> i.getArgument(0));

        HallOfFameEntryDto created = service.createEntry(new HallOfFameEntryRequest(
                "  ICPC Regional  ", "   ", "", LocalDate.of(2026, 1, 10), null, null
        ), "admin@dau.ac.in");

        assertEquals("ICPC Regional", created.heading());
        assertNull(created.subheading());
        assertNull(created.details());
        assertTrue(created.links().isEmpty(), "null links are treated as none");
        assertTrue(created.photos().isEmpty(), "null photos are treated as none");
    }

    @Test
    @DisplayName("A record is still saved when the admin's account cannot be found")
    void createEntry_toleratesAMissingAdmin() {
        // created_by is nullable so the record belongs to the club, not the typist.
        when(userRepository.findByEmail("gone@dau.ac.in")).thenReturn(Optional.empty());
        when(entryRepository.save(any(HallOfFameEntry.class))).thenAnswer(i -> i.getArgument(0));

        service.createEntry(request(List.of(), List.of()), "gone@dau.ac.in");

        ArgumentCaptor<HallOfFameEntry> saved = ArgumentCaptor.forClass(HallOfFameEntry.class);
        verify(entryRepository).save(saved.capture());
        assertNull(saved.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("Updating an entry replaces its links rather than appending to them")
    void updateEntry_replacesChildren() {
        HallOfFameEntry existing = new HallOfFameEntry();
        existing.setId(5L);
        existing.getLinks().add(HallOfFameLink.builder()
                .entry(existing).label("Old").url("https://old.example").position(0).build());

        when(entryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(entryRepository.save(any(HallOfFameEntry.class))).thenAnswer(i -> i.getArgument(0));

        HallOfFameEntryDto updated = service.updateEntry(5L,
                request(List.of(link("New", "https://new.example")), List.of()));

        assertEquals(List.of("New"),
                updated.links().stream().map(HallOfFameEntryDto.LinkDto::label).toList());
    }

    @Test
    @DisplayName("Editing an entry that does not exist is a 404")
    void updateEntry_rejectsUnknownEntry() {
        when(entryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateEntry(99L, request(List.of(), List.of())));
        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an entry that does not exist is a 404, not a silent success")
    void deleteEntry_rejectsUnknownEntry() {
        when(entryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteEntry(99L));
        verify(entryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("The listing uses the newest-first query")
    void listEntries_isNewestFirst() {
        when(entryRepository.findAllByOrderByAchievedOnDescIdDesc()).thenReturn(List.of());

        service.listEntries();

        verify(entryRepository).findAllByOrderByAchievedOnDescIdDesc();
    }

    private HallOfFameEntryRequest request(
            List<HallOfFameEntryRequest.Link> links, List<HallOfFameEntryRequest.Photo> photos) {
        return new HallOfFameEntryRequest(
                "ICPC Amritapuri Regional", "Team DAU Alpha", "Ranked 42nd.",
                LocalDate.of(2026, 1, 10), links, photos);
    }

    private HallOfFameEntryRequest.Link link(String label, String url) {
        return new HallOfFameEntryRequest.Link(label, url);
    }

    private HallOfFameEntryRequest.Photo photo(String url, String caption) {
        return new HallOfFameEntryRequest.Photo(url, caption);
    }

    private User admin() {
        User user = new User("Admin", "admin@dau.ac.in", null, Role.ROLE_ADMIN);
        user.setId(1L);
        return user;
    }
}
