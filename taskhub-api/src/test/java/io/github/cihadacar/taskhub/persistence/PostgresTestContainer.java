package io.github.cihadacar.taskhub.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

public interface PostgresTestContainer {

    @Container
    @ServiceConnection
    PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
}
