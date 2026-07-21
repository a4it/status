package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.DropRuleRequest;
import org.automatize.status.api.response.DropRuleResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.DropRule;
import org.automatize.status.services.DropRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for drop rule management.
 * <p>
 * Base route: {@code /api/drop-rules}. Provides CRUD operations for drop rules
 * that determine which incoming log entries are discarded during ingestion.
 * All endpoints require authentication; mutating operations additionally require
 * the ADMIN or MANAGER role.
 * </p>
 *
 * @see DropRuleService
 * @see DropRuleResponse
 */
@RestController
@RequestMapping("/api/drop-rules")
// MED-02: removed @CrossOrigin(origins = "*"); global CORS policy in SecurityConfig applies
@PreAuthorize("isAuthenticated()")
public class DropRuleController {

    @Autowired
    private DropRuleService dropRuleService;

    /**
     * Retrieves all drop rules.
     * <p>
     * Handles {@code GET /api/drop-rules}.
     * </p>
     *
     * @return ResponseEntity containing the list of drop rule responses
     */
    @GetMapping
    public ResponseEntity<List<DropRuleResponse>> findAll() {
        return ResponseEntity.ok(dropRuleService.findAll().stream().map(this::mapToResponse).toList());
    }

    /**
     * Retrieves a single drop rule by its identifier.
     * <p>
     * Handles {@code GET /api/drop-rules/{id}}.
     * </p>
     *
     * @param id the UUID of the drop rule
     * @return ResponseEntity containing the drop rule response
     */
    @GetMapping("/{id}")
    public ResponseEntity<DropRuleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(dropRuleService.findById(id)));
    }

    /**
     * Creates a new drop rule.
     * <p>
     * Handles {@code POST /api/drop-rules}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the drop rule creation request
     * @return ResponseEntity containing the created drop rule with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DropRuleResponse> create(@Valid @RequestBody DropRuleRequest request) {
        DropRule rule = dropRuleService.create(
                request.getTenantId(),
                request.getName(),
                request.getLevel(),
                request.getService(),
                request.getMessagePattern(),
                request.isActive()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(rule));
    }

    /**
     * Updates an existing drop rule.
     * <p>
     * Handles {@code PUT /api/drop-rules/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the drop rule to update
     * @param request the drop rule update request
     * @return ResponseEntity containing the updated drop rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DropRuleResponse> update(@PathVariable UUID id, @Valid @RequestBody DropRuleRequest request) {
        DropRule rule = dropRuleService.update(id,
                request.getName(),
                request.getLevel(),
                request.getService(),
                request.getMessagePattern(),
                request.isActive());
        return ResponseEntity.ok(mapToResponse(rule));
    }

    /**
     * Toggles the active state of a drop rule.
     * <p>
     * Handles {@code POST /api/drop-rules/{id}/toggle}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the drop rule to toggle
     * @return ResponseEntity containing the updated drop rule
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DropRuleResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(dropRuleService.toggleActive(id)));
    }

    /**
     * Deletes a drop rule.
     * <p>
     * Handles {@code DELETE /api/drop-rules/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the drop rule to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        dropRuleService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Drop rule deleted", true));
    }

    /**
     * Maps a {@link DropRule} entity to its API response representation.
     *
     * @param rule the drop rule entity to map
     * @return the mapped drop rule response
     */
    private DropRuleResponse mapToResponse(DropRule rule) {
        DropRuleResponse r = new DropRuleResponse();
        r.setId(rule.getId());
        r.setTenantId(rule.getTenant() != null ? rule.getTenant().getId() : null);
        r.setName(rule.getName());
        r.setLevel(rule.getLevel());
        r.setService(rule.getService());
        r.setMessagePattern(rule.getMessagePattern());
        r.setIsActive(rule.getIsActive());
        r.setCreatedDate(rule.getCreatedDate());
        return r;
    }
}
