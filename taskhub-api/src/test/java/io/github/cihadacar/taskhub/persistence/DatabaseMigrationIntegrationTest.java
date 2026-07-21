package io.github.cihadacar.taskhub.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DatabaseMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Test
    void cleanDatabaseMigratesValidatesAndRepeatedStartupIsIdempotent() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .validateMigrationNaming(true)
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isOne();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            assertThat(queryCount(statement, """
                    select count(*) from information_schema.tables
                    where table_schema = 'public'
                      and table_name in ('users', 'user_roles', 'projects', 'tasks', 'tags', 'task_tags')
                    """)).isEqualTo(6);
            assertThat(queryCount(statement, """
                    select count(*) from pg_indexes
                    where schemaname = 'public'
                      and indexname in ('idx_tasks_project_id', 'idx_tasks_status',
                                        'idx_tasks_assignee_id', 'idx_task_tags_tag_id')
                    """)).isEqualTo(4);
            assertThat(queryCount(statement, """
                    select count(*) from information_schema.table_constraints
                    where constraint_schema = 'public' and constraint_type = 'FOREIGN KEY'
                    """)).isEqualTo(6);
        }
    }

    private long queryCount(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
