package io.github.cihadacar.taskhub.project;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.cihadacar.taskhub.user.UserAccount;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaProjectRepository implements ProjectRepository {

    private final EntityManager entityManager;

    JpaProjectRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Project create(String name, String description, Long ownerId) {
        Project project = new Project(name, description, ownerId,
                entityManager.getReference(UserAccount.class, ownerId), Instant.now());
        entityManager.persist(project);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Project.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return entityManager.createQuery("select p from Project p order by p.id", Project.class).getResultList();
    }

    @Override
    @Transactional
    public Project update(Long id, String name, String description) {
        Project project = entityManager.find(Project.class, id);
        if (project != null) {
            project.update(name, description, Instant.now());
        }
        return project;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = entityManager.find(Project.class, id);
        if (project != null) {
            entityManager.remove(project);
        }
    }
}
