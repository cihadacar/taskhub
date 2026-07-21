package io.github.cihadacar.taskhub.notification.grpc;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventResponse;
import io.github.cihadacar.taskhub.proto.notification.v1.SubscribeTaskEventsRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private final InMemoryTaskEventStore eventStore;
    private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public NotificationGrpcService(InMemoryTaskEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void notifyTaskEvent(
            NotifyTaskEventRequest request, StreamObserver<NotifyTaskEventResponse> responseObserver) {
        if (!request.hasEvent()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("event is required").asRuntimeException());
            return;
        }

        TaskEvent event = request.getEvent();
        String validationError = validate(event);
        if (validationError != null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(validationError).asRuntimeException());
            return;
        }
        eventStore.save(event);
        subscriptions.stream().filter(subscription -> subscription.accepts(event)).forEach(subscription -> {
            try {
                subscription.observer().onNext(event);
            } catch (RuntimeException exception) {
                subscriptions.remove(subscription);
            }
        });

        responseObserver.onNext(NotifyTaskEventResponse.newBuilder()
                .setEventId(event.getEventId())
                .setAccepted(true)
                .build());
        responseObserver.onCompleted();
    }

    private String validate(TaskEvent event) {
        if (event.getEventId().isBlank() || event.getEventId().length() > 100) {
            return "event.event_id must contain between 1 and 100 characters";
        }
        if (event.getTaskId() <= 0 || event.getProjectId() <= 0 || event.getActorId() <= 0) {
            return "event task_id, project_id, and actor_id must be positive";
        }
        if (!event.hasOccurredAt()) {
            return "event.occurred_at is required";
        }
        return switch (event.getPayloadCase()) {
            case TASK_CREATED -> event.getTaskCreated().getTitle().isBlank()
                    || event.getTaskCreated().getTitle().length() > 200
                    ? "task_created.title must contain between 1 and 200 characters" : null;
            case TASK_UPDATED -> event.getTaskUpdated().getTitle().isBlank()
                    || event.getTaskUpdated().getTitle().length() > 200
                    || event.getTaskUpdated().getStatus().isBlank()
                    ? "task_updated title and status are required" : null;
            case TASK_ASSIGNED -> event.getTaskAssigned().getAssigneeId() <= 0
                    ? "task_assigned.assignee_id must be positive" : null;
            case TASK_COMPLETED -> event.getTaskCompleted().hasAssigneeId()
                    && event.getTaskCompleted().getAssigneeId() <= 0
                    ? "task_completed.assignee_id must be positive when present" : null;
            case PAYLOAD_NOT_SET -> "event payload is required";
        };
    }

    @Override
    public void subscribeTaskEvents(
            SubscribeTaskEventsRequest request, StreamObserver<TaskEvent> responseObserver) {
        Set<Long> projectIds = Set.copyOf(request.getProjectIdsList());
        Subscription subscription = new Subscription(projectIds, responseObserver);
        subscriptions.add(subscription);
        if (responseObserver instanceof ServerCallStreamObserver<TaskEvent> serverObserver) {
            serverObserver.setOnCancelHandler(() -> subscriptions.remove(subscription));
        }
    }

    private record Subscription(Set<Long> projectIds, StreamObserver<TaskEvent> observer) {

        private boolean accepts(TaskEvent event) {
            return projectIds.isEmpty() || projectIds.contains(event.getProjectId());
        }
    }
}
