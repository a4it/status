package org.automatize.status.controllers.api;

import org.automatize.status.services.UptimeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for uptime history management.
 * <p>
 * Provides endpoints for managing uptime history calculations,
 * including manual triggers and backfilling historical data.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 * @see UptimeHistoryService
 */
@RestController
@RequestMapping("/api/uptime-history")
@PreAuthorize("isAuthenticated()")
public class UptimeHistoryController {

    private final UptimeHistoryService uptimeHistoryService;

    public UptimeHistoryController(UptimeHistoryService uptimeHistoryService) {
        this.uptimeHistoryService = uptimeHistoryService;
    }

    /**
     * Backfill uptime history for a specified number of days.
     * <p>
     * This endpoint triggers calculation of uptime history for the specified
     * number of days counting backwards from today. Useful for populating
     * historical data or recalculating after data corrections.
     * </p>
     *
     * @param days the number of days to backfill (max 365)
     * @return a response indicating success and the number of days processed
     */
    @PostMapping("/backfill")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> backfillUptimeHistory(
            @RequestParam(defaultValue = "90") int days) {

        if (days < 1) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Days must be at least 1");
            return ResponseEntity.badRequest().body(error);
        }

        if (days > 365) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Days cannot exceed 365");
            return ResponseEntity.badRequest().body(error);
        }

        long startTime = System.currentTimeMillis();
        int processed = uptimeHistoryService.backfillUptimeHistory(days);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Backfilled " + processed + " days of uptime history");
        response.put("daysRequested", days);
        response.put("daysProcessed", processed);
        response.put("durationMs", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger uptime calculation for a specific date.
     * <p>
     * Useful for recalculating uptime for a specific day, for example
     * after incident data has been corrected.
     * </p>
     *
     * @param date the date to calculate uptime for (format: yyyy-MM-dd)
     * @return a response indicating success
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> calculateUptimeForDate(
            @RequestParam String date) {

        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid date format. Use yyyy-MM-dd");
            return ResponseEntity.badRequest().body(error);
        }

        if (targetDate.isAfter(LocalDate.now().minusDays(1))) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Cannot calculate uptime for today or future dates");
            return ResponseEntity.badRequest().body(error);
        }

        long startTime = System.currentTimeMillis();
        uptimeHistoryService.calculateUptimeForDate(targetDate);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Calculated uptime for " + date);
        response.put("date", date);
        response.put("durationMs", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger the daily uptime calculation manually.
     * <p>
     * This runs the same calculation that normally runs at 00:05 daily.
     * Calculates uptime for the previous day.
     * </p>
     *
     * @return a response indicating success
     */
    @PostMapping("/trigger-daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> triggerDailyCalculation() {
        long startTime = System.currentTimeMillis();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        uptimeHistoryService.calculateUptimeForDate(yesterday);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Calculated uptime for " + yesterday);
        response.put("date", yesterday.toString());
        response.put("durationMs", duration);

        return ResponseEntity.ok(response);
    }
}
