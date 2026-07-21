package io.github.cihadacar.taskhub.notification;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("taskhub.notification.grpc")
public record NotificationClientProperties(
        boolean enabled, String address, boolean plaintext, Duration deadline) {

    public NotificationClientProperties {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("taskhub.notification.grpc.address is required");
        }
        if (deadline == null || deadline.isZero() || deadline.isNegative()) {
            throw new IllegalArgumentException("taskhub.notification.grpc.deadline must be positive");
        }
    }
}
