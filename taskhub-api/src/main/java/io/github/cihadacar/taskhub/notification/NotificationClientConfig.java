package io.github.cihadacar.taskhub.notification;

import io.github.cihadacar.taskhub.proto.notification.v1.NotificationServiceGrpc;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationClientProperties.class)
public class NotificationClientConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "taskhub.notification.grpc.enabled", havingValue = "true")
    ManagedChannel notificationChannel(NotificationClientProperties properties) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(properties.address());
        if (properties.plaintext()) {
            builder.usePlaintext();
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "taskhub.notification.grpc.enabled", havingValue = "true")
    TaskEventPublisher grpcTaskEventPublisher(
            ManagedChannel notificationChannel, NotificationClientProperties properties) {
        var channel = ClientInterceptors.intercept(notificationChannel, new TaskhubGrpcClientInterceptor());
        return new GrpcTaskEventPublisher(NotificationServiceGrpc.newBlockingStub(channel), properties.deadline());
    }

    @Bean
    @ConditionalOnProperty(name = "taskhub.notification.grpc.enabled", havingValue = "false")
    TaskEventPublisher noOpTaskEventPublisher() {
        return notification -> {
        };
    }
}
