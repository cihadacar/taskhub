package io.github.cihadacar.taskhub.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import io.github.cihadacar.taskhub.security.RequestActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private ProjectRepository projectRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, new ProjectMapper());
    }

    @Test
    void createNormalizesTextAndAssignsTheCurrentOwner() {
        Project project = new Project("Roadmap", null, 7L, null, NOW);
        when(projectRepository.create("Roadmap", null, 7L)).thenReturn(project);

        ProjectResponse response = projectService.create(
                new ProjectRequest(" Roadmap ", "   "), new RequestActor(7L, false));

        assertThat(response.name()).isEqualTo("Roadmap");
        assertThat(response.description()).isNull();
        assertThat(response.ownerId()).isEqualTo(7L);
    }

    @Test
    void listReturnsOnlyOwnedProjectsForARegularUser() {
        when(projectRepository.findAll()).thenReturn(List.of(
                new Project("Owned", null, 7L, null, NOW),
                new Project("Other", null, 8L, null, NOW)));

        var response = projectService.list(0, 20, new RequestActor(7L, false));

        assertThat(response.content()).extracting(ProjectResponse::name).containsExactly("Owned");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void adminCanListProjectsOwnedByDifferentUsers() {
        when(projectRepository.findAll()).thenReturn(List.of(
                new Project("First", null, 7L, null, NOW),
                new Project("Second", null, 8L, null, NOW)));

        var response = projectService.list(0, 20, new RequestActor(99L, true));

        assertThat(response.content()).extracting(ProjectResponse::name)
                .containsExactly("First", "Second");
    }

    @Test
    void getRejectsAProjectOwnedByAnotherUser() {
        when(projectRepository.findById(3L))
                .thenReturn(Optional.of(new Project("Private", null, 8L, null, NOW)));

        assertThatThrownBy(() -> projectService.get(3L, new RequestActor(7L, false)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteRejectsAnUnknownProject() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(404L, new RequestActor(7L, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void ownerCanUpdateAndDeleteAProject() {
        Project existing = new Project("Old", null, 7L, null, NOW);
        Project updated = new Project("New", "Details", 7L, null, NOW);
        when(projectRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(projectRepository.update(3L, "New", "Details")).thenReturn(updated);
        RequestActor actor = new RequestActor(7L, false);

        ProjectResponse response = projectService.update(
                3L, new ProjectRequest(" New ", " Details "), actor);
        projectService.delete(3L, actor);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Details");
        verify(projectRepository).delete(3L);
    }
}
