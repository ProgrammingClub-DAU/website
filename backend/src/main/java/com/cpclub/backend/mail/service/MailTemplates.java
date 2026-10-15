package com.cpclub.backend.mail.service;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Text and HTML email templates used by the club site.
 * Implemented using plain Java text blocks without external template engine overhead.
 */
public final class MailTemplates {

    private MailTemplates() {}

    public static String verificationText(String name, String verificationUrl) {
        return """
                Hello %s,

                Thank you for joining the CP Club. Please verify your email address by opening the following link:
                %s

                This link will expire in 24 hours.

                If you did not create this account, please disregard this email.

                Best regards,
                CP Club Team
                """.formatted(name, verificationUrl);
    }

    public static String verificationHtml(String name, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; line-height: 1.5; color: #333;">
                    <h2>Verify Your Email</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for joining the CP Club. Please verify your email address by clicking the link below:</p>
                    <p>
                        <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #2563eb; color: #fff; text-decoration: none; border-radius: 6px;">
                            Verify Email Address
                        </a>
                    </p>
                    <p>Or copy and paste this link into your browser:<br/><a href="%s">%s</a></p>
                    <p>This link will expire in 24 hours.</p>
                    <hr style="border: none; border-top: 1px solid #eaeaea; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #777;">If you did not request this, please disregard this email.</p>
                </body>
                </html>
                """.formatted(name, verificationUrl, verificationUrl, verificationUrl);
    }

    /*
     * The reminder and digest below were reshaped in Stage 5B, before anything sent
     * them: they now carry the signed unsubscribe link every reminder must have (D29),
     * and they escape everything that reaches HTML. An event title is admin input,
     * and admin input in an HTML email is still input.
     */

    /** One line of the contest digest. */
    public record DigestItem(String name, String platform, String when, String url) {
    }

    public static String eventReminderText(String eventName, String when, String location, String eventUrl,
                                           boolean registered, String unsubscribeUrl) {
        return """
                Tomorrow: %s

                When: %s
                Where: %s
                %s
                Details: %s

                --
                You asked for event reminders from the Programming Club.
                Stop them: %s
                """.formatted(eventName, when, location,
                registered ? "You're registered." : "", eventUrl, unsubscribeUrl);
    }

    public static String eventReminderHtml(String eventName, String when, String location, String eventUrl,
                                           boolean registered, String unsubscribeUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; line-height: 1.5; color: #333;">
                    <h2>Tomorrow: %s</h2>
                    <p><strong>When:</strong> %s<br/><strong>Where:</strong> %s</p>
                    %s
                    <p><a href="%s" style="display: inline-block; padding: 8px 16px; background-color: #5e6ad2; color: #fff; text-decoration: none; border-radius: 4px;">Event details</a></p>
                    <hr style="border: none; border-top: 1px solid #eaeaea; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #777;">You asked for event reminders from the Programming Club. <a href="%s" style="color: #777;">Stop them</a>.</p>
                </body>
                </html>
                """.formatted(esc(eventName), esc(when), esc(location),
                registered ? "<p><strong>You're registered.</strong></p>" : "",
                esc(eventUrl), esc(unsubscribeUrl));
    }

    public static String contestDigestText(String dayLabel, List<DigestItem> items, String calendarUrl,
                                           String unsubscribeUrl) {
        StringBuilder lines = new StringBuilder();
        for (DigestItem item : items) {
            lines.append("- ").append(item.name()).append(" (").append(item.platform()).append("), ")
                    .append(item.when()).append("\n  ").append(item.url()).append('\n');
        }
        return """
                Contests in the next 24 hours -- %s

                %s
                Full calendar: %s

                --
                You asked for the contest digest from the Programming Club.
                Stop it: %s
                """.formatted(dayLabel, lines, calendarUrl, unsubscribeUrl);
    }

    public static String contestDigestHtml(String dayLabel, List<DigestItem> items, String calendarUrl,
                                           String unsubscribeUrl) {
        StringBuilder rows = new StringBuilder();
        for (DigestItem item : items) {
            rows.append("<li><a href=\"").append(esc(item.url())).append("\">").append(esc(item.name()))
                    .append("</a> <span style=\"color: #777;\">").append(esc(item.platform())).append(" &middot; ")
                    .append(esc(item.when())).append("</span></li>");
        }
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; line-height: 1.5; color: #333;">
                    <h2>Contests in the next 24 hours</h2>
                    <p style="color: #777;">%s</p>
                    <ul>%s</ul>
                    <p><a href="%s">Full calendar</a></p>
                    <hr style="border: none; border-top: 1px solid #eaeaea; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #777;">You asked for the contest digest from the Programming Club. <a href="%s" style="color: #777;">Stop it</a>.</p>
                </body>
                </html>
                """.formatted(esc(dayLabel), rows, esc(calendarUrl), esc(unsubscribeUrl));
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
