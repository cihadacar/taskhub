package io.github.cihadacar.taskhub.task;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.github.cihadacar.taskhub.persistence.PostgresTestContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestContainer.class)
class TaskQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void filteredTaskPageLoadsAssigneeAndTagsWithBoundedQueries() throws Exception {
        String registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"query-owner@example.com","username":"query-owner","password":"Str0ngPass!"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long ownerId = extractLong(registration, "id");
        String token = login();
        String projectLocation = createProject(token);
        long backendTagId = createTag(token, "query-backend", "#3366FF");
        long urgentTagId = createTag(token, "query-urgent", "#FF0000");

        for (int index = 1; index <= 5; index++) {
            createTask(token, projectLocation, "Task " + index, "TODO", ownerId, backendTagId, urgentTagId);
        }
        createTask(token, projectLocation, "Completed", "DONE", ownerId, backendTagId, urgentTagId);
        createTask(token, projectLocation, "Unassigned", "TODO", null, backendTagId, urgentTagId);
        createTask(token, projectLocation, "Different tag", "TODO", ownerId, urgentTagId);

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get(projectLocation + "/tasks")
                        .header("Authorization", bearer(token))
                        .param("page", "0")
                        .param("size", "3")
                        .param("status", "TODO")
                        .param("assigneeId", Long.toString(ownerId))
                        .param("tagId", Long.toString(backendTagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].tags.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"query-owner@example.com","password":"Str0ngPass!"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return extractString(response, "accessToken");
    }

    private String createProject(String token) throws Exception {
        return mockMvc.perform(post("/api/projects").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Query Project\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
    }

    private long createTag(String token, String name, String color) throws Exception {
        String location = mockMvc.perform(post("/api/tags").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"color\":\"%s\"}".formatted(name, color)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private void createTask(String token, String projectLocation, String title, String status, Long assigneeId,
            long... tagIds) throws Exception {
        String tags = java.util.Arrays.stream(tagIds).mapToObj(Long::toString)
                .collect(java.util.stream.Collectors.joining(","));
        String assignee = assigneeId == null ? "null" : assigneeId.toString();
        mockMvc.perform(post(projectLocation + "/tasks").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","status":"%s","assigneeId":%s,"tagIds":[%s]}
                                """.formatted(title, status, assignee, tags)))
                .andExpect(status().isCreated());
    }

    private long extractLong(String json, String field) {
        return Long.parseLong(json.replaceFirst(".*\\\"" + field + "\\\":(\\d+).*", "$1"));
    }

    private String extractString(String json, String field) {
        return json.replaceFirst(".*\\\"" + field + "\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
