package org.automatize.status.controllers.api;

import org.automatize.status.api.request.ProcessMiningRetentionRequest;
import org.automatize.status.api.response.ProcessMiningRetentionResponse;
import org.automatize.status.services.ProcessMiningRetentionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/process-mining/retention")
@PreAuthorize("isAuthenticated()")
public class ProcessMiningRetentionController {

    @Autowired
    private ProcessMiningRetentionService service;

    @GetMapping
    public ResponseEntity<List<ProcessMiningRetentionResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessMiningRetentionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProcessMiningRetentionResponse> create(@RequestBody ProcessMiningRetentionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcessMiningRetentionResponse> update(
            @PathVariable UUID id,
            @RequestBody ProcessMiningRetentionRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        return ResponseEntity.ok(service.runRetentionNow());
    }
}
