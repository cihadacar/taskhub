package io.github.cihadacar.taskhub.task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.cihadacar.taskhub.notification.TaskEventPublisher;
import io.github.cihadacar.taskhub.notification.TaskNotification;
import io.github.cihadacar.taskhub.notification.TaskNotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TaskAndTagApiIntegrationTest.RecordingPublisherConfig.class)
class TaskAndTagApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingTaskEventPublisher taskEventPublisher;

    @BeforeEach
    void clearPublishedEvents() {
        taskEventPublisher.clear();
    }

    @Test
    void ownerCanManageTaggedTasksWhileAnotherUserCannot() throws Exception {
        String ownerToken = registerAndLogin("task-owner@example.com", "task-owner");
        String otherToken = registerAndLogin("task-other@example.com", "task-other");
        String projectLocation = createProject(ownerToken);

        String tagLocation = mockMvc.perform(post("/api/tags")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"backend\",\"color\":\"#3366FF\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("backend"))
                .andReturn().getResponse().getHeader("Location");
        long tagId = Long.parseLong(tagLocation.substring(tagLocation.lastIndexOf('/') + 1));

        String taskLocation = mockMvc.perform(post(projectLocation + "/tasks")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Secure API","description":"JWT and RBAC","priority":"HIGH","tagIds":[%d]}
                                """.formatted(tagId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.tags[0].name").value("backend"))
                .andReturn().getResponse().getHeader("Location");
        org.assertj.core.api.Assertions.assertThat(taskEventPublisher.events())
                .extracting(TaskNotification::type)
                .containsExactly(TaskNotificationType.CREATED);

        mockMvc.perform(get(projectLocation + "/tasks?size=500")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(get(taskLocation).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(taskLocation)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Secure API","status":"IN_PROGRESS","priority":"MEDIUM","tagIds":[%d]}
                                """.formatted(tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        org.assertj.core.api.Assertions.assertThat(taskEventPublisher.events())
                .extracting(TaskNotification::type)
                .containsExactly(TaskNotificationType.CREATED, TaskNotificationType.UPDATED);

        mockMvc.perform(delete(taskLocation).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(taskLocation).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateTagReturnsConflict() throws Exception {
        String token = registerAndLogin("tag-owner@example.com", "tag-owner");
        String body = "{\"name\":\"urgent\",\"color\":\"#FF0000\"}";
        mockMvc.perform(post("/api/tags").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/tags").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    private String createProject(String token) throws Exception {
        return mockMvc.perform(post("/api/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Delivery\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    private String registerAndLogin(String email, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","username":"%s","password":"Str0ngPass!"}
                                """.formatted(email, username)))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!"}
                                """.formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingPublisherConfig {

        @Bean
        @Primary
        RecordingTaskEventPublisher recordingTaskEventPublisher() {
            return new RecordingTaskEventPublisher();
        }
    }

    static final class RecordingTaskEventPublisher implements TaskEventPublisher {

        private final CopyOnWriteArrayList<TaskNotification> events = new CopyOnWriteArrayList<>();

        @Override
        public void publish(TaskNotification notification) {
            events.add(notification);
        }

        List<TaskNotification> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
