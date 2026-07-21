package io.github.cihadacar.taskhub.security;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
