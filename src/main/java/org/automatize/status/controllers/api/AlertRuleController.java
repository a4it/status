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

@RestController
@RequestMapping("/api/alert-rules")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class AlertRuleController {

    @Autowired
    private AlertRuleService alertRuleService;

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> findAll() {
        return ResponseEntity.ok(alertRuleService.findAll().stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(alertRuleService.findById(id)));
    }

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

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertRuleResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(alertRuleService.toggleActive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        alertRuleService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Alert rule deleted", true));
    }

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
