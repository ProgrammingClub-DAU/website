package com.cpclub.backend.common.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Shared infrastructure configuration for application services.
 */
@Configuration
public class AppConfig {

    /**
     * Provides the HTTP client used for Codeforces API calls.
     *
     * <p>Timeouts are mandatory here, not tuning. A bare {@code new RestTemplate()}
     * waits forever, and the only caller is a {@code @Scheduled} job running on
     * Spring's single-threaded scheduler — so one half-open connection to
     * Codeforces would block that thread permanently and the sync would never run
     * again until the process restarted.
     *
     * @return reusable synchronous REST client bean
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return new RestTemplate(factory);
    }

    /**
     * Global rate limiter for all outbound Codeforces API calls.
     *
     * <p>One permit every 2 seconds (0.5 permits/s). Codeforces's unofficial
     * limit is ~1 request/second per IP; 0.5 gives a safety margin that still
     * lets a full sync of 100 handles complete in under 3.5 minutes.</p>
     *
     * <p>This is a singleton: a single shared instance across the entire JVM.
     * Every path to Codeforces — the scheduled job, the admin trigger, and the
     * batch-fallback bisect — goes through {@code fetch()}, which acquires one
     * permit before each call. There is no way to bypass it accidentally.</p>
     *
     * @return application-wide Codeforces request gate
     */
    @Bean
    public RateLimiter codeforcesRateLimiter() {
        return RateLimiter.create(0.5); // 1 request every 2 seconds
    }
}
