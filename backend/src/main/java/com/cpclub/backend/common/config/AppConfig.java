package com.cpclub.backend.common.config;

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
}
