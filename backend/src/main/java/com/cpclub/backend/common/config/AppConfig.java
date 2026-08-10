package com.cpclub.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Shared infrastructure configuration for application services.
 */
@Configuration
public class AppConfig {

    /**
     * Provides the HTTP client used for Codeforces API calls.
     *
     * @return reusable synchronous REST client bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
