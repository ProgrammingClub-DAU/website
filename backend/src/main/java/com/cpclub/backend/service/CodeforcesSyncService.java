package com.cpclub.backend.service;

import com.cpclub.backend.dto.CodeforcesResponse;
import com.cpclub.backend.dto.CodeforcesUserDto;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.repository.UserRepository;
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

    @Scheduled(cron = "0 0 * * * *") // Runs at minute 0 of every hour
    public void syncCodeforcesRatings() {
        log.info("Starting Codeforces rating sync...");
        List<User> users = userRepository.findByCodeforcesHandleIsNotNull();
        int successCount = 0;
        int failureCount = 0;

        for (User user : users) {
            String handle = user.getCodeforcesHandle();
            try {
                CodeforcesResponse response = restTemplate.getForObject(CODEFORCES_API_URL + handle, CodeforcesResponse.class);
                
                if (response != null && "OK".equals(response.getStatus()) && response.getResult() != null && !response.getResult().isEmpty()) {
                    CodeforcesUserDto cfUser = response.getResult().get(0);
                    
                    if (cfUser.getRating() != null) {
                        user.setRating(cfUser.getRating());
                    } else {
                        // User has no rating (e.g. unrated), ensure it drops back to 0
                        user.setRating(0);
                    }
                    userRepository.save(user);
                    successCount++;
                    log.debug("Successfully updated rating for handle: {}", handle);
                } else {
                    log.warn("Codeforces API returned non-OK status or empty result for handle: {}", handle);
                    failureCount++;
                }
            } catch (RestClientException e) {
                log.warn("HTTP Error fetching rating for handle {}: {}", handle, e.getMessage());
                failureCount++;
            } catch (Exception e) {
                log.error("Unexpected error fetching rating for handle {}: {}", handle, e.getMessage());
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
}
