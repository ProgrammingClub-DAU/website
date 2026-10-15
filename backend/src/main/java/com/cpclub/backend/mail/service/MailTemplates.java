package com.cpclub.backend.mail.service;

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

    public static String eventReminderText(String eventName, String eventTime, String eventUrl) {
        return """
                Upcoming Event Reminder: %s

                When: %s
                Details: %s

                We look forward to seeing you there!
                CP Club Team
                """.formatted(eventName, eventTime, eventUrl);
    }

    public static String eventReminderHtml(String eventName, String eventTime, String eventUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; line-height: 1.5; color: #333;">
                    <h2>Reminder: %s</h2>
                    <p>This is a quick reminder for our upcoming event:</p>
                    <p><strong>Time:</strong> %s</p>
                    <p><a href="%s" style="display: inline-block; padding: 8px 16px; background-color: #2563eb; color: #fff; text-decoration: none; border-radius: 4px;">View Event Details</a></p>
                    <hr style="border: none; border-top: 1px solid #eaeaea; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #777;">CP Club Team</p>
                </body>
                </html>
                """.formatted(eventName, eventTime, eventUrl);
    }

    public static String contestDigestText(String weekLabel, String contestsSummary, String calendarUrl) {
        return """
                Contest Digest for %s

                %s

                Check the full calendar at: %s

                Good luck and high ratings!
                CP Club Team
                """.formatted(weekLabel, contestsSummary, calendarUrl);
    }

    public static String contestDigestHtml(String weekLabel, String contestsSummary, String calendarUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; line-height: 1.5; color: #333;">
                    <h2>Upcoming Contests: %s</h2>
                    <pre style="font-family: sans-serif; white-space: pre-wrap;">%s</pre>
                    <p><a href="%s">View Club Calendar</a></p>
                    <hr style="border: none; border-top: 1px solid #eaeaea; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #777;">CP Club Team</p>
                </body>
                </html>
                """.formatted(weekLabel, contestsSummary, calendarUrl);
    }
}
