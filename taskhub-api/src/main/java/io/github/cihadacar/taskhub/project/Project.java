package io.github.cihadacar.taskhub.project;

import java.time.Instant;

record Project(Long id, String name, String description, Long ownerId, Instant createdAt, Instant updatedAt) {
}
