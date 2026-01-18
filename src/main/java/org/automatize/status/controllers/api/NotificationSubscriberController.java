package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.NotificationSubscriberRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.NotificationSubscriberResponse;
import org.automatize.status.services.NotificationSubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * REST API controller for notification subscriber management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for notification subscribers</li>
 *   <li>Handle subscriber filtering by application</li>
 *   <li>Manage role-based access control for subscriber operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see NotificationSubscriberService
 * @see NotificationSubscriberResponse
 */
@RestController
@RequestMapping("/api/notification-subscribers")
@PreAuthorize("isAuthenticated()")
public class NotificationSubscriberController {

    @Autowired
    private NotificationSubscriberService subscriberService;

    /**
     * Retrieves all notification subscribers.
     * <p>
     * Optionally filter by application ID using the appId query parameter.
     * </p>
     *
     * @param appId optional filter by status application ID
     * @return ResponseEntity containing a list of subscribers
     */
    @GetMapping
    public ResponseEntity<List<NotificationSubscriberResponse>> getAllSubscribers(
            @RequestParam(required = false) UUID appId) {
        List<NotificationSubscriberResponse> subscribers;
        if (appId != null) {
            subscribers = subscriberService.getSubscribersByAppId(appId);
        } else {
            subscribers = subscriberService.getAllSubscribers();
        }
        return ResponseEntity.ok(subscribers);
    }

    /**
     * Retrieves a subscriber by its unique identifier.
     *
     * @param id the UUID of the subscriber
     * @return ResponseEntity containing the subscriber details
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationSubscriberResponse> getSubscriberById(@PathVariable UUID id) {
        NotificationSubscriberResponse subscriber = subscriberService.getSubscriberById(id);
        return ResponseEntity.ok(subscriber);
    }

    /**
     * Creates a new notification subscriber.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param request the subscriber creation request
     * @return ResponseEntity containing the created subscriber with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<NotificationSubscriberResponse> createSubscriber(
            @Valid @RequestBody NotificationSubscriberRequest request) {
        NotificationSubscriberResponse subscriber = subscriberService.createSubscriber(
                request.getAppId(),
                request.getEmail(),
                request.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriber);
    }

    /**
     * Updates an existing subscriber.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the subscriber to update
     * @param request the subscriber update request
     * @return ResponseEntity containing the updated subscriber
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<NotificationSubscriberResponse> updateSubscriber(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationSubscriberRequest request) {
        NotificationSubscriberResponse subscriber = subscriberService.updateSubscriber(
                id,
                request.getEmail(),
                request.getName(),
                request.getIsActive()
        );
        return ResponseEntity.ok(subscriber);
    }

    /**
     * Deletes a subscriber by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the subscriber to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteSubscriber(@PathVariable UUID id) {
        subscriberService.deleteSubscriber(id);
        return ResponseEntity.ok(new MessageResponse("Subscriber deleted successfully", true));
    }

    /**
     * Retrieves subscribers for a specific application.
     *
     * @param appId the application ID
     * @return ResponseEntity containing a list of subscribers for the application
     */
    @GetMapping("/by-app/{appId}")
    public ResponseEntity<List<NotificationSubscriberResponse>> getSubscribersByApp(
            @PathVariable UUID appId) {
        List<NotificationSubscriberResponse> subscribers = subscriberService.getSubscribersByAppId(appId);
        return ResponseEntity.ok(subscribers);
    }

    /**
     * Gets the count of subscribers for an application.
     *
     * @param appId the application ID
     * @return ResponseEntity containing the subscriber count
     */
    @GetMapping("/count/{appId}")
    public ResponseEntity<Long> getSubscriberCount(@PathVariable UUID appId) {
        Long count = subscriberService.countSubscribersByAppId(appId);
        return ResponseEntity.ok(count);
    }
}
