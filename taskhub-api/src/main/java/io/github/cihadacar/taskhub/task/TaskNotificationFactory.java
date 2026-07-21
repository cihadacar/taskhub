package io.github.cihadacar.taskhub.task;

import java.util.Objects;
import java.util.UUID;

import io.github.cihadacar.taskhub.notification.TaskNotification;
import io.github.cihadacar.taskhub.notification.TaskNotificationType;
import io.github.cihadacar.taskhub.security.RequestActor;

final class TaskNotificationFactory {

    private TaskNotificationFactory() {
    }

    static TaskNotification created(Task task, RequestActor actor) {
        return from(task, actor, TaskNotificationType.CREATED, task.createdAt());
    }

    static TaskNotification updated(Task before, Task after, RequestActor actor) {
        TaskNotificationType type;
        if (before.status() != TaskStatus.DONE && after.status() == TaskStatus.DONE) {
            type = TaskNotificationType.COMPLETED;
        } else if (!Objects.equals(before.assigneeId(), after.assigneeId()) && after.assigneeId() != null) {
            type = TaskNotificationType.ASSIGNED;
        } else {
            type = TaskNotificationType.UPDATED;
        }
        return from(after, actor, type, after.updatedAt());
    }

    private static TaskNotification from(
            Task task, RequestActor actor, TaskNotificationType type, java.time.Instant occurredAt) {
        return new TaskNotification(UUID.randomUUID().toString(), type, task.id(), task.projectId(), actor.userId(),
                task.title(), task.status().name(), task.assigneeId(), occurredAt);
    }
}
