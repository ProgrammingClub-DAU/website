package com.cpclub.backend.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs every Flyway migration against a real PostgreSQL and validates the entities
 * against the schema it produces.
 *
 * <p>The rest of the suite uses the H2 {@code test} profile, which has Flyway off and
 * builds its schema from the entities with {@code create-drop}. That suite cannot
 * notice a migration that disagrees with an entity -- it never runs the migration.
 * Before this test, the only check was booting against PostgreSQL by hand.</p>
 *
 * <p>No profile is activated, so this uses the production settings from
 * {@code application.yml}: Flyway enabled and {@code ddl-auto: validate}. Reaching
 * the test method at all therefore proves two things: every migration applied on an
 * empty database, and Hibernate found every mapped column in the result.</p>
 *
 * <p>{@code disabledWithoutDocker} skips the class on a machine with no Docker
 * daemon rather than failing it. CI runs on {@code ubuntu-latest}, which has Docker,
 * so the check always runs where it matters.</p>
 */
@SpringBootTest(properties = {
        // application.yml reads JWT_SECRET with no default, and JwtUtils refuses to
        // start without a strong secret. Same test-only value as application-test.yml.
        "app.jwt.secret=test-only-signing-key-not-used-anywhere-outside-this-suite-0123456789",
        // Likewise GOOGLE_CLIENT_ID, which application.yml reads with no default so
        // that a deployment cannot come up with sign-in quietly broken. This test
        // runs on the production settings, so it has to supply one. Never used:
        // no token is verified here.
        "app.google.client-id=test-only-client-id.apps.googleusercontent.com",
        "spring.jpa.show-sql=false"
})
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    /**
     * Matches the major version the club deploys on (PostgreSQL 15+). Static, so one
     * container serves the whole class.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Every migration applies to an empty PostgreSQL and the entities validate against it")
    void everyMigrationAppliesAndMatchesTheEntities() {
        MigrationInfo[] applied = flyway.info().applied();

        assertTrue(applied.length > 0, "no migrations ran -- is Flyway pointed at db/migration?");
        assertEquals(0, flyway.info().pending().length,
                "a migration was found but not applied");
        for (MigrationInfo migration : applied) {
            assertFalse(migration.getState().isFailed(),
                    "migration " + migration.getVersion() + " (" + migration.getDescription() + ") failed");
        }
    }
}
