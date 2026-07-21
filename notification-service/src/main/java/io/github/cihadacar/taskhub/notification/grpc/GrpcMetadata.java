package io.github.cihadacar.taskhub.notification.grpc;

import io.grpc.Context;
import io.grpc.Metadata;
import org.springframework.security.oauth2.jwt.Jwt;

public final class GrpcMetadata {

    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> CORRELATION_ID =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    static final Context.Key<Jwt> JWT = Context.key("taskhub-jwt");

    private GrpcMetadata() {
    }
}
