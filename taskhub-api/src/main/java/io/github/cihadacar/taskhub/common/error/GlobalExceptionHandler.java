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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI ABOUT_BLANK = URI.create("about:blank");
    private static final URI GENERIC_HTTP_ERROR = URI.create("urn:taskhub:problem:http-error");
    private static final URI RESOURCE_NOT_FOUND = URI.create("urn:taskhub:problem:resource-not-found");
    private static final URI VALIDATION_ERROR = URI.create("urn:taskhub:problem:validation-error");
    private static final URI CONFLICT = URI.create("urn:taskhub:problem:conflict");

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(RESOURCE_NOT_FOUND);
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(CONFLICT);
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    ProblemDetail handleBadCredentials() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid email or password.");
        problem.setType(URI.create("urn:taskhub:problem:unauthorized"));
        problem.setTitle("Unauthorized");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        java.util.Map<String, String> errors = new java.util.TreeMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");
        problem.setType(VALIDATION_ERROR);
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        return createResponseEntity(problem, headers, status, request);
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
