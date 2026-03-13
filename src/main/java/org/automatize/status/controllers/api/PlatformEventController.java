package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.ApiKeyEventRequest;
import org.automatize.status.api.request.PlatformEventRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.PlatformEventResponse;
import org.automatize.status.models.PlatformEvent;
import org.automatize.status.services.PlatformEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * REST API controller for platform event management.
 * <p>
 * This controller provides endpoints for logging and retrieving events
 * from platforms and their components. Events are searchable and filterable
 * in the admin interface.
 * </p>
 *
 * @see PlatformEventService
 * @see PlatformEventResponse
 */
@RestController
@RequestMapping("/api/events")
@PreAuthorize("isAuthenticated()")
public class PlatformEventController {

    @Autowired
    private PlatformEventService platformEventService;

    /**
     * Creates a new platform event using API key authentication.
     * <p>
     * This endpoint allows external platforms and components to log events
     * using their API key. The API key is passed in the X-API-Key header.
     * This endpoint does not require JWT authentication.
     * </p>
     *
     * @param apiKey the API key from the X-API-Key header
     * @param request the event creation request
     * @return ResponseEntity containing the created event with HTTP 201 status
     */
    @PostMapping("/log")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> logEventWithApiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody ApiKeyEventRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("X-API-Key header is required", false));
        }

        try {
            PlatformEvent event = platformEventService.createEventWithApiKey(
                    apiKey,
                    request.getSeverity(),
                    request.getSource(),
                    request.getMessage(),
                    request.getDetails(),
                    request.getEventTime()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(event));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Retrieves a paginated list of events with optional filtering and search.
     *
     * @param appId optional filter by app ID
     * @param componentId optional filter by component ID
     * @param severity optional filter by severity
     * @param startDate optional filter by start date
     * @param endDate optional filter by end date
     * @param search optional text search in message, details, and source
     * @param pageable pagination parameters
     * @return ResponseEntity containing a page of platform events
     */
    @GetMapping
    public ResponseEntity<Page<PlatformEventResponse>> getEvents(
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) UUID componentId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<PlatformEvent> events = platformEventService.searchEvents(
                appId, componentId, severity, startDate, endDate, search, pageable);
        return ResponseEntity.ok(events.map(this::mapToResponse));
    }

    /**
     * Retrieves an event by its unique identifier.
     *
     * @param id the UUID of the event
     * @return ResponseEntity containing the event details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlatformEventResponse> getEventById(@PathVariable UUID id) {
        PlatformEvent event = platformEventService.getEventById(id);
        return ResponseEntity.ok(mapToResponse(event));
    }

    /**
     * Creates a new platform event.
     *
     * @param request the event creation request
     * @return ResponseEntity containing the created event with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<PlatformEventResponse> createEvent(@Valid @RequestBody PlatformEventRequest request) {
        PlatformEvent event = platformEventService.createEvent(
                request.getAppId(),
                request.getComponentId(),
                request.getSeverity(),
                request.getSource(),
                request.getMessage(),
                request.getDetails(),
                request.getEventTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(event));
    }

    /**
     * Deletes an event by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the event to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteEvent(@PathVariable UUID id) {
        platformEventService.deleteEvent(id);
        return ResponseEntity.ok(new MessageResponse("Event deleted successfully", true));
    }

    /**
     * Regenerates the API key for a component.
     *
     * @param componentId the UUID of the component
     * @return ResponseEntity containing the new API key
     */
    @PostMapping("/regenerate-key/component/{componentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> regenerateComponentApiKey(@PathVariable UUID componentId) {
        try {
            String newApiKey = platformEventService.regenerateComponentApiKey(componentId);
            return ResponseEntity.ok(java.util.Map.of("apiKey", newApiKey, "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Regenerates the API key for an app.
     *
     * @param appId the UUID of the app
     * @return ResponseEntity containing the new API key
     */
    @PostMapping("/regenerate-key/app/{appId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> regenerateAppApiKey(@PathVariable UUID appId) {
        try {
            String newApiKey = platformEventService.regenerateAppApiKey(appId);
            return ResponseEntity.ok(java.util.Map.of("apiKey", newApiKey, "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * Maps a PlatformEvent entity to a PlatformEventResponse DTO.
     *
     * @param event the PlatformEvent entity to map
     * @return the mapped PlatformEventResponse
     */
    private PlatformEventResponse mapToResponse(PlatformEvent event) {
        PlatformEventResponse response = new PlatformEventResponse();
        response.setId(event.getId());
        response.setAppId(event.getApp() != null ? event.getApp().getId() : null);
        response.setAppName(event.getApp() != null ? event.getApp().getName() : null);
        response.setComponentId(event.getComponent() != null ? event.getComponent().getId() : null);
        response.setComponentName(event.getComponent() != null ? event.getComponent().getName() : null);
        response.setSeverity(event.getSeverity());
        response.setSource(event.getSource());
        response.setMessage(event.getMessage());
        response.setDetails(event.getDetails());
        response.setEventTime(event.getEventTime());
        response.setCreatedDate(event.getCreatedDate());
        return response;
    }
}
