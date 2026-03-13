package org.automatize.status.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * <p>
 * Service responsible for sending email notifications.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Send simple text and HTML-formatted emails</li>
 *   <li>Execute email sending asynchronously</li>
 *   <li>Handle email enable/disable configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@status.local}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Sends a simple text email asynchronously.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param text the email body text
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            logger.info("Email disabled. Would have sent email to {} with subject: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Sent email to {} with subject: {}", to, subject);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an HTML-formatted email asynchronously.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param htmlContent the HTML content of the email
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            logger.info("Email disabled. Would have sent HTML email to {} with subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Sent HTML email to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an incident notification email.
     *
     * @param to the recipient email address
     * @param platformName the name of the affected platform
     * @param incidentTitle the title of the incident
     * @param incidentDescription the description of the incident
     * @param severity the severity level
     * @param status the current status of the incident
     */
    @Async
    public void sendIncidentNotification(String to, String platformName, String incidentTitle,
                                         String incidentDescription, String severity, String status) {
        String subject = String.format("[%s] Incident Alert: %s", platformName, incidentTitle);

        String htmlContent = buildIncidentEmailHtml(platformName, incidentTitle,
                incidentDescription, severity, status);

        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Builds the HTML content for an incident notification email.
     *
     * @param platformName the platform name
     * @param incidentTitle the incident title
     * @param incidentDescription the incident description
     * @param severity the severity level
     * @param status the incident status
     * @return the HTML content
     */
    private String buildIncidentEmailHtml(String platformName, String incidentTitle,
                                          String incidentDescription, String severity, String status) {
        String severityColor = getSeverityColor(severity);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .incident-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; }
                    .label { font-weight: bold; color: #666; }
                    .badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
                    .badge-severity { background-color: %s; color: white; }
                    .badge-status { background-color: #6c757d; color: white; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Incident Alert</h1>
                        <p>%s</p>
                    </div>
                    <div class="content">
                        <p class="incident-title">%s</p>
                        <p><span class="label">Severity:</span> <span class="badge badge-severity">%s</span></p>
                        <p><span class="label">Status:</span> <span class="badge badge-status">%s</span></p>
                        <p><span class="label">Description:</span></p>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>You are receiving this email because you are subscribed to incident notifications for %s.</p>
                        <p>Status Monitoring System</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            severityColor,
            severityColor,
            escapeHtml(platformName),
            escapeHtml(incidentTitle),
            escapeHtml(severity),
            escapeHtml(status),
            escapeHtml(incidentDescription != null ? incidentDescription : "No description provided."),
            escapeHtml(platformName)
        );
    }

    /**
     * Gets the color code for a severity level.
     *
     * @param severity the severity level
     * @return the hex color code
     */
    private String getSeverityColor(String severity) {
        if (severity == null) return "#6c757d";
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "#dc3545";
            case "MAJOR" -> "#fd7e14";
            case "MINOR" -> "#ffc107";
            case "MAINTENANCE" -> "#17a2b8";
            default -> "#6c757d";
        };
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     *
     * @param text the text to escape
     * @return the escaped text
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
