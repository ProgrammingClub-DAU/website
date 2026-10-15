package com.cpclub.backend.mail.service;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRegistrationRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.mail.entity.ReminderKind;
import com.cpclub.backend.mail.repository.ReminderLogRepository;
import com.cpclub.backend.mail.service.MailTemplates.DigestItem;
import com.cpclub.backend.mail.service.UnsubscribeTokenService.Kind;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Opt-in reminder emails (D29): a 24-hour notice before each club event, and a daily
 * digest of contests starting in the next 24 hours.
 *
 * <p><b>Idempotency.</b> Each send first claims a {@code reminder_log} row with
 * {@code INSERT ... ON CONFLICT DO NOTHING}, and sends only if that insert won. A cron
 * that runs twice, or two instances running at once, cannot email anyone twice. The
 * trade is deliberate: a send that fails <em>after</em> its row is claimed is never
 * retried, because a missed reminder is better than a duplicate one.</p>
 *
 * <p><b>Times.</b> Event reminders work in IST wall-clock, because that is what
 * {@code events.event_date} holds (see EventRegistrationService). The digest works in
 * UTC, because that is what external and club contest start times hold (D31), and
 * converts to IST only to display them.</p>
 *
 * <p>Nothing runs while mail is disabled -- not even the claims. Claiming rows for
 * emails that are only logged would mark those reminders sent, and turning mail on
 * later would then skip them.</p>
 */
