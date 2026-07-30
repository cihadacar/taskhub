package io.github.cihadacar.taskhub.task;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

interface TaskRepository {

    Task create(String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate,
            Long projectId, Long assigneeId, Set<Long> tagIds);

    Optional<Task> findById(Long id);

    TaskPage findByProjectId(Long projectId, TaskFilter filter, int page, int size);

    Task update(Long id, String title, String description, TaskStatus status, TaskPriority priority,
            LocalDate dueDate, Long assigneeId, Set<Long> tagIds);

    void delete(Long id);
}
