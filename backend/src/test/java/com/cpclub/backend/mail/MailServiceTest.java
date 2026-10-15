package com.cpclub.backend.mail;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cpclub.backend.mail.service.MailService;
import com.cpclub.backend.mail.service.MailTemplates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MailServiceTest {

    private MailService mailService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mockProvider = mock(ObjectProvider.class);

        // mail is disabled (enabled = false)
        mailService = new MailService(mockProvider, false, "no-reply@localhost");

        logger = (Logger) LoggerFactory.getLogger(MailService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Mail disabled, verification email: log line holds the subject, and does not contain the token")
    void mailDisabled_logsSubjectAndNeverContainsToken() {
        String sensitiveToken = "super-secret-token-abcdef123456";
        String verificationUrl = "http://localhost:3000/verify-email?token=" + sensitiveToken;
        String subject = "Verify your CP Club email";
        String text = MailTemplates.verificationText("Ravi", verificationUrl);
        String html = MailTemplates.verificationHtml("Ravi", verificationUrl);

        mailService.send("ravi@dau.ac.in", subject, text, html);

        // Verify log events
        boolean foundSubject = false;
        for (ILoggingEvent event : listAppender.list) {
            String formattedMessage = event.getFormattedMessage();
            if (formattedMessage.contains(subject)) {
                foundSubject = true;
            }
            // Ensure the raw token is NEVER leaked into the logs!
            assertFalse(formattedMessage.contains(sensitiveToken),
                    "Log message must never contain the sensitive token");
        }

        assertTrue(foundSubject, "Log must contain the email subject");
    }
}
