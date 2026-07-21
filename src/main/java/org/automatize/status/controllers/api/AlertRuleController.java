package org.automatize.status.controllers.api;

import jakarta.validation.Valid;
import org.automatize.status.api.request.AlertRuleRequest;
import org.automatize.status.api.response.AlertRuleResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.AlertRule;
import org.automatize.status.services.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for alert rule management.
 * <p>
 * Base route: {@code /api/alert-rules}. Provides CRUD operations for alert rules
 * that define log-based alerting thresholds and their notification targets.
 * All endpoints require authentication; mutating operations additionally require
 * the ADMIN or MANAGER role.
 * </p>
 *
 * @see AlertRuleService
 * @see AlertRuleResponse
 */
@RestController
@RequestMapping("/api/alert-rules")
// MED-02: removed @CrossOrigin(origins = "*"); global CORS policy in SecurityConfig applies
@PreAuthorize("isAuthenticated()")
public class AlertRuleController {

    @Autowired
    private AlertRuleService alertRuleService;

    /**
     * Retrieves all alert rules.
     * <p>
     * Handles {@code GET /api/alert-rules}.
     * </p>
     *
     * @return ResponseEntity containing the list of alert rule responses
     */
    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> findAll() {
        return ResponseEntity.ok(alertRuleService.findAll().stream().map(this::mapToResponse).toList());
    }

    /**
     * Retrieves a single alert rule by its identifier.
     * <p>
     * Handles {@code GET /api/alert-rules/{id}}.
     * </p>
     *
     * @param id the UUID of the alert rule
     * @return ResponseEntity containing the alert rule response
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(alertRuleService.findById(id)));
    }

    /**
     * Creates a new alert rule.
     * <p>
     * Handles {@code POST /api/alert-rules}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param request the alert rule creation request
     * @return ResponseEntity containing the created alert rule with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertRuleResponse> create(@Valid @RequestBody AlertRuleRequest request) {
        AlertRule rule = alertRuleService.create(
                request.getTenantId(),
                request.getName(),
                request.getService(),
                request.getLevel(),
                request.getThresholdCount(),
                request.getWindowMinutes(),
                request.getCooldownMinutes(),
                request.getNotificationType(),
                request.getNotificationTarget(),
                request.isActive()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(rule));
    }

    /**
     * Updates an existing alert rule.
     * <p>
     * Handles {@code PUT /api/alert-rules/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the alert rule to update
     * @param request the alert rule update request
     * @return ResponseEntity containing the updated alert rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertRuleResponse> update(@PathVariable UUID id, @Valid @RequestBody AlertRuleRequest request) {
        AlertRule rule = alertRuleService.update(id,
                request.getName(),
                request.getService(),
                request.getLevel(),
                request.getThresholdCount(),
                request.getWindowMinutes(),
                request.getCooldownMinutes(),
                request.getNotificationType(),
                request.getNotificationTarget(),
                request.isActive());
        return ResponseEntity.ok(mapToResponse(rule));
    }

    /**
     * Toggles the active state of an alert rule.
     * <p>
     * Handles {@code POST /api/alert-rules/{id}/toggle}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the alert rule to toggle
     * @return ResponseEntity containing the updated alert rule
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertRuleResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(alertRuleService.toggleActive(id)));
    }

    /**
     * Deletes an alert rule.
     * <p>
     * Handles {@code DELETE /api/alert-rules/{id}}. Restricted to ADMIN or MANAGER roles.
     * </p>
     *
     * @param id the UUID of the alert rule to delete
     * @return ResponseEntity containing a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        alertRuleService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Alert rule deleted", true));
    }

    /**
     * Maps an {@link AlertRule} entity to its API response representation.
     *
     * @param rule the alert rule entity to map
     * @return the mapped alert rule response
     */
    private AlertRuleResponse mapToResponse(AlertRule rule) {
        AlertRuleResponse r = new AlertRuleResponse();
        r.setId(rule.getId());
        r.setTenantId(rule.getTenant() != null ? rule.getTenant().getId() : null);
        r.setName(rule.getName());
        r.setService(rule.getService());
        r.setLevel(rule.getLevel());
        r.setThresholdCount(rule.getThresholdCount());
        r.setWindowMinutes(rule.getWindowMinutes());
        r.setCooldownMinutes(rule.getCooldownMinutes());
        r.setNotificationType(rule.getNotificationType());
        r.setNotificationTarget(rule.getNotificationTarget());
        r.setIsActive(rule.getIsActive());
        r.setLastFiredAt(rule.getLastFiredAt());
        r.setCreatedDate(rule.getCreatedDate());
        return r;
    }
}
