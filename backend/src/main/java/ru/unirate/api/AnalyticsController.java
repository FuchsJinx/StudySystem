package ru.unirate.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.AnalyticsDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.AnalyticsService;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/groups/{id}/ar-dynamics")
    public AnalyticsDtos.ArDynamicsResponse arDynamics(@AuthenticationPrincipal AuthUser user,
                                                       @PathVariable Long id,
                                                       @RequestParam(defaultValue = "30") int days) {
        return analyticsService.arDynamics(user, id, days);
    }

    @GetMapping("/groups/{id}/activity-ranking")
    public AnalyticsDtos.ActivityRankingResponse activityRanking(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "90") int days) {
        return analyticsService.activityRanking(user, id);
    }

    @GetMapping("/teachers/me/budget-stats")
    public AnalyticsDtos.BudgetStatsResponse budgetStats(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "6") int months) {
        return analyticsService.budgetStats(user, months);
    }

    @GetMapping("/teachers/me/overview")
    public AnalyticsDtos.TeacherOverviewResponse overview(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.teacherOverview(user);
    }
}
