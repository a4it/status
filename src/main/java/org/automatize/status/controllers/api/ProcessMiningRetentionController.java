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

/**
 * <p>
 * REST API controller for process mining retention policy management.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide CRUD operations for process mining retention policies</li>
 *   <li>Trigger on-demand execution of the retention purge routine</li>
 * </ul>
 * </p>
 *
 * <p>
 * Base route: {@code /api/process-mining/retention}. All endpoints require an
 * authenticated user.
 * </p>
 *
 * @see ProcessMiningRetentionService
 */
@RestController
@RequestMapping("/api/process-mining/retention")
@PreAuthorize("isAuthenticated()")
public class ProcessMiningRetentionController {

    @Autowired
    private ProcessMiningRetentionService service;

    /**
     * Retrieves all process mining retention policies.
     * <p>
     * HTTP GET {@code /api/process-mining/retention}
     * </p>
     *
     * @return ResponseEntity containing a list of retention policies
     */
    @GetMapping
    public ResponseEntity<List<ProcessMiningRetentionResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * Retrieves a single retention policy by its unique identifier.
     * <p>
     * HTTP GET {@code /api/process-mining/retention/{id}}
     * </p>
     *
     * @param id the UUID of the retention policy
     * @return ResponseEntity containing the retention policy
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessMiningRetentionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Creates a new retention policy.
     * <p>
     * HTTP POST {@code /api/process-mining/retention}
     * </p>
     *
     * @param req the retention policy creation request
     * @return ResponseEntity containing the created policy with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ProcessMiningRetentionResponse> create(@RequestBody ProcessMiningRetentionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    /**
     * Updates an existing retention policy.
     * <p>
     * HTTP PUT {@code /api/process-mining/retention/{id}}
     * </p>
     *
     * @param id the UUID of the retention policy to update
     * @param req the retention policy update request
     * @return ResponseEntity containing the updated policy
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProcessMiningRetentionResponse> update(
            @PathVariable UUID id,
            @RequestBody ProcessMiningRetentionRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /**
     * Deletes a retention policy by its unique identifier.
     * <p>
     * HTTP DELETE {@code /api/process-mining/retention/{id}}
     * </p>
     *
     * @param id the UUID of the retention policy to delete
     * @return ResponseEntity with HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Triggers immediate execution of the retention purge routine.
     * <p>
     * HTTP POST {@code /api/process-mining/retention/run}
     * </p>
     *
     * @return ResponseEntity containing a map summarizing the retention run results
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        return ResponseEntity.ok(service.runRetentionNow());
    }
}
