package org.automatize.status.controllers.api;

import org.automatize.status.api.response.ProcessMiningResponse;
import org.automatize.status.services.ProcessMiningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs/process-mining")
@PreAuthorize("isAuthenticated()")
public class LogProcessMiningController {

    @Autowired
    private ProcessMiningService processMiningService;

    /**
     * Builds a process mining dataset from logs grouped by trace ID.
     *
     * @param scope       "platform" or "application"
     * @param scopeId     UUID of the selected platform or app
     * @param tenantId    optional tenant filter
     * @param from        start of time window (ISO-8601, defaults to 24h ago)
     * @param to          end of time window (ISO-8601, defaults to now)
     * @param maxCases    maximum number of distinct trace IDs to include (default 300, max 1000)
     * @param minEvents   minimum events per trace to include (default 2)
     */
    @GetMapping
    public ResponseEntity<ProcessMiningResponse> getProcessMiningData(
            @RequestParam String scope,
            @RequestParam UUID scopeId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) ZonedDateTime from,
            @RequestParam(required = false) ZonedDateTime to,
            @RequestParam(defaultValue = "300") int maxCases,
            @RequestParam(defaultValue = "2") int minEvents) {

        if (from == null) {
            from = ZonedDateTime.now().minusHours(24);
        }
        if (to == null) {
            to = ZonedDateTime.now();
        }
        maxCases = Math.min(maxCases, 1000);

        ProcessMiningResponse response = processMiningService.buildCases(
            scope, scopeId, tenantId, from, to, maxCases, minEvents);

        return ResponseEntity.ok(response);
    }
}
