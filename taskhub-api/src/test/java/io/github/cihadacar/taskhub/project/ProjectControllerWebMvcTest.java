package io.github.cihadacar.taskhub.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import io.github.cihadacar.taskhub.security.RequestActor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@Import(ProjectControllerWebMvcTest.MethodSecurityConfiguration.class)
class ProjectControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roadmap\"}"))
                .andExpect(status().isUnauthorized());

        verify(projectService, never()).create(any(), any());
    }

    @Test
    void userCanCreateAValidatedProject() throws Exception {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(projectService.create(
                new ProjectRequest("Roadmap", "Delivery plan"), new RequestActor(7L, false)))
                .thenReturn(new ProjectResponse(3L, "Roadmap", "Delivery plan", 7L, now, now));

        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(jwt()
                                .jwt(token -> token.subject("7").claim("roles", List.of("USER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Roadmap","description":"Delivery plan"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/3"))
                .andExpect(jsonPath("$.name").value("Roadmap"))
                .andExpect(jsonPath("$.ownerId").value(7));
    }

    @Test
    void invalidProjectIsRejectedBeforeTheServiceBoundary() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(jwt()
                                .jwt(token -> token.subject("7").claim("roles", List.of("USER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:validation-error"))
                .andExpect(jsonPath("$.errors.name").exists());

        verify(projectService, never()).create(any(), any());
    }

    @Test
    void authenticatedCallerWithoutAnApplicationRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(jwt().jwt(token -> token.subject("7").claim("roles", List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roadmap\"}"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
