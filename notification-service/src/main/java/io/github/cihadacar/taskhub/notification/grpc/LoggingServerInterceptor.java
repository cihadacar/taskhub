package io.github.cihadacar.taskhub.notification.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingServerInterceptor implements ServerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        String correlationId = headers.get(GrpcMetadata.CORRELATION_ID);
        LOGGER.info("gRPC call started method={} correlationId={}", method, correlationId);

        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                LOGGER.info("gRPC call completed method={} correlationId={} status={}",
                        method, correlationId, status.getCode());
                super.close(status, trailers);
            }
        };
        return next.startCall(loggingCall, headers);
    }
}
