package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.StatusPlatformRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.StatusPlatformResponse;
import org.automatize.status.models.StatusPlatform;
import org.automatize.status.services.StatusPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * REST API controller for status platform management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for status platforms</li>
 *   <li>Handle platform filtering by tenant and organization</li>
 *   <li>Manage role-based access control for platform operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see StatusPlatformService
 * @see StatusPlatformResponse
 */
@RestController
@RequestMapping("/api/status-platforms")
@PreAuthorize("isAuthenticated()")
public class StatusPlatformController {

    @Autowired
    private StatusPlatformService statusPlatformService;

    /**
     * Retrieves a paginated list of all status platforms with optional filtering.
     *
     * @param tenantId optional filter by tenant ID
     * @param organizationId optional filter by organization ID
     * @param search optional search term for filtering by name
     * @param pageable pagination parameters (page, size, sort)
     * @return ResponseEntity containing a page of status platforms
     */
    @GetMapping
    public ResponseEntity<Page<StatusPlatformResponse>> getAllPlatforms(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StatusPlatformResponse> platforms = statusPlatformService.getAllPlatforms(tenantId, organizationId, search, pageable);
        return ResponseEntity.ok(platforms);
    }

    /**
     * Retrieves all platforms ordered by position.
     *
     * @return ResponseEntity containing a list of all status platforms
     */
    @GetMapping("/all")
    public ResponseEntity<List<StatusPlatformResponse>> getAllPlatformsOrdered() {
        List<StatusPlatformResponse> platforms = statusPlatformService.getAllPlatformsOrdered();
        return ResponseEntity.ok(platforms);
    }

    /**
     * Retrieves a status platform by its unique identifier.
     *
     * @param id the UUID of the status platform
     * @return ResponseEntity containing the status platform details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusPlatformResponse> getPlatformById(@PathVariable UUID id) {
        StatusPlatformResponse platform = statusPlatformService.getPlatformById(id);
        return ResponseEntity.ok(platform);
    }

    /**
     * Retrieves a status platform by its slug.
     *
     * @param slug the URL-friendly identifier of the platform
     * @return ResponseEntity containing the status platform details
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<StatusPlatformResponse> getPlatformBySlug(@PathVariable String slug) {
        StatusPlatformResponse platform = statusPlatformService.getPlatformBySlug(slug);
        return ResponseEntity.ok(platform);
    }

    /**
     * Creates a new status platform.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the status platform creation request
     * @return ResponseEntity containing the created status platform with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusPlatformResponse> createPlatform(@Valid @RequestBody StatusPlatformRequest request) {
        StatusPlatform platform = mapRequestToPlatform(request);
        StatusPlatformResponse response = statusPlatformService.createPlatform(platform, request.getTenantId(), request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing status platform.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status platform to update
     * @param request the status platform update request
     * @return ResponseEntity containing the updated status platform
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StatusPlatformResponse> updatePlatform(
            @PathVariable UUID id,
            @Valid @RequestBody StatusPlatformRequest request) {
        StatusPlatform platform = mapRequestToPlatform(request);
        StatusPlatformResponse response = statusPlatformService.updatePlatform(id, platform, request.getTenantId(), request.getOrganizationId());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a status platform by its unique identifier.
     * <p>
     * This endpoint is restricted to users with ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the status platform to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deletePlatform(@PathVariable UUID id) {
        statusPlatformService.deletePlatform(id);
        return ResponseEntity.ok(new MessageResponse("Platform deleted successfully", true));
    }

    /**
     * Updates the operational status of a status platform.
     * <p>
     * This endpoint is accessible to users with ADMIN, MANAGER, or USER roles.
     * </p>
     *
     * @param id the UUID of the status platform
     * @param status the new operational status value
     * @return ResponseEntity containing the updated status platform
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<StatusPlatformResponse> updatePlatformStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        StatusPlatformResponse platform = statusPlatformService.updateStatus(id, status);
        return ResponseEntity.ok(platform);
    }

    /**
     * Retrieves all status platforms belonging to a specific tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return ResponseEntity containing a list of status platforms
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<StatusPlatformResponse>> getPlatformsByTenant(@PathVariable UUID tenantId) {
        List<StatusPlatformResponse> platforms = statusPlatformService.getPlatformsByTenant(tenantId);
        return ResponseEntity.ok(platforms);
    }

    /**
     * Maps a StatusPlatformRequest to a StatusPlatform entity.
     *
     * @param request the request to map
     * @return the mapped StatusPlatform entity
     */
    private StatusPlatform mapRequestToPlatform(StatusPlatformRequest request) {
        StatusPlatform platform = new StatusPlatform();
        platform.setName(request.getName());
        platform.setDescription(request.getDescription());
        platform.setSlug(request.getSlug());
        platform.setLogoUrl(request.getLogoUrl());
        platform.setWebsiteUrl(request.getWebsiteUrl());
        platform.setStatus(request.getStatus() != null ? request.getStatus() : "OPERATIONAL");
        platform.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        platform.setPosition(request.getPosition() != null ? request.getPosition() : 0);
        return platform;
    }
}
