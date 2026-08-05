package org.automatize.status.services;

import org.automatize.status.api.response.HealthCheckStatusResponse;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.util.OffsetPageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * Service assembling the health check status overview shown in the admin UI.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Apply the platform/status/enabled filters in the database rather than in memory</li>
 *   <li>Return apps and components as a single paginated stream, apps first</li>
 *   <li>Map both entity types onto the shared {@link HealthCheckStatusResponse}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see HealthCheckStatusResponse
 */
@Service
public class HealthCheckStatusService {

    private static final Sort BY_NAME = Sort.by(Sort.Direction.ASC, "name");

    private final StatusAppRepository statusAppRepository;
    private final StatusComponentRepository statusComponentRepository;

    /**
     * Constructs the service with the repositories it reads from.
     *
     * @param statusAppRepository repository providing access to status apps
     * @param statusComponentRepository repository providing access to status components
     */
    public HealthCheckStatusService(StatusAppRepository statusAppRepository,
                                    StatusComponentRepository statusComponentRepository) {
        this.statusAppRepository = statusAppRepository;
        this.statusComponentRepository = statusComponentRepository;
    }

    /**
     * Returns one page of health check status rows.
     * <p>
     * Apps occupy the first {@code countApps} positions of the overall result, components
     * the remainder, so a page may straddle the boundary and draw from both sources. Every
     * filter is evaluated by the database and only the rows of the requested page are loaded.
     * </p>
     *
     * @param platformId optional platform to scope to; {@code null} disables the filter
     * @param status optional status to match; {@code null} disables the filter
     * @param checkEnabled optional health check enabled flag; {@code null} disables the filter
     * @param pageable the requested page
     * @return a page of health check status rows
     */
    @Transactional(readOnly = true)
    public Page<HealthCheckStatusResponse> getHealthCheckStatus(UUID platformId,
                                                                String status,
                                                                Boolean checkEnabled,
                                                                Pageable pageable) {

        long appCount = statusAppRepository.countForHealthCheckStatus(platformId, status, checkEnabled);
        long componentCount = statusComponentRepository.countForHealthCheckStatus(platformId, status, checkEnabled);

        int pageSize = pageable.getPageSize();
        long offset = pageable.getOffset();
        List<HealthCheckStatusResponse> rows = new ArrayList<>(pageSize);

        // Draw from the apps section only while the requested window still overlaps it
        if (offset < appCount) {
            statusAppRepository
                    .findForHealthCheckStatus(platformId, status, checkEnabled,
                            new OffsetPageable(offset, pageSize, BY_NAME))
                    .forEach(app -> rows.add(buildAppResponse(app)));
        }

        // Fill the remainder of the page from the components section
        if (rows.size() < pageSize) {
            long componentOffset = Math.max(0, offset - appCount);
            int remaining = pageSize - rows.size();
            statusComponentRepository
                    .findForHealthCheckStatus(platformId, status, checkEnabled,
                            new OffsetPageable(componentOffset, remaining, BY_NAME))
                    .forEach(component -> rows.add(buildComponentResponse(component)));
        }

        return new PageImpl<>(rows, pageable, appCount + componentCount);
    }

    /**
     * Maps an app to its health check status response.
     *
     * @param app the app to map
     * @return the mapped response
     */
    private HealthCheckStatusResponse buildAppResponse(StatusApp app) {
        HealthCheckStatusResponse response = new HealthCheckStatusResponse();
        response.setEntityId(app.getId());
        response.setEntityType("APP");
        response.setName(app.getName());
        // Populate platform details only when the app is associated with a platform
        if (app.getPlatform() != null) {
            response.setPlatformId(app.getPlatform().getId().toString());
            response.setPlatformName(app.getPlatform().getName());
        }
        response.setCheckEnabled(app.getCheckEnabled());
        response.setCheckType(app.getCheckType());
        response.setCheckUrl(app.getCheckUrl());
        response.setCheckIntervalSeconds(app.getCheckIntervalSeconds());
        response.setCheckTimeoutSeconds(app.getCheckTimeoutSeconds());
        response.setCheckExpectedStatus(app.getCheckExpectedStatus());
        response.setCheckFailureThreshold(app.getCheckFailureThreshold());
        response.setLastCheckAt(app.getLastCheckAt());
        response.setLastCheckSuccess(app.getLastCheckSuccess());
        response.setLastCheckMessage(app.getLastCheckMessage());
        response.setConsecutiveFailures(app.getConsecutiveFailures());
        response.setStatus(app.getStatus());
        return response;
    }

    /**
     * Maps a component to its health check status response.
     *
     * @param component the component to map
     * @return the mapped response
     */
    private HealthCheckStatusResponse buildComponentResponse(StatusComponent component) {
        HealthCheckStatusResponse response = new HealthCheckStatusResponse();
        response.setEntityId(component.getId());
        response.setEntityType("COMPONENT");
        response.setName(component.getName());
        // Populate platform details only when the component's parent app has a platform
        if (component.getApp() != null && component.getApp().getPlatform() != null) {
            response.setPlatformId(component.getApp().getPlatform().getId().toString());
            response.setPlatformName(component.getApp().getPlatform().getName());
        }
        response.setCheckEnabled(component.getCheckEnabled());
        response.setCheckType(component.getCheckType());
        response.setCheckUrl(component.getCheckUrl());
        response.setCheckIntervalSeconds(component.getCheckIntervalSeconds());
        response.setCheckTimeoutSeconds(component.getCheckTimeoutSeconds());
        response.setCheckExpectedStatus(component.getCheckExpectedStatus());
        response.setCheckFailureThreshold(component.getCheckFailureThreshold());
        response.setLastCheckAt(component.getLastCheckAt());
        response.setLastCheckSuccess(component.getLastCheckSuccess());
        response.setLastCheckMessage(component.getLastCheckMessage());
        response.setConsecutiveFailures(component.getConsecutiveFailures());
        response.setStatus(component.getStatus());
        return response;
    }
}
