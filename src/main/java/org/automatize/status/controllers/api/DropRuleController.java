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

@RestController
@RequestMapping("/api/drop-rules")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class DropRuleController {

    @Autowired
    private DropRuleService dropRuleService;

    @GetMapping
    public ResponseEntity<List<DropRuleResponse>> findAll() {
        return ResponseEntity.ok(dropRuleService.findAll().stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DropRuleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(dropRuleService.findById(id)));
    }

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

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DropRuleResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(dropRuleService.toggleActive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        dropRuleService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Drop rule deleted", true));
    }

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
