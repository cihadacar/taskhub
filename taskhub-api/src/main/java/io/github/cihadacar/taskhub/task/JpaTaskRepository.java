package io.github.cihadacar.taskhub.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.cihadacar.taskhub.project.Project;
import io.github.cihadacar.taskhub.tag.Tag;
import io.github.cihadacar.taskhub.user.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaTaskRepository implements TaskRepository {

    private final EntityManager entityManager;

    JpaTaskRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Task create(String title, String description, TaskStatus status, TaskPriority priority,
            LocalDate dueDate, Long projectId, Long assigneeId, Set<Long> tagIds) {
        Task task = new Task(title, description, status, priority, dueDate,
                projectId, entityManager.getReference(Project.class, projectId), assigneeId,
                userReference(assigneeId), tags(tagIds), Instant.now());
        entityManager.persist(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> findById(Long id) {
        return entityManager.createQuery("""
                select distinct t from Task t
                left join fetch t.assignee
                left join fetch t.tags
                where t.id = :id
                """, Task.class).setParameter("id", id).getResultStream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskPage findByProjectId(Long projectId, TaskFilter filter, int page, int size) {
        String tagJoin = filter.tagId() == null ? "" : " join t.tags filterTag";
        String conditions = conditions(filter);

        Query countQuery = entityManager.createQuery(
                "select count(distinct t.id) from Task t" + tagJoin + conditions);
        bind(countQuery, projectId, filter);
        long totalElements = (long) countQuery.getSingleResult();

        var idQuery = entityManager.createQuery(
                "select distinct t.id from Task t" + tagJoin + conditions + " order by t.id", Long.class);
        bind(idQuery, projectId, filter);
        int offset = (int) Math.min((long) page * size, Integer.MAX_VALUE);
        List<Long> ids = idQuery.setFirstResult(offset).setMaxResults(size).getResultList();
        if (ids.isEmpty()) {
            return new TaskPage(List.of(), totalElements);
        }

        List<Task> tasks = entityManager.createQuery("""
                select distinct t from Task t
                left join fetch t.assignee
                left join fetch t.tags
                where t.id in :ids
                order by t.id
                """, Task.class).setParameter("ids", ids).getResultList();
        return new TaskPage(tasks, totalElements);
    }

    @Override
    @Transactional
    public Task update(Long id, String title, String description, TaskStatus status, TaskPriority priority,
            LocalDate dueDate, Long assigneeId, Set<Long> tagIds) {
        Task task = entityManager.find(Task.class, id);
        if (task != null) {
            task.update(title, description, status, priority, dueDate, assigneeId, userReference(assigneeId),
                    tags(tagIds), Instant.now());
        }
        return task;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Task task = entityManager.find(Task.class, id);
        if (task != null) {
            entityManager.remove(task);
        }
    }

    private UserAccount userReference(Long id) {
        return id == null ? null : entityManager.getReference(UserAccount.class, id);
    }

    private Set<Tag> tags(Set<Long> ids) {
        return ids.stream().map(id -> entityManager.getReference(Tag.class, id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String conditions(TaskFilter filter) {
        StringBuilder jpql = new StringBuilder(" where t.projectId = :projectId");
        if (filter.status() != null) {
            jpql.append(" and t.status = :status");
        }
        if (filter.assigneeId() != null) {
            jpql.append(" and t.assigneeId = :assigneeId");
        }
        if (filter.tagId() != null) {
            jpql.append(" and filterTag.id = :tagId");
        }
        return jpql.toString();
    }

    private void bind(Query query, Long projectId, TaskFilter filter) {
        query.setParameter("projectId", projectId);
        if (filter.status() != null) {
            query.setParameter("status", filter.status());
        }
        if (filter.assigneeId() != null) {
            query.setParameter("assigneeId", filter.assigneeId());
        }
        if (filter.tagId() != null) {
            query.setParameter("tagId", filter.tagId());
        }
    }
}
