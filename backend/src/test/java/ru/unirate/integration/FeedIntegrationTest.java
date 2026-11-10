package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.application.DeadlineReminderProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DeadlineReminderProcessor deadlineReminderProcessor;

    private long createCheckpoint(String title, Instant deadline) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Лента",
                                "deadline", deadline.toString(),
                                "cost", 100,
                                "groupIds", List.of(1)))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode personalFeed(String token, String query) throws Exception {
        return getJson("/api/v1/feed/personal" + query, token);
    }

    private long countEvents(JsonNode page, String type, String titlePart) {
        long count = 0;
        for (JsonNode item : page.get("items")) {
            if (item.get("type").asText().equals(type)
                    && item.get("message").asText().contains(titlePart)) {
                count++;
            }
        }
        return count;
    }

    @Test
    void deadlineReminderIsCreatedOncePerStudentAndCheckpoint() throws Exception {
        String title = "Скоро дедлайн " + System.nanoTime();
        createCheckpoint(title, Instant.now().plusSeconds(1800));

        int first = deadlineReminderProcessor.process();
        assertThat(first).isGreaterThanOrEqualTo(2);

        JsonNode feed = personalFeed(studentToken(), "?limit=50");
        assertThat(countEvents(feed, "DEADLINE_SOON", title)).isEqualTo(1);
        assertThat(countEvents(feed, "CHECKPOINT_ASSIGNED", title)).isEqualTo(1);

        int second = deadlineReminderProcessor.process();
        assertThat(second).isZero();
        JsonNode feedAgain = personalFeed(studentToken(), "?limit=50");
        assertThat(countEvents(feedAgain, "DEADLINE_SOON", title)).isEqualTo(1);
    }

    @Test
    void reminderIsNotCreatedOutsideThreshold() throws Exception {
        String title = "Далёкий дедлайн " + System.nanoTime();
        createCheckpoint(title, Instant.now().plusSeconds(48 * 3600));
        deadlineReminderProcessor.process();
        JsonNode feed = personalFeed(studentToken(), "?limit=50");
        assertThat(countEvents(feed, "DEADLINE_SOON", title)).isZero();
        assertThat(countEvents(feed, "CHECKPOINT_ASSIGNED", title)).isEqualTo(1);
    }

    @Test
    void personalFeedIsPrivateAndPaginated() throws Exception {
        String title = "Приватная лента " + System.nanoTime();
        createCheckpoint(title, Instant.now().plusSeconds(1800));
        deadlineReminderProcessor.process();

        JsonNode other = personalFeed(student2Token(), "?limit=50");
        assertThat(countEvents(other, "CHECKPOINT_ASSIGNED", title)).isZero();
        assertThat(countEvents(other, "DEADLINE_SOON", title)).isZero();

        JsonNode firstPage = personalFeed(studentToken(), "?limit=1");
        assertThat(firstPage.get("items").size()).isEqualTo(1);
        assertThat(firstPage.get("nextCursor").isNull()).isFalse();
        long firstId = firstPage.get("items").get(0).get("id").asLong();

        JsonNode secondPage = personalFeed(studentToken(),
                "?limit=1&cursor=" + firstPage.get("nextCursor").asLong());
        assertThat(secondPage.get("items").size()).isEqualTo(1);
        assertThat(secondPage.get("items").get(0).get("id").asLong()).isNotEqualTo(firstId);
    }

    @Test
    void personalFeedRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/feed/personal")).andExpect(status().isUnauthorized());
    }
}
