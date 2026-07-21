package io.github.cihadacar.taskhub.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
class InMemoryTaskRepository implements TaskRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Task create(String title, String description, TaskStatus status, TaskPriority priority,
            LocalDate dueDate, Long projectId, Long assigneeId, Set<Long> tagIds) {
        Instant now = Instant.now();
        Task task = new Task(sequence.incrementAndGet(), title, description, status, priority, dueDate, projectId,
                assigneeId, tagIds, now, now);
        tasks.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        return tasks.values().stream().filter(task -> task.projectId().equals(projectId))
                .sorted(Comparator.comparing(Task::id)).toList();
    }

    @Override
    public Task update(Long id, String title, String description, TaskStatus status, TaskPriority priority,
            LocalDate dueDate, Long assigneeId, Set<Long> tagIds) {
        return tasks.computeIfPresent(id, (ignored, current) -> new Task(current.id(), title, description, status,
                priority, dueDate, current.projectId(), assigneeId, tagIds, current.createdAt(), Instant.now()));
    }

    @Override
    public void delete(Long id) {
        tasks.remove(id);
    }
}
