package org.automatize.status.controllers.api;

import org.automatize.status.services.UptimeHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link UptimeHistoryController}. This controller returns
 * plain {@code Map} responses and performs in-controller parameter validation
 * (returning 400 directly, not via bean validation). Security filters are
 * disabled ({@code addFilters = false}).
 */
@WebMvcTest(controllers = UptimeHistoryController.class)
class UptimeHistoryControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private UptimeHistoryService uptimeHistoryService;

    /**
     * Verifies that POST {@code /api/uptime-history/backfill} with a valid
     * {@code days} parameter returns 200 and reports the number of days
     * processed by the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void backfill_valid_returnsOk() throws Exception {
        when(uptimeHistoryService.backfillUptimeHistory(90)).thenReturn(90);

        mockMvc.perform(post("/api/uptime-history/backfill").param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.daysProcessed").value(90));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/backfill} without a
     * {@code days} parameter applies the default of 90 days and returns 200.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void backfill_defaultDays_returnsOk() throws Exception {
        when(uptimeHistoryService.backfillUptimeHistory(90)).thenReturn(90);

        mockMvc.perform(post("/api/uptime-history/backfill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/backfill} with {@code days}
     * below 1 is rejected by in-controller validation and returns 400 with a
     * failure flag.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void backfill_daysBelowOne_returns400() throws Exception {
        mockMvc.perform(post("/api/uptime-history/backfill").param("days", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/backfill} with {@code days}
     * above 365 is rejected by in-controller validation and returns 400 with a
     * failure flag.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void backfill_daysAbove365_returns400() throws Exception {
        mockMvc.perform(post("/api/uptime-history/backfill").param("days", "366"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/calculate} with a valid past
     * date returns 200 and delegates the calculation to the service for that
     * date.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void calculate_validPastDate_returnsOk() throws Exception {
        mockMvc.perform(post("/api/uptime-history/calculate").param("date", "2020-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.date").value("2020-01-01"));

        verify(uptimeHistoryService).calculateUptimeForDate(LocalDate.of(2020, 1, 1));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/calculate} with a
     * malformed date string returns 400 with a failure flag.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void calculate_invalidFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/uptime-history/calculate").param("date", "01-01-2020"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/calculate} with a future
     * date is rejected by in-controller validation and returns 400 with a
     * failure flag.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void calculate_futureDate_returns400() throws Exception {
        String future = LocalDate.now().plusDays(5).toString();
        mockMvc.perform(post("/api/uptime-history/calculate").param("date", future))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Verifies that POST {@code /api/uptime-history/calculate} without the
     * required {@code date} parameter returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void calculate_missingDateParam_returns400() throws Exception {
        mockMvc.perform(post("/api/uptime-history/calculate"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/uptime-history/trigger-daily} returns 200
     * and delegates a calculation for a date to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void triggerDaily_returnsOk() throws Exception {
        mockMvc.perform(post("/api/uptime-history/trigger-daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(uptimeHistoryService).calculateUptimeForDate(any(LocalDate.class));
    }
}
