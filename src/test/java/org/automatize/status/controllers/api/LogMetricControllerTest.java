package org.automatize.status.controllers.api;

import org.automatize.status.models.LogMetric;
import org.automatize.status.services.LogMetricService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link LogMetricController}. Presence of the {@code tenantId}
 * query param selects the tenant-scoped service method.
 */
@WebMvcTest(controllers = LogMetricController.class)
class LogMetricControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private LogMetricService logMetricService;

    /**
     * Builds a sample {@link LogMetric} entity populated with representative field
     * values for use as service-mock return data.
     *
     * @return a fully populated {@link LogMetric} instance
     */
    private LogMetric sampleMetric() {
        LogMetric m = new LogMetric();
        m.setId(UUID.randomUUID());
        m.setService("orders");
        m.setLevel("ERROR");
        m.setBucket(ZonedDateTime.now());
        m.setBucketType("MINUTE");
        m.setCount(7L);
        return m;
    }

    /**
     * Verifies that {@code GET /api/log-metrics} without a {@code tenantId} param returns
     * {@code 200 OK} and delegates to {@link LogMetricService#findSince}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getMetrics_noTenant_returnsOk() throws Exception {
        when(logMetricService.findSince(any())).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/log-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].service").value("orders"))
                .andExpect(jsonPath("$[0].count").value(7));

        verify(logMetricService).findSince(any());
    }

    /**
     * Verifies that supplying a {@code tenantId} param returns {@code 200 OK} and routes
     * the call to the tenant-scoped {@link LogMetricService#findByTenantSince}.
     *
     * @throws Exception if the {@link org.springframework.test.web.servlet.MockMvc} request fails
     */
    @Test
    void getMetrics_withTenant_usesTenantScopedQuery() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(logMetricService.findByTenantSince(eq(tenantId), any()))
                .thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/log-metrics").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("ERROR"));

        verify(logMetricService).findByTenantSince(eq(tenantId), any());
    }
}
