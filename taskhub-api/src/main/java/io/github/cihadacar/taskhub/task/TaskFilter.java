package io.github.cihadacar.taskhub.task;

public record TaskFilter(TaskStatus status, Long assigneeId, Long tagId) {
}
