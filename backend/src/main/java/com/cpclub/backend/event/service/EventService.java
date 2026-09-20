package com.cpclub.backend.event.service;

import com.cpclub.backend.event.entity.EventWinner;
import com.cpclub.backend.event.dto.SetEventWinnersRequest;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.dto.AddEventPhotoRequest;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.dto.EventCreateRequest;
import com.cpclub.backend.event.dto.EventDetailDto;
import com.cpclub.backend.event.dto.EventPhotoDto;
import com.cpclub.backend.event.dto.EventResponseDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Club events, their attendance record, and their photo galleries.
 *
 * <p>Attendance is the club's official record of who was present. That shapes the
 * guards below more than anything else: the service refuses writes that would
 * corrupt the record, and permits writes that merely leave it incomplete.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventAttendeeRepository eventAttendeeRepository;
    private final EventPhotoRepository eventPhotoRepository;
    private final UserRepository userRepository;

    // â”€â”€ Events â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Creates an event, owned by the admin who created it.
     *
     * @param request event details
     * @param adminEmail authenticated admin's email
     * @return the created event
     * @throws ResourceNotFoundException if the admin no longer exists
     */
    @Transactional
    public EventResponseDto createEvent(EventCreateRequest request, String adminEmail) {
        User admin = requireUserByEmail(adminEmail);

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .eventDate(request.eventDate())
                .location(request.location())
                .coverImageUrl(request.coverImageUrl())
                .eventType(request.eventType())
                .codeforcesContestUrl(trimToNull(request.codeforcesContestUrl()))
                .showContestLink(request.showContestLink())
                .showWinners(request.showWinners())
                .showAttendeeCount(request.showAttendeeCount())
                .status(EventStatus.UPCOMING)
                .createdBy(admin)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Created event id {} ('{}') by {}", saved.getId(), saved.getTitle(), adminEmail);
        // Only an admin can reach this, so the created event comes back unredacted.
        return EventResponseDto.fromEntity(saved, true);
    }

    /**
     * Events still to come, soonest first.
     *
     * @return upcoming events
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listUpcomingEvents(boolean viewerIsAdmin) {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.UPCOMING)
                .stream()
                .map(event -> EventResponseDto.fromEntity(event, viewerIsAdmin))
                .toList();
    }

    /**
     * Events that have happened, most recent first.
     *
     * <p>Cancelled events are not included. They are visible to admins through the
     * full listing, but a cancelled event never took place and does not belong in
     * the public record of what the club has run.</p>
     *
     * @return completed events
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listCompletedEvents(boolean viewerIsAdmin) {
        return eventRepository.findByStatusInOrderByEventDateDesc(List.of(EventStatus.COMPLETED))
                .stream()
                .map(event -> EventResponseDto.fromEntity(event, viewerIsAdmin))
                .toList();
    }

    /**
     * Every event in any status, most recent first. The admin listing.
     *
     * @return all events
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listAllEvents() {
        // Admin-only endpoint, so nothing is withheld.
        return eventRepository.findAllByOrderByEventDateDesc()
                .stream()
                .map(event -> EventResponseDto.fromEntity(event, true))
                .toList();
    }

    /**
     * One event with its gallery and headcount.
     *
     * @param id event identifier
     * @return event detail
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional(readOnly = true)
    public EventDetailDto getEventDetail(Long id, boolean viewerIsAdmin) {
        Event event = requireEvent(id);

        List<EventPhotoDto> photos = eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(id)
                .stream()
                .map(EventPhotoDto::fromEntity)
                .toList();

        long attendeeCount = eventAttendeeRepository.countByEventId(id);
        return EventDetailDto.of(event, photos, (int) attendeeCount, viewerIsAdmin);
    }

    /**
     * Replaces an event's details.
     *
     * <p>Does not touch status. Moving between upcoming, completed and cancelled
     * is what {@link #markEventCompleted} and {@link #cancelEvent} are for, and
     * folding it in here would let an ordinary edit silently reopen a completed
     * event.</p>
     *
     * @param id event identifier
     * @param request new details
     * @return the updated event
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional
    public EventResponseDto updateEvent(Long id, EventCreateRequest request) {
        Event event = requireEvent(id);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setCoverImageUrl(request.coverImageUrl());
        event.setEventType(request.eventType());
        event.setCodeforcesContestUrl(trimToNull(request.codeforcesContestUrl()));
        event.setShowContestLink(request.showContestLink());
        event.setShowWinners(request.showWinners());
        event.setShowAttendeeCount(request.showAttendeeCount());

        Event saved = eventRepository.save(event);
        log.info("Updated event id {}", id);
        return EventResponseDto.fromEntity(saved, true);
    }

    /**
     * Marks an event as having taken place.
     *
     * <p>This closes attendance: {@link #addAttendee} only accepts upcoming
     * events, so completing one freezes its record.</p>
     *
     * @param id event identifier
     * @return the updated event
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional
    public EventResponseDto markEventCompleted(Long id) {
        Event event = requireEvent(id);
        event.setStatus(EventStatus.COMPLETED);
        Event saved = eventRepository.save(event);
        log.info("Marked event id {} as completed", id);
        return EventResponseDto.fromEntity(saved, true);
    }

    /**
     * Puts an event back to upcoming.
     *
     * <p>The way back from a mis-click. Completing an event freezes its
     * attendance -- {@link #addAttendee} refuses anything not upcoming -- so an
     * event finished by accident, or finished before the last few people were
     * recorded, could not be corrected at all. Cancelling had the same problem in
     * the other direction.</p>
     *
     * <p>Changes the status and nothing else. The contest link, the podium and
     * the three visibility switches are left exactly as they were: an admin
     * reopening an event to fix its attendance has not asked to un-announce its
     * results, and silently clearing settings they would have to rebuild is a
     * worse surprise than leaving them. The admin panel warns when something is
     * still published.</p>
     *
     * <p>Idempotent: reopening an event that is already upcoming does nothing and
     * succeeds, so two admins pressing at once do not produce an error.</p>
     *
     * @param id event identifier
     * @return the updated event
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional
    public EventResponseDto reopenEvent(Long id) {
        Event event = requireEvent(id);
        EventStatus previous = event.getStatus();

        event.setStatus(EventStatus.UPCOMING);
        Event saved = eventRepository.save(event);

        log.info("Reopened event id {} (was {})", id, previous);
        return EventResponseDto.fromEntity(saved, true);
    }

    /**
     * Deletes an event permanently, with everything recorded against it.
     *
     * <p>Events used to be undeletable -- cancelling was the only way to take one
     * down -- which kept the attendance record safe but left admins no way to
     * remove a test event or a duplicate. This allows deletion while keeping the
     * safety: an event that has attendance, photos or winners is refused unless
     * the caller passes {@code force}, and the refusal says exactly what would be
     * lost. An empty event deletes straight away.</p>
     *
     * <p>Attendance and photo records are removed explicitly before the event,
     * rather than relying on the database's cascade: the test schema has none,
     * and a delete that only works in production is untested. Winners go with the
     * event through their own cascade. Image files stay on Cloudinary, which this
     * server holds no credentials for.</p>
     *
     * @param id event identifier
     * @param force delete even though attendance, photos or winners exist
     * @throws ResourceNotFoundException if the event does not exist
     * @throws ConflictException if the event has records and force is false
     */
    @Transactional
    public void deleteEvent(Long id, boolean force) {
        Event event = requireEvent(id);

        long attendees = eventAttendeeRepository.countByEventId(id);
        long photos = eventPhotoRepository.countByEventId(id);
        int winners = event.getWinners().size();

        if (!force && (attendees > 0 || photos > 0 || winners > 0)) {
            throw new ConflictException(describeLoss(attendees, photos, winners)
                    + " Deleting the event removes them permanently. Cancel it instead to keep the record,"
                    + " or confirm the deletion.");
        }

        int removedAttendees = eventAttendeeRepository.deleteAllByEventId(id);
        int removedPhotos = eventPhotoRepository.deleteAllByEventId(id);
        eventRepository.delete(event);

        log.warn("Deleted event id {} ('{}') with {} attendee(s), {} photo(s) and {} winner(s)",
                id, event.getTitle(), removedAttendees, removedPhotos, winners);
    }

    /** "This event has 12 attendees, 4 photos and 3 winners." */
    private static String describeLoss(long attendees, long photos, int winners) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (attendees > 0) {
            parts.add(attendees + (attendees == 1 ? " attendee" : " attendees"));
        }
        if (photos > 0) {
            parts.add(photos + (photos == 1 ? " photo" : " photos"));
        }
        if (winners > 0) {
            parts.add(winners + (winners == 1 ? " winner" : " winners"));
        }
        String joined = parts.size() == 1
                ? parts.get(0)
                : String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
        return "This event has " + joined + ".";
    }

    /**
     * Cancels an event.
     *
     * @param id event identifier
     * @return the updated event
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional
    public EventResponseDto cancelEvent(Long id) {
        Event event = requireEvent(id);
        event.setStatus(EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        log.info("Cancelled event id {}", id);
        return EventResponseDto.fromEntity(saved, true);
    }

    // â”€â”€ Attendance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Records that a member attended an event.
     *
     * <p>The guards run in a fixed order, and the order is the point: each one
     * assumes the previous has passed, so the caller always gets the most specific
     * reason the add failed rather than whichever check happened to run first.</p>
     *
     * <p>A missing phone number is deliberately <em>not</em> a guard. Every account
     * created before Phase 2 has a null phone number, and only the member
     * themselves can set one â€” {@code PUT /api/users/profile} is self-only. Blocking
     * here would mean an admin standing at an event could add nobody and had no way
     * to fix it. Losing the attendance record is worse than a sparse phone column,
     * so the add proceeds and {@code EventAttendeeDto.hasPhone} carries the warning
     * to the admin panel instead.</p>
     *
     * @param eventId event to add to
     * @param userId member who attended
     * @param adminEmail authenticated admin's email
     * @return the recorded attendance
     * @throws ResourceNotFoundException if the event, the member or the admin is missing
     * @throws BadRequestException if the event is closed or the member is already recorded
     */
    @Transactional
    public EventAttendeeDto addAttendee(Long eventId, Long userId, String adminEmail) {
        // 1. The event has to exist before anything else can be judged.
        Event event = requireEvent(eventId);

        // 2. Attendance belongs to an event that is still open. A completed event
        //    is a closed record; a cancelled one never happened.
        if (event.getStatus() != EventStatus.UPCOMING) {
            throw new BadRequestException("Cannot add attendees to a completed or cancelled event.");
        }

        // 3. The member has to exist.
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // 4. No phone-number guard. See the javadoc above.

        // 5. Duplicate check before the insert, so a repeat add reads as a clear
        //    400 rather than a unique-constraint violation at flush time.
        if (eventAttendeeRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BadRequestException("Student is already registered.");
        }

        // 6. Record who added them, for the audit trail.
        User admin = requireUserByEmail(adminEmail);

        EventAttendee attendee = EventAttendee.builder()
                .event(event)
                .user(student)
                .addedBy(admin)
                .build();

        EventAttendee saved = eventAttendeeRepository.save(attendee);
        log.info("Added user {} to event {} by {}", userId, eventId, adminEmail);
        return EventAttendeeDto.fromEntity(saved);
    }

    /**
     * Removes a member from an event's attendance list.
     *
     * @param eventId event to remove from
     * @param userId member to remove
     * @throws ResourceNotFoundException if there is no such attendance row
     */
    @Transactional
    public void removeAttendee(Long eventId, Long userId) {
        EventAttendee attendee = eventAttendeeRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendee not found for event " + eventId + " and user " + userId));
        eventAttendeeRepository.delete(attendee);
        log.info("Removed user {} from event {}", userId, eventId);
    }

    /**
     * The full attendance list for an event.
     *
     * <p>Loads the event first so a bad event id is a 404 rather than an empty
     * list, which would otherwise be indistinguishable from a real event nobody
     * attended.</p>
     *
     * @param eventId event to list
     * @return attendees in the order they were added
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional(readOnly = true)
    public List<EventAttendeeDto> getAttendees(Long eventId) {
        requireEvent(eventId);
        return eventAttendeeRepository.findByEventIdOrderByAddedAtAsc(eventId)
                .stream()
                .map(EventAttendeeDto::fromEntity)
                .toList();
    }

    // â”€â”€ Photos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Attaches a photo to an event.
     *
     * @param eventId event to attach to
     * @param request image URL and optional caption
     * @param adminEmail authenticated admin's email
     * @return the stored photo
     * @throws ResourceNotFoundException if the event or the admin is missing
     */
    @Transactional
    public EventPhotoDto addEventPhoto(Long eventId, AddEventPhotoRequest request, String adminEmail) {
        Event event = requireEvent(eventId);
        User admin = requireUserByEmail(adminEmail);

        EventPhoto photo = EventPhoto.builder()
                .event(event)
                .imageUrl(request.imageUrl())
                .caption(request.caption())
                .uploadedBy(admin)
                .build();

        EventPhoto saved = eventPhotoRepository.save(photo);
        log.info("Added photo {} to event {} by {}", saved.getId(), eventId, adminEmail);
        return EventPhotoDto.fromEntity(saved);
    }

    /**
     * Deletes one photo.
     *
     * <p>Removes the row only. The image itself stays in Cloudinary, which this
     * server has no credentials to delete from â€” the upload goes browser-side.</p>
     *
     * @param photoId photo to delete
     * @throws ResourceNotFoundException if the photo does not exist
     */
    @Transactional
    public void deleteEventPhoto(Long photoId) {
        EventPhoto photo = eventPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id: " + photoId));
        eventPhotoRepository.delete(photo);
        log.info("Deleted event photo {}", photoId);
    }

    /**
     * An event's photos, in upload order.
     *
     * <p>Loads the event first, for the same reason {@link #getAttendees} does.</p>
     *
     * @param eventId event to list
     * @return the event gallery
     * @throws ResourceNotFoundException if the event does not exist
     */
    @Transactional(readOnly = true)
    public List<EventPhotoDto> getEventPhotos(Long eventId) {
        requireEvent(eventId);
        return eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(eventId)
                .stream()
                .map(EventPhotoDto::fromEntity)
                .toList();
    }

    // â”€â”€ Shared lookups â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Loads an event or fails with a 404.
     *
     * @param id event identifier
     * @return the event
     * @throws ResourceNotFoundException if it does not exist
     */
    /**
     * Replaces an event's podium.
     *
     * <p>The whole podium is set at once because the rules worth enforcing are
     * between the placings: no two firsts, and nobody standing in two places.
     * Those cannot be checked one placing at a time. An empty list clears it,
     * which is how a mistake gets undone.</p>
     *
     * <p>Winners must already be recorded as attending. Announcing somebody who
     * was never marked present is nearly always a mis-click on a name that
     * looked right in a dropdown, and it is cheap to refuse here and expensive
     * to notice once it is on the public page.</p>
     *
     * @param eventId event identifier
     * @param request the placings, at most three
     * @return the event's detail view, unredacted
     * @throws ResourceNotFoundException if the event does not exist
     * @throws BadRequestException if a placing or a winner is repeated, or a
     *                             winner did not attend
     */
    @Transactional
    public EventDetailDto setWinners(Long eventId, SetEventWinnersRequest request) {
        Event event = requireEvent(eventId);
        List<SetEventWinnersRequest.Winner> placings = request.winners();

        if (placings.stream().map(SetEventWinnersRequest.Winner::position).distinct().count()
                != placings.size()) {
            throw new BadRequestException("Each placing can only be awarded once.");
        }

        if (placings.stream().map(SetEventWinnersRequest.Winner::userId).distinct().count()
                != placings.size()) {
            throw new BadRequestException("A member cannot hold two placings in the same event.");
        }

        // Cleared and rebuilt rather than diffed. orphanRemoval turns this into
        // the delete-then-insert it would have to be anyway, and a podium is at
        // most three rows.
        event.getWinners().clear();

        for (SetEventWinnersRequest.Winner placing : placings) {
            if (!eventAttendeeRepository.existsByEventIdAndUserId(eventId, placing.userId())) {
                throw new BadRequestException(
                        "Winners must be recorded as attending the event. Add them to the attendance list first.");
            }

            User winner = userRepository.findById(placing.userId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + placing.userId()));

            event.getWinners().add(EventWinner.builder()
                    .event(event)
                    .user(winner)
                    .position(placing.position())
                    .build());
        }

        eventRepository.save(event);
        log.info("Set {} winner(s) on event id {}", placings.size(), eventId);

        return getEventDetail(eventId, true);
    }

    /** Blank input means "not set", so it is stored as absent rather than as "". */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Event requireEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    /**
     * Loads the acting admin by their authenticated email.
     *
     * <p>A miss here means the account was deleted between authenticating and this
     * call, which is a 404 rather than a 401: the token was valid.</p>
     *
     * @param email authenticated principal's email
     * @return the admin
     * @throws ResourceNotFoundException if the account no longer exists
     */
    private User requireUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
