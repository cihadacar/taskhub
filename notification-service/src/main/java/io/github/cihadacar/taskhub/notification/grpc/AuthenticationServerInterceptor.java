package io.github.cihadacar.taskhub.notification.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public final class AuthenticationServerInterceptor implements ServerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public AuthenticationServerInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String authorization = headers.get(GrpcMetadata.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Bearer authentication is required"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            Context context = Context.current().withValue(GrpcMetadata.JWT, jwt);
            return Contexts.interceptCall(context, call, headers, next);
        } catch (RuntimeException exception) {
            call.close(Status.UNAUTHENTICATED.withDescription("Bearer token is invalid"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
    }
}
