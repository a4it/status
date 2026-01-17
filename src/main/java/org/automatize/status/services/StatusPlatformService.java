package org.automatize.status.services;

import org.automatize.status.api.response.StatusAppResponse;
import org.automatize.status.api.response.StatusPlatformResponse;
import org.automatize.status.models.*;
import org.automatize.status.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing status platforms.
 * <p>
 * Status platforms represent higher-level groupings that can contain multiple
 * status applications. This service provides CRUD operations for platforms,
 * including tenant and organization associations and status management.
 * </p>
 */
@Service
@Transactional
public class StatusPlatformService {

    @Autowired
    private StatusPlatformRepository statusPlatformRepository;

    @Autowired
    private StatusAppRepository statusAppRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Retrieves a paginated list of status platforms with optional filtering.
     *
     * @param tenantId optional tenant ID to filter platforms
     * @param organizationId optional organization ID to filter platforms
     * @param search optional search term for name matching
     * @param pageable pagination information
     * @return a page of StatusPlatformResponse objects matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<StatusPlatformResponse> getAllPlatforms(UUID tenantId, UUID organizationId, String search, Pageable pageable) {
        List<StatusPlatform> platforms;

        if (tenantId != null) {
            platforms = statusPlatformRepository.findByTenantIdOrderByPosition(tenantId);
        } else if (organizationId != null) {
            platforms = statusPlatformRepository.findByOrganizationId(organizationId);
        } else if (search != null && !search.isEmpty()) {
            platforms = statusPlatformRepository.search(search);
        } else {
            return statusPlatformRepository.findAll(pageable).map(this::mapToResponse);
        }

        List<StatusPlatformResponse> responses = platforms.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, responses.size());
    }

    /**
     * Retrieves all platforms ordered by position.
     *
     * @return a list of all StatusPlatformResponse objects
     */
    @Transactional(readOnly = true)
    public List<StatusPlatformResponse> getAllPlatformsOrdered() {
        return statusPlatformRepository.findAllByOrderByPosition().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a platform by its unique identifier.
     *
     * @param id the UUID of the platform
     * @return the StatusPlatformResponse for the requested platform
     * @throws RuntimeException if the platform is not found
     */
    @Transactional(readOnly = true)
    public StatusPlatformResponse getPlatformById(UUID id) {
        StatusPlatform platform = statusPlatformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platform not found with id: " + id));
        return mapToResponse(platform);
    }

    /**
     * Retrieves a platform by its slug.
     *
     * @param slug the URL-friendly identifier of the platform
     * @return the StatusPlatformResponse for the requested platform
     * @throws RuntimeException if the platform is not found
     */
    @Transactional(readOnly = true)
    public StatusPlatformResponse getPlatformBySlug(String slug) {
        StatusPlatform platform = statusPlatformRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Platform not found with slug: " + slug));
        return mapToResponse(platform);
    }

    /**
     * Retrieves all platforms belonging to a specific tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return a list of StatusPlatformResponse objects for the tenant's platforms
     */
    @Transactional(readOnly = true)
    public List<StatusPlatformResponse> getPlatformsByTenant(UUID tenantId) {
        return statusPlatformRepository.findByTenantIdOrderByPosition(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new platform with the provided details.
     *
     * @param platform the platform entity to create
     * @return the newly created StatusPlatformResponse
     * @throws RuntimeException if the slug already exists in the tenant
     */
    public StatusPlatformResponse createPlatform(StatusPlatform platform) {
        if (platform.getTenant() != null &&
            statusPlatformRepository.existsByTenantIdAndSlug(platform.getTenant().getId(), platform.getSlug())) {
            throw new RuntimeException("Platform with slug already exists in this tenant: " + platform.getSlug());
        }

        String currentUser = getCurrentUsername();
        platform.setCreatedBy(currentUser);
        platform.setLastModifiedBy(currentUser);

        StatusPlatform savedPlatform = statusPlatformRepository.save(platform);
        return mapToResponse(savedPlatform);
    }

    /**
     * Creates a new platform using tenant and organization IDs.
     *
     * @param platform the platform entity
     * @param tenantId optional tenant ID
     * @param organizationId optional organization ID
     * @return the newly created StatusPlatformResponse
     */
    public StatusPlatformResponse createPlatform(StatusPlatform platform, UUID tenantId, UUID organizationId) {
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            platform.setTenant(tenant);
        }

        if (organizationId != null) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            platform.setOrganization(organization);
        }

        return createPlatform(platform);
    }

    /**
     * Updates an existing platform with the provided details.
     *
     * @param id the UUID of the platform to update
     * @param updatedPlatform the updated platform data
     * @return the updated StatusPlatformResponse
     * @throws RuntimeException if the platform is not found
     */
    public StatusPlatformResponse updatePlatform(UUID id, StatusPlatform updatedPlatform) {
        StatusPlatform platform = statusPlatformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platform not found with id: " + id));

        if (!platform.getSlug().equals(updatedPlatform.getSlug()) &&
            platform.getTenant() != null &&
            statusPlatformRepository.existsByTenantIdAndSlug(platform.getTenant().getId(), updatedPlatform.getSlug())) {
            throw new RuntimeException("Platform with slug already exists in this tenant: " + updatedPlatform.getSlug());
        }

        platform.setName(updatedPlatform.getName());
        platform.setDescription(updatedPlatform.getDescription());
        platform.setSlug(updatedPlatform.getSlug());
        platform.setLogoUrl(updatedPlatform.getLogoUrl());
        platform.setWebsiteUrl(updatedPlatform.getWebsiteUrl());
        platform.setStatus(updatedPlatform.getStatus() != null ? updatedPlatform.getStatus() : "OPERATIONAL");
        platform.setIsPublic(updatedPlatform.getIsPublic() != null ? updatedPlatform.getIsPublic() : true);
        platform.setPosition(updatedPlatform.getPosition() != null ? updatedPlatform.getPosition() : 0);
        platform.setLastModifiedBy(getCurrentUsername());

        StatusPlatform savedPlatform = statusPlatformRepository.save(platform);
        return mapToResponse(savedPlatform);
    }

    /**
     * Updates an existing platform with tenant and organization IDs.
     *
     * @param id the UUID of the platform to update
     * @param updatedPlatform the updated platform data
     * @param tenantId optional tenant ID
     * @param organizationId optional organization ID
     * @return the updated StatusPlatformResponse
     */
    public StatusPlatformResponse updatePlatform(UUID id, StatusPlatform updatedPlatform, UUID tenantId, UUID organizationId) {
        StatusPlatform platform = statusPlatformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platform not found with id: " + id));

        if (tenantId != null && (platform.getTenant() == null || !platform.getTenant().getId().equals(tenantId))) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            platform.setTenant(tenant);
        }

        if (organizationId != null && (platform.getOrganization() == null || !platform.getOrganization().getId().equals(organizationId))) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            platform.setOrganization(organization);
        }

        platform.setName(updatedPlatform.getName());
        platform.setDescription(updatedPlatform.getDescription());
        platform.setSlug(updatedPlatform.getSlug());
        platform.setLogoUrl(updatedPlatform.getLogoUrl());
        platform.setWebsiteUrl(updatedPlatform.getWebsiteUrl());
        platform.setStatus(updatedPlatform.getStatus() != null ? updatedPlatform.getStatus() : "OPERATIONAL");
        platform.setIsPublic(updatedPlatform.getIsPublic() != null ? updatedPlatform.getIsPublic() : true);
        platform.setPosition(updatedPlatform.getPosition() != null ? updatedPlatform.getPosition() : 0);
        platform.setLastModifiedBy(getCurrentUsername());

        StatusPlatform savedPlatform = statusPlatformRepository.save(platform);
        return mapToResponse(savedPlatform);
    }

    /**
     * Updates the status of a platform.
     *
     * @param id the UUID of the platform
     * @param status the new status value
     * @return the updated StatusPlatformResponse
     * @throws RuntimeException if the platform is not found
     */
    public StatusPlatformResponse updateStatus(UUID id, String status) {
        StatusPlatform platform = statusPlatformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platform not found with id: " + id));

        platform.setStatus(status);
        platform.setLastModifiedBy(getCurrentUsername());

        StatusPlatform savedPlatform = statusPlatformRepository.save(platform);
        return mapToResponse(savedPlatform);
    }

    /**
     * Deletes a platform by its unique identifier.
     *
     * @param id the UUID of the platform to delete
     * @throws RuntimeException if the platform is not found
     * @throws RuntimeException if the platform has associated apps
     */
    public void deletePlatform(UUID id) {
        StatusPlatform platform = statusPlatformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Platform not found with id: " + id));

        // Check for associated apps
        List<StatusApp> apps = statusAppRepository.findByPlatformId(id);
        if (!apps.isEmpty()) {
            throw new RuntimeException("Cannot delete platform with associated applications. Remove or reassign apps first.");
        }

        statusPlatformRepository.delete(platform);
    }

    /**
     * Maps a StatusPlatform entity to a StatusPlatformResponse.
     *
     * @param platform the StatusPlatform entity to map
     * @return the mapped StatusPlatformResponse
     */
    private StatusPlatformResponse mapToResponse(StatusPlatform platform) {
        StatusPlatformResponse response = new StatusPlatformResponse();
        response.setId(platform.getId());
        response.setName(platform.getName());
        response.setDescription(platform.getDescription());
        response.setSlug(platform.getSlug());
        response.setLogoUrl(platform.getLogoUrl());
        response.setWebsiteUrl(platform.getWebsiteUrl());
        response.setStatus(platform.getStatus());
        response.setIsPublic(platform.getIsPublic());
        response.setPosition(platform.getPosition());
        response.setLastUpdated(platform.getLastModifiedDate());

        if (platform.getTenant() != null) {
            response.setTenantId(platform.getTenant().getId());
        }
        if (platform.getOrganization() != null) {
            response.setOrganizationId(platform.getOrganization().getId());
        }

        // Load associated apps
        List<StatusApp> apps = statusAppRepository.findByPlatformId(platform.getId());
        response.setApps(apps.stream()
                .map(this::mapAppToResponse)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Maps a StatusApp entity to a simplified StatusAppResponse.
     *
     * @param app the StatusApp entity to map
     * @return the mapped StatusAppResponse
     */
    private StatusAppResponse mapAppToResponse(StatusApp app) {
        StatusAppResponse response = new StatusAppResponse();
        response.setId(app.getId());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setSlug(app.getSlug());
        response.setStatus(app.getStatus());
        response.setIsPublic(app.getIsPublic());
        response.setLastUpdated(app.getLastModifiedDate());
        return response;
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return "system";
    }
}
