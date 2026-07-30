package io.github.cihadacar.taskhub.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import io.github.cihadacar.taskhub.notification.TaskEventPublisher;
import io.github.cihadacar.taskhub.notification.TaskNotification;
import io.github.cihadacar.taskhub.project.ProjectService;
import io.github.cihadacar.taskhub.security.RequestActor;
import io.github.cihadacar.taskhub.tag.TagService;
import io.github.cihadacar.taskhub.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final RequestActor ACTOR = new RequestActor(7L, false);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskEventPublisher taskEventPublisher;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectService, tagService, userRepository,
                new TaskMapper(), taskEventPublisher);
    }

    @Test
    void createAppliesDefaultsAndPublishesTheCreatedEvent() {
        Task task = new Task("Ship tests", null, TaskStatus.TODO, TaskPriority.MEDIUM, null,
                3L, null, null, null, Set.of(), NOW);
        ReflectionTestUtils.setField(task, "id", 5L);
        when(taskRepository.create("Ship tests", null, TaskStatus.TODO, TaskPriority.MEDIUM,
                null, 3L, null, Set.of())).thenReturn(task);

        TaskResponse response = taskService.create(3L,
                new TaskRequest(" Ship tests ", " ", null, null, null, null, null), ACTOR);

        assertThat(response.title()).isEqualTo("Ship tests");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        verify(projectService).assertAccessible(3L, ACTOR);
        verify(taskEventPublisher).publish(any(TaskNotification.class));
    }

    @Test
    void createRejectsAnUnknownAssigneeBeforeWritingTheTask() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(3L,
                new TaskRequest("Ship tests", null, null, null, null, 99L, Set.of()), ACTOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(taskRepository, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listBoundsTheRequestedPageSize() {
        TaskFilter filter = new TaskFilter(TaskStatus.TODO, null, null);
        when(taskRepository.findByProjectId(3L, filter, 0, 100))
                .thenReturn(new TaskPage(List.of(), 0));

        var response = taskService.list(3L, 0, 500, filter, ACTOR);

        assertThat(response.size()).isEqualTo(100);
        verify(projectService).assertAccessible(3L, ACTOR);
    }

    @Test
    void getRejectsAnUnknownTask() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(404L, ACTOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    void updateKeepsExistingStatusAndPriorityWhenTheyAreOmitted() {
        Task existing = new Task("Before", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                LocalDate.parse("2026-02-01"), 3L, null, null, null, Set.of(), NOW);
        Task updated = new Task("After", "Details", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                null, 3L, null, null, null, Set.of(), NOW.plusSeconds(60));
        ReflectionTestUtils.setField(updated, "id", 5L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.update(5L, "After", "Details", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                null, null, Set.of())).thenReturn(updated);

        TaskResponse response = taskService.update(5L,
                new TaskRequest(" After ", " Details ", null, null, null, null, null), ACTOR);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        verify(projectService).assertAccessible(3L, ACTOR);
        verify(taskEventPublisher).publish(any(TaskNotification.class));
    }

    @Test
    void deleteChecksProjectAccessBeforeDeleting() {
        Task task = new Task("Delete", null, TaskStatus.TODO, TaskPriority.LOW, null,
                3L, null, null, null, Set.of(), NOW);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        taskService.delete(5L, ACTOR);

        verify(projectService).assertAccessible(3L, ACTOR);
        verify(taskRepository).delete(5L);
    }
}
