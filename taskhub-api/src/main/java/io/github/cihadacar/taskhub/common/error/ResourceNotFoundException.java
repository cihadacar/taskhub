package io.github.cihadacar.taskhub.common.error;

public final class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super("%s '%s' was not found.".formatted(resourceType, identifier));
    }
}
