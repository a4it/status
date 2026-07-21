package org.automatize.status.controllers.api;

import org.automatize.status.api.request.GcScheduleRequest;
import org.automatize.status.api.response.JvmStatsResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.services.JvmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for JVM monitoring and garbage-collection management.
 * <p>
 * Base route: {@code /api/jvm}. Exposes JVM runtime statistics and lets callers
 * inspect or update the scheduled garbage-collection configuration, as well as
 * trigger a garbage collection on demand. All endpoints require authentication.
 * </p>
 *
 * @see JvmService
 */
@RestController
@RequestMapping("/api/jvm")
@PreAuthorize("isAuthenticated()")
public class JvmController {

    @Autowired
    private JvmService jvmService;

    /**
     * Retrieves current JVM runtime statistics.
     * <p>
     * Handles {@code GET /api/jvm/stats}.
     * </p>
     *
     * @return ResponseEntity containing the JVM statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<JvmStatsResponse> getStats() {
        return ResponseEntity.ok(jvmService.getStats());
    }

    /**
     * Retrieves the scheduled garbage-collection configuration.
     * <p>
     * Handles {@code GET /api/jvm/gc/schedule}.
     * </p>
     *
     * @return ResponseEntity containing the current GC schedule configuration
     */
    @GetMapping("/gc/schedule")
    public ResponseEntity<GcScheduleRequest> getGcSchedule() {
        return ResponseEntity.ok(jvmService.getScheduleConfig());
    }

    /**
     * Updates the scheduled garbage-collection configuration.
     * <p>
     * Handles {@code PUT /api/jvm/gc/schedule}.
     * </p>
     *
     * @param request the new GC schedule configuration (enabled flag and cron expression)
     * @return ResponseEntity with HTTP 200 and no body
     */
    @PutMapping("/gc/schedule")
    public ResponseEntity<Void> updateGcSchedule(@RequestBody GcScheduleRequest request) {
        jvmService.updateSchedule(request.isEnabled(), request.getCron());
        return ResponseEntity.ok().build();
    }

    /**
     * Triggers a garbage collection immediately.
     * <p>
     * Handles {@code POST /api/jvm/gc/run}.
     * </p>
     *
     * @return ResponseEntity containing a confirmation message
     */
    @PostMapping("/gc/run")
    public ResponseEntity<MessageResponse> runGcNow() {
        jvmService.triggerGcNow();
        return ResponseEntity.ok(new MessageResponse("GC triggered", true));
    }
}
