package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.domain.model.MembershipStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipIntegrationTest extends IntegrationTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    private ru.unirate.infrastructure.persistence.repo.StudentProfileRepository studentProfileRepository;

    @Test
    void teacherSeesAndConfirmsPendingStudent() throws Exception {
        String teacherToken = teacherToken();
        MvcResult pendingResult = mockMvc.perform(get("/api/v1/membership/requests")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pending = json(pendingResult);
        boolean containsPending = false;
        for (JsonNode item : pending) {
            if (item.get("studentUserId").asLong() == 5L) {
                containsPending = true;
            }
        }
        assertThat(containsPending).isTrue();

        mockMvc.perform(post("/api/v1/membership/5/confirm")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        StudentProfile profile = studentProfileRepository.findByUserId(5L).orElseThrow();
        assertThat(profile.getMembershipStatus()).isEqualTo(MembershipStatus.CONFIRMED);
        assertThat(profile.getMembershipConfirmedBy()).isNotNull();
    }

    @Test
    void groupLeadCanConfirmClassmate() throws Exception {
        mockMvc.perform(post("/api/v1/membership/5/confirm")
                        .header("Authorization", "Bearer " + leadToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void regularStudentCannotConfirm() throws Exception {
        mockMvc.perform(get("/api/v1/membership/requests")
                        .header("Authorization", "Bearer " + student2Token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/membership/5/confirm")
                        .header("Authorization", "Bearer " + student2Token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectChangesStatus() throws Exception {
        mockMvc.perform(post("/api/v1/membership/5/reject")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
