package com.cpclub.backend.compete;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CompeteContextTest {

    @Test
    void contextLoads() {
        // Validates JPA entities and Flyway migrations
    }
}
