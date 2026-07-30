package io.github.cihadacar.taskhub.security;

import java.time.Instant;

import io.github.cihadacar.taskhub.persistence.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestContainer.class)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void registerLoginAndAuthorizationBoundariesWorkTogether() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:unauthorized"))
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","username":"member","password":"Str0ngPass!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"Str0ngPass!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString()
                .replaceFirst(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:forbidden"));
    }

    @Test
    void invalidRegistrationReturnsFieldLevelProblemDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","username":"","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:validation-error"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void tokenFromAnUnexpectedIssuerIsRejected() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("attacker")
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", java.util.List.of("USER"))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
