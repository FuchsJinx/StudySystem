package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.unirate.application.PollEndedProcessor;
import ru.unirate.infrastructure.persistence.entity.Poll;
import ru.unirate.infrastructure.persistence.repo.PollRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PollIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollEndedProcessor pollEndedProcessor;

    private long createPoll(String title, boolean anonymous, String questionType,
                            List<String> options, Instant endsAt, List<Long> groupIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/polls")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Опрос для E2E",
                                "endsAt", endsAt.toString(),
                                "targetGroupIds", groupIds,
                                "isAnonymous", anonymous,
                                "questions", List.of(Map.of(
                                        "text", "Предпочтительный формат",
                                        "type", questionType,
                                        "options", options))))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode detail(long pollId, String token) throws Exception {
        return getJson("/api/v1/polls/" + pollId, token);
    }

    private long firstQuestionId(JsonNode poll) {
        return poll.get("questions").get(0).get("id").asLong();
    }

    private long optionId(JsonNode poll, int index) {
        return poll.get("questions").get(0).get("options").get(index).get("id").asLong();
    }

    private void vote(long pollId, String token, long questionId, List<Long> optionIds, int expectedStatus)
            throws Exception {
        mockMvc.perform(post("/api/v1/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answers", List.of(Map.of("questionId", questionId, "optionIds", optionIds))))))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void anonymousSingleChoicePollFlow() throws Exception {
        long pollId = createPoll("Анонимный опрос", true, "SINGLE",
                List.of("Устно", "Письменно"), Instant.now().plusSeconds(3600), List.of(1L));

        JsonNode before = detail(pollId, studentToken());
        assertThat(before.get("hasVoted").asBoolean()).isFalse();
        assertThat(before.get("canVote").asBoolean()).isTrue();
        assertThat(before.get("results").isNull()).isTrue();
        assertThat(before.get("voters").size()).isZero();

        long questionId = firstQuestionId(before);
        long firstOption = optionId(before, 0);
        long secondOption = optionId(before, 1);
        vote(pollId, studentToken(), questionId, List.of(firstOption), 200);

        JsonNode after = detail(pollId, studentToken());
        assertThat(after.get("hasVoted").asBoolean()).isTrue();
        assertThat(after.get("results").get(0).get("options").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(after.get("voters").size()).isZero();

        vote(pollId, leadToken(), questionId, List.of(secondOption), 200);
        vote(pollId, studentToken(), questionId, List.of(firstOption), 409);

        JsonNode teacherView = detail(pollId, teacherToken());
        assertThat(teacherView.get("voters").size()).isZero();
        assertThat(teacherView.get("results").get(0).get("options").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(teacherView.get("results").get(0).get("options").get(1).get("count").asInt()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/polls/" + pollId + "/results")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(2));
    }

    @Test
    void nonAnonymousPollExposesVotersOnlyToTeacher() throws Exception {
        long pollId = createPoll("Открытый опрос", false, "SINGLE",
                List.of("Да", "Нет"), Instant.now().plusSeconds(3600), List.of(1L));
        JsonNode studentView = detail(pollId, studentToken());
        vote(pollId, studentToken(), firstQuestionId(studentView), List.of(optionId(studentView, 0)), 200);

        JsonNode teacherView = detail(pollId, teacherToken());
        assertThat(teacherView.get("voters").size()).isEqualTo(1);
        assertThat(teacherView.get("voters").get(0).get("studentName").asText()).isEqualTo("Иван Иванов");

        JsonNode afterVote = detail(pollId, studentToken());
        assertThat(afterVote.get("voters").size()).isZero();
    }

    @Test
    void multipleChoiceAllowsSeveralOptions() throws Exception {
        long pollId = createPoll("Множественный выбор", true, "MULTIPLE",
                List.of("Лекции", "Семинары", "Практика"), Instant.now().plusSeconds(3600), List.of(1L));
        JsonNode poll = detail(pollId, studentToken());
        long questionId = firstQuestionId(poll);
        vote(pollId, studentToken(), questionId,
                List.of(optionId(poll, 0), optionId(poll, 2)), 200);
        JsonNode results = getJson("/api/v1/polls/" + pollId + "/results", studentToken());
        assertThat(results.get("results").get(0).get("options").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(results.get("results").get(0).get("options").get(1).get("count").asInt()).isZero();
        assertThat(results.get("results").get(0).get("options").get(2).get("count").asInt()).isEqualTo(1);
    }

    @Test
    void voteValidationRejectsWrongOptionCountAndMissingAnswers() throws Exception {
        long pollId = createPoll("Валидация", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        JsonNode poll = detail(pollId, studentToken());
        long questionId = firstQuestionId(poll);
        vote(pollId, studentToken(), questionId, List.of(optionId(poll, 0), optionId(poll, 1)), 400);

        mockMvc.perform(post("/api/v1/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", List.of()))))
                .andExpect(status().isBadRequest());

        vote(pollId, studentToken(), questionId, List.of(999999L), 400);
    }

    @Test
    void resultsAreHiddenBeforeVoteAndForForeignGroup() throws Exception {
        long pollId = createPoll("Скрытые результаты", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        mockMvc.perform(get("/api/v1/polls/" + pollId + "/results")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RESULTS_NOT_AVAILABLE"));

        mockMvc.perform(get("/api/v1/polls/" + pollId)
                        .header("Authorization", "Bearer " + student2Token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_TARGETED"));
    }

    @Test
    void endedPollCannotBeVotedAndResultsAreVisible() throws Exception {
        long pollId = createPoll("Завершённый опрос", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        Poll poll = pollRepository.findById(pollId).orElseThrow();
        poll.setEndsAt(Instant.now().minusSeconds(60));
        pollRepository.save(poll);

        JsonNode detail = detail(pollId, studentToken());
        assertThat(detail.get("status").asText()).isEqualTo("ENDED");
        assertThat(detail.get("canVote").asBoolean()).isFalse();
        assertThat(detail.get("results").isNull()).isFalse();

        JsonNode pollJson = detail;
        vote(pollId, studentToken(), firstQuestionId(pollJson), List.of(optionId(pollJson, 0)), 400);
    }

    @Test
    void pendingStudentAndTeacherRoleRestrictions() throws Exception {
        long pollId = createPoll("Роли", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        JsonNode poll = detail(pollId, studentToken());
        vote(pollId, tokenFor("pending@demo.local", "Demo123!"),
                firstQuestionId(poll), List.of(optionId(poll, 0)), 403);
        mockMvc.perform(post("/api/v1/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answers", List.of(Map.of("questionId", firstQuestionId(poll),
                                        "optionIds", List.of(optionId(poll, 0))))))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/polls")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Студенческий опрос",
                                "endsAt", Instant.now().plusSeconds(3600).toString(),
                                "targetGroupIds", List.of(1),
                                "isAnonymous", true,
                                "questions", List.of(Map.of(
                                        "text", "Вопрос", "type", "SINGLE",
                                        "options", List.of("A", "B")))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listContainsOnlyTargetedPolls() throws Exception {
        long pollId = createPoll("Только ИВТ-21", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        JsonNode studentList = getJson("/api/v1/polls", studentToken());
        assertThat(studentList.findValues("id").stream().anyMatch(node -> node.asLong() == pollId)).isTrue();
        JsonNode foreignList = getJson("/api/v1/polls", student2Token());
        assertThat(foreignList.findValues("id").stream().noneMatch(node -> node.asLong() == pollId)).isTrue();
    }

    @Test
    void pollEndedProcessorIsIdempotent() throws Exception {
        long pollId = createPoll("Джоба завершения", true, "SINGLE",
                List.of("A", "B"), Instant.now().plusSeconds(3600), List.of(1L));
        Poll poll = pollRepository.findById(pollId).orElseThrow();
        poll.setEndsAt(Instant.now().minusSeconds(60));
        pollRepository.save(poll);

        int first = pollEndedProcessor.processEnded();
        assertThat(first).isGreaterThanOrEqualTo(1);
        assertThat(pollRepository.findById(pollId).orElseThrow().isEndedNotified()).isTrue();
        int second = pollEndedProcessor.processEnded();
        assertThat(second).isZero();
    }
}
