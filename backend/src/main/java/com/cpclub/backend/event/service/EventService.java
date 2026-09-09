package com.cpclub.backend.event.service;

import com.cpclub.backend.common.exception.BadRequestException;
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

    // ── Events ────────────────────────────────────────────────────────────────

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
                .status(EventStatus.UPCOMING)
                .createdBy(admin)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Created event id {} ('{}') by {}", saved.getId(), saved.getTitle(), adminEmail);
        return EventResponseDto.fromEntity(saved);
    }

    /**
     * Events still to come, soonest first.
     *
     * @return upcoming events
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listUpcomingEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.UPCOMING)
                .stream()
                .map(EventResponseDto::fromEntity)
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
    public List<EventResponseDto> listCompletedEvents() {
        return eventRepository.findByStatusInOrderByEventDateDesc(List.of(EventStatus.COMPLETED))
                .stream()
                .map(EventResponseDto::fromEntity)
                .toList();
    }

    /**
     * Every event in any status, most recent first. The admin listing.
     *
     * @return all events
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc()
                .stream()
                .map(EventResponseDto::fromEntity)
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
    public EventDetailDto getEventDetail(Long id) {
        Event event = requireEvent(id);

        List<EventPhotoDto> photos = eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(id)
                .stream()
                .map(EventPhotoDto::fromEntity)
                .toList();

        long attendeeCount = eventAttendeeRepository.countByEventId(id);
        return EventDetailDto.of(event, photos, (int) attendeeCount);
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

        Event saved = eventRepository.save(event);
        log.info("Updated event id {}", id);
        return EventResponseDto.fromEntity(saved);
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
        return EventResponseDto.fromEntity(saved);
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
        return EventResponseDto.fromEntity(saved);
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    /**
     * Records that a member attended an event.
     *
     * <p>The guards run in a fixed order, and the order is the point: each one
     * assumes the previous has passed, so the caller always gets the most specific
     * reason the add failed rather than whichever check happened to run first.</p>
     *
     * <p>A missing phone number is deliberately <em>not</em> a guard. Every account
     * created before Phase 2 has a null phone number, and only the member
     * themselves can set one — {@code PUT /api/users/profile} is self-only. Blocking
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

    // ── Photos ────────────────────────────────────────────────────────────────

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
     * server has no credentials to delete from — the upload goes browser-side.</p>
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

    // ── Shared lookups ────────────────────────────────────────────────────────

    /**
     * Loads an event or fails with a 404.
     *
     * @param id event identifier
     * @return the event
     * @throws ResourceNotFoundException if it does not exist
     */
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
