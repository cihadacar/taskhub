package io.github.cihadacar.taskhub.project;

import java.util.List;
import java.util.Optional;

interface ProjectRepository {

    Project create(String name, String description, Long ownerId);

    Optional<Project> findById(Long id);

    List<Project> findAll();

    Project update(Long id, String name, String description);

    void delete(Long id);
}
