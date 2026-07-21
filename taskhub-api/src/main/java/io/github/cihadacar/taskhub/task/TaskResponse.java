package io.github.cihadacar.taskhub.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.github.cihadacar.taskhub.tag.TagResponse;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long projectId,
        Long assigneeId,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt) {

    public TaskResponse {
        tags = List.copyOf(tags);
    }
}
