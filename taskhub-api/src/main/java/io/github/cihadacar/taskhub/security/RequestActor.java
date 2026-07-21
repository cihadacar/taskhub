package io.github.cihadacar.taskhub.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

public record RequestActor(Long userId, boolean admin) {

    public static RequestActor from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new RequestActor(Long.valueOf(jwt.getSubject()), roles != null && roles.contains("ADMIN"));
    }
}
