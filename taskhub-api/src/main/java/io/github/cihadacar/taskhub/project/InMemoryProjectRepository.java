package io.github.cihadacar.taskhub.project;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
class InMemoryProjectRepository implements ProjectRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, Project> projects = new ConcurrentHashMap<>();

    @Override
    public Project create(String name, String description, Long ownerId) {
        Instant now = Instant.now();
        Project project = new Project(sequence.incrementAndGet(), name, description, ownerId, now, now);
        projects.put(project.id(), project);
        return project;
    }

    @Override
    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<Project> findAll() {
        return projects.values().stream().sorted(Comparator.comparing(Project::id)).toList();
    }

    @Override
    public Project update(Long id, String name, String description) {
        return projects.computeIfPresent(id, (ignored, current) -> new Project(current.id(), name, description,
                current.ownerId(), current.createdAt(), Instant.now()));
    }

    @Override
    public void delete(Long id) {
        projects.remove(id);
    }
}
