package com.cpclub.backend.mail.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Outbound email service.
 *
 * <p>Dispatched asynchronously on a background worker thread. When mail is disabled
 * ({@code cpclub.mail.enabled = false}), it logs the subject only -- never the body,
 * which can hold single-use verification tokens or other secrets. Delivery failures
 * are caught and logged, never bubbling back to user requests.</p>
 */
@Service
@Slf4j
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String fromAddress;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${cpclub.mail.enabled:false}") boolean enabled,
            @Value("${cpclub.mail.from:no-reply@localhost}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Async
    public void send(String to, String subject, String text, String html) {
        send(to, subject, text, html, Map.of());
    }

    /**
     * Sends with extra headers. Reminders use this for {@code List-Unsubscribe} and
     * {@code List-Unsubscribe-Post}, which mail clients turn into a one-click
     * unsubscribe button (RFC 8058).
     */
    public void send(String to, String subject, String text, String html, Map<String, String> headers) {
        if (!enabled) {
            // Logs the subject only -- NEVER the body, which can hold a token (Table 4.11)
            log.info("Mail disabled; would have sent email to: {} with subject: '{}'", to, subject);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is enabled but no JavaMailSender is configured. Skipping email to {} with subject: '{}'", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            if (html != null && !html.isBlank()) {
                if (text != null && !text.isBlank()) {
                    helper.setText(text, html);
                } else {
                    helper.setText(html, true);
                }
            } else {
                helper.setText(text != null ? text : "", false);
            }

            for (Map.Entry<String, String> header : headers.entrySet()) {
                message.setHeader(header.getKey(), header.getValue());
            }
            mailSender.send(message);
            log.info("Successfully sent email to {} with subject: '{}'", to, subject);
        } catch (Exception e) {
            // Delivery failures are logged and never reach the request (Table 4.11)
            log.error("Failed to send email to {} with subject '{}': {}", to, subject, e.getMessage());
        }
    }
}
