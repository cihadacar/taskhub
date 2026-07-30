package io.github.cihadacar.taskhub.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.cihadacar.taskhub.project.Project;
import io.github.cihadacar.taskhub.tag.Tag;
import io.github.cihadacar.taskhub.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, insertable = false, updatable = false)
    private Project project;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", insertable = false, updatable = false)
    private UserAccount assignee;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    Task(String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate,
            Long projectId, Project project, Long assigneeId, UserAccount assignee, Set<Tag> tags, Instant now) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.projectId = projectId;
        this.project = project;
        this.assigneeId = assigneeId;
        this.assignee = assignee;
        this.tags.addAll(tags);
        this.createdAt = now;
        this.updatedAt = now;
    }

    void update(String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate,
            Long assigneeId, UserAccount assignee, Set<Tag> tags, Instant now) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.assigneeId = assigneeId;
        this.assignee = assignee;
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = now;
    }

    public Long id() { return id; }
    public String title() { return title; }
    public String description() { return description; }
    public TaskStatus status() { return status; }
    public TaskPriority priority() { return priority; }
    public LocalDate dueDate() { return dueDate; }
    public Long projectId() { return projectId; }
    public Long assigneeId() { return assigneeId; }
    public Set<Long> tagIds() { return tags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet()); }
    public Set<Tag> tags() { return Set.copyOf(tags); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