@Slf4j
@Service
public class ReminderService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ExternalContestRepository externalContestRepository;
    private final ClubContestRepository clubContestRepository;
    private final UserRepository userRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final MailService mailService;
    private final UnsubscribeTokenService unsubscribeTokens;
    private final VerifiedMemberGuard verifiedMemberGuard;
    private final RateLimiter mailRateLimiter;
    /** The Asia/Kolkata clock from AppConfig. */
    private final Clock clock;
    private final String siteUrl;
    private final String apiUrl;

    public ReminderService(EventRepository eventRepository,
                           EventRegistrationRepository registrationRepository,
                           ExternalContestRepository externalContestRepository,
                           ClubContestRepository clubContestRepository,
                           UserRepository userRepository,
                           ReminderLogRepository reminderLogRepository,
                           MailService mailService,
                           UnsubscribeTokenService unsubscribeTokens,
                           VerifiedMemberGuard verifiedMemberGuard,
                           @Qualifier("mailRateLimiter") RateLimiter mailRateLimiter,
                           Clock clock,
                           @Value("${cpclub.public-site-url:http://localhost:3000}") String siteUrl,
                           @Value("${cpclub.public-api-url:http://localhost:8080}") String apiUrl) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.externalContestRepository = externalContestRepository;
        this.clubContestRepository = clubContestRepository;
        this.userRepository = userRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.mailService = mailService;
        this.unsubscribeTokens = unsubscribeTokens;
        this.verifiedMemberGuard = verifiedMemberGuard;
        this.mailRateLimiter = mailRateLimiter;
        this.clock = clock;
        this.siteUrl = stripTrailingSlash(siteUrl);
        this.apiUrl = stripTrailingSlash(apiUrl);
    }

    /**
     * Hourly, on the hour: every UPCOMING event starting in (now + 23h, now + 24h].
     *
     * <p>The window is anchored to the top of the hour, not to the moment the job
     * happens to fire, so consecutive runs tile the timeline exactly: each event falls
     * into one run's window and no other, whatever the scheduling jitter.</p>
     *
     * @return emails sent
     */
    @Scheduled(cron = "0 0 * * * *", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public int sendEventReminders() {
        if (!mailService.isEnabled()) {
            return 0;
        }
        LocalDateTime hour = LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime after = hour.plusHours(23);
        LocalDateTime until = hour.plusHours(24);
        List<Event> events = eventRepository
                .findByEventDateBetweenAndStatusIn(after, until, List.of(EventStatus.UPCOMING)).stream()
                .filter(e -> e.getEventDate().isAfter(after))
                .toList();
        if (events.isEmpty()) {
            return 0;
        }

        List<User> recipients = eligible(userRepository.findByNotifyEventsTrue());
        int sent = 0;
        for (Event event : events) {
            Set<Long> registered = registrationRepository.findUserIdsByEventId(event.getId());
            String ref = "event:" + event.getId();
            for (User user : recipients) {
                if (!claim(user, ReminderKind.EVENT_24H, ref)) {
                    continue;
                }
                mailRateLimiter.acquire();
                String unsubscribe = unsubscribePage(user, Kind.EVENTS);
                String when = WHEN.format(event.getEventDate()) + " IST";
                String eventUrl = siteUrl + "/events/" + event.getId();
                boolean isRegistered = registered.contains(user.getId());
                mailService.send(user.getEmail(), "Tomorrow: " + event.getTitle(),
                        MailTemplates.eventReminderText(event.getTitle(), when, event.getLocation(), eventUrl,
                                isRegistered, unsubscribe),
                        MailTemplates.eventReminderHtml(event.getTitle(), when, event.getLocation(), eventUrl,
                                isRegistered, unsubscribe),
                        unsubscribeHeaders(user, Kind.EVENTS));
                sent++;
            }
        }
        log.info("Event reminders: {} email(s) for {} event(s)", sent, events.size());
        return sent;
    }

    /**
     * Daily at 09:00 IST: external and club contests starting in the next 24 hours.
     * No email at all on a day with nothing starting.
     *
     * @return emails sent
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public int sendContestDigest() {
        if (!mailService.isEnabled()) {
            return 0;
        }
        LocalDateTime from = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime to = from.plusHours(24);

        record Starting(LocalDateTime startsAtUtc, DigestItem item) {
        }
        List<Starting> starting = new ArrayList<>();
        for (ExternalContest c : externalContestRepository.findByStartsAtGreaterThanEqualAndStartsAtLessThanEqual(from, to)) {
            if (c.getStartsAt().isBefore(to)) {
                starting.add(new Starting(c.getStartsAt(),
                        new DigestItem(c.getName(), platformLabel(c.getPlatform().name()), istLabel(c.getStartsAt()), c.getUrl())));
            }
        }
        for (ClubContest c : clubContestRepository.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(from, to)) {
            starting.add(new Starting(c.getStartsAt(),
                    new DigestItem(c.getName(), "Club contest", istLabel(c.getStartsAt()), codeforcesUrl(c.getCfContestId()))));
        }
        if (starting.isEmpty()) {
            return 0;
        }
        starting.sort(Comparator.comparing(Starting::startsAtUtc));
        List<DigestItem> items = starting.stream().map(Starting::item).toList();

        LocalDate today = LocalDate.now(clock);
        String ref = "digest:" + today;
        String dayLabel = DAY.format(today);
        int sent = 0;
        for (User user : eligible(userRepository.findByNotifyContestsTrue())) {
            if (!claim(user, ReminderKind.CONTEST_DIGEST, ref)) {
                continue;
            }
            mailRateLimiter.acquire();
            String unsubscribe = unsubscribePage(user, Kind.CONTESTS);
            mailService.send(user.getEmail(), "Contests in the next 24 hours",
                    MailTemplates.contestDigestText(dayLabel, items, siteUrl + "/calendar", unsubscribe),
                    MailTemplates.contestDigestHtml(dayLabel, items, siteUrl + "/calendar", unsubscribe),
                    unsubscribeHeaders(user, Kind.CONTESTS));
            sent++;
        }
        log.info("Contest digest: {} email(s), {} contest(s)", sent, items.size());
        return sent;
    }

    // ------------------------------------------------------------------ helpers

    /** Opted-in members, minus the unverified ones when verification is enforced (D16). */
    private List<User> eligible(List<User> optedIn) {
        if (!verifiedMemberGuard.isRequireEmailVerification()) {
            return optedIn;
        }
        return optedIn.stream().filter(User::isEmailVerified).toList();
    }

    private boolean claim(User user, ReminderKind kind, String ref) {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        return reminderLogRepository.claim(user.getId(), kind.name(), ref, nowUtc) == 1;
    }

    /** The link in the email body: the site's unsubscribe page, which confirms what stopped. */
    private String unsubscribePage(User user, Kind kind) {
        return siteUrl + "/unsubscribe?token=" + unsubscribeTokens.issue(user.getId(), kind);
    }

    /**
     * RFC 8058 one-click unsubscribe. Gmail and others POST
     * {@code List-Unsubscribe=One-Click} straight to this URL, so it must be the API
     * itself -- the frontend does not proxy /api.
     */
    private Map<String, String> unsubscribeHeaders(User user, Kind kind) {
        String endpoint = apiUrl + "/api/notifications/unsubscribe?token=" + unsubscribeTokens.issue(user.getId(), kind);
        return Map.of(
                "List-Unsubscribe", "<" + endpoint + ">",
                "List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
    }

    private static String istLabel(LocalDateTime utc) {
        return WHEN.format(utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(IST)) + " IST";
    }

    private static String platformLabel(String platform) {
        return switch (platform) {
            case "CODEFORCES" -> "Codeforces";
            case "LEETCODE" -> "LeetCode";
            case "CODECHEF" -> "CodeChef";
            case "ATCODER" -> "AtCoder";
            default -> platform;
        };
    }

    private static String codeforcesUrl(int cfContestId) {
        return "https://codeforces.com/" + (CodeforcesAuthorizedClient.isGym(cfContestId) ? "gym/" : "contest/") + cfContestId;
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
