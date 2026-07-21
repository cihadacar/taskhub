package io.github.cihadacar.taskhub.notification;

import java.time.Instant;

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
}
