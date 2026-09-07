package com.cpclub.backend.event.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.event.dto.AddAttendeeRequest;
import com.cpclub.backend.event.dto.AddEventPhotoRequest;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.dto.EventCreateRequest;
import com.cpclub.backend.event.dto.EventDetailDto;
import com.cpclub.backend.event.dto.EventPhotoDto;
import com.cpclub.backend.event.dto.EventResponseDto;
import com.cpclub.backend.event.service.EventExportService;
import com.cpclub.backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Club events, their attendance, and their photo galleries.
 *
 * <p>Three visibility levels live on this one path, so each method carries its own
 * {@link PreAuthorize} rather than relying on the path patterns in
 * {@code SecurityConfig}. The two overlap deliberately: a URL-pattern rule and a
 * method annotation have to both be wrong before something leaks, and the
 * annotation sits next to the code it protects where a reviewer will see it.</p>
 *
 * <p>Public: the event listings, one event's detail, and its photos. Everything
 * else is admin-only, including every attendee endpoint — the attendance list
 * carries members' phone numbers and email addresses.</p>
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Club events, attendance and galleries")
public class EventController {

    private final EventService eventService;
    private final EventExportService eventExportService;

    // ── Events ────────────────────────────────────────────────────────────────

    /**
     * Creates an event.
     *
     * @param request event details
     * @param userDetails authenticated admin
     * @return the created event
     */
    @PostMapping
    @Operation(summary = "Create an event (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EventResponseDto created = eventService.createEvent(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(created, "Event created successfully"));
    }

    /**
     * Lists events still to come. Public.
     *
     * @return upcoming events, soonest first
     */
    @GetMapping("/upcoming")
    @Operation(summary = "List upcoming events")
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> listUpcomingEvents() {
        List<EventResponseDto> events = eventService.listUpcomingEvents();
        return ResponseEntity.ok(ApiResponse.success(events, "Fetched upcoming events successfully"));
    }

    /**
     * Lists events that have taken place. Public.
     *
     * @return completed events, most recent first
     */
    @GetMapping("/completed")
    @Operation(summary = "List completed events")
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> listCompletedEvents() {
        List<EventResponseDto> events = eventService.listCompletedEvents();
        return ResponseEntity.ok(ApiResponse.success(events, "Fetched completed events successfully"));
    }

    /**
     * Lists every event in any status, cancelled ones included.
     *
     * <p>Admin-only, which is the difference between this and the two public
     * listings: cancelled events are club administration, not public record.</p>
     *
     * @return all events, most recent first
     */
    @GetMapping
    @Operation(summary = "List all events including cancelled (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> listAllEvents() {
        List<EventResponseDto> events = eventService.listAllEvents();
        return ResponseEntity.ok(ApiResponse.success(events, "Fetched all events successfully"));
    }

