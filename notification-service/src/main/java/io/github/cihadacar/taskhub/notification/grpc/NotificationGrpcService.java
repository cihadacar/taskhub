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
