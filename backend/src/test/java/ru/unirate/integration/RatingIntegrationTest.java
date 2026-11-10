package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.application.RatingDeductionService;
import ru.unirate.application.SettingsService;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RatingDeductionService deductionService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    private long createCheckpoint(String title, Instant deadline, int cost) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Описание задачи",
                                "deadline", deadline.toString(),
                                "cost", cost,
                                "groupIds", java.util.List.of(1)))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json(result);
        assertThat(body.get("roster").size()).isGreaterThan(0);
        return body.get("id").asLong();
    }

    private void submitUrl(String token, long checkpointId, String url) throws Exception {
        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", url))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    private int studentRating() {
        return studentProfileRepository.findByUserId(4L).orElseThrow().getAcademicRating();
    }

    private int studentDebited(long checkpointId) {
        StudentProfile profile = studentProfileRepository.findByUserId(4L).orElseThrow();
        return submissionRepository.findByCheckpointIdAndStudentProfileId(checkpointId, profile.getId())
                .orElseThrow().getArDebited();
    }

    @Test
    void checkpointIsAssignedToConfirmedStudentsOnly() throws Exception {
        long checkpointId = createCheckpoint("Только подтверждённым",
                Instant.now().plusSeconds(3600), 100);

        JsonNode my = getJson("/api/v1/users/me/checkpoints", studentToken());
        assertThat(my.findValues("checkpointId").stream().anyMatch(node -> node.asLong() == checkpointId)).isTrue();

        JsonNode pending = getJson("/api/v1/users/me/checkpoints",
                tokenFor("pending@demo.local", "Demo123!"));
        assertThat(pending.findValues("checkpointId").stream().anyMatch(node -> node.asLong() == checkpointId)).isFalse();

        JsonNode roster = getJson("/api/v1/checkpoints/" + checkpointId + "/roster", teacherToken());
        assertThat(roster.get("roster").size()).isEqualTo(2);
    }

    @Test
    void deductionStopsAfterSubmissionAndAccept() throws Exception {
        Instant past = Instant.now().minusSeconds(600);
        long checkpointId = createCheckpoint("Просроченная сдача", past, 100);

        var firstTick = deductionService.tick();
        assertThat(firstTick.totalDebited()).isGreaterThan(0);
        assertThat(studentRating()).isEqualTo(980);
        assertThat(studentDebited(checkpointId)).isEqualTo(20);

        submitUrl(studentToken(), checkpointId, "https://example.com/work/1");
        deductionService.tick();
        assertThat(studentRating()).isEqualTo(980);

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/accept")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", java.util.List.of(4)))))
                .andExpect(status().isOk());

        deductionService.tick();
        assertThat(studentRating()).isEqualTo(980);
        assertThat(studentDebited(checkpointId)).isEqualTo(20);
    }

    @Test
    void deductionIsCappedByCost() throws Exception {
        long checkpointId = createCheckpoint("Дорогая задача", Instant.now().minusSeconds(600), 50);

        deductionService.tick();
        deductionService.tick();
        deductionService.tick();
        assertThat(studentRating()).isEqualTo(950);
        assertThat(studentDebited(checkpointId)).isEqualTo(50);

        deductionService.tick();
        assertThat(studentRating()).isEqualTo(950);
    }

    @Test
    void deductionStopsAtZero() throws Exception {
        StudentProfile profile = studentProfileRepository.findByUserId(4L).orElseThrow();
        profile.setAcademicRating(15);
        studentProfileRepository.save(profile);

        long checkpointId = createCheckpoint("Нулевой рейтинг", Instant.now().minusSeconds(600), 100);
        deductionService.tick();

        assertThat(studentRating()).isZero();
        assertThat(studentDebited(checkpointId)).isEqualTo(15);
    }

    @Test
    void returnAfterDeadlineResumesDeduction() throws Exception {
        Instant past = Instant.now().minusSeconds(600);
        long checkpointId = createCheckpoint("Возврат на доработку", past, 100);

        deductionService.tick();
        assertThat(studentRating()).isEqualTo(980);

        submitUrl(studentToken(), checkpointId, "https://example.com/work/2");
        deductionService.tick();
        assertThat(studentRating()).isEqualTo(980);

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/return")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentIds", java.util.List.of(4),
                                "comment", "Доработайте введение"))))
                .andExpect(status().isOk());

        deductionService.tick();
        assertThat(studentRating()).isEqualTo(960);
        assertThat(studentDebited(checkpointId)).isEqualTo(40);
    }

    @Test
    void overdueStatusIsSetByTick() throws Exception {
        long checkpointId = createCheckpoint("Статус просрочено", Instant.now().minusSeconds(600), 100);
        deductionService.tick();

        JsonNode my = getJson("/api/v1/users/me/checkpoints", studentToken());
        boolean overdue = false;
        for (JsonNode item : my) {
            if (item.get("checkpointId").asLong() == checkpointId) {
                overdue = item.get("status").asText().equals("OVERDUE");
            }
        }
        assertThat(overdue).isTrue();
    }

    @Test
    void submitRequiresLinkOrFileAndValidatesFileType() throws Exception {
        long checkpointId = createCheckpoint("Сдача работы", Instant.now().plusSeconds(3600), 100);

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SUBMISSION_EMPTY"));

        MockMultipartFile invalid = new MockMultipartFile(
                "file", "report.exe", "application/octet-stream", "malware".getBytes());
        mockMvc.perform(multipart("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .file(invalid)
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FILE_INVALID_TYPE"));

        MockMultipartFile valid = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .file(valid)
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("report.pdf"))
                .andReturn();

        long submissionId = json(result).get("submissionId").asLong();
        mockMvc.perform(get("/api/v1/files/submissions/" + submissionId)
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/files/submissions/" + submissionId)
                        .header("Authorization", "Bearer " + student2Token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void batchAcceptAndRoleRestrictions() throws Exception {
        long checkpointId = createCheckpoint("Пакетная проверка", Instant.now().minusSeconds(600), 100);
        submitUrl(studentToken(), checkpointId, "https://example.com/a");
        submitUrl(leadToken(), checkpointId, "https://example.com/b");

        mockMvc.perform(post("/api/v1/checkpoints")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Чужая задача", "deadline", Instant.now().plusSeconds(100).toString(),
                                "cost", 10, "groupIds", java.util.List.of(1)))))
                .andExpect(status().isForbidden());

        MvcResult accepted = mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/accept")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", java.util.List.of(4, 6)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount").value(2))
                .andReturn();
        assertThat(json(accepted).get("roster").size()).isEqualTo(2);
    }

    @Test
    void settingsChangeAffectsDeductionAmount() throws Exception {
        settingsService.update(Map.of("arDeductAmount", "10"));
        long checkpointId = createCheckpoint("Новая настройка", Instant.now().minusSeconds(600), 100);
        deductionService.tick();
        assertThat(studentRating()).isEqualTo(990);
        assertThat(studentDebited(checkpointId)).isEqualTo(10);
    }

    @Test
    void teacherCannotModifyAnotherTeachersCheckpoint() throws Exception {
        long checkpointId = createCheckpoint("Чужая проверка", Instant.now().minusSeconds(600), 100);
        String secondTeacher = tokenFor("teacher2@demo.local", "Demo123!");
        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/accept")
                        .header("Authorization", "Bearer " + secondTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", java.util.List.of(4)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void submissionStatusTransitionsAreValidated() throws Exception {
        long checkpointId = createCheckpoint("Повторная проверка", Instant.now().plusSeconds(3600), 100);
        submitUrl(studentToken(), checkpointId, "https://example.com/x");

        mockMvc.perform(post("/api/v1/checkpoints/" + checkpointId + "/submit")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://example.com/y"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SUBMISSION_INVALID_STATUS"));
    }
}
