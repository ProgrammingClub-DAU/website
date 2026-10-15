package com.cpclub.backend.event.service;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.dto.EventRegistrationDto;
import com.cpclub.backend.event.dto.MarkAttendedResultDto;
import com.cpclub.backend.event.dto.RegistrationStatusDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventRegistration;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventRegistrationRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Member RSVPs, and turning them into attendance (D13).
 *
 * <p>An RSVP is a member's own statement of intent; attendance is the club's official
 * record and stays admin-only, exactly as Phase 2 decided. The two are separate rows
 * in separate tables, and the only bridge between them is {@link #markAttended},
 * which an admin calls.</p>
 *
 * <p><b>Times.</b> {@code events.event_date} predates the Phase 3 rule that timestamps
 * are UTC (D31): the admin form sends the wall-clock time an admin typed, in IST, and
 * that is what is stored. "Has the event started?" is therefore asked of the
 * Asia/Kolkata clock, not UTC -- comparing against UTC would keep RSVPs open for five
 * and a half hours after an event had begun.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final UserRepository userRepository;
    private final VerifiedMemberGuard verifiedMemberGuard;
    /** The Asia/Kolkata clock from AppConfig: the zone event_date is stored in. */
    private final Clock clock;

    /**
     * RSVPs the member. Idempotent: a member already registered gets 200 and their
     * status, not an error.
     *
     * <p>The event row is locked ({@code SELECT ... FOR UPDATE}) before registrations
     * are counted, and the lock is held until this transaction commits. Two requests
     * for the last seat are therefore serialised: the second waits, then counts the
     * first one's row and is told the event is full.</p>
     */
    @Transactional
    public RegistrationStatusDto register(Long eventId, Long userId) {
        User user = requireUser(userId);
        verifiedMemberGuard.require(user);

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> eventNotFound(eventId));
        if (!isOpen(event)) {
            throw new ConflictException("Registration closed");
        }
        if (registrationRepository.existsByEventIdAndUserId(eventId, userId)) {
            return status(event, userId);
        }
        long taken = registrationRepository.countByEventId(eventId);
        if (event.getCapacity() != null && taken >= event.getCapacity()) {
            throw new ConflictException("Event is full");
        }

        registrationRepository.save(EventRegistration.builder().event(event).user(user).build());
        log.info("User {} registered for event {}", userId, eventId);
        return status(event, userId);
    }

    /** Cancels an RSVP. Idempotent; allowed until the event starts. */
    @Transactional
    public void unregister(Long eventId, Long userId) {
        Event event = requireEvent(eventId);
        if (!event.getEventDate().isAfter(nowIst())) {
            throw new ConflictException("Registration closed");
        }
        if (registrationRepository.deleteByEventIdAndUserId(eventId, userId) > 0) {
            log.info("User {} cancelled their RSVP for event {}", userId, eventId);
        }
    }

    @Transactional(readOnly = true)
    public RegistrationStatusDto getStatus(Long eventId, Long userId) {
        return status(requireEvent(eventId), userId);
    }

    /** Everyone who RSVP'd, for the admin attendance panel. */
    @Transactional(readOnly = true)
    public List<EventRegistrationDto> getRegistrations(Long eventId) {
        requireEvent(eventId);
        Set<Long> attending = attendingUserIds(eventId);
        return registrationRepository.findByEventIdWithUser(eventId).stream()
                .map(r -> new EventRegistrationDto(
                        r.getUser().getId(),
                        r.getUser().getName(),
                        r.getUser().getEmail(),
                        r.getUser().getPhoneNumber(),
                        r.getUser().getClubRole(),
                        r.getRegisteredAt(),
                        attending.contains(r.getUser().getId())))
                .toList();
    }

    /**
     * Puts members on the attendance record in bulk, skipping anyone already on it.
     *
     * <p>The Phase 2 rules for adding an attendee apply unchanged -- including that the
     * event must still be UPCOMING. Attendance has to be marked before the event is
     * marked completed; a completed event is a closed record.</p>
     */
    @Transactional
    public MarkAttendedResultDto markAttended(Long eventId, List<Long> userIds, String adminEmail) {
        Event event = requireEvent(eventId);
        if (event.getStatus() != EventStatus.UPCOMING) {
            throw new BadRequestException("Cannot add attendees to a completed or cancelled event.");
        }
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + adminEmail));

        Set<Long> wanted = new LinkedHashSet<>(userIds);
        Map<Long, User> users = userRepository.findAllById(wanted).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (users.size() != wanted.size()) {
            throw new ResourceNotFoundException("Student not found");
        }

        Set<Long> attending = attendingUserIds(eventId);
        List<EventAttendee> toAdd = new ArrayList<>();
        for (Long id : wanted) {
            if (!attending.contains(id)) {
                toAdd.add(EventAttendee.builder().event(event).user(users.get(id)).addedBy(admin).build());
            }
        }
        attendeeRepository.saveAll(toAdd);
        log.info("Marked {} member(s) attended for event {} by {} ({} already attending)",
                toAdd.size(), eventId, adminEmail, wanted.size() - toAdd.size());
        return new MarkAttendedResultDto(toAdd.size(), wanted.size() - toAdd.size());
    }

    // ------------------------------------------------------------------ helpers

    private RegistrationStatusDto status(Event event, Long userId) {
        return new RegistrationStatusDto(
                userId != null && registrationRepository.existsByEventIdAndUserId(event.getId(), userId),
                registrationRepository.countByEventId(event.getId()),
                event.getCapacity(),
                isOpen(event));
    }

    /** RSVPs are taken while the event is UPCOMING and has not started. */
    private boolean isOpen(Event event) {
        return event.getStatus() == EventStatus.UPCOMING && event.getEventDate().isAfter(nowIst());
    }

    private LocalDateTime nowIst() {
        return LocalDateTime.now(clock);
    }

    private Set<Long> attendingUserIds(Long eventId) {
        return attendeeRepository.findByEventIdOrderByAddedAtAsc(eventId).stream()
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());
    }

    private Event requireEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> eventNotFound(eventId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private static ResourceNotFoundException eventNotFound(Long eventId) {
        return new ResourceNotFoundException("Event not found with id: " + eventId);
    }
}
