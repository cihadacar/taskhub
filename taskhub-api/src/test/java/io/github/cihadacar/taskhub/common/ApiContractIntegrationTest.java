package io.github.cihadacar.taskhub.common;

import io.github.cihadacar.taskhub.persistence.PostgresTestContainer;
import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestContainer.class)
@Import(ApiContractIntegrationTest.TestEndpointConfiguration.class)
class ApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorDiscoveryExposesHealthOnly() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.health.href").exists())
                .andExpect(jsonPath("$._links.env").doesNotExist());
    }

    @Test
    @WithMockUser
    void unmappedRouteReturnsCompleteProblemDetails() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:http-error"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/does-not-exist"));
    }

    @Test
    @WithMockUser
    void applicationExceptionReturnsResourceNotFoundProblem() throws Exception {
        mockMvc.perform(get("/test/missing-task"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:taskhub:problem:resource-not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Task '42' was not found."))
                .andExpect(jsonPath("$.instance").value("/test/missing-task"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestEndpointConfiguration {

        @Bean
        MissingTaskController missingTaskController() {
            return new MissingTaskController();
        }
    }

    @RestController
    static class MissingTaskController {

        @GetMapping("/test/missing-task")
        void getMissingTask() {
            throw new ResourceNotFoundException("Task", 42L);
        }
    }
}
