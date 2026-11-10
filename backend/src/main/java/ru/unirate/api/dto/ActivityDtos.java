package ru.unirate.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.unirate.domain.model.ActivityType;
import ru.unirate.domain.model.PointCurrency;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record CreateActivityRequest(
            @NotBlank(message = "Укажите название активности")
            @Size(max = 255, message = "Название слишком длинное")
            String title,
            @Size(max = 5000, message = "Описание слишком длинное")
            String description,
            @NotNull(message = "Укажите тип активности")
            ActivityType type,
            @NotNull(message = "Укажите дату проведения")
            LocalDate eventDate,
            @NotNull(message = "Укажите максимум баллов")
            @Min(value = 1, message = "Минимум 1 балл")
            @Max(value = 1000, message = "Максимум 1000 баллов")
            Integer maxPoints,
            @NotEmpty(message = "Выберите хотя бы одну группу")
            List<Long> groupIds
    ) {
    }

    public record ActivitySummaryDto(
            Long id,
            String title,
            String description,
            ActivityType type,
            LocalDate eventDate,
            int maxPoints,
            List<CatalogDtos.GroupRef> groups,
            Instant createdAt,
            String teacherName,
            long enrolledCount,
            long awardedCount,
            Boolean enrolled,
            Integer myAwardedPoints,
            Integer myAwardedLevel
    ) {
    }

    public record AwardItem(
            @NotNull(message = "Укажите студента")
            Long studentId,
            @NotNull(message = "Укажите уровень участия")
            Integer level
    ) {
    }

    public record AwardRequest(
            @NotEmpty(message = "Выберите хотя бы одного студента")
            List<AwardItem> awards
    ) {
    }

    public record AwardResultDto(
            Long studentUserId,
            String studentName,
            int points,
            PointCurrency currency,
            int remainingBudget
    ) {
    }

    public record AwardResponse(
            String yearMonth,
            int budgetLimit,
            List<AwardResultDto> awarded
    ) {
    }

    public record ParticipantDto(
            Long studentUserId,
            Long studentProfileId,
            String fullName,
            String groupName,
            boolean enrolled,
            Integer awardedPoints,
            Integer awardedLevel
    ) {
    }

    public record BudgetAwardDto(
            Long awardId,
            String studentName,
            String activityTitle,
            int points,
            PointCurrency currency,
            Instant date
    ) {
    }

    public record BudgetDto(
            String yearMonth,
            Long groupId,
            String groupName,
            int limit,
            int spent,
            int remaining,
            List<BudgetAwardDto> awards
    ) {
    }

    public record GroupBudgetDto(
            Long groupId,
            String groupName,
            int limit,
            int spent,
            int remaining
    ) {
    }

    public record BudgetOverviewDto(
            String yearMonth,
            int totalLimit,
            int totalSpent,
            int totalRemaining,
            List<GroupBudgetDto> groups
    ) {
    }
}
