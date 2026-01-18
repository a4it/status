package org.automatize.status.services;

import org.automatize.status.api.response.NotificationSubscriberResponse;
import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.StatusApp;
import org.automatize.status.repositories.NotificationSubscriberRepository;
import org.automatize.status.repositories.StatusAppRepository;
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
                .orElseThrow(() -> new RuntimeException("Subscriber not found with id: " + id));
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
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + appId));

        if (subscriberRepository.existsByAppIdAndEmail(appId, email)) {
            throw new RuntimeException("Email already subscribed to this application");
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
                .orElseThrow(() -> new RuntimeException("Subscriber not found with id: " + id));

        if (email != null && !email.equals(subscriber.getEmail())) {
            if (subscriberRepository.existsByAppIdAndEmail(subscriber.getApp().getId(), email)) {
                throw new RuntimeException("Email already subscribed to this application");
            }
            subscriber.setEmail(email);
        }

        if (name != null) {
            subscriber.setName(name);
        }

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
        if (!subscriberRepository.existsById(id)) {
            throw new RuntimeException("Subscriber not found with id: " + id);
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
            if (principal instanceof String) {
                return (String) principal;
            }
        } catch (Exception e) {
            // Ignore
        }
        return "system";
    }
}
