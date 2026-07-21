package org.automatize.status.services;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailService}.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@status.local");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
    }

    private MimeMessage newMimeMessage() {
        return new MimeMessage((Session) null);
    }

    // ---------------------------------------------------------------- simple

    @Test
    void sendSimpleEmail_whenDisabled_doesNotSend() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendSimpleEmail("to@x.com", "subject", "body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_whenEnabled_sendsMessage() {
        emailService.sendSimpleEmail("to@x.com", "subject", "body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_whenSenderThrows_doesNotPropagate() {
        doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendSimpleEmail("to@x.com", "s", "b"))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ------------------------------------------------------------------ html

    @Test
    void sendHtmlEmail_whenDisabled_doesNotSend() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendHtmlEmail("to@x.com", "subject", "<p>hi</p>");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmail_whenEnabled_sendsMimeMessage() {
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

        emailService.sendHtmlEmail("to@x.com", "subject", "<p>hi</p>");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // --------------------------------------------------- incident delegation

    @Test
    void sendIncidentNotification_whenEnabled_sendsHtmlEmail() {
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

        emailService.sendIncidentNotification("to@x.com", "Platform", "Title",
                "Description", "CRITICAL", "INVESTIGATING");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendIncidentNotification_whenDisabled_doesNotSend() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendIncidentNotification("to@x.com", "Platform", "Title",
                "Description", null, "INVESTIGATING");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
