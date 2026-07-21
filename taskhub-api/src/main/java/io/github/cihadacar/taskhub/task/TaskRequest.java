package io.github.cihadacar.taskhub.task;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        @Positive Long assigneeId,
        @Size(max = 20) Set<@Valid @Positive Long> tagIds) {
}
