package io.github.cihadacar.taskhub.notification.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("taskhub.notification.security")
public record NotificationSecurityProperties(String jwtSecret) {

    public NotificationSecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("taskhub.notification.security.jwt-secret must contain at least 32 bytes");
        }
    }
}
