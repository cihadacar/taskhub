package io.github.cihadacar.taskhub.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.NotifyTaskEventResponse;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class GrpcTaskEventPublisherIntegrationTest {

    private final AtomicReference<Metadata> receivedHeaders = new AtomicReference<>();
    private final AtomicReference<TaskEvent> receivedEvent = new AtomicReference<>();
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        var service = new NotificationServiceGrpc.NotificationServiceImplBase() {
            @Override
            public void notifyTaskEvent(
                    NotifyTaskEventRequest request, StreamObserver<NotifyTaskEventResponse> observer) {
                receivedEvent.set(request.getEvent());
                observer.onNext(NotifyTaskEventResponse.newBuilder()
                        .setEventId(request.getEvent().getEventId()).setAccepted(true).build());
                observer.onCompleted();
            }
        };
        server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(ServerInterceptors.intercept(service, new ServerInterceptor() {
                    @Override
                    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                            ServerCall<ReqT, RespT> call, Metadata headers,
                            ServerCallHandler<ReqT, RespT> next) {
                        receivedHeaders.set(headers);
                        return next.startCall(call, headers);
                    }
                }))
                .build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void publishesMappedEventWithJwtAndCorrelationMetadata() {
        Jwt jwt = new Jwt("signed-jwt", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"), Map.of("sub", "42", "roles", List.of("USER")));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MDC.put("correlationId", "correlation-42");
        var stub = NotificationServiceGrpc.newBlockingStub(channel)
                .withInterceptors(new TaskhubGrpcClientInterceptor());
        var publisher = new GrpcTaskEventPublisher(stub, Duration.ofSeconds(1));

        publisher.publish(new TaskNotification("event-42", TaskNotificationType.CREATED,
                11, 7, 42, "Build Session 3", "TODO", 5L, Instant.parse("2026-07-21T22:00:00Z")));

        assertThat(receivedHeaders.get().get(TaskhubGrpcClientInterceptor.AUTHORIZATION))
                .isEqualTo("Bearer signed-jwt");
        assertThat(receivedHeaders.get().get(TaskhubGrpcClientInterceptor.CORRELATION_ID))
                .isEqualTo("correlation-42");
        assertThat(receivedEvent.get().getTaskCreated().getTitle()).isEqualTo("Build Session 3");
        assertThat(receivedEvent.get().getTaskCreated().getAssigneeId()).isEqualTo(5);
    }
}
