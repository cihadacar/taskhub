package io.github.cihadacar.taskhub.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("taskhub.notification.grpc")
public record NotificationGrpcProperties(int port, String certificateChain, String privateKey) {

    public NotificationGrpcProperties {
        if (port == 0) {
            port = 9090;
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("taskhub.notification.grpc.port must be between 1 and 65535");
        }
        if ((certificateChain == null) != (privateKey == null)) {
            throw new IllegalArgumentException(
                    "taskhub.notification.grpc certificate-chain and private-key must be configured together");
        }
    }

    public boolean tlsEnabled() {
        return certificateChain != null;
    }
}
