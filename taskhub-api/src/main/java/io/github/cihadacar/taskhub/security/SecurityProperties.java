package io.github.cihadacar.taskhub.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("taskhub.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("taskhub.security.jwt-secret must contain at least 32 bytes");
        }
        if (tokenTtl == null || tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalArgumentException("taskhub.security.token-ttl must be positive");
        }
    }
}
