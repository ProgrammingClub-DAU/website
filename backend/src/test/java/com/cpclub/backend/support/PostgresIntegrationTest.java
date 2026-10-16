package com.cpclub.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that require a real PostgreSQL database rather
 * than H2. Boots testcontainers with postgres:15-alpine.
 *
 * <p>Uses the default test profile but overrides the DB connection to the container
 * using @ServiceConnection. Flyway runs automatically on startup because it's not
 * disabled in the default application.yml (only disabled in application-test.yml).
 * By keeping the profile default, or overriding to a specific db profile if needed,
 * we get full schema validation.</p>
 *
 * <p><b>One container for the whole run, started here and never stopped.</b> This
 * used to be a JUnit-managed {@code @Container}, which is started before each test
 * class and stopped after it. Spring, meanwhile, caches the application context
 * across test classes, and every subclass without extra configuration shares one
 * cached context. The second such class therefore got a context whose connection
 * pool pointed at the first class's container -- already stopped -- and failed with
 * "Connection is not available ... (total=0)". Whether it failed depended on which
 * classes happened to run together, so a suite could pass on one branch and fail on
 * the next.</p>
 *
 * <p>This is Testcontainers' singleton-container pattern: start once in a static
 * initialiser, let Ryuk remove the container when the JVM exits.</p>
 */
@SpringBootTest(properties = {
    "spring.datasource.url=", // Clear H2 URL
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cpclub_test")
            .withUsername("testuser")
            .withPassword("testpass");

    static {
        postgres.start();
    }
}
