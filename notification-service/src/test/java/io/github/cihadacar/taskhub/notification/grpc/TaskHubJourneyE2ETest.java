package io.github.cihadacar.taskhub.notification.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.github.cihadacar.taskhub.proto.notification.v1.SubscribeTaskEventsRequest;
import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class TaskHubJourneyE2ETest {

    private static final String JWT_SECRET = "taskhub-test-only-signing-key-at-least-32-bytes";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static ObservableNotificationService notificationService;
    private static Server grpcServer;
    private static ManagedChannel subscriberChannel;
    private static Process apiProcess;
    private static URI apiBaseUri;

    @BeforeAll
    static void startServices() throws Exception {
        notificationService = new ObservableNotificationService();
        grpcServer = ServerBuilder.forPort(0)
                .addService(ServerInterceptors.intercept(notificationService,
                        new AuthenticationServerInterceptor(jwtDecoder()), new LoggingServerInterceptor()))
                .build()
                .start();

        int apiPort = availablePort();
        apiBaseUri = URI.create("http://localhost:" + apiPort);
        Path moduleDirectory = Path.of(System.getProperty("basedir")).toAbsolutePath();
        Path apiJar;
        try (var jars = Files.list(moduleDirectory.resolve("../taskhub-api/target"))) {
            apiJar = jars.filter(path -> path.getFileName().toString().matches("taskhub-api-.*\\.jar"))
                    .filter(path -> !path.getFileName().toString().endsWith(".jar.original"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Packaged taskhub-api jar was not found"));
        }
        Path logFile = moduleDirectory.resolve("target/taskhub-e2e-api.log");

        ProcessBuilder processBuilder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar", apiJar.normalize().toString(),
                "--server.port=" + apiPort,
                "--taskhub.cors.allowed-origins=http://localhost:3000");
        processBuilder.environment().put("SPRING_DATASOURCE_URL", POSTGRES.getJdbcUrl());
        processBuilder.environment().put("SPRING_DATASOURCE_USERNAME", POSTGRES.getUsername());
        processBuilder.environment().put("SPRING_DATASOURCE_PASSWORD", POSTGRES.getPassword());
        processBuilder.environment().put("TASKHUB_SECURITY_JWT_SECRET", JWT_SECRET);
        processBuilder.environment().put("TASKHUB_SECURITY_TOKEN_TTL", "PT1H");
        processBuilder.environment().put("TASKHUB_NOTIFICATION_GRPC_ENABLED", "true");
        processBuilder.environment().put(
                "TASKHUB_NOTIFICATION_GRPC_ADDRESS", "localhost:" + grpcServer.getPort());
        processBuilder.environment().put("TASKHUB_NOTIFICATION_GRPC_PLAINTEXT", "true");
        processBuilder.environment().put("TASKHUB_NOTIFICATION_GRPC_DEADLINE", "PT2S");
        apiProcess = processBuilder.redirectErrorStream(true).redirectOutput(logFile.toFile()).start();

        awaitApiHealth(logFile);
    }

    @AfterAll
    static void stopServices() {
        if (subscriberChannel != null) {
            subscriberChannel.shutdownNow();
        }
        if (apiProcess != null) {
            apiProcess.destroy();
            try {
                if (!apiProcess.waitFor(5, TimeUnit.SECONDS)) {
                    apiProcess.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                apiProcess.destroyForcibly();
            }
        }
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
    }

    @Test
    void registerLoginProjectTaskAndStreamedNotificationWorkEndToEnd() throws Exception {
        HttpResponse<String> registration = post("/api/auth/register", null, """
                {"email":"journey@example.com","username":"journey-user","password":"Str0ngPass!"}
                """);
        assertThat(registration.statusCode()).isEqualTo(201);
        long userId = extractLong(registration.body(), "id");

        HttpResponse<String> login = post("/api/auth/login", null, """
                {"email":"journey@example.com","password":"Str0ngPass!"}
                """);
        assertThat(login.statusCode()).isEqualTo(200);
        String token = extractString(login.body(), "accessToken");

        HttpResponse<String> project = post("/api/projects", token, """
                {"name":"Session 5"}
                """);
        assertThat(project.statusCode()).isEqualTo(201);
        long projectId = idFromLocation(project);

        CompletableFuture<TaskEvent> streamedEvent = subscribe(token, projectId);
        assertThat(notificationService.awaitSubscriber(TIMEOUT)).isTrue();

        HttpResponse<String> task = post("/api/projects/" + projectId + "/tasks", token, """
                {"title":"Prove the pyramid","assigneeId":%d}
                """.formatted(userId));
        assertThat(task.statusCode()).isEqualTo(201);
        long taskId = idFromLocation(task);

        TaskEvent event = streamedEvent.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertThat(event.getProjectId()).isEqualTo(projectId);
        assertThat(event.getTaskId()).isEqualTo(taskId);
        assertThat(event.getActorId()).isEqualTo(userId);
        assertThat(event.getTaskCreated().getTitle()).isEqualTo("Prove the pyramid");
        assertThat(event.getTaskCreated().getAssigneeId()).isEqualTo(userId);
    }

    private static CompletableFuture<TaskEvent> subscribe(String token, long projectId) {
        subscriberChannel = ManagedChannelBuilder.forAddress("localhost", grpcServer.getPort())
                .usePlaintext()
                .build();
        Metadata headers = new Metadata();
        headers.put(GrpcMetadata.AUTHORIZATION, "Bearer " + token);
        headers.put(GrpcMetadata.CORRELATION_ID, "session-5-e2e");
        var stub = NotificationServiceGrpc.newStub(subscriberChannel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
        CompletableFuture<TaskEvent> received = new CompletableFuture<>();
        stub.subscribeTaskEvents(
                SubscribeTaskEventsRequest.newBuilder().addProjectIds(projectId).build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(TaskEvent event) {
                        received.complete(event);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        received.completeExceptionally(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        if (!received.isDone()) {
                            received.completeExceptionally(
                                    new IllegalStateException("Notification stream completed without an event"));
                        }
                    }
                });
        return received;
    }

    private static HttpResponse<String> post(String path, String token, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(apiBaseUri.resolve(path))
                .timeout(TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void awaitApiHealth(Path logFile) throws Exception {
        URI healthUri = apiBaseUri.resolve("/actuator/health");
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (!apiProcess.isAlive()) {
                throw new IllegalStateException("TaskHub API exited during startup:\n"
                        + Files.readString(logFile, StandardCharsets.UTF_8));
            }
            try {
                HttpResponse<String> response = HTTP_CLIENT.send(
                        HttpRequest.newBuilder(healthUri).timeout(Duration.ofSeconds(1)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException ignored) {
                // The socket is not accepting requests yet.
            }
            Thread.sleep(25);
        }
        throw new IllegalStateException("TaskHub API did not become healthy:\n"
                + Files.readString(logFile, StandardCharsets.UTF_8));
    }

    private static long idFromLocation(HttpResponse<String> response) {
        String location = response.headers().firstValue(HttpHeaders.LOCATION).orElseThrow();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private static long extractLong(String json, String field) {
        return Long.parseLong(extract(json, "\"%s\"\\s*:\\s*(\\d+)".formatted(field)));
    }

    private static String extractString(String json, String field) {
        return extract(json, "\"%s\"\\s*:\\s*\"([^\"]+)\"".formatted(field));
    }

    private static String extract(String json, String expression) {
        var matcher = Pattern.compile(expression).matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing expected JSON field in: " + json);
        }
        return matcher.group(1);
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("taskhub-api"));
        return decoder;
    }

    private static final class ObservableNotificationService extends NotificationGrpcService {

        private final CountDownLatch subscriberReady = new CountDownLatch(1);

        private ObservableNotificationService() {
            super(new InMemoryTaskEventStore());
        }

        @Override
        public void subscribeTaskEvents(
                SubscribeTaskEventsRequest request, StreamObserver<TaskEvent> responseObserver) {
            super.subscribeTaskEvents(request, responseObserver);
            subscriberReady.countDown();
        }

        private boolean awaitSubscriber(Duration timeout) throws InterruptedException {
            return subscriberReady.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
