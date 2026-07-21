package io.github.cihadacar.taskhub.task;

import java.net.URI;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.github.cihadacar.taskhub.security.RequestActor;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Create a task in a project")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TaskResponse response = taskService.create(projectId, request, RequestActor.from(jwt));
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "List a project's tasks")
    public PageResponse<TaskResponse> list(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return taskService.list(projectId, page, size, RequestActor.from(jwt));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get a task")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return taskService.get(id, RequestActor.from(jwt));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Replace a task")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return taskService.update(id, request, RequestActor.from(jwt));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        taskService.delete(id, RequestActor.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
