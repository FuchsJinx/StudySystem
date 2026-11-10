package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.infrastructure.persistence.entity.EmailVerificationToken;
import ru.unirate.infrastructure.persistence.repo.EmailVerificationTokenRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Test
    void studentRegistrationCreatesPendingProfileWithStartRating() throws Exception {
        String email = uniqueEmail("newstudent");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1!",
                                "fullName", "Новый Студентов",
                                "facultyId", 1,
                                "groupId", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.student.membershipStatus").value("PENDING"))
                .andExpect(jsonPath("$.user.student.academicRating").value(1000))
                .andReturn();

        JsonNode body = json(result);
        long userId = body.get("user").get("id").asLong();
        assertThat(studentProfileRepository.findByUserId(userId)).isPresent();
        assertThat(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByIdDesc(userId)).isPresent();
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail("duplicate");
        Map<String, Object> request = Map.of(
                "email", email, "password", "Password1!", "fullName", "Дублин Дублинов",
                "facultyId", 1, "groupId", 1);
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void groupMustBelongToSelectedFaculty() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail("wronggroup"),
                                "password", "Password1!",
                                "fullName", "Ошибочная Группова",
                                "facultyId", 2,
                                "groupId", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"));
    }

    @Test
    void loginAndMeWork() throws Exception {
        String token = studentToken();
        JsonNode me = getJson("/api/v1/auth/me", token);
        assertThat(me.get("email").asText()).isEqualTo("student@demo.local");
        assertThat(me.get("student").get("academicRating").asInt()).isEqualTo(1000);
        assertThat(me.get("student").get("groupName").asText()).isEqualTo("ИВТ-21");
    }

    @Test
    void badPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "student@demo.local", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void anonymousRequestIsRejectedWithJsonError() throws Exception {
        mockMvc.perform(get("/api/v1/users/4/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void emailVerificationFlow() throws Exception {
        String email = uniqueEmail("verify");
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1!",
                                "fullName", "Верификация Тестова",
                                "facultyId", 1,
                                "groupId", 1))))
                .andExpect(status().isCreated())
                .andReturn();
        long userId = json(registered).get("user").get("id").asLong();
        EmailVerificationToken token = tokenRepository
                .findTopByUserIdAndUsedAtIsNullOrderByIdDesc(userId).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token.getToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String userToken = tokenFor(email, "Password1!");
        JsonNode me = getJson("/api/v1/auth/me", userToken);
        assertThat(me.get("emailVerified").asBoolean()).isTrue();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token.getToken()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMAIL_VERIFICATION_INVALID"));
    }

    @Test
    void publicProfileVisibleButLedgerIsPrivate() throws Exception {
        String viewerToken = student2Token();
        JsonNode profile = getJson("/api/v1/users/4/profile", viewerToken);
        assertThat(profile.get("fullName").asText()).isEqualTo("Иван Иванов");
        assertThat(profile.get("academicRating").asInt()).isEqualTo(1000);
        assertThat(profile.get("groupName").asText()).isEqualTo("ИВТ-21");

        mockMvc.perform(get("/api/v1/users/4/ledger").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void notificationSettingsCanBeUpdated() throws Exception {
        String token = studentToken();
        mockMvc.perform(put("/api/v1/users/me/notification-settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "emailEnabled", false,
                                "telegramEnabled", true,
                                "telegramChatId", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.telegramEnabled").value(true))
                .andExpect(jsonPath("$.telegramChatId").value("123456"));
    }

    @Test
    void teacherRegistrationWorks() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail("teacher"),
                                "password", "Password1!",
                                "fullName", "Новый Преподавателев",
                                "departmentId", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("TEACHER"))
                .andExpect(jsonPath("$.user.teacher.departmentId").value(1));
    }

    @Test
    void catalogIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/faculties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
        mockMvc.perform(get("/api/v1/catalog/faculties/1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ИВТ-21"));
        mockMvc.perform(get("/api/v1/catalog/departments"))
                .andExpect(status().isOk());
    }
}
