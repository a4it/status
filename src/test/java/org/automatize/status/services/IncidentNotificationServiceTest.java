package org.automatize.status.services;

import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusIncident;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IncidentNotificationService}.
 *
 * <p>Testing approach: the {@link NotificationSubscriberService} and {@link EmailService}
 * collaborators are Mockito mocks and the service under test is created via
 * {@link InjectMocks}. Tests use small builder helpers to assemble incident graphs, then
 * assert on the fan-out to subscribers - covering null-guard short circuits, the
 * empty-subscriber no-op, per-subscriber dispatch, and resilience when one subscriber's
 * send throws - by verifying the appropriate email methods on the mock.</p>
 */
@ExtendWith(MockitoExtension.class)
class IncidentNotificationServiceTest {

    private static final String SUBSCRIBER_A_EMAIL = "a@x.com";
    private static final String SUBSCRIBER_B_EMAIL = "b@x.com";

    @Mock
    private NotificationSubscriberService subscriberService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private IncidentNotificationService service;

    /**
     * Builds a named {@link StatusApp} fixture with the given id.
     *
     * @param id the app identifier
     * @return a populated {@link StatusApp} test fixture
     */
    private StatusApp app(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("Platform");
        return app;
    }

    /**
     * Builds a critical, investigating {@link StatusIncident} fixture attached to the given
     * app.
     *
     * @param app the app the incident belongs to (may be null to exercise guards)
     * @return a populated {@link StatusIncident} test fixture
     */
    private StatusIncident incident(StatusApp app) {
        StatusIncident incident = new StatusIncident();
        incident.setApp(app);
        incident.setTitle("Outage");
        incident.setDescription("Something broke");
        incident.setSeverity("CRITICAL");
        incident.setStatus("INVESTIGATING");
        return incident;
    }

    /**
     * Builds a {@link NotificationSubscriber} fixture with the given email address.
     *
     * @param email the subscriber's email address
     * @return a populated {@link NotificationSubscriber} test fixture
     */
    private NotificationSubscriber subscriber(String email) {
        NotificationSubscriber sub = new NotificationSubscriber();
        sub.setEmail(email);
        return sub;
    }

    // -------------------------------------------------- new incident

    /**
     * Verifies that notifying of a new incident with a null incident short circuits without
     * touching either collaborator.
     */
    @Test
    void notifyNew_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfNewIncident(null);

        verifyNoInteractions(subscriberService, emailService);
    }

    /**
     * Verifies that notifying of a new incident whose app is null short circuits without
     * touching either collaborator.
     */
    @Test
    void notifyNew_whenAppNull_doesNothing() {
        StatusIncident incident = incident(null);

        service.notifySubscribersOfNewIncident(incident);

        verifyNoInteractions(subscriberService, emailService);
    }

    /**
     * Verifies that a new incident with no active verified subscribers sends no incident
     * notification.
     */
    @Test
    void notifyNew_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService, never()).sendIncidentNotification(any(), any(), any(), any(), any(), any());
    }

    /**
     * Verifies that a new incident sends one incident notification to each subscriber,
     * carrying the incident's platform, title, description, severity, and status.
     */
    @Test
    void notifyNew_sendsIncidentNotificationToEachSubscriber() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber(SUBSCRIBER_A_EMAIL), subscriber(SUBSCRIBER_B_EMAIL)));

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService).sendIncidentNotification(eq(SUBSCRIBER_A_EMAIL), eq("Platform"),
                eq("Outage"), eq("Something broke"), eq("CRITICAL"), eq("INVESTIGATING"));
        verify(emailService).sendIncidentNotification(eq(SUBSCRIBER_B_EMAIL), anyString(),
                anyString(), any(), anyString(), anyString());
    }

    /**
     * Verifies that if sending to one subscriber throws, the remaining subscribers are still
     * notified (both sends attempted).
     */
    @Test
    void notifyNew_whenOneSubscriberFails_stillNotifiesOthers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber(SUBSCRIBER_A_EMAIL), subscriber(SUBSCRIBER_B_EMAIL)));
        doThrow(new RuntimeException("smtp"))
                .when(emailService).sendIncidentNotification(eq(SUBSCRIBER_A_EMAIL), any(), any(), any(), any(), any());

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService, times(2)).sendIncidentNotification(any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------- incident update

    /**
     * Verifies that notifying of an incident update with a null incident short circuits
     * without touching either collaborator.
     */
    @Test
    void notifyUpdate_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfIncidentUpdate(null, "msg");

        verifyNoInteractions(subscriberService, emailService);
    }

    /**
     * Verifies that an incident update with no active verified subscribers sends no HTML
     * email.
     */
    @Test
    void notifyUpdate_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfIncidentUpdate(incident, "update msg");

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    /**
     * Verifies that an incident update sends an HTML email to each subscriber.
     */
    @Test
    void notifyUpdate_sendsHtmlEmailToSubscribers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber(SUBSCRIBER_A_EMAIL)));

        service.notifySubscribersOfIncidentUpdate(incident, "update msg");

        verify(emailService).sendHtmlEmail(eq(SUBSCRIBER_A_EMAIL), anyString(), anyString());
    }

    // -------------------------------------------------- incident resolution

    /**
     * Verifies that notifying of an incident resolution with a null incident short circuits
     * without touching either collaborator.
     */
    @Test
    void notifyResolution_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfIncidentResolution(null, "msg");

        verifyNoInteractions(subscriberService, emailService);
    }

    /**
     * Verifies that an incident resolution with no active verified subscribers sends no HTML
     * email.
     */
    @Test
    void notifyResolution_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfIncidentResolution(incident, "resolved");

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    /**
     * Verifies that an incident resolution sends an HTML email to each subscriber.
     */
    @Test
    void notifyResolution_sendsHtmlEmailToSubscribers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber(SUBSCRIBER_A_EMAIL)));

        service.notifySubscribersOfIncidentResolution(incident, "resolved");

        verify(emailService).sendHtmlEmail(eq(SUBSCRIBER_A_EMAIL), anyString(), anyString());
    }
}
