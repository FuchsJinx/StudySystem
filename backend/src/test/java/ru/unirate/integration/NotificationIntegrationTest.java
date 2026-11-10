package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends IntegrationTestBase {

    private long createCheckpoint(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "deadline", Instant.now().plusSeconds(3600).toString(),
                                "cost", 100,
                                "groupIds", List.of(1)))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode notifications(String token) throws Exception {
        return getJson("/api/v1/notifications?limit=50", token);
    }

    @Test
    void inAppNotificationLifecycle() throws Exception {
        String title = "Уведомление " + System.nanoTime();
        long checkpointId = createCheckpoint(title);

        JsonNode page = notifications(studentToken());
        long notificationId = 0;
        boolean found = false;
        for (JsonNode item : page.get("items")) {
            if (item.get("title").asText().equals("Новая задача")) {
                found = true;
                notificationId = item.get("id").asLong();
                break;
            }
        }
        assertThat(found).isTrue();
        long unreadBefore = getJson("/api/v1/notifications/unread-count", studentToken())
                .get("count").asLong();
        assertThat(unreadBefore).isGreaterThanOrEqualTo(1);

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        long unreadAfter = getJson("/api/v1/notifications/unread-count", studentToken())
                .get("count").asLong();
        assertThat(unreadAfter).isEqualTo(unreadBefore - 1);

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk());
        assertThat(getJson("/api/v1/notifications/unread-count", studentToken()).get("count").asLong())
                .isZero();

        mockMvc.perform(get("/api/v1/checkpoints/" + checkpointId)
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk());
    }

    @Test
    void submissionAndAcceptanceCreateNotifications() throws Exception {
        String title = "Проверка уведомлений " + System.nanoTime();
        long checkpointId = createCheckpoint(title);

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://example.com/n"))))
                .andExpect(status().isOk());

        JsonNode teacherNotifications = notifications(teacherToken());
        boolean teacherNotified = false;
        for (JsonNode item : teacherNotifications.get("items")) {
            if (item.get("title").asText().equals("Работа сдана на проверку")) {
                teacherNotified = true;
            }
        }
        assertThat(teacherNotified).isTrue();

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/accept")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(4)))))
                .andExpect(status().isOk());

        JsonNode studentNotifications = notifications(studentToken());
        boolean studentNotified = false;
        for (JsonNode item : studentNotifications.get("items")) {
            if (item.get("title").asText().equals("Работа принята")) {
                studentNotified = true;
            }
        }
        assertThat(studentNotified).isTrue();
    }

    @Test
    void usersCannotReadForeignNotifications() throws Exception {
        createCheckpoint("Чужое уведомление " + System.nanoTime());
        JsonNode page = notifications(studentToken());
        long studentNotificationId = page.get("items").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/v1/notifications/" + studentNotificationId + "/read")
                        .header("Authorization", "Bearer " + student2Token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void notificationListSupportsCursorPagination() throws Exception {
        createCheckpoint("Пагинация 1 " + System.nanoTime());
        createCheckpoint("Пагинация 2 " + System.nanoTime());
        JsonNode firstPage = getJson("/api/v1/notifications?limit=1", studentToken());
        assertThat(firstPage.get("items").size()).isEqualTo(1);
        assertThat(firstPage.get("nextCursor").isNull()).isFalse();
        long cursor = firstPage.get("nextCursor").asLong();
        JsonNode secondPage = getJson("/api/v1/notifications?limit=1&cursor=" + cursor, studentToken());
        assertThat(secondPage.get("items").size()).isEqualTo(1);
        assertThat(secondPage.get("items").get(0).get("id").asLong()).isNotEqualTo(
                firstPage.get("items").get(0).get("id").asLong());
    }
}
