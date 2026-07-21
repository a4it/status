package org.automatize.status.controllers.api;

import org.automatize.status.api.request.LogLevelRequest;
import org.automatize.status.api.response.LogViewerResponse;
import org.automatize.status.api.response.LoggerInfoResponse;
import org.automatize.status.services.LogViewerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for viewing and managing system logs.
 * <p>
 * Base route: {@code /api/log-viewer}. Provides read access to the application log
 * and syslog files, lists configured loggers, and allows adjusting logger levels at
 * runtime. Because these endpoints expose sensitive system logs, access is restricted
 * to the ADMIN and SUPERADMIN roles.
 * </p>
 *
 * @see LogViewerService
 */
@RestController
@RequestMapping("/api/log-viewer")
// MED-05: log viewer restricted to admin roles only (reads sensitive system logs)
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class LogViewerController {

    @Autowired
    private LogViewerService logViewerService;

    /**
     * Reads the tail of the application log.
     * <p>
     * Handles {@code GET /api/log-viewer/app-log}.
     * </p>
     *
     * @param lines the number of trailing lines to return (default 500)
     * @param search optional case-insensitive filter applied to log lines
     * @return ResponseEntity containing the matching application log lines
     */
    @GetMapping("/app-log")
    public ResponseEntity<LogViewerResponse> getAppLog(
            @RequestParam(defaultValue = "500") int lines,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(logViewerService.readAppLog(lines, search));
    }

    /**
     * Reads the tail of the system log.
     * <p>
     * Handles {@code GET /api/log-viewer/syslog}.
     * </p>
     *
     * @param lines the number of trailing lines to return (default 500)
     * @param search optional case-insensitive filter applied to log lines
     * @return ResponseEntity containing the matching syslog lines
     */
    @GetMapping("/syslog")
    public ResponseEntity<LogViewerResponse> getSyslog(
            @RequestParam(defaultValue = "500") int lines,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(logViewerService.readSyslog(lines, search));
    }

    /**
     * Lists all configured loggers and their current levels.
     * <p>
     * Handles {@code GET /api/log-viewer/loggers}.
     * </p>
     *
     * @return ResponseEntity containing the list of logger information
     */
    @GetMapping("/loggers")
    public ResponseEntity<List<LoggerInfoResponse>> getLoggers() {
        return ResponseEntity.ok(logViewerService.getLoggers());
    }

    /**
     * Updates the log level of a specific logger at runtime.
     * <p>
     * Handles {@code PUT /api/log-viewer/loggers/{name}}.
     * </p>
     *
     * @param name the name of the logger to reconfigure
     * @param request the request carrying the new log level
     * @return ResponseEntity with HTTP 200 and no body
     */
    @PutMapping("/loggers/{name}")
    public ResponseEntity<?> setLogLevel(
            @PathVariable String name,
            @RequestBody LogLevelRequest request) {
        logViewerService.setLogLevel(name, request.getLevel());
        return ResponseEntity.ok().build();
    }
}
