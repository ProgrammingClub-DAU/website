package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronizes stored member ratings with the public Codeforces {@code user.info} API.
 *
 * <p>It batches all valid linked handles into one provider request to limit traffic,
 * updates only matching rated accounts, and contains external failures so a Codeforces
 * outage never interrupts the rest of the application or discards prior ratings.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSyncService {

    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/user.info?handles=";

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * Performs the scheduled or administrator-triggered rating refresh.
     *
     * <p>Blank handles are excluded before the external call. Provider errors are logged
     * and intentionally not rethrown because the next scheduled execution can retry.</p>
     */
    @Scheduled(cron = "${cpclub.codeforces.sync-cron:0 0 */6 * * *}")
    @Transactional
    public void syncCodeforcesRatings() {
        log.info("Starting scheduled Codeforces rating synchronization job...");

        List<User> usersWithHandle = userRepository.findByCodeforcesHandleIsNotNull();
        if (usersWithHandle.isEmpty()) {
            log.info("No users with Codeforces handles found. Skipping sync.");
            return;
        }

        List<String> handles = usersWithHandle.stream()
                .map(User::getCodeforcesHandle)
                .filter(Objects::nonNull)
                .filter(h -> !h.isBlank())
                .toList();

        if (handles.isEmpty()) {
            log.info("No valid non-blank handles to sync.");
            return;
        }

        String handlesQueryParam = String.join(";", handles);
        String requestUrl = CODEFORCES_API_URL + handlesQueryParam;

        try {
            log.info("Fetching rating info from Codeforces API for {} handles...", handles.size());
            CodeforcesResponse response = restTemplate.getForObject(requestUrl, CodeforcesResponse.class);

            if (response != null && "OK".equalsIgnoreCase(response.status()) && response.result() != null) {
                Map<String, User> userMap = usersWithHandle.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                u -> u.getCodeforcesHandle().toLowerCase(),
                                u -> u,
                                (existing, replacement) -> existing
                        ));

                // Match provider results case-insensitively because Codeforces handles are not case-sensitive.
                int updatedCount = 0;
                for (CodeforcesUserDto cfUser : response.result()) {
                    User user = userMap.get(cfUser.handle().toLowerCase());
                    if (user != null && cfUser.rating() != null) {
                        user.setRating(cfUser.rating());
                        userRepository.save(user);
                        updatedCount++;
                    }
                }
                log.info("Successfully updated ratings for {} users from Codeforces API.", updatedCount);
            } else {
                log.warn("Codeforces API response returned non-OK status: {}", response != null ? response.comment() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to sync ratings from Codeforces API: {}", e.getMessage());
        }
    }
}
