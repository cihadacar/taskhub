package io.github.cihadacar.taskhub.task;

import java.util.Comparator;
import java.util.List;

import io.github.cihadacar.taskhub.tag.Tag;
import io.github.cihadacar.taskhub.tag.TagResponse;
import org.springframework.stereotype.Component;

@Component
class TaskMapper {

    TaskResponse toResponse(Task task, List<TagResponse> tags) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.status(), task.priority(),
                task.dueDate(), task.projectId(), task.assigneeId(), tags, task.createdAt(), task.updatedAt());
    }

    TaskResponse toResponse(Task task) {
        List<TagResponse> tags = task.tags().stream()
                .sorted(Comparator.comparing(Tag::id))
                .map(tag -> new TagResponse(tag.id(), tag.name(), tag.color()))
                .toList();
        return toResponse(task, tags);
    }
}
