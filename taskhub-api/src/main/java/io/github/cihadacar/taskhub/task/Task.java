package io.github.cihadacar.taskhub.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

record Task(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long projectId,
        Long assigneeId,
        Set<Long> tagIds,
        Instant createdAt,
        Instant updatedAt) {

    Task {
        tagIds = Set.copyOf(tagIds);
    }
}
