package io.github.cihadacar.taskhub.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Timestamp;
import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskAssigned;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskCompleted;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskCreated;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskUpdated;

public final class GrpcTaskEventPublisher implements TaskEventPublisher {

    private final NotificationServiceGrpc.NotificationServiceBlockingStub stub;
    private final Duration deadline;

    public GrpcTaskEventPublisher(
            NotificationServiceGrpc.NotificationServiceBlockingStub stub, Duration deadline) {
        this.stub = stub;
        this.deadline = deadline;
    }

    @Override
    public void publish(TaskNotification notification) {
        TaskEvent event = toProto(notification);
        var response = stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .notifyTaskEvent(NotifyTaskEventRequest.newBuilder().setEvent(event).build());
        if (!response.getAccepted() || !notification.eventId().equals(response.getEventId())) {
            throw new IllegalStateException("Notification service did not acknowledge event " + notification.eventId());
        }
    }

    private TaskEvent toProto(TaskNotification notification) {
        TaskEvent.Builder event = TaskEvent.newBuilder()
                .setEventId(notification.eventId())
                .setTaskId(notification.taskId())
                .setProjectId(notification.projectId())
                .setActorId(notification.actorId())
                .setOccurredAt(toTimestamp(notification.occurredAt()));

        switch (notification.type()) {
            case CREATED -> {
                TaskCreated.Builder payload = TaskCreated.newBuilder().setTitle(notification.title());
                setAssignee(payload, notification.assigneeId());
                event.setTaskCreated(payload);
            }
            case UPDATED -> {
                TaskUpdated.Builder payload = TaskUpdated.newBuilder()
                        .setTitle(notification.title()).setStatus(notification.status());
                setAssignee(payload, notification.assigneeId());
                event.setTaskUpdated(payload);
            }
            case ASSIGNED -> event.setTaskAssigned(
                    TaskAssigned.newBuilder().setAssigneeId(notification.assigneeId()));
            case COMPLETED -> {
                TaskCompleted.Builder payload = TaskCompleted.newBuilder();
                if (notification.assigneeId() != null) {
                    payload.setAssigneeId(notification.assigneeId());
                }
                event.setTaskCompleted(payload);
            }
        }
        return event.build();
    }

    private void setAssignee(TaskCreated.Builder payload, Long assigneeId) {
        if (assigneeId != null) {
            payload.setAssigneeId(assigneeId);
        }
    }

    private void setAssignee(TaskUpdated.Builder payload, Long assigneeId) {
        if (assigneeId != null) {
            payload.setAssigneeId(assigneeId);
        }
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }
}
