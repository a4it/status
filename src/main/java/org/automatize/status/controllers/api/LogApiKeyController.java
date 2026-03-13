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

@RestController
@RequestMapping("/api/log-api-keys")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class LogApiKeyController {

    @Autowired
    private LogApiKeyService logApiKeyService;

    @GetMapping
    public ResponseEntity<List<LogApiKeyResponse>> findAll() {
        return ResponseEntity.ok(logApiKeyService.findAll().stream().map(this::mapToResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LogApiKeyResponse> create(@Valid @RequestBody LogApiKeyRequest request) {
        LogApiKey key = logApiKeyService.create(request.getTenantId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(key));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LogApiKeyResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(logApiKeyService.toggleActive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        logApiKeyService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Log API key deleted", true));
    }

    private LogApiKeyResponse mapToResponse(LogApiKey key) {
        LogApiKeyResponse r = new LogApiKeyResponse();
        r.setId(key.getId());
        r.setTenantId(key.getTenant() != null ? key.getTenant().getId() : null);
        r.setName(key.getName());
        r.setApiKey(key.getApiKey());
        r.setIsActive(key.getIsActive());
        r.setCreatedDate(key.getCreatedDate());
        return r;
    }
}
