package io.github.cihadacar.taskhub.user;

import java.time.Instant;
import java.util.Set;

public record UserResponse(Long id, String email, String username, Set<Role> roles, Instant createdAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.id(), user.email(), user.username(), user.roles(), user.createdAt());
    }
}
