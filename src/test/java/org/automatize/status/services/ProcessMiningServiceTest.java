package org.automatize.status.services;

import org.automatize.status.api.response.ProcessMiningResponse;
import org.automatize.status.models.Log;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.LogRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProcessMiningService} — scope resolution, trace grouping,
 * min-event filtering, truncation and event mapping.
 */
@ExtendWith(MockitoExtension.class)
class ProcessMiningServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private StatusAppRepository statusAppRepository;

    @Mock
    private StatusComponentRepository statusComponentRepository;

    @InjectMocks
    private ProcessMiningService service;

    private final ZonedDateTime from = ZonedDateTime.now().minusDays(1);
    private final ZonedDateTime to = ZonedDateTime.now();

    /**
     * Builds a {@link Log} fixture populated with the supplied trace id, service,
     * level, message and timestamp.
     *
     * @param traceId the trace identifier grouping the log into a case
     * @param service the originating service name (may be {@code null})
     * @param level   the log level (e.g. INFO, ERROR)
     * @param message the log message
     * @param ts      the log timestamp
     * @return a populated {@link Log} instance
     */
    private Log log(String traceId, String service, String level, String message, ZonedDateTime ts) {
        Log l = new Log();
        l.setTraceId(traceId);
        l.setService(service);
        l.setLevel(level);
        l.setMessage(message);
        l.setLogTimestamp(ts);
        return l;
    }

    /**
     * Builds a {@link StatusApp} fixture with the given name.
     *
     * @param name the application/service name
     * @return a populated {@link StatusApp} instance
     */
    private StatusApp app(String name) {
        StatusApp a = new StatusApp();
        a.setName(name);
        return a;
    }

    /**
     * Builds a {@link StatusComponent} fixture with the given name.
     *
     * @param name the component name
     * @return a populated {@link StatusComponent} instance
     */
    private StatusComponent component(String name) {
        StatusComponent c = new StatusComponent();
        c.setName(name);
        return c;
    }

    /**
     * Verifies that an unrecognised scope short-circuits to an empty, non-truncated
     * response and never queries the log repository for trace ids.
     */
    @Test
    void buildCases_unknownScope_returnsEmptyResponse() {
        ProcessMiningResponse resp = service.buildCases("mystery", UUID.randomUUID(), null,
                from, to, 100, 1);

        assertThat(resp.getCases()).isEmpty();
        assertThat(resp.getTotalCases()).isZero();
        assertThat(resp.isTruncated()).isFalse();
        verify(logRepository, never()).findDistinctTraceIdsForServices(any(), any(), any(), any(), any());
    }

    /**
     * Verifies that a platform scope resolving to services but no matching trace ids
     * yields an empty, non-truncated response and skips fetching logs by trace id.
     */
    @Test
    void buildCases_platformScopeNoTraceIds_returnsEmptyResponse() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ProcessMiningResponse resp = service.buildCases("platform", platformId, null,
                from, to, 100, 1);

        assertThat(resp.getCases()).isEmpty();
        assertThat(resp.isTruncated()).isFalse();
        verify(logRepository, never()).findByTraceIdIn(any());
    }

    /**
     * Verifies that an application scope resolves service names from the component
     * repository (via {@code findByAppId}) rather than the app repository.
     */
    @Test
    void buildCases_applicationScope_resolvesComponentNames() {
        UUID appId = UUID.randomUUID();
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(component("comp-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ProcessMiningResponse resp = service.buildCases("application", appId, null,
                from, to, 100, 1);

        assertThat(resp.getCases()).isEmpty();
        verify(statusComponentRepository).findByAppId(appId);
    }

    /**
     * Verifies that logs sharing a trace id are grouped into a single case, sorted
     * ascending by timestamp, and mapped to events carrying a non-blank icon.
     */
    @Test
    void buildCases_groupsByTraceIdAndMapsEventsWithIcons() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of("trace-1"));

        ZonedDateTime t1 = from.plusMinutes(1);
        ZonedDateTime t2 = from.plusMinutes(2);
        // deliberately out of order so we can assert sorting by timestamp
        when(logRepository.findByTraceIdIn(List.of("trace-1"))).thenReturn(List.of(
                log("trace-1", "svc-a", "ERROR", "later", t2),
                log("trace-1", "svc-a", "INFO", "earlier", t1)
        ));

        ProcessMiningResponse resp = service.buildCases("platform", platformId, null,
                from, to, 100, 1);

        assertThat(resp.getCases()).hasSize(1);
        assertThat(resp.getTotalCases()).isEqualTo(1);
        ProcessMiningResponse.ProcessCase c = resp.getCases().get(0);
        assertThat(c.getCaseId()).isEqualTo("trace-1");
        assertThat(c.getEvents()).hasSize(2);
        // sorted ascending by timestamp
        assertThat(c.getEvents().get(0).getMessage()).isEqualTo("earlier");
        assertThat(c.getEvents().get(1).getMessage()).isEqualTo("later");
        assertThat(c.getEvents().get(0).getLevel()).isEqualTo("INFO");
        assertThat(c.getEvents().get(0).getIcon()).isNotBlank();
    }

    /**
     * Verifies that a case with fewer events than the requested minimum is filtered
     * out, producing an empty response with a zero total case count.
     */
    @Test
    void buildCases_belowMinEvents_caseFilteredOut() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of("trace-1"));
        when(logRepository.findByTraceIdIn(any())).thenReturn(List.of(
                log("trace-1", "svc-a", "INFO", "only one", from.plusMinutes(1))
        ));

        ProcessMiningResponse resp = service.buildCases("platform", platformId, null,
                from, to, 100, 2);

        assertThat(resp.getCases()).isEmpty();
        assertThat(resp.getTotalCases()).isZero();
    }

    /**
     * Verifies that when the number of trace ids reaches the max-cases limit the
     * response is flagged as truncated while still returning all cases.
     */
    @Test
    void buildCases_traceIdsEqualMaxCases_flagsTruncated() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of("t1", "t2"));
        when(logRepository.findByTraceIdIn(any())).thenReturn(List.of(
                log("t1", "svc-a", "INFO", "m1", from.plusMinutes(1)),
                log("t2", "svc-a", "INFO", "m2", from.plusMinutes(2))
        ));

        ProcessMiningResponse resp = service.buildCases("platform", platformId, null,
                from, to, 2, 1);

        assertThat(resp.isTruncated()).isTrue();
        assertThat(resp.getCases()).hasSize(2);
    }

    /**
     * Verifies that a log with a null service and an unrecognised level maps to the
     * default activity ("unknown") and the default icon.
     */
    @Test
    void buildCases_nullServiceAndUnknownLevel_useDefaults() {
        UUID platformId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of("trace-1"));
        Log noService = log("trace-1", null, "TRACE", "msg", from.plusMinutes(1));
        when(logRepository.findByTraceIdIn(any())).thenReturn(List.of(noService));

        ProcessMiningResponse resp = service.buildCases("platform", platformId, null,
                from, to, 100, 1);

        ProcessMiningResponse.ProcessEvent event = resp.getCases().get(0).getEvents().get(0);
        assertThat(event.getActivity()).isEqualTo("unknown");           // null service default
        assertThat(event.getIcon()).isEqualTo("📋");            // default icon for unknown level
    }

    /**
     * Verifies that the tenant id, time window and resolved service names are passed
     * through unchanged to the log repository's trace-id lookup.
     */
    @Test
    void buildCases_passesTenantAndWindowThrough() {
        UUID platformId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(statusAppRepository.findByPlatformId(platformId)).thenReturn(List.of(app("svc-a")));
        when(logRepository.findDistinctTraceIdsForServices(
                eq(tenantId), eq(from), eq(to), eq(List.of("svc-a")), any(Pageable.class)))
                .thenReturn(List.of());

        service.buildCases("platform", platformId, tenantId, from, to, 50, 1);

        verify(logRepository).findDistinctTraceIdsForServices(
                eq(tenantId), eq(from), eq(to), eq(List.of("svc-a")), any(Pageable.class));
    }
}
