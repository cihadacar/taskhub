package io.github.cihadacar.taskhub.user;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.github.cihadacar.taskhub.common.error.ConflictException;
import org.springframework.stereotype.Repository;

@Repository
class InMemoryUserRepository implements UserRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, UserAccount> users = new ConcurrentHashMap<>();

    @Override
    public synchronized UserAccount save(String email, String username, String passwordHash) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (users.values().stream().anyMatch(user -> user.email().equals(normalizedEmail))) {
            throw new ConflictException("A user with that email already exists.");
        }
        if (users.values().stream().anyMatch(user -> user.username().equalsIgnoreCase(username))) {
            throw new ConflictException("A user with that username already exists.");
        }
        UserAccount user = new UserAccount(sequence.incrementAndGet(), normalizedEmail, username, passwordHash,
                Set.of(Role.USER), Instant.now());
        users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        return users.values().stream().filter(user -> user.email().equals(normalizedEmail)).findFirst();
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<UserAccount> findAll() {
        return users.values().stream().sorted(Comparator.comparing(UserAccount::id)).toList();
    }
}
