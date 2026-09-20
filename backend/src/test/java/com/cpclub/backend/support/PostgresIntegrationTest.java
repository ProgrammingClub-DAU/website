package com.cpclub.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a real PostgreSQL database rather
 * than H2. Boots testcontainers with postgres:15-alpine.
 * 
 * <p>Uses the default test profile but overrides the DB connection to the container
 * using @ServiceConnection. Flyway runs automatically on startup because it's not
 * disabled in the default application.yml (only disabled in application-test.yml).
 * By keeping the profile default, or overriding to a specific db profile if needed,
 * we get full schema validation.</p>
 */
@SpringBootTest(properties = {
    "spring.datasource.url=", // Clear H2 URL
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cpclub_test")
            .withUsername("testuser")
            .withPassword("testpass");
}
