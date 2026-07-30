package io.github.cihadacar.taskhub.user;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.github.cihadacar.taskhub.common.error.ConflictException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaUserRepository implements UserRepository {

    private final EntityManager entityManager;

    JpaUserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public UserAccount save(String email, String username, String passwordHash) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (exists("email", normalizedEmail)) {
            throw new ConflictException("A user with that email already exists.");
        }
        if (exists("lower(username)", username.toLowerCase(Locale.ROOT))) {
            throw new ConflictException("A user with that username already exists.");
        }
        UserAccount user = new UserAccount(normalizedEmail, username, passwordHash, Instant.now());
        entityManager.persist(user);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return entityManager.createQuery("""
                select distinct u from UserAccount u left join fetch u.roles
                where u.email = :email
                """, UserAccount.class)
                .setParameter("email", email.toLowerCase(Locale.ROOT))
                .getResultStream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(Long id) {
        return entityManager.createQuery("""
                select distinct u from UserAccount u left join fetch u.roles
                where u.id = :id
                """, UserAccount.class)
                .setParameter("id", id)
                .getResultStream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return entityManager.createQuery("""
                select distinct u from UserAccount u left join fetch u.roles order by u.id
                """, UserAccount.class).getResultList();
    }

    private boolean exists(String field, String value) {
        return entityManager.createQuery("select count(u) from UserAccount u where " + field + " = :value", Long.class)
                .setParameter("value", value)
                .getSingleResult() > 0;
    }
}
