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

    @Test
    void getMetrics_noTenant_returnsOk() throws Exception {
        when(logMetricService.findSince(any())).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/log-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].service").value("orders"))
                .andExpect(jsonPath("$[0].count").value(7));

        verify(logMetricService).findSince(any());
    }

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
