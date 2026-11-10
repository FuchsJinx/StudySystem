package ru.unirate.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.unirate.domain.model.SubmissionStatus;

import java.time.Instant;
import java.util.List;

public final class CheckpointDtos {

    private CheckpointDtos() {
    }

    public record CreateCheckpointRequest(
            @NotBlank(message = "Укажите название задачи")
            @Size(max = 255, message = "Название слишком длинное")
            String title,
            @Size(max = 5000, message = "Описание слишком длинное")
            String description,
            @NotNull(message = "Укажите дедлайн")
            Instant deadline,
            @NotNull(message = "Укажите стоимость задачи в АР")
            @Min(value = 1, message = "Стоимость должна быть не меньше 1 АР")
            @Max(value = 1000, message = "Стоимость не может превышать 1000 АР")
            Integer cost,
            @NotEmpty(message = "Выберите хотя бы одну группу")
            List<Long> groupIds
    ) {
    }

    public record CheckpointSummaryDto(
            Long id,
            String title,
            String description,
            Instant deadline,
            int cost,
            List<CatalogDtos.GroupRef> groups,
            Instant createdAt,
            String teacherName,
            long studentsCount,
            long submittedCount,
            long acceptedCount,
            long overdueCount
    ) {
    }

    public record RosterRowDto(
            Long studentUserId,
            Long studentProfileId,
            String fullName,
            String groupName,
            SubmissionStatus status,
            int arDebited,
            int cost,
            Instant submittedAt,
            String submissionUrl,
            Long submissionId,
            String fileName,
            String returnComment
    ) {
    }

    public record StudentCheckpointDto(
            Long submissionId,
            Long checkpointId,
            String title,
            String description,
            Instant deadline,
            int cost,
            SubmissionStatus status,
            int arDebited,
            Instant submittedAt,
            String submissionUrl,
            String fileName,
            String returnComment,
            String teacherName
    ) {
    }

    public record CheckpointDetailDto(
            Long id,
            String title,
            String description,
            Instant deadline,
            int cost,
            List<CatalogDtos.GroupRef> groups,
            Instant createdAt,
            String teacherName,
            Long studentsCount,
            Long submittedCount,
            Long acceptedCount,
            Long overdueCount,
            List<RosterRowDto> roster,
            StudentCheckpointDto mySubmission
    ) {
    }

    public record AcceptRequest(
            @NotEmpty(message = "Выберите хотя бы одного студента")
            List<Long> studentIds
    ) {
    }

    public record ReturnRequest(
            @NotEmpty(message = "Выберите хотя бы одного студента")
            List<Long> studentIds,
            @Size(max = 2000, message = "Комментарий слишком длинный")
            String comment
    ) {
    }

    public record SubmitRequest(
            @Size(max = 2048, message = "Ссылка слишком длинная")
            String url
    ) {
    }
}
