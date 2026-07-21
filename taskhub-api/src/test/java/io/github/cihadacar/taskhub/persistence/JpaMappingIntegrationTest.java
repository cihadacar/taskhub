package io.github.cihadacar.taskhub.persistence;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestContainer.class)
class JpaMappingIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Flyway flyway;

    @Test
    void migratedSchemaValidatesAllDomainEntityMappings() {
        Set<String> entities = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(entity -> entity.getJavaType().getSimpleName())
                .collect(Collectors.toSet());

        assertThat(entities).containsExactlyInAnyOrder("UserAccount", "Project", "Task", "Tag");
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }
}
