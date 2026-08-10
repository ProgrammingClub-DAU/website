package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.CodeforcesResponse;
import com.cpclub.backend.codeforces.CodeforcesUserDto;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSyncService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/user.info?handles=";

    @Scheduled(cron = "0 0 */6 * * *") // Runs every 6 hours
    public void syncCodeforcesRatings() {
        log.info("Starting Codeforces rating sync...");
        List<User> users = userRepository.findByCodeforcesHandleIsNotNull();
        int successCount = 0;
        int failureCount = 0;

        for (User user : users) {
            String handle = user.getCodeforcesHandle();
            try {
                if (syncUserRating(user)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error syncing user {}: {}", user.getCodeforcesHandle(), e.getMessage());
                failureCount++;
            }

            // Sleep to respect Codeforces rate limits (1 req/sec)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Codeforces sync interrupted", e);
                break;
            }
        }

        log.info("Codeforces sync completed. Successful: {}, Failed: {}", successCount, failureCount);
    }

    public boolean syncUserRating(User user) {
        if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            return false;
        }
        String handle = user.getCodeforcesHandle();
        try {
            CodeforcesResponse response = restTemplate.getForObject(CODEFORCES_API_URL + handle, CodeforcesResponse.class);
            
            if (response != null && "OK".equals(response.status()) && response.result() != null && !response.result().isEmpty()) {
                CodeforcesUserDto cfUser = response.result().get(0);
                
                if (cfUser.rating() != null) {
                    user.setRating(cfUser.rating());
                } else {
                    // User has no rating (e.g. unrated), ensure it drops back to null
                    user.setRating(null);
                }
                userRepository.save(user);
                log.debug("Successfully updated rating for handle: {}", handle);
                return true;
            } else {
                log.warn("Codeforces API returned non-OK status or empty result for handle: {}", handle);
                return false;
            }
        } catch (RestClientException e) {
            log.warn("HTTP Error fetching rating for handle {}: {}", handle, e.getMessage());
            return false;
        }
    }
}
