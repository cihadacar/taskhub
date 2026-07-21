package io.github.cihadacar.taskhub.notification.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Timestamp;
import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.SubscribeTaskEventsRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskCreated;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class NotificationGrpcIntegrationTest {

    private Server server;
    private ManagedChannel channel;
    private InMemoryTaskEventStore eventStore;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        eventStore = new InMemoryTaskEventStore();
        JwtDecoder decoder = token -> {
            if (!"valid-token".equals(token)) {
                throw new IllegalArgumentException("invalid token");
            }
            return new Jwt(token, Instant.now(), Instant.now().plusSeconds(60),
                    Map.of("alg", "none"), Map.of("sub", "42", "iss", "taskhub-api"));
        };
        NotificationGrpcService service = new NotificationGrpcService(eventStore);
        server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(ServerInterceptors.intercept(service,
                        new AuthenticationServerInterceptor(decoder), new LoggingServerInterceptor()))
                .build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void unaryCallIsAcknowledgedAndStored() {
        TaskEvent event = taskCreatedEvent("event-1", 7);

        var response = authenticatedBlockingStub().notifyTaskEvent(
                NotifyTaskEventRequest.newBuilder().setEvent(event).build());

        assertThat(response.getAccepted()).isTrue();
        assertThat(response.getEventId()).isEqualTo("event-1");
        assertThat(eventStore.findAll()).containsExactly(event);
    }

    @Test
    void subscriberReceivesPublishedEventsInRealTime() throws Exception {
        LinkedBlockingQueue<TaskEvent> received = new LinkedBlockingQueue<>();
        authenticatedAsyncStub().subscribeTaskEvents(
                SubscribeTaskEventsRequest.newBuilder().addProjectIds(7).build(), queueingObserver(received));

        TaskEvent event = taskCreatedEvent("event-2", 7);
        authenticatedBlockingStub().notifyTaskEvent(
                NotifyTaskEventRequest.newBuilder().setEvent(event).build());

        assertThat(received.poll(1, TimeUnit.SECONDS)).isEqualTo(event);
    }

    @Test
    void unauthenticatedCallsAreRejected() {
        var stub = NotificationServiceGrpc.newBlockingStub(channel);

        assertThatThrownBy(() -> stub.notifyTaskEvent(NotifyTaskEventRequest.newBuilder()
                .setEvent(taskCreatedEvent("event-3", 7)).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    @Test
    void requestsWithoutAnEventAreRejected() {
        assertThatThrownBy(() -> authenticatedBlockingStub()
                .notifyTaskEvent(NotifyTaskEventRequest.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    private NotificationServiceGrpc.NotificationServiceBlockingStub authenticatedBlockingStub() {
        return NotificationServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()));
    }

    private NotificationServiceGrpc.NotificationServiceStub authenticatedAsyncStub() {
        return NotificationServiceGrpc.newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()));
    }

    private Metadata authHeaders() {
        Metadata headers = new Metadata();
        headers.put(GrpcMetadata.AUTHORIZATION, "Bearer valid-token");
        headers.put(GrpcMetadata.CORRELATION_ID, "correlation-1");
        return headers;
    }

    private StreamObserver<TaskEvent> queueingObserver(LinkedBlockingQueue<TaskEvent> received) {
        return new StreamObserver<>() {
            @Override
            public void onNext(TaskEvent event) {
                received.add(event);
            }

            @Override
            public void onError(Throwable throwable) {
                // The test owns channel shutdown.
            }

            @Override
            public void onCompleted() {
                // The subscription remains open until channel shutdown.
            }
        };
    }

    private TaskEvent taskCreatedEvent(String eventId, long projectId) {
        return TaskEvent.newBuilder()
                .setEventId(eventId)
                .setTaskId(11)
                .setProjectId(projectId)
                .setActorId(42)
                .setOccurredAt(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()))
                .setTaskCreated(TaskCreated.newBuilder().setTitle("Ship Session 3"))
                .build();
    }
}
