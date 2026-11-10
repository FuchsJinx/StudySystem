package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.application.ActivityService;
import ru.unirate.application.RatingDeductionService;
import ru.unirate.application.SettingsService;
import ru.unirate.domain.model.ActivityType;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.repo.ActivityAwardRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ActivityAwardRepository awardRepository;

    @Autowired
    private PointLedgerRepository ledgerRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private RatingDeductionService deductionService;

    private long createActivity(String title, ActivityType type, int maxPoints, List<Long> groupIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Описание активности",
                                "type", type.name(),
                                "eventDate", LocalDate.now().plusDays(2).toString(),
                                "maxPoints", maxPoints,
                                "groupIds", groupIds))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private void award(long activityId, long studentUserId, int level, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/award")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "awards", List.of(Map.of("studentId", studentUserId, "level", level))))))
                .andExpect(status().is(expectedStatus));
    }

    private int bua(long studentUserId) {
        StudentProfile profile = studentProfileRepository.findByUserId(studentUserId).orElseThrow();
        return (int) ledgerRepository.sumCredits(profile.getId(), ru.unirate.domain.model.PointCurrency.BUA);
    }

    private int boa(long studentUserId) {
        StudentProfile profile = studentProfileRepository.findByUserId(studentUserId).orElseThrow();
        return (int) ledgerRepository.sumCredits(profile.getId(), ru.unirate.domain.model.PointCurrency.BOA);
    }

    @Test
    void socialActivityAwardFlow() throws Exception {
        long activityId = createActivity("Волонтёрство", ActivityType.SOCIAL, 100, List.of(1L));
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/enroll")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true));

        award(activityId, 4L, 50, 200);

        assertThat(boa(4L)).isEqualTo(50);
        assertThat(bua(4L)).isZero();
        StudentProfile student = studentProfileRepository.findByUserId(4L).orElseThrow();
        assertThat(awardRepository.findByActivityIdAndStudentProfileId(activityId, student.getId())).isPresent();

        mockMvc.perform(get("/api/v1/teachers/me/budget")
                        .param("groupId", "1")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spent").value(50))
                .andExpect(jsonPath("$.remaining").value(950));
    }

    @Test
    void academicActivityAwardsBuaWithoutEnrollment() throws Exception {
        long activityId = createActivity("Научный семинар", ActivityType.ACADEMIC, 100, List.of(1L));
        award(activityId, 4L, 75, 200);
        assertThat(bua(4L)).isEqualTo(75);
        assertThat(boa(4L)).isZero();
    }

    @Test
    void socialAwardRequiresEnrollment() throws Exception {
        long activityId = createActivity("Субботник", ActivityType.SOCIAL, 100, List.of(1L));
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/award")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "awards", List.of(Map.of("studentId", 4, "level", 50))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NOT_ENROLLED"));
    }

    @Test
    void academicActivityRejectsEnrollment() throws Exception {
        long activityId = createActivity("Лекция", ActivityType.ACADEMIC, 50, List.of(1L));
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/enroll")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentsCannotSeeOrEnrollForeignGroupActivity() throws Exception {
        long activityId = createActivity("Активность ИВТ-22", ActivityType.SOCIAL, 100, List.of(2L));
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/enroll")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_TARGETED"));

        JsonNode list = getJson("/api/v1/activities", studentToken());
        assertThat(list.findValues("id").stream().noneMatch(node -> node.asLong() == activityId)).isTrue();
    }

    @Test
    void budgetIsEnforcedAndRolledBackAtomically() throws Exception {
        settingsService.update(Map.of("teacherMonthlyBudgetPerGroup", "60"));
        long firstActivity = createActivity("Бюджет 1", ActivityType.ACADEMIC, 100, List.of(1L));
        award(firstActivity, 4L, 50, 200);
        assertThat(bua(4L)).isEqualTo(50);

        long secondActivity = createActivity("Бюджет 2", ActivityType.ACADEMIC, 100, List.of(1L));
        award(secondActivity, 6L, 25, 400);

        StudentProfile lead = studentProfileRepository.findByUserId(6L).orElseThrow();
        assertThat(awardRepository.findByActivityIdAndStudentProfileId(secondActivity, lead.getId())).isEmpty();
        assertThat(bua(6L)).isZero();

        mockMvc.perform(get("/api/v1/teachers/me/budget")
                        .param("groupId", "1")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(jsonPath("$.spent").value(50))
                .andExpect(jsonPath("$.remaining").value(10));
    }

    @Test
    void budgetIsSeparatePerGroupAndResetsForNewMonth() throws Exception {
        settingsService.update(Map.of("teacherMonthlyBudgetPerGroup", "100"));
        long activityGroup1 = createActivity("ИВТ-21 активность", ActivityType.ACADEMIC, 100, List.of(1L));
        long activityGroup2 = createActivity("ИВТ-22 активность", ActivityType.ACADEMIC, 100, List.of(2L));
        award(activityGroup1, 4L, 100, 200);
        award(activityGroup2, 7L, 100, 200);

        mockMvc.perform(get("/api/v1/teachers/me/budget")
                        .param("groupId", "1")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(jsonPath("$.spent").value(100));
        mockMvc.perform(get("/api/v1/teachers/me/budget")
                        .param("groupId", "2")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(jsonPath("$.spent").value(100));

        mockMvc.perform(get("/api/v1/teachers/me/budget")
                        .param("groupId", "1")
                        .param("yearMonth", "2030-01")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(jsonPath("$.spent").value(0))
                .andExpect(jsonPath("$.remaining").value(100));
    }

    @Test
    void duplicateAwardIsRejected() throws Exception {
        long activityId = createActivity("Повтор", ActivityType.ACADEMIC, 100, List.of(1L));
        award(activityId, 4L, 50, 200);
        award(activityId, 4L, 50, 409);
        assertThat(bua(4L)).isEqualTo(50);
    }

    @Test
    void pendingAndForeignStudentsCannotBeAwarded() throws Exception {
        long activityId = createActivity("Проверка групп", ActivityType.ACADEMIC, 100, List.of(1L));
        award(activityId, 5L, 50, 403);
        award(activityId, 8L, 50, 403);
    }

    @Test
    void onlyOwnerTeacherCanAwardAndSeeParticipants() throws Exception {
        long activityId = createActivity("Владение", ActivityType.ACADEMIC, 100, List.of(1L));
        String otherTeacher = tokenFor("teacher2@demo.local", "Demo123!");
        mockMvc.perform(get("/api/v1/activities/" + activityId + "/participants")
                        .header("Authorization", "Bearer " + otherTeacher))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/activities/" + activityId + "/award")
                        .header("Authorization", "Bearer " + otherTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "awards", List.of(Map.of("studentId", 4, "level", 50))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void awardFeedContainsOnlyPointAwardsAndPaginates() throws Exception {
        long academic = createActivity("Лента 1", ActivityType.ACADEMIC, 100, List.of(1L));
        long social = createActivity("Лента 2", ActivityType.SOCIAL, 100, List.of(1L));
        award(academic, 4L, 25, 200);
        mockMvc.perform(post("/api/v1/activities/" + social + "/enroll")
                        .header("Authorization", "Bearer " + leadToken()))
                .andExpect(status().isOk());
        award(social, 6L, 50, 200);

        JsonNode firstPage = getJson("/api/v1/feed/awards?limit=1", student2Token());
        assertThat(firstPage.get("items").size()).isEqualTo(1);
        assertThat(firstPage.get("nextCursor").isNull()).isFalse();
        String firstTitle = firstPage.get("items").get(0).get("activityTitle").asText();
        assertThat(firstTitle).isIn("Лента 1", "Лента 2");

        long cursor = firstPage.get("nextCursor").asLong();
        JsonNode secondPage = getJson("/api/v1/feed/awards?limit=1&cursor=" + cursor, student2Token());
        assertThat(secondPage.get("items").size()).isEqualTo(1);

        JsonNode all = getJson("/api/v1/feed/awards?limit=20", student2Token());
        for (JsonNode item : all.get("items")) {
            assertThat(item.get("currency").asText()).isIn("BUA", "BOA");
        }

        mockMvc.perform(get("/api/v1/feed/awards")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentsCannotAccessOtherBudgetOrParticipants() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me/budget").param("groupId", "1")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());
        long activityId = createActivity("Недоступные участники", ActivityType.ACADEMIC, 50, List.of(1L));
        mockMvc.perform(get("/api/v1/activities/" + activityId + "/participants")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void arDebitIsNotPublishedToAwardFeed() throws Exception {
        long before = getJson("/api/v1/feed/awards?limit=50", adminToken()).get("items").size();
        mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Просрочка для ленты",
                                "deadline", java.time.Instant.now().minusSeconds(300).toString(),
                                "cost", 100,
                                "groupIds", List.of(1)))))
                .andExpect(status().isCreated());
        deductionService.tick();
        long after = getJson("/api/v1/feed/awards?limit=50", adminToken()).get("items").size();
        assertThat(after).isEqualTo(before);
    }
}
