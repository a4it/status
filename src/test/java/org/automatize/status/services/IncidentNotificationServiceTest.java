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
 */
@ExtendWith(MockitoExtension.class)
class IncidentNotificationServiceTest {

    @Mock
    private NotificationSubscriberService subscriberService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private IncidentNotificationService service;

    private StatusApp app(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("Platform");
        return app;
    }

    private StatusIncident incident(StatusApp app) {
        StatusIncident incident = new StatusIncident();
        incident.setApp(app);
        incident.setTitle("Outage");
        incident.setDescription("Something broke");
        incident.setSeverity("CRITICAL");
        incident.setStatus("INVESTIGATING");
        return incident;
    }

    private NotificationSubscriber subscriber(String email) {
        NotificationSubscriber sub = new NotificationSubscriber();
        sub.setEmail(email);
        return sub;
    }

    // -------------------------------------------------- new incident

    @Test
    void notifyNew_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfNewIncident(null);

        verifyNoInteractions(subscriberService, emailService);
    }

    @Test
    void notifyNew_whenAppNull_doesNothing() {
        StatusIncident incident = incident(null);

        service.notifySubscribersOfNewIncident(incident);

        verifyNoInteractions(subscriberService, emailService);
    }

    @Test
    void notifyNew_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService, never()).sendIncidentNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notifyNew_sendsIncidentNotificationToEachSubscriber() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber("a@x.com"), subscriber("b@x.com")));

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService).sendIncidentNotification(eq("a@x.com"), eq("Platform"),
                eq("Outage"), eq("Something broke"), eq("CRITICAL"), eq("INVESTIGATING"));
        verify(emailService).sendIncidentNotification(eq("b@x.com"), anyString(),
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void notifyNew_whenOneSubscriberFails_stillNotifiesOthers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber("a@x.com"), subscriber("b@x.com")));
        doThrow(new RuntimeException("smtp"))
                .when(emailService).sendIncidentNotification(eq("a@x.com"), any(), any(), any(), any(), any());

        service.notifySubscribersOfNewIncident(incident);

        verify(emailService, times(2)).sendIncidentNotification(any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------- incident update

    @Test
    void notifyUpdate_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfIncidentUpdate(null, "msg");

        verifyNoInteractions(subscriberService, emailService);
    }

    @Test
    void notifyUpdate_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfIncidentUpdate(incident, "update msg");

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void notifyUpdate_sendsHtmlEmailToSubscribers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber("a@x.com")));

        service.notifySubscribersOfIncidentUpdate(incident, "update msg");

        verify(emailService).sendHtmlEmail(eq("a@x.com"), anyString(), anyString());
    }

    // -------------------------------------------------- incident resolution

    @Test
    void notifyResolution_whenIncidentNull_doesNothing() {
        service.notifySubscribersOfIncidentResolution(null, "msg");

        verifyNoInteractions(subscriberService, emailService);
    }

    @Test
    void notifyResolution_whenNoSubscribers_sendsNothing() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId)).thenReturn(List.of());

        service.notifySubscribersOfIncidentResolution(incident, "resolved");

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void notifyResolution_sendsHtmlEmailToSubscribers() {
        UUID appId = UUID.randomUUID();
        StatusIncident incident = incident(app(appId));
        when(subscriberService.getActiveVerifiedSubscribers(appId))
                .thenReturn(List.of(subscriber("a@x.com")));

        service.notifySubscribersOfIncidentResolution(incident, "resolved");

        verify(emailService).sendHtmlEmail(eq("a@x.com"), anyString(), anyString());
    }
}
