package ru.unirate.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.unirate.domain.model.PollQuestionType;

import java.time.Instant;
import java.util.List;

public final class PollDtos {

    private PollDtos() {
    }

    public record CreatePollRequest(
            @NotBlank(message = "Укажите название опроса")
            @Size(max = 255, message = "Название слишком длинное")
            String title,
            @Size(max = 5000, message = "Описание слишком длинное")
            String description,
            @NotNull(message = "Укажите дату окончания")
            @Future(message = "Дата окончания должна быть в будущем")
            Instant endsAt,
            @NotEmpty(message = "Выберите хотя бы одну группу")
            List<Long> targetGroupIds,
            boolean isAnonymous,
            @NotEmpty(message = "Добавьте хотя бы один вопрос")
            @Valid
            List<QuestionInput> questions
    ) {
    }

    public record QuestionInput(
            @NotBlank(message = "Укажите текст вопроса")
            @Size(max = 1024, message = "Текст вопроса слишком длинный")
            String text,
            @NotNull(message = "Укажите тип вопроса")
            PollQuestionType type,
            @NotEmpty(message = "Добавьте варианты ответа")
            @Size(min = 2, message = "Нужно минимум два варианта ответа")
            List<@NotBlank(message = "Вариант ответа не может быть пустым") String> options
    ) {
    }

    public record PollSummaryDto(
            Long id,
            String title,
            Instant endsAt,
            boolean isAnonymous,
            String status,
            boolean hasVoted,
            int questionCount,
            Instant createdAt,
            List<CatalogDtos.GroupRef> targetGroups
    ) {
    }

    public record OptionDto(Long id, String text) {
    }

    public record QuestionDto(Long id, String text, PollQuestionType type, List<OptionDto> options) {
    }

    public record OptionResultDto(Long optionId, String text, int count, double percent) {
    }

    public record QuestionResultDto(
            Long questionId,
            String text,
            PollQuestionType type,
            int totalVotes,
            List<OptionResultDto> options
    ) {
    }

    public record VoterAnswerDto(Long questionId, List<String> options) {
    }

    public record VoterDto(String studentName, String groupName, List<VoterAnswerDto> answers) {
    }

    public record PollDetailDto(
            Long id,
            String title,
            String description,
            Instant endsAt,
            boolean isAnonymous,
            String status,
            boolean hasVoted,
            boolean canVote,
            List<CatalogDtos.GroupRef> targetGroups,
            Instant createdAt,
            String teacherName,
            List<QuestionDto> questions,
            List<QuestionResultDto> results,
            List<VoterDto> voters
    ) {
    }

    public record VoteAnswer(
            @NotNull(message = "Укажите вопрос")
            Long questionId,
            @NotEmpty(message = "Выберите вариант ответа")
            List<Long> optionIds
    ) {
    }

    public record VoteRequest(
            @NotEmpty(message = "Ответьте на вопросы опроса")
            List<@Valid VoteAnswer> answers
    ) {
    }

    public record PollResultsDto(
            Long pollId,
            String title,
            String status,
            boolean isAnonymous,
            int totalResponses,
            List<QuestionResultDto> results,
            List<VoterDto> voters
    ) {
    }
}
