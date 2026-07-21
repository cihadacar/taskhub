package io.github.cihadacar.taskhub.user;

import java.time.Instant;
import java.util.Set;

public record UserAccount(
        Long id,
        String email,
        String username,
        String passwordHash,
        Set<Role> roles,
        Instant createdAt) {

    public UserAccount {
        roles = Set.copyOf(roles);
    }
}
