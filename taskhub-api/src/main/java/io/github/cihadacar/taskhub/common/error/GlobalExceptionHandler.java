package io.github.cihadacar.taskhub.common.error;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI ABOUT_BLANK = URI.create("about:blank");
    private static final URI GENERIC_HTTP_ERROR = URI.create("urn:taskhub:problem:http-error");
    private static final URI RESOURCE_NOT_FOUND = URI.create("urn:taskhub:problem:resource-not-found");

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(RESOURCE_NOT_FOUND);
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getType() == null || ABOUT_BLANK.equals(problem.getType()))) {
            problem.setType(GENERIC_HTTP_ERROR);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
