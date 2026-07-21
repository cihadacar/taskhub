package io.github.cihadacar.taskhub.task;

import java.util.List;
import java.util.Set;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import io.github.cihadacar.taskhub.notification.TaskEventPublisher;
import io.github.cihadacar.taskhub.project.ProjectService;
import io.github.cihadacar.taskhub.security.RequestActor;
import io.github.cihadacar.taskhub.tag.TagResponse;
import io.github.cihadacar.taskhub.tag.TagService;
import io.github.cihadacar.taskhub.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final TagService tagService;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final TaskEventPublisher taskEventPublisher;

    public TaskService(TaskRepository taskRepository, ProjectService projectService, TagService tagService,
            UserRepository userRepository, TaskMapper taskMapper, TaskEventPublisher taskEventPublisher) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.tagService = tagService;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.taskEventPublisher = taskEventPublisher;
    }

    public TaskResponse create(Long projectId, TaskRequest request, RequestActor actor) {
        projectService.assertAccessible(projectId, actor);
        Set<Long> tagIds = normalizeTags(request.tagIds());
        List<TagResponse> tags = tagService.getAll(tagIds);
        validateAssignee(request.assigneeId());
        Task task = taskRepository.create(request.title().strip(), normalize(request.description()),
                request.status() == null ? TaskStatus.TODO : request.status(),
                request.priority() == null ? TaskPriority.MEDIUM : request.priority(), request.dueDate(), projectId,
                request.assigneeId(), tagIds);
        taskEventPublisher.publish(TaskNotificationFactory.created(task, actor));
        return taskMapper.toResponse(task, tags);
    }

    public PageResponse<TaskResponse> list(Long projectId, int page, int size, RequestActor actor) {
        projectService.assertAccessible(projectId, actor);
        List<TaskResponse> tasks = taskRepository.findByProjectId(projectId).stream().map(this::toResponse).toList();
        return PageResponse.from(tasks, page, size);
    }

    public TaskResponse get(Long id, RequestActor actor) {
        return toResponse(requireAccessible(id, actor));
    }

    public TaskResponse update(Long id, TaskRequest request, RequestActor actor) {
        Task existing = requireAccessible(id, actor);
        Set<Long> tagIds = normalizeTags(request.tagIds());
        List<TagResponse> tags = tagService.getAll(tagIds);
        validateAssignee(request.assigneeId());
        Task task = taskRepository.update(id, request.title().strip(), normalize(request.description()),
                request.status() == null ? existing.status() : request.status(),
                request.priority() == null ? existing.priority() : request.priority(), request.dueDate(),
                request.assigneeId(), tagIds);
        taskEventPublisher.publish(TaskNotificationFactory.updated(existing, task, actor));
        return taskMapper.toResponse(task, tags);
    }

    public void delete(Long id, RequestActor actor) {
        requireAccessible(id, actor);
        taskRepository.delete(id);
    }

    private Task requireAccessible(Long id, RequestActor actor) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
        projectService.assertAccessible(task.projectId(), actor);
        return task;
    }

    private TaskResponse toResponse(Task task) {
        return taskMapper.toResponse(task, tagService.getAll(task.tagIds()));
    }

    private void validateAssignee(Long assigneeId) {
        if (assigneeId != null && userRepository.findById(assigneeId).isEmpty()) {
            throw new ResourceNotFoundException("User", assigneeId);
        }
    }

    private Set<Long> normalizeTags(Set<Long> tagIds) {
        return tagIds == null ? Set.of() : Set.copyOf(tagIds);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
