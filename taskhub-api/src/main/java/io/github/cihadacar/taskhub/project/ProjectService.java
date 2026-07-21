package io.github.cihadacar.taskhub.project;

import java.util.List;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import io.github.cihadacar.taskhub.security.RequestActor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    public ProjectResponse create(ProjectRequest request, RequestActor actor) {
        Project project = projectRepository.create(request.name().strip(), normalize(request.description()),
                actor.userId());
        return projectMapper.toResponse(project);
    }

    public PageResponse<ProjectResponse> list(int page, int size, RequestActor actor) {
        List<ProjectResponse> projects = projectRepository.findAll().stream()
                .filter(project -> actor.admin() || project.ownerId().equals(actor.userId()))
                .map(projectMapper::toResponse)
                .toList();
        return PageResponse.from(projects, page, size);
    }

    public ProjectResponse get(Long id, RequestActor actor) {
        return projectMapper.toResponse(requireAccessible(id, actor));
    }

    public ProjectResponse update(Long id, ProjectRequest request, RequestActor actor) {
        requireAccessible(id, actor);
        Project project = projectRepository.update(id, request.name().strip(), normalize(request.description()));
        return projectMapper.toResponse(project);
    }

    public void delete(Long id, RequestActor actor) {
        requireAccessible(id, actor);
        projectRepository.delete(id);
    }

    Project requireAccessible(Long id, RequestActor actor) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        if (!actor.admin() && !project.ownerId().equals(actor.userId())) {
            throw new AccessDeniedException("You do not have access to this project.");
        }
        return project;
    }

    public void assertAccessible(Long id, RequestActor actor) {
        requireAccessible(id, actor);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
