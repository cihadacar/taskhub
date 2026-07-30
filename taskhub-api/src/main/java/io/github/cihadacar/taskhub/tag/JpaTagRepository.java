package io.github.cihadacar.taskhub.tag;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.github.cihadacar.taskhub.common.error.ConflictException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaTagRepository implements TagRepository {

    private final EntityManager entityManager;

    JpaTagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Tag create(String name, String color) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        long matches = entityManager.createQuery("select count(t) from Tag t where t.name = :name", Long.class)
                .setParameter("name", normalizedName).getSingleResult();
        if (matches > 0) {
            throw new ConflictException("A tag with that name already exists.");
        }
        Tag tag = new Tag(normalizedName, color.toUpperCase(Locale.ROOT));
        entityManager.persist(tag);
        return tag;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Tag.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return entityManager.createQuery("select t from Tag t order by t.id", Tag.class).getResultList();
    }
}
