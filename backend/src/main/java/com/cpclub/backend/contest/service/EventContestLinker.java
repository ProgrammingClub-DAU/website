package com.cpclub.backend.contest.service;

import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.dto.ClubContestCreateRequest;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connects an event's Codeforces contest link to a club contest.
 *
 * <p>An IPC is entered as an event with its contest link, and its standings live on
 * the Contests page as a club contest. Admins used to add the same contest twice,
 * once in each place. Saving an event with a link now adds the club contest too,
 * so its ranklist -- with the per-year prize podiums -- is there straight away and
 * syncs on its own during the round.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventContestLinker {

    /** codeforces.com/contest/1843, /gym/724781, /group/AbCdE/contest/567890, with anything after. */
    private static final Pattern LINK = Pattern.compile(
            "codeforces\\.com/(?:(group)/[^/\\s]+/contest|(gym)|contest)/(\\d+)", Pattern.CASE_INSENSITIVE);

    private final ClubContestService clubContestService;
    private final ClubContestRepository contestRepository;
    private final UserRepository userRepository;

    public record ContestLink(int cfContestId, ClubContestKind kind) {
    }

    /** The Codeforces contest a link points at, or empty when it is not a contest link. */
    public static Optional<ContestLink> parse(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        Matcher m = LINK.matcher(url);
        if (!m.find()) return Optional.empty();
        int id;
        try {
            id = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        ClubContestKind kind;
        if (m.group(1) != null) {
            kind = ClubContestKind.GROUP;
        } else if (m.group(2) != null || CodeforcesAuthorizedClient.isGym(id)) {
            kind = ClubContestKind.GYM;
        } else {
            kind = ClubContestKind.OFFICIAL;
        }
        return Optional.of(new ContestLink(id, kind));
    }

    /** The club contest behind an event's link, if there is one. */
    public Optional<Long> findClubContestId(String url) {
        return parse(url)
                .flatMap(link -> contestRepository.findByCfContestId(link.cfContestId()))
                .map(ClubContest::getId);
    }

    /**
     * Adds the contest behind {@code url} as a club contest unless it already is one.
     *
     * <p>Never throws: the event is already saved, and a Codeforces problem -- no API
     * key, no access to a private gym, no start time yet -- must not look like the
     * event failed. The outcome comes back as a sentence for the admin.</p>
     *
     * @return what happened, or null when there was nothing to do
     */
    public String linkIfNeeded(String url, String adminEmail) {
        Optional<ContestLink> link = parse(url);
        if (link.isEmpty() || contestRepository.existsByCfContestId(link.get().cfContestId())) {
            return null;
        }
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) return null;

        try {
            clubContestService.create(ClubContestCreateRequest.builder()
                    .cfContestId(link.get().cfContestId())
                    .kind(link.get().kind())
                    .build(), admin.getId());
            log.info("Added Codeforces contest {} as a club contest from an event link", link.get().cfContestId());
            return "Its Codeforces contest was added to the Contests page and will sync standings automatically.";
        } catch (RuntimeException e) {
            log.warn("Could not add Codeforces contest {} from an event link: {}", link.get().cfContestId(), e.getMessage());
            return "The Codeforces contest could not be added to the Contests page automatically ("
                    + e.getMessage() + "). Add it from Admin > Contests once that is fixed.";
        }
    }
}
