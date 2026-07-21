package org.automatize.status.services;

import org.automatize.status.api.response.NotificationSubscriberResponse;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.StatusApp;
import org.automatize.status.repositories.NotificationSubscriberRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * Service responsible for managing notification subscribers.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for subscriber entities</li>
 *   <li>Manage subscriber verification and active status</li>
 *   <li>Retrieve active subscribers for incident notifications</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@Service
@Transactional
public class NotificationSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSubscriberService.class);

    @Autowired
    private NotificationSubscriberRepository subscriberRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    /**
     * Retrieves all subscribers for a specific application.
     *
     * @param appId the application ID
     * @return list of subscriber responses
     */
    @Transactional(readOnly = true)
    public List<NotificationSubscriberResponse> getSubscribersByAppId(UUID appId) {
        List<NotificationSubscriber> subscribers = subscriberRepository.findByAppId(appId);
        return subscribers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all subscribers across all applications.
     *
     * @return list of all subscriber responses
     */
    @Transactional(readOnly = true)
    public List<NotificationSubscriberResponse> getAllSubscribers() {
        List<NotificationSubscriber> subscribers = subscriberRepository.findAll();
        return subscribers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a subscriber by ID.
     *
     * @param id the subscriber ID
     * @return the subscriber response
     * @throws RuntimeException if subscriber not found
     */
    @Transactional(readOnly = true)
    public NotificationSubscriberResponse getSubscriberById(UUID id) {
        NotificationSubscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with id: " + id));
        return mapToResponse(subscriber);
    }

    /**
     * Creates a new notification subscriber.
     *
     * @param appId the application ID
     * @param email the email address
     * @param name the display name (optional)
     * @return the created subscriber response
     * @throws RuntimeException if app not found or email already exists
     */
    public NotificationSubscriberResponse createSubscriber(UUID appId, String email, String name) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Status app not found with id: " + appId));

        // Reject duplicate subscriptions of the same email to this application
        if (subscriberRepository.existsByAppIdAndEmail(appId, email)) {
            throw new DuplicateResourceException("Email already subscribed to this application");
        }

        NotificationSubscriber subscriber = new NotificationSubscriber();
        subscriber.setApp(app);
        subscriber.setEmail(email);
        subscriber.setName(name);
        subscriber.setIsActive(true);
        subscriber.setIsVerified(true); // Auto-verify when created by admin

        String currentUser = getCurrentUsername();
        subscriber.setCreatedBy(currentUser);
        subscriber.setLastModifiedBy(currentUser);

        NotificationSubscriber saved = subscriberRepository.save(subscriber);
        return mapToResponse(saved);
    }

    /**
     * Updates an existing subscriber.
     *
     * @param id the subscriber ID
     * @param email the new email address (optional)
     * @param name the new display name (optional)
     * @param isActive the new active status (optional)
     * @return the updated subscriber response
     * @throws RuntimeException if subscriber not found
     */
    public NotificationSubscriberResponse updateSubscriber(UUID id, String email, String name, Boolean isActive) {
        NotificationSubscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with id: " + id));

        // Apply an email change only when a different address is supplied
        if (email != null && !email.equals(subscriber.getEmail())) {
            // Ensure the new email is not already subscribed to the same application
            if (subscriberRepository.existsByAppIdAndEmail(subscriber.getApp().getId(), email)) {
                throw new DuplicateResourceException("Email already subscribed to this application");
            }
            subscriber.setEmail(email);
        }

        // Update the display name when one is provided
        if (name != null) {
            subscriber.setName(name);
        }

        // Update the active flag when one is provided
        if (isActive != null) {
            subscriber.setIsActive(isActive);
        }

        subscriber.setLastModifiedBy(getCurrentUsername());

        NotificationSubscriber saved = subscriberRepository.save(subscriber);
        return mapToResponse(saved);
    }

    /**
     * Deletes a subscriber.
     *
     * @param id the subscriber ID
     * @throws RuntimeException if subscriber not found
     */
    public void deleteSubscriber(UUID id) {
        // Fail fast when the subscriber to delete does not exist
        if (!subscriberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscriber not found with id: " + id);
        }
        subscriberRepository.deleteById(id);
    }

    /**
     * Retrieves all active and verified subscribers for an application.
     * These are the subscribers who should receive incident notifications.
     *
     * @param appId the application ID
     * @return list of active verified subscribers
     */
    @Transactional(readOnly = true)
    public List<NotificationSubscriber> getActiveVerifiedSubscribers(UUID appId) {
        return subscriberRepository.findActiveVerifiedByAppId(appId);
    }

    /**
     * Counts subscribers for an application.
     *
     * @param appId the application ID
     * @return the subscriber count
     */
    @Transactional(readOnly = true)
    public Long countSubscribersByAppId(UUID appId) {
        return subscriberRepository.countByAppId(appId);
    }

    /**
     * Maps a NotificationSubscriber entity to a response object.
     *
     * @param subscriber the entity to map
     * @return the response object
     */
    private NotificationSubscriberResponse mapToResponse(NotificationSubscriber subscriber) {
        NotificationSubscriberResponse response = new NotificationSubscriberResponse();
        response.setId(subscriber.getId());
        response.setAppId(subscriber.getApp() != null ? subscriber.getApp().getId() : null);
        response.setAppName(subscriber.getApp() != null ? subscriber.getApp().getName() : null);
        response.setEmail(subscriber.getEmail());
        response.setName(subscriber.getName());
        response.setIsActive(subscriber.getIsActive());
        response.setIsVerified(subscriber.getIsVerified());
        response.setCreatedDate(subscriber.getCreatedDate());
        response.setCreatedBy(subscriber.getCreatedBy());
        return response;
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            // Only a String principal carries a directly usable username
            if (principal instanceof String) {
                return (String) principal;
            }
        } catch (Exception e) {
            logger.debug("Could not resolve current username, falling back to 'system'", e);
        }
        return "system";
    }
}
