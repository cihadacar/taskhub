package io.github.cihadacar.taskhub.project;

import java.net.URI;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.github.cihadacar.taskhub.security.RequestActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a project")
    @ApiResponse(responseCode = "201", description = "Project created")
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request, @AuthenticationPrincipal Jwt jwt) {
        ProjectResponse response = projectService.create(request, RequestActor.from(jwt));
        return ResponseEntity.created(URI.create("/api/projects/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List accessible projects")
    public PageResponse<ProjectResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return projectService.list(page, size, RequestActor.from(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project")
    public ProjectResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return projectService.get(id, RequestActor.from(jwt));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a project")
    public ProjectResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return projectService.update(id, request, RequestActor.from(jwt));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    @ApiResponse(responseCode = "204", description = "Project deleted")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        projectService.delete(id, RequestActor.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
