package io.github.cihadacar.taskhub.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import io.github.cihadacar.taskhub.persistence.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaTaskRepository.class)
@ImportTestcontainers(PostgresTestContainer.class)
class TaskRepositoryDataJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JpaTaskRepository taskRepository;

    private long ownerId;
    private long projectId;
    private long tagId;

    @BeforeEach
    void seedRelations() {
        ownerId = insertReturningId("""
                insert into users (email, username, password_hash)
                values ('slice@example.com', 'slice-user', 'encoded')
                returning id
                """);
        projectId = insertReturningId("""
                insert into projects (name, owner_id)
                values ('Slice project', %d)
                returning id
                """.formatted(ownerId));
        tagId = insertReturningId("""
                insert into tags (name, color)
                values ('backend', '#3366FF')
                returning id
                """);
    }

    @Test
    void persistsAndLoadsTaskRelationsAgainstPostgres() {
        Task created = taskRepository.create(
                "Repository slice", "Real PostgreSQL", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                LocalDate.parse("2026-02-01"), projectId, ownerId, Set.of(tagId));
        entityManager.flush();
        entityManager.clear();

        Task loaded = taskRepository.findById(created.id()).orElseThrow();

        assertThat(loaded.title()).isEqualTo("Repository slice");
        assertThat(loaded.assigneeId()).isEqualTo(ownerId);
        assertThat(loaded.tagIds()).containsExactly(tagId);
    }

    @Test
    void filtersAProjectPageByStatusAssigneeAndTag() {
        taskRepository.create("Matching", null, TaskStatus.TODO, TaskPriority.MEDIUM,
                null, projectId, ownerId, Set.of(tagId));
        taskRepository.create("Wrong status", null, TaskStatus.DONE, TaskPriority.MEDIUM,
                null, projectId, ownerId, Set.of(tagId));
        entityManager.flush();
        entityManager.clear();

        TaskPage page = taskRepository.findByProjectId(projectId,
                new TaskFilter(TaskStatus.TODO, ownerId, tagId), 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(Task::title).containsExactly("Matching");
    }

    private long insertReturningId(String sql) {
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
