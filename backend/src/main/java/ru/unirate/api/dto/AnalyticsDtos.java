package ru.unirate.api.dto;

import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record ArPointDto(LocalDate date, double average, int median) {
    }

    public record StudentArDto(Long studentUserId, String fullName, int academicRating) {
    }

    public record ArDynamicsResponse(
            Long groupId,
            String groupName,
            List<ArPointDto> points,
            List<StudentArDto> students
    ) {
    }

    public record RankRowDto(
            Long studentUserId,
            String fullName,
            String groupName,
            long bua,
            long boa,
            long total
    ) {
    }

    public record ActivityRankingResponse(Long groupId, String groupName, List<RankRowDto> students) {
    }

    public record GroupSpendDto(Long groupId, String groupName, int limit, int spent) {
    }

    public record MonthBudgetDto(
            String yearMonth,
            int limit,
            int spent,
            List<GroupSpendDto> groups
    ) {
    }

    public record BudgetStatsResponse(List<MonthBudgetDto> months) {
    }

    public record TeacherOverviewResponse(
            int groups,
            int students,
            int checkpoints,
            int activities,
            int polls,
            int pendingMemberships,
            String yearMonth,
            List<ActivityDtos.GroupBudgetDto> currentBudget
    ) {
    }
}
