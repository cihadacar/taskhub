package io.github.cihadacar.taskhub.notification.config;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.github.cihadacar.taskhub.notification.grpc.AuthenticationServerInterceptor;
import io.github.cihadacar.taskhub.notification.grpc.LoggingServerInterceptor;
import io.github.cihadacar.taskhub.notification.grpc.NotificationGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NotificationGrpcProperties.class, NotificationSecurityProperties.class})
public class GrpcServerConfig {

    @Bean
    JwtDecoder notificationJwtDecoder(NotificationSecurityProperties properties) {
        SecretKey key = new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("taskhub-api"));
        return decoder;
    }

    @Bean
    AuthenticationServerInterceptor authenticationServerInterceptor(JwtDecoder notificationJwtDecoder) {
        return new AuthenticationServerInterceptor(notificationJwtDecoder);
    }

    @Bean
    LoggingServerInterceptor loggingServerInterceptor() {
        return new LoggingServerInterceptor();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    Server notificationGrpcServer(
            NotificationGrpcProperties properties,
            NotificationGrpcService service,
            AuthenticationServerInterceptor authenticationInterceptor,
            LoggingServerInterceptor loggingInterceptor) {
        ServerBuilder<?> builder = ServerBuilder.forPort(properties.port())
                .addService(ServerInterceptors.intercept(service, loggingInterceptor, authenticationInterceptor));
        if (properties.tlsEnabled()) {
            builder.useTransportSecurity(new File(properties.certificateChain()), new File(properties.privateKey()));
        }
        return builder.build();
    }
}
