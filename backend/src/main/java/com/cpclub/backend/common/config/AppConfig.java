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
     * Spring's single-threaded scheduler â€” so one half-open connection to
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
     * Rate limiter for all outbound Codeforces API calls (D23).
     *
     * <p>One permit every 2 seconds (0.5 permits/s). Codeforces's unofficial
     * limit is ~1 request/second per IP; 0.5 gives a safety margin that still
     * lets a full sync of 100 handles complete in under 3.5 minutes.</p>
     *
     * <p>Qualified so it can be injected alongside {@link #leetcodeRateLimiter}
     * without Spring needing to guess which RateLimiter to inject by type.</p>
     *
     * @return application-wide Codeforces request gate
     */
    @Bean("codeforcesRateLimiter")
    public RateLimiter codeforcesRateLimiter() {
        return RateLimiter.create(0.5); // 1 request every 2 seconds
    }

    /**
     * Rate limiter for all outbound LeetCode GraphQL calls (D23).
     *
     * <p>Previously LeetCode shared the Codeforces limiter, so a long CF backfill
     * blocked all LC calls. One bean per host means the two syncs are independent.</p>
     *
     * @return application-wide LeetCode request gate
     */
    @Bean("leetcodeRateLimiter")
    public RateLimiter leetcodeRateLimiter() {
        return RateLimiter.create(0.5); // polite rate for an unofficial endpoint
    }

    @Bean
    public java.time.Clock clock(@org.springframework.beans.factory.annotation.Value("${cpclub.scheduling.zone:Asia/Kolkata}") String zone) {
        return java.time.Clock.system(java.time.ZoneId.of(zone));
    }
}
