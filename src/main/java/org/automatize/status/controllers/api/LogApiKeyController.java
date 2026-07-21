package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.LogApiKeyRequest;
import org.automatize.status.api.response.LogApiKeyResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.LogApiKey;
import org.automatize.status.services.LogApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for log API key management.
 * <p>
 * Base route: {@code /api/log-api-keys}. Manages the API keys used to authenticate
 * external log ingestion requests. All endpoints require authentication; mutating
 * operations additionally require the ADMIN or MANAGER role. The raw key value is
 * only ever returned once, immediately after creation.
 * </p>
 *
 * @see LogApiKeyService
 * @see LogApiKeyResponse
 */
@RestController
@RequestMapping("/api/log-api-keys")
// MED-02: removed @CrossOrigin(origins = "*"); global CORS policy in SecurityConfig applies
@PreAuthorize("isAuthenticated()")
public class LogApiKeyController {

    @Autowired
    private LogApiKeyService logApiKeyService;

    /**
     * Retrieves all log API keys.
     * <p>
     * Handles {@code GET /api/log-api-keys}. Only key prefixes are exposed, never the raw key.
     * </p>
     *
     * @return ResponseEntity containing the list of log API key responses
     */
    @GetMapping
    public ResponseEntity<List<LogApiKeyResponse>> findAll() {
        return ResponseEntity.ok(logApiKeyService.findAll().stream().map(this::mapToResponse).toList());
    }

    /**
     * Creates a new log API key.
     * <p>
     * Handles {@code POST /api/log-api-keys}. Restricted to ADMIN or MANAGER roles.
     * The response includes the raw key value, which is returned only on creation.
     * </p>
     *
     * @param request the log API key creation request
     * @return ResponseEntity containing the created log API key with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LogApiKeyResponse> create(@Valid @RequestBody LogApiKeyRequest request) {
        LogApiKey key = logApiKeyService.create(request.getTenantId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(key));
    }

    /**
     * Toggles the active state of a log API key.
     * <p>
     * Handles {@code POST /api/log-api-keys/{id}/toggle}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the log API key to toggle
     * @return ResponseEntity containing the updated log API key
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LogApiKeyResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(logApiKeyService.toggleActive(id)));
    }

    /**
     * Deletes a log API key.
     * <p>
     * Handles {@code DELETE /api/log-api-keys/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the log API key to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        logApiKeyService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Log API key deleted", true));
    }

    /**
     * Maps a {@link LogApiKey} entity to its API response representation.
     *
     * @param key the log API key entity to map
     * @return the mapped log API key response
     */
    private LogApiKeyResponse mapToResponse(LogApiKey key) {
        LogApiKeyResponse r = new LogApiKeyResponse();
        r.setId(key.getId());
        r.setTenantId(key.getTenant() != null ? key.getTenant().getId() : null);
        r.setName(key.getName());
        // MED-03: only expose prefix in list; full key only present on creation (transient field)
        r.setKeyPrefix(key.getKeyPrefix());
        r.setRawKey(key.getRawKeyOnceOnly()); // null except immediately after create()
        r.setIsActive(key.getIsActive());
        r.setCreatedDate(key.getCreatedDate());
        return r;
    }
}
