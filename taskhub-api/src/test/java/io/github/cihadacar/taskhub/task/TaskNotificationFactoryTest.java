package io.github.cihadacar.taskhub.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import io.github.cihadacar.taskhub.notification.TaskNotificationType;
import io.github.cihadacar.taskhub.security.RequestActor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskNotificationFactoryTest {

    private final RequestActor actor = new RequestActor(42L, false);

    @Test
    void createProducesCreatedEvent() {
        Task created = task(TaskStatus.TODO, null);

        assertThat(TaskNotificationFactory.created(created, actor).type())
                .isEqualTo(TaskNotificationType.CREATED);
    }

    @Test
    void ordinaryUpdateProducesUpdatedEvent() {
        Task before = task(TaskStatus.TODO, null);
        Task after = task(TaskStatus.IN_PROGRESS, null);

        assertThat(TaskNotificationFactory.updated(before, after, actor).type())
                .isEqualTo(TaskNotificationType.UPDATED);
    }

    @Test
    void newAssigneeProducesAssignedEvent() {
        Task before = task(TaskStatus.IN_PROGRESS, null);
        Task after = task(TaskStatus.IN_PROGRESS, 5L);

        assertThat(TaskNotificationFactory.updated(before, after, actor).type())
                .isEqualTo(TaskNotificationType.ASSIGNED);
    }

    @Test
    void completionTakesPrecedenceWhenStatusAndAssigneeChangeTogether() {
        Task before = task(TaskStatus.IN_PROGRESS, null);
        Task after = task(TaskStatus.DONE, 5L);

        assertThat(TaskNotificationFactory.updated(before, after, actor).type())
                .isEqualTo(TaskNotificationType.COMPLETED);
    }

    private Task task(TaskStatus status, Long assigneeId) {
        Instant now = Instant.parse("2026-07-21T22:00:00Z");
        Task task = mock(Task.class);
        when(task.id()).thenReturn(11L);
        when(task.title()).thenReturn("Build Session 3");
        when(task.status()).thenReturn(status);
        when(task.projectId()).thenReturn(7L);
        when(task.assigneeId()).thenReturn(assigneeId);
        when(task.createdAt()).thenReturn(now);
        when(task.updatedAt()).thenReturn(now);
        return task;
    }
}
