package org.automatize.status.services;

import org.automatize.status.models.PlatformEvent;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.PlatformEventRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Service responsible for managing platform events.
 * <p>
 * Platform events are log entries from monitored platforms and their components.
 * This service provides event logging, retrieval, and search capabilities.
 * </p>
 */
@Service
@Transactional
public class PlatformEventService {

    @Autowired
    private PlatformEventRepository platformEventRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private StatusComponentRepository statusComponentRepository;

    /**
     * Validates an API key and returns the associated app.
     *
     * @param apiKey the API key to validate
     * @return the StatusApp if found
     * @throws RuntimeException if the API key is invalid
     */
    @Transactional(readOnly = true)
    public StatusApp validateAppApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API key is required");
        }
        return statusAppRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API key"));
    }

    /**
     * Validates an API key and returns the associated component.
     *
     * @param apiKey the API key to validate
     * @return the StatusComponent if found
     * @throws RuntimeException if the API key is invalid
     */
    @Transactional(readOnly = true)
    public StatusComponent validateComponentApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API key is required");
        }
        return statusComponentRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API key"));
    }

    /**
     * Creates a new platform event using API key authentication.
     *
     * @param apiKey the API key for authentication
     * @param severity the severity level
     * @param source optional source identifier
     * @param message the event message
     * @param details optional additional details
     * @param eventTime optional event time (defaults to now)
     * @return the created PlatformEvent
     */
    public PlatformEvent createEventWithApiKey(String apiKey, String severity,
                                                String source, String message, String details,
                                                ZonedDateTime eventTime) {
        // First try to find by app API key
        StatusApp app = statusAppRepository.findByApiKey(apiKey).orElse(null);
        StatusComponent component = null;

        if (app == null) {
            // Try to find by component API key
            component = statusComponentRepository.findByApiKey(apiKey)
                    .orElseThrow(() -> new RuntimeException("Invalid API key"));
            app = component.getApp();
        }

        PlatformEvent event = new PlatformEvent();
        event.setApp(app);
        event.setComponent(component);
        event.setSeverity(severity);
        event.setSource(source);
        event.setMessage(message);
        event.setDetails(details);
        event.setEventTime(eventTime != null ? eventTime : ZonedDateTime.now());

        return platformEventRepository.save(event);
    }

    /**
     * Creates a new platform event.
     *
     * @param appId the UUID of the app
     * @param componentId optional component UUID
     * @param severity the severity level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
     * @param source optional source identifier
     * @param message the event message
     * @param details optional additional details
     * @param eventTime optional event time (defaults to now)
     * @return the created PlatformEvent
     */
    public PlatformEvent createEvent(UUID appId, UUID componentId, String severity,
                                      String source, String message, String details,
                                      ZonedDateTime eventTime) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Status app not found with id: " + appId));

        StatusComponent component = null;
        if (componentId != null) {
            component = statusComponentRepository.findById(componentId)
                    .orElseThrow(() -> new RuntimeException("Component not found with id: " + componentId));

            if (!component.getApp().getId().equals(appId)) {
                throw new RuntimeException("Component does not belong to the specified app");
            }
        }

        PlatformEvent event = new PlatformEvent();
        event.setApp(app);
        event.setComponent(component);
        event.setSeverity(severity);
        event.setSource(source);
        event.setMessage(message);
        event.setDetails(details);
        event.setEventTime(eventTime != null ? eventTime : ZonedDateTime.now());

        return platformEventRepository.save(event);
    }

    /**
     * Retrieves an event by its unique identifier.
     *
     * @param id the UUID of the event
     * @return the PlatformEvent
     */
    @Transactional(readOnly = true)
    public PlatformEvent getEventById(UUID id) {
        return platformEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    /**
     * Retrieves events with optional filtering.
     *
     * @param appId optional app ID filter
     * @param componentId optional component ID filter
     * @param severity optional severity filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param pageable pagination information
     * @return a page of PlatformEvent objects
     */
    @Transactional(readOnly = true)
    public Page<PlatformEvent> getEvents(UUID appId, UUID componentId, String severity,
                                          ZonedDateTime startDate, ZonedDateTime endDate,
                                          Pageable pageable) {
        return platformEventRepository.findWithFilters(appId, componentId, severity,
                startDate, endDate, pageable);
    }

    /**
     * Searches events with optional filtering and text search.
     *
     * @param appId optional app ID filter
     * @param componentId optional component ID filter
     * @param severity optional severity filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param searchText text to search in message, details, and source
     * @param pageable pagination information
     * @return a page of PlatformEvent objects
     */
    @Transactional(readOnly = true)
    public Page<PlatformEvent> searchEvents(UUID appId, UUID componentId, String severity,
                                             ZonedDateTime startDate, ZonedDateTime endDate,
                                             String searchText, Pageable pageable) {
        if (searchText != null && !searchText.trim().isEmpty()) {
            return platformEventRepository.searchWithFilters(appId, componentId, severity,
                    startDate, endDate, searchText.trim(), pageable);
        }
        return getEvents(appId, componentId, severity, startDate, endDate, pageable);
    }

    /**
     * Deletes an event by its unique identifier.
     *
     * @param id the UUID of the event to delete
     */
    public void deleteEvent(UUID id) {
        PlatformEvent event = platformEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        platformEventRepository.delete(event);
    }

    /**
     * Deletes all events for a specific app.
     *
     * @param appId the UUID of the app
     */
    public void deleteEventsByAppId(UUID appId) {
        platformEventRepository.deleteByAppId(appId);
    }

    /**
     * Counts events for a specific app.
     *
     * @param appId the UUID of the app
     * @return the count of events
     */
    @Transactional(readOnly = true)
    public Long countEventsByAppId(UUID appId) {
        return platformEventRepository.countByAppId(appId);
    }

    /**
     * Regenerates the API key for a component.
     *
     * @param componentId the UUID of the component
     * @return the new API key
     */
    public String regenerateComponentApiKey(UUID componentId) {
        StatusComponent component = statusComponentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component not found with id: " + componentId));
        String newApiKey = org.automatize.status.util.ApiKeyGenerator.generateApiKey();
        component.setApiKey(newApiKey);
        statusComponentRepository.save(component);
        return newApiKey;
    }

    /**
     * Regenerates the API key for an app.
     *
     * @param appId the UUID of the app
     * @return the new API key
     */
    public String regenerateAppApiKey(UUID appId) {
        StatusApp app = statusAppRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found with id: " + appId));
        String newApiKey = org.automatize.status.util.ApiKeyGenerator.generateApiKey();
        app.setApiKey(newApiKey);
        statusAppRepository.save(app);
        return newApiKey;
    }
}
