package io.github.cihadacar.taskhub.project;

import org.springframework.stereotype.Component;

@Component
class ProjectMapper {

    ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.id(), project.name(), project.description(), project.ownerId(),
                project.createdAt(), project.updatedAt());
    }
}
