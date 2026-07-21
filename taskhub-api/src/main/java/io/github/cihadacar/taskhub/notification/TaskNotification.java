package io.github.cihadacar.taskhub.notification;

import java.time.Instant;
import java.util.Objects;

public record TaskNotification(
        String eventId,
        TaskNotificationType type,
        long taskId,
        long projectId,
        long actorId,
        String title,
        String status,
        Long assigneeId,
        Instant occurredAt) {

    public TaskNotification {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (eventId.isBlank() || taskId <= 0 || projectId <= 0 || actorId <= 0) {
            throw new IllegalArgumentException("Task notification identity fields must be present and positive");
        }
        if (type == TaskNotificationType.ASSIGNED && (assigneeId == null || assigneeId <= 0)) {
            throw new IllegalArgumentException("Assigned task notifications require a positive assigneeId");
        }
    }
}
