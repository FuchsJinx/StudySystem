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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiIntegrationTest extends IntegrationTestBase {

    @Test
    void draftCheckpointFallsBackToDemoText() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/draft-checkpoint")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Курсовая работа, этап 1",
                                "theses", "Введение, обзор литературы, постановка задачи"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("demo"))
                .andReturn();
        JsonNode body = json(result);
        assertThat(body.get("text").asText()).contains("Курсовая работа");
        assertThat(body.get("text").asText()).contains("обзор литературы");

        mockMvc.perform(post("/api/v1/ai/draft-checkpoint")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Запрещено", "theses", "Студент не должен"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/ai/draft-checkpoint")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Без тезисов", "theses", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ratingTipsAreGeneratedForStudentOnly() throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/ai/rating-tips")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("demo"))
                .andReturn());
        assertThat(response.get("tips").size()).isGreaterThanOrEqualTo(3);
        for (JsonNode tip : response.get("tips")) {
            assertThat(tip.asText()).isNotBlank();
        }

        mockMvc.perform(post("/api/v1/ai/rating-tips")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void pollSummaryUsesAggregatesWithoutPersonalData() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/polls")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Формат зачёта " + System.nanoTime(),
                                "endsAt", Instant.now().plusSeconds(3600).toString(),
                                "targetGroupIds", List.of(1),
                                "isAnonymous", true,
                                "questions", List.of(Map.of(
                                        "text", "Формат",
                                        "type", "SINGLE",
                                        "options", List.of("Устно", "Письменно")))))))
                .andExpect(status().isCreated())
                .andReturn();
        long pollId = json(created).get("id").asLong();

        JsonNode poll = getJson("/api/v1/polls/" + pollId, studentToken());
        long questionId = poll.get("questions").get(0).get("id").asLong();
        long optionId = poll.get("questions").get(0).get("options").get(0).get("id").asLong();
        mockMvc.perform(post("/api/v1/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answers", List.of(Map.of("questionId", questionId,
                                        "optionIds", List.of(optionId)))))))
                .andExpect(status().isOk());

        JsonNode summary = json(mockMvc.perform(post("/api/v1/ai/poll-summary")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pollId", pollId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("demo"))
                .andReturn());
        assertThat(summary.get("text").asText()).contains("1");
        assertThat(summary.get("text").asText()).doesNotContain("Иван Иванов");

        String otherTeacher = tokenFor("teacher2@demo.local", "Demo123!");
        mockMvc.perform(post("/api/v1/ai/poll-summary")
                        .header("Authorization", "Bearer " + otherTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pollId", pollId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/ai/rating-tips")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/feed/personal")).andExpect(status().isUnauthorized());
    }
}
