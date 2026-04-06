package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.SchedulerDatasourceRequest;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.api.response.SchedulerDatasourceResponse;
import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.scheduler.DbType;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.scheduler.SchedulerDatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for managing scheduler JDBC datasource configurations.
 *
 * <p>Licensed under the Creative Commons Attribution-NonCommercial 4.0
 * International (CC BY-NC 4.0).</p>
 *
 * @author Tim De Smedt
 */
@RestController
@RequestMapping("/api/scheduler/datasources")
@PreAuthorize("isAuthenticated()")
public class SchedulerDatasourceApiController {

    @Autowired
    private SchedulerDatasourceService datasourceService;

    @Autowired
    private OrganizationRepository organizationRepository;

    // -------------------------------------------------------------------------
    // List datasources for the current tenant
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<SchedulerDatasourceResponse>> listDatasources() {
        UserPrincipal principal = currentPrincipal();
        List<SchedulerJdbcDatasource> datasources = datasourceService.list(principal.getTenantId());
        return ResponseEntity.ok(datasources.stream()
                .map(SchedulerDatasourceResponse::fromEntity)
                .toList());
    }

    // -------------------------------------------------------------------------
    // Get single datasource
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<SchedulerDatasourceResponse> getDatasource(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJdbcDatasource ds = datasourceService.get(id, principal.getTenantId());
        return ResponseEntity.ok(SchedulerDatasourceResponse.fromEntity(ds));
    }

    // -------------------------------------------------------------------------
    // Create datasource
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerDatasourceResponse> createDatasource(
            @Valid @RequestBody SchedulerDatasourceRequest request) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJdbcDatasource ds = buildFromRequest(request);
        SchedulerJdbcDatasource saved = datasourceService.create(ds, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchedulerDatasourceResponse.fromEntity(saved));
    }

    // -------------------------------------------------------------------------
    // Update datasource
    // -------------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulerDatasourceResponse> updateDatasource(
            @PathVariable UUID id,
            @Valid @RequestBody SchedulerDatasourceRequest request) {
        UserPrincipal principal = currentPrincipal();
        SchedulerJdbcDatasource dsData = buildFromRequest(request);
        SchedulerJdbcDatasource updated = datasourceService.update(id, dsData, principal.getTenantId(), principal.getUsername());
        return ResponseEntity.ok(SchedulerDatasourceResponse.fromEntity(updated));
    }

    // -------------------------------------------------------------------------
    // Delete datasource
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> deleteDatasource(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        datasourceService.delete(id, principal.getTenantId());
        return ResponseEntity.ok(new MessageResponse("Datasource deleted", true));
    }

    // -------------------------------------------------------------------------
    // Test connection
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable UUID id) {
        UserPrincipal principal = currentPrincipal();
        Map<String, Object> result = datasourceService.testConnection(id, principal.getTenantId());
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Helper: build entity from request
    // -------------------------------------------------------------------------

    private SchedulerJdbcDatasource buildFromRequest(SchedulerDatasourceRequest req) {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setName(req.getName());
        ds.setDescription(req.getDescription());
        if (req.getDbType() != null) {
            ds.setDbType(DbType.valueOf(req.getDbType().toUpperCase()));
        }
        ds.setHost(req.getHost());
        ds.setPort(req.getPort());
        ds.setDatabaseName(req.getDatabaseName());
        ds.setSchemaName(req.getSchemaName());
        ds.setJdbcUrlOverride(req.getJdbcUrlOverride());
        ds.setUsername(req.getUsername());
        // Pass plaintext password; service layer encrypts it
        ds.setPasswordEnc(req.getPassword());
        ds.setMinPoolSize(req.getMinPoolSize() != null ? req.getMinPoolSize() : 1);
        ds.setMaxPoolSize(req.getMaxPoolSize() != null ? req.getMaxPoolSize() : 5);
        ds.setConnectionTimeoutMs(req.getConnectionTimeoutMs() != null ? req.getConnectionTimeoutMs() : 5000);
        ds.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        if (req.getOrganizationId() != null) {
            organizationRepository.findById(req.getOrganizationId())
                    .ifPresent(ds::setOrganization);
        }
        return ds;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
