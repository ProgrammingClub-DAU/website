package com.cpclub.backend.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that Stage 0 migrations run cleanly against a real PostgreSQL 15 instance,
 * and that the JPA entities exactly match the migrated schema (ddl-auto: validate).
 */
public class Stage0MigrationTest extends PostgresIntegrationTest {

    @Test
    void contextLoadsAndMigrationsSucceed() {
        // If the context loads successfully, it means Flyway successfully applied
        // V1 through V21, and Hibernate's SchemaValidator found no discrepancies
        // between the SQL tables and the JPA @Entity classes.
        assertThat(postgres.isRunning()).isTrue();
    }
}
