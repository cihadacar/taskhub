package io.github.cihadacar.taskhub.project;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt) {
}