    /**
     * One event with its gallery and headcount. Public.
     *
     * @param id event identifier
     * @return event detail
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get one event with photos and attendee count")
    public ResponseEntity<ApiResponse<EventDetailDto>> getEventDetail(@PathVariable Long id) {
        EventDetailDto event = eventService.getEventDetail(id);
        return ResponseEntity.ok(ApiResponse.success(event, "Fetched event successfully"));
    }

    /**
     * Replaces an event's details.
     *
     * @param id event identifier
     * @param request new details
     * @return the updated event
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an event (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventCreateRequest request
    ) {
        EventResponseDto updated = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Event updated successfully"));
    }

    /**
     * Marks an event as having taken place, which closes its attendance list.
     *
     * @param id event identifier
     * @return the updated event
     */
    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark an event completed (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponseDto>> markEventCompleted(@PathVariable Long id) {
        EventResponseDto updated = eventService.markEventCompleted(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Event marked as completed"));
    }

    /**
     * Cancels an event.
     *
     * @param id event identifier
     * @return the updated event
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an event (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponseDto>> cancelEvent(@PathVariable Long id) {
        EventResponseDto updated = eventService.cancelEvent(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Event cancelled"));
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    /**
     * Records that a member attended.
     *
     * @param id event identifier
     * @param request the member to add
     * @param userDetails authenticated admin
     * @return the recorded attendance
     */
    @PostMapping("/{id}/attendees")
    @Operation(summary = "Add an attendee (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventAttendeeDto>> addAttendee(
            @PathVariable Long id,
            @Valid @RequestBody AddAttendeeRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EventAttendeeDto attendee = eventService.addAttendee(id, request.userId(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(attendee, "Attendee added successfully"));
    }

    /**
     * Removes a member from an event's attendance list.
     *
     * @param id event identifier
     * @param uid member identifier
     * @return empty success response
     */
    @DeleteMapping("/{id}/attendees/{uid}")
    @Operation(summary = "Remove an attendee (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeAttendee(
            @PathVariable Long id,
            @PathVariable Long uid
    ) {
        eventService.removeAttendee(id, uid);
        return ResponseEntity.ok(ApiResponse.success(null, "Attendee removed successfully"));
    }

    /**
     * The attendance list.
     *
     * <p>Admin-only: every row carries a phone number and an email address.</p>
     *
     * @param id event identifier
     * @return attendees in the order they were added
     */
    @GetMapping("/{id}/attendees")
    @Operation(summary = "List attendees (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EventAttendeeDto>>> getAttendees(@PathVariable Long id) {
        List<EventAttendeeDto> attendees = eventService.getAttendees(id);
        return ResponseEntity.ok(ApiResponse.success(attendees, "Fetched attendees successfully"));
    }

    /**
     * Downloads the attendance list as a spreadsheet.
     *
     * <p>Returns the bytes directly rather than wrapping them in
     * {@code ApiResponse}: the browser saves this to a file, and a JSON envelope
     * around base64 would not open in Excel.</p>
     *
     * @param id event identifier
     * @return an .xlsx attachment
     */
    @GetMapping("/{id}/attendees/export")
    @Operation(summary = "Export attendees as .xlsx (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAttendees(@PathVariable Long id) {
        List<EventAttendeeDto> attendees = eventService.getAttendees(id);
        byte[] workbook = eventExportService.exportToExcel(attendees);

        // RFC 5987 encoding. A club event is as likely as not to have a name with
        // a space or an accent in it, and an unencoded filename in this header is
        // either mangled or dropped entirely by the browser.
        String filename = URLEncoder.encode("event-" + id + "-attendees.xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }

    // ── Photos ────────────────────────────────────────────────────────────────

    /**
     * Attaches a photo to an event.
     *
     * @param id event identifier
     * @param request image URL and optional caption
     * @param userDetails authenticated admin
     * @return the stored photo
     */
    @PostMapping("/{id}/photos")
    @Operation(summary = "Add an event photo (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventPhotoDto>> addEventPhoto(
            @PathVariable Long id,
            @Valid @RequestBody AddEventPhotoRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EventPhotoDto photo = eventService.addEventPhoto(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(photo, "Photo added successfully"));
    }

    /**
     * Deletes one photo.
     *
     * <p>Addressed by photo id alone, without the event, because a photo id is
     * already unique — requiring the event too would only invite a mismatched pair
     * that has to be validated for no gain.</p>
     *
     * @param photoId photo identifier
     * @return empty success response
     */
    @DeleteMapping("/photos/{photoId}")
    @Operation(summary = "Delete an event photo (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEventPhoto(@PathVariable Long photoId) {
        eventService.deleteEventPhoto(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo deleted successfully"));
    }

    /**
     * An event's photos. Public.
     *
     * @param id event identifier
     * @return the event gallery, in upload order
     */
    @GetMapping("/{id}/photos")
    @Operation(summary = "Get event photos")
    public ResponseEntity<ApiResponse<List<EventPhotoDto>>> getEventPhotos(@PathVariable Long id) {
        List<EventPhotoDto> photos = eventService.getEventPhotos(id);
        return ResponseEntity.ok(ApiResponse.success(photos, "Fetched event photos successfully"));
    }
}
