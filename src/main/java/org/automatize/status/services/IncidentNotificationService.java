package org.automatize.status.services;

import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.StatusIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for sending incident notifications to subscribers.
 * <p>
 * This service coordinates between the NotificationSubscriberService and EmailService
 * to send notifications to all subscribed users when incidents are created or updated.
 * </p>
 *
 * @author Status Monitoring Team
 * @since 1.0
 */
@Service
public class IncidentNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentNotificationService.class);

    @Autowired
    private NotificationSubscriberService subscriberService;

    @Autowired
    private EmailService emailService;

    /**
     * Notifies all active subscribers about a new incident.
     * <p>
     * This method is called when a new incident is created. It retrieves all
     * active and verified subscribers for the affected platform and sends
     * them an email notification.
     * </p>
     *
     * @param incident the newly created incident
     */
    @Async
    @Transactional(readOnly = true)
    public void notifySubscribersOfNewIncident(StatusIncident incident) {
        if (incident == null || incident.getApp() == null) {
            logger.warn("Cannot notify subscribers: incident or app is null");
            return;
        }

        try {
            List<NotificationSubscriber> subscribers =
                    subscriberService.getActiveVerifiedSubscribers(incident.getApp().getId());

            if (subscribers.isEmpty()) {
                logger.info("No active subscribers for app {}, skipping notification",
                        incident.getApp().getName());
                return;
            }

            String platformName = incident.getApp().getName();
            String incidentTitle = incident.getTitle();
            String incidentDescription = incident.getDescription();
            String severity = incident.getSeverity();
            String status = incident.getStatus();

            logger.info("Sending incident notification to {} subscribers for platform {}",
                    subscribers.size(), platformName);

            for (NotificationSubscriber subscriber : subscribers) {
                try {
                    emailService.sendIncidentNotification(
                            subscriber.getEmail(),
                            platformName,
                            incidentTitle,
                            incidentDescription,
                            severity,
                            status
                    );
                } catch (Exception e) {
                    logger.error("Failed to send notification to {}: {}",
                            subscriber.getEmail(), e.getMessage());
                }
            }

            logger.info("Completed sending incident notifications for incident: {}",
                    incident.getTitle());

        } catch (Exception e) {
            logger.error("Error notifying subscribers of incident: {}", e.getMessage());
        }
    }

    /**
     * Notifies all active subscribers about an incident update.
     *
     * @param incident the updated incident
     * @param updateMessage the update message
     */
    @Async
    @Transactional(readOnly = true)
    public void notifySubscribersOfIncidentUpdate(StatusIncident incident, String updateMessage) {
        if (incident == null || incident.getApp() == null) {
            logger.warn("Cannot notify subscribers: incident or app is null");
            return;
        }

        try {
            List<NotificationSubscriber> subscribers =
                    subscriberService.getActiveVerifiedSubscribers(incident.getApp().getId());

            if (subscribers.isEmpty()) {
                logger.info("No active subscribers for app {}, skipping update notification",
                        incident.getApp().getName());
                return;
            }

            String platformName = incident.getApp().getName();

            logger.info("Sending incident update notification to {} subscribers for platform {}",
                    subscribers.size(), platformName);

            for (NotificationSubscriber subscriber : subscribers) {
                try {
                    String subject = String.format("[%s] Incident Update: %s",
                            platformName, incident.getTitle());

                    String content = buildUpdateEmailContent(platformName, incident, updateMessage);

                    emailService.sendHtmlEmail(subscriber.getEmail(), subject, content);
                } catch (Exception e) {
                    logger.error("Failed to send update notification to {}: {}",
                            subscriber.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error notifying subscribers of incident update: {}", e.getMessage());
        }
    }

    /**
     * Notifies all active subscribers that an incident has been resolved.
     *
     * @param incident the resolved incident
     * @param resolutionMessage the resolution message
     */
    @Async
    @Transactional(readOnly = true)
    public void notifySubscribersOfIncidentResolution(StatusIncident incident, String resolutionMessage) {
        if (incident == null || incident.getApp() == null) {
            logger.warn("Cannot notify subscribers: incident or app is null");
            return;
        }

        try {
            List<NotificationSubscriber> subscribers =
                    subscriberService.getActiveVerifiedSubscribers(incident.getApp().getId());

            if (subscribers.isEmpty()) {
                return;
            }

            String platformName = incident.getApp().getName();

            logger.info("Sending incident resolution notification to {} subscribers for platform {}",
                    subscribers.size(), platformName);

            for (NotificationSubscriber subscriber : subscribers) {
                try {
                    String subject = String.format("[%s] Incident Resolved: %s",
                            platformName, incident.getTitle());

                    String content = buildResolutionEmailContent(platformName, incident, resolutionMessage);

                    emailService.sendHtmlEmail(subscriber.getEmail(), subject, content);
                } catch (Exception e) {
                    logger.error("Failed to send resolution notification to {}: {}",
                            subscriber.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error notifying subscribers of incident resolution: {}", e.getMessage());
        }
    }

    /**
     * Builds the HTML content for an incident update email.
     */
    private String buildUpdateEmailContent(String platformName, StatusIncident incident, String updateMessage) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #17a2b8; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .label { font-weight: bold; color: #666; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Incident Update</h1>
                        <p>%s</p>
                    </div>
                    <div class="content">
                        <p><strong>Incident:</strong> %s</p>
                        <p><strong>Current Status:</strong> %s</p>
                        <p><strong>Update:</strong></p>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>Status Monitoring System</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            escapeHtml(platformName),
            escapeHtml(incident.getTitle()),
            escapeHtml(incident.getStatus()),
            escapeHtml(updateMessage != null ? updateMessage : "No update message provided.")
        );
    }

    /**
     * Builds the HTML content for an incident resolution email.
     */
    private String buildResolutionEmailContent(String platformName, StatusIncident incident, String resolutionMessage) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .label { font-weight: bold; color: #666; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Incident Resolved</h1>
                        <p>%s</p>
                    </div>
                    <div class="content">
                        <p><strong>Incident:</strong> %s</p>
                        <p><strong>Resolution:</strong></p>
                        <p>%s</p>
                        <p style="color: #28a745; font-weight: bold;">The service has been restored to normal operation.</p>
                    </div>
                    <div class="footer">
                        <p>Status Monitoring System</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            escapeHtml(platformName),
            escapeHtml(incident.getTitle()),
            escapeHtml(resolutionMessage != null ? resolutionMessage : "Incident has been resolved.")
        );
    }

    /**
     * Escapes HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
