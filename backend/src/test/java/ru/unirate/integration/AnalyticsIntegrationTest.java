package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.application.RatingDeductionService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RatingDeductionService deductionService;

    private long createCheckpoint(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "deadline", Instant.now().minusSeconds(600).toString(),
                                "cost", 100,
                                "groupIds", List.of(1)))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long createActivity(String title, String type, int maxPoints) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "type", type,
                                "eventDate", LocalDate.now().plusDays(2).toString(),
                                "maxPoints", maxPoints,
                                "groupIds", List.of(1)))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private void award(long activityId, long studentUserId, int level) throws Exception {
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/award")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "awards", List.of(Map.of("studentId", studentUserId, "level", level))))))
                .andExpect(status().isOk());
    }

    @Test
    void arDynamicsReconstructHistoryFromLedger() throws Exception {
        createCheckpoint("Аналитика АР " + System.nanoTime());
        deductionService.tick();

        JsonNode dynamics = getJson("/api/v1/analytics/groups/1/ar-dynamics?days=7", teacherToken());
        assertThat(dynamics.get("groupName").asText()).isEqualTo("ИВТ-21");
        assertThat(dynamics.get("points").size()).isEqualTo(7);
        JsonNode last = dynamics.get("points").get(6);
        assertThat(last.get("average").asDouble()).isEqualTo(980.0);
        assertThat(last.get("median").asInt()).isEqualTo(980);
        assertThat(dynamics.get("students").size()).isEqualTo(2);
    }

    @Test
    void activityRankingSortsByBuaAndBoa() throws Exception {
        long academic = createActivity("Аналитика БУА " + System.nanoTime(), "ACADEMIC", 100);
        long social = createActivity("Аналитика БОА " + System.nanoTime(), "SOCIAL", 100);
        award(academic, 4L, 75);
        mockMvc.perform(post("/api/v1/activities/" + social + "/enroll")
                        .header("Authorization", "Bearer " + leadToken()))
                .andExpect(status().isOk());
        award(social, 6L, 50);

        JsonNode ranking = getJson("/api/v1/analytics/groups/1/activity-ranking", teacherToken());
        assertThat(ranking.get("students").size()).isEqualTo(2);
        JsonNode first = ranking.get("students").get(0);
        assertThat(first.get("fullName").asText()).isEqualTo("Иван Иванов");
        assertThat(first.get("bua").asLong()).isEqualTo(75);
        JsonNode second = ranking.get("students").get(1);
        assertThat(second.get("boa").asLong()).isEqualTo(50);
        assertThat(second.get("bua").asLong()).isZero();
    }

    @Test
    void budgetStatsReflectAwardsAndLimits() throws Exception {
        long activity = createActivity("Аналитика бюджета " + System.nanoTime(), "ACADEMIC", 100);
        award(activity, 4L, 50);

        JsonNode stats = getJson("/api/v1/analytics/teachers/me/budget-stats?months=3", teacherToken());
        assertThat(stats.get("months").size()).isEqualTo(3);
        JsonNode current = stats.get("months").get(0);
        assertThat(current.get("spent").asInt()).isEqualTo(50);
        assertThat(current.get("groups").size()).isEqualTo(2);
        boolean group1 = false;
        for (JsonNode group : current.get("groups")) {
            if (group.get("groupId").asLong() == 1L) {
                group1 = true;
                assertThat(group.get("spent").asInt()).isEqualTo(50);
                assertThat(group.get("limit").asInt()).isEqualTo(1000);
            }
        }
        assertThat(group1).isTrue();
    }

    @Test
    void teacherOverviewAggregatesWorkload() throws Exception {
        createCheckpoint("Обзор " + System.nanoTime());
        createActivity("Обзор активности " + System.nanoTime(), "ACADEMIC", 50);
        mockMvc.perform(post("/api/v1/polls")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Обзор опроса " + System.nanoTime(),
                                "endsAt", Instant.now().plusSeconds(3600).toString(),
                                "targetGroupIds", List.of(1),
                                "isAnonymous", true,
                                "questions", List.of(Map.of(
                                        "text", "Вопрос", "type", "SINGLE",
                                        "options", List.of("A", "B")))))))
                .andExpect(status().isCreated());

        JsonNode overview = getJson("/api/v1/analytics/teachers/me/overview", teacherToken());
        assertThat(overview.get("groups").asInt()).isEqualTo(2);
        assertThat(overview.get("checkpoints").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(overview.get("activities").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(overview.get("polls").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(overview.get("pendingMemberships").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(overview.get("currentBudget").size()).isEqualTo(2);
    }

    @Test
    void analyticsAccessIsRestrictedToTeachersOfTheGroup() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/groups/1/ar-dynamics")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());

        String otherTeacher = tokenFor("teacher2@demo.local", "Demo123!");
        mockMvc.perform(get("/api/v1/analytics/groups/1/ar-dynamics")
                        .header("Authorization", "Bearer " + otherTeacher))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/groups/1/ar-dynamics")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/analytics/teachers/me/overview")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());
    }
}
