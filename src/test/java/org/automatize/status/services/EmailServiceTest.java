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
 *
 * <p>Testing approach: the {@link JavaMailSender} collaborator is mocked with Mockito and
 * the service under test is created via {@link InjectMocks}. The {@code fromEmail} and
 * {@code emailEnabled} configuration fields are seeded through {@link ReflectionTestUtils}
 * in {@link #setUp()} since they are normally supplied by property injection. Each test
 * verifies the send/never-send behaviour against the mock rather than dispatching real
 * mail, and confirms that transport failures are swallowed instead of propagated.</p>
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    /**
     * Seeds the property-injected configuration fields before each test so the service
     * behaves as if it were fully wired: a valid from-address and email sending enabled.
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@status.local");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
    }

    /**
     * Creates a bare {@link MimeMessage} not bound to any mail session, used to stub the
     * mock sender's {@code createMimeMessage()} in HTML-email tests.
     *
     * @return a new, session-less {@link MimeMessage} instance
     */
    private MimeMessage newMimeMessage() {
        return new MimeMessage((Session) null);
    }

    // ---------------------------------------------------------------- simple

    /**
     * Verifies that when email is disabled, requesting a simple email results in no message
     * being handed to the mail sender.
     */
    @Test
    void sendSimpleEmail_whenDisabled_doesNotSend() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        emailService.sendSimpleEmail("to@x.com", "subject", "body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /**
     * Verifies that when email is enabled, a simple email is dispatched exactly once as a
     * {@link SimpleMailMessage}.
     */
    @Test
    void sendSimpleEmail_whenEnabled_sendsMessage() {
        emailService.sendSimpleEmail("to@x.com", "subject", "body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /**
     * Verifies that a failure thrown by the mail sender while sending a simple email is
     * caught internally and does not propagate out of the service.
     */
    @Test
    void sendSimpleEmail_whenSenderThrows_doesNotPropagate() {
        doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendSimpleEmail("to@x.com", "s", "b"))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ------------------------------------------------------------------ html

    /**
     * Verifies that when email is disabled, an HTML email request neither creates a MIME
     * message nor sends anything.
     */
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
