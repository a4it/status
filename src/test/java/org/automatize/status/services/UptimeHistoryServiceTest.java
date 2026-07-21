package org.automatize.status.services;

import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.models.StatusIncident;
import org.automatize.status.models.StatusUptimeHistory;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.automatize.status.repositories.StatusIncidentComponentRepository;
import org.automatize.status.repositories.StatusIncidentRepository;
import org.automatize.status.repositories.StatusUptimeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UptimeHistoryService}.
 */
@ExtendWith(MockitoExtension.class)
class UptimeHistoryServiceTest {

    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;
    @Mock
    private StatusIncidentRepository statusIncidentRepository;
    @Mock
    private StatusIncidentComponentRepository statusIncidentComponentRepository;
    @Mock
    private StatusUptimeHistoryRepository statusUptimeHistoryRepository;

    @InjectMocks
    private UptimeHistoryService uptimeHistoryService;

    /**
     * Enables the service before each test by setting its {@code enabled} flag to true
     * via reflection, since it is normally driven by configuration.
     */
    @BeforeEach
    void enableService() {
        ReflectionTestUtils.setField(uptimeHistoryService, "enabled", true);
    }

    /**
     * Builds a {@link StatusApp} test fixture with the given identifier.
     *
     * @param id the app identifier to assign
     * @return a populated {@link StatusApp} instance for use in tests
     */
    private StatusApp newApp(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App");
        return app;
    }

    /**
     * Builds a {@link StatusIncident} that fully spans the given date, starting the day
     * before and resolving two days after, with the supplied severity.
     *
     * @param date     the date the incident should cover
     * @param severity the severity value to assign
     * @return a populated {@link StatusIncident} instance for use in tests
     */
    private StatusIncident incidentSpanningDate(LocalDate date, String severity) {
        StatusIncident incident = new StatusIncident();
        incident.setId(UUID.randomUUID());
        incident.setSeverity(severity);
        incident.setStartedAt(date.minusDays(1).atStartOfDay(ZoneId.systemDefault()));
        incident.setResolvedAt(date.plusDays(2).atStartOfDay(ZoneId.systemDefault()));
        return incident;
    }

    /**
     * Verifies that {@code calculateDailyUptime} performs no work when the service is
     * disabled, never touching the app repository.
     */
    @Test
    void calculateDailyUptime_disabled_doesNothing() {
        ReflectionTestUtils.setField(uptimeHistoryService, "enabled", false);

        uptimeHistoryService.calculateDailyUptime();

        verify(statusAppRepository, never()).findAll();
    }

    /**
     * Verifies that {@code calculateDailyUptime} processes each app and persists an
     * uptime history record when the service is enabled.
     */
    @Test
    void calculateDailyUptime_enabled_processesApps() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        when(statusAppRepository.findAll()).thenReturn(List.of(app));
        when(statusIncidentRepository.findPublicIncidentsAffectingDate(eq(appId), any(), any())).thenReturn(List.of());
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        uptimeHistoryService.calculateDailyUptime();

        verify(statusUptimeHistoryRepository).save(any(StatusUptimeHistory.class));
    }

    /**
     * Verifies that {@code calculateUptimeForDate} records an OPERATIONAL status with
     * zero outage minutes and 100% uptime when there are no incidents on the date.
     */
    @Test
    void calculateUptimeForDate_noIncidents_savesOperationalFullUptime() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        LocalDate date = LocalDate.now().minusDays(1);
        when(statusAppRepository.findAll()).thenReturn(List.of(app));
        when(statusIncidentRepository.findPublicIncidentsAffectingDate(eq(appId), any(), any())).thenReturn(List.of());
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        uptimeHistoryService.calculateUptimeForDate(date);

        ArgumentCaptor<StatusUptimeHistory> captor = ArgumentCaptor.forClass(StatusUptimeHistory.class);
        verify(statusUptimeHistoryRepository).save(captor.capture());
        StatusUptimeHistory record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo("OPERATIONAL");
        assertThat(record.getOutageMinutes()).isZero();
        assertThat(record.getUptimePercentage().doubleValue()).isEqualTo(100.0);
    }

    /**
     * Verifies that {@code calculateUptimeForDate} records a MAJOR_OUTAGE status with
     * positive outage minutes when a critical incident spans the date.
     */
    @Test
    void calculateUptimeForDate_severeIncident_savesMajorOutage() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        LocalDate date = LocalDate.now().minusDays(1);
        when(statusAppRepository.findAll()).thenReturn(List.of(app));
        when(statusIncidentRepository.findPublicIncidentsAffectingDate(eq(appId), any(), any()))
                .thenReturn(List.of(incidentSpanningDate(date, "CRITICAL")));
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        uptimeHistoryService.calculateUptimeForDate(date);

        ArgumentCaptor<StatusUptimeHistory> captor = ArgumentCaptor.forClass(StatusUptimeHistory.class);
        verify(statusUptimeHistoryRepository).save(captor.capture());
        StatusUptimeHistory record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo("MAJOR_OUTAGE");
        assertThat(record.getOutageMinutes()).isPositive();
    }

    /**
     * Verifies that {@code calculateUptimeForDate} records a DEGRADED status with
     * positive degraded minutes and zero outage minutes when a minor incident spans the date.
     */
    @Test
    void calculateUptimeForDate_minorIncident_savesDegraded() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        LocalDate date = LocalDate.now().minusDays(1);
        when(statusAppRepository.findAll()).thenReturn(List.of(app));
        when(statusIncidentRepository.findPublicIncidentsAffectingDate(eq(appId), any(), any()))
                .thenReturn(List.of(incidentSpanningDate(date, "MINOR")));
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of());

        uptimeHistoryService.calculateUptimeForDate(date);

        ArgumentCaptor<StatusUptimeHistory> captor = ArgumentCaptor.forClass(StatusUptimeHistory.class);
        verify(statusUptimeHistoryRepository).save(captor.capture());
        StatusUptimeHistory record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo("DEGRADED");
        assertThat(record.getDegradedMinutes()).isPositive();
        assertThat(record.getOutageMinutes()).isZero();
    }

    /**
     * Verifies that {@code calculateUptimeForDate} persists two uptime history records
     * (one for the app and one for its component) when the app has a component.
     */
    @Test
    void calculateUptimeForDate_withComponent_savesAppAndComponentRecords() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        StatusComponent component = new StatusComponent();
        component.setId(UUID.randomUUID());
        component.setApp(app);
        component.setName("Comp");
        LocalDate date = LocalDate.now().minusDays(1);

        when(statusAppRepository.findAll()).thenReturn(List.of(app));
        when(statusIncidentRepository.findPublicIncidentsAffectingDate(eq(appId), any(), any())).thenReturn(List.of());
        when(statusComponentRepository.findByAppId(appId)).thenReturn(List.of(component));
        when(statusIncidentComponentRepository.findPublicIncidentsAffectingComponentOnDate(eq(component.getId()), any(), any()))
                .thenReturn(List.of());

        uptimeHistoryService.calculateUptimeForDate(date);

        verify(statusUptimeHistoryRepository, times(2)).save(any(StatusUptimeHistory.class));
    }

    /**
     * Verifies that {@code backfillUptimeHistory} processes the requested number of days,
     * returning that count and invoking app processing once per day.
     */
    @Test
    void backfillUptimeHistory_processesRequestedNumberOfDays() {
        when(statusAppRepository.findAll()).thenReturn(List.of());

        int processed = uptimeHistoryService.backfillUptimeHistory(3);

        assertThat(processed).isEqualTo(3);
        verify(statusAppRepository, times(3)).findAll();
    }
}
