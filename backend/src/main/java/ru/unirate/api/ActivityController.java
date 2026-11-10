package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.ActivityDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.ActivityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ActivityDtos.ActivitySummaryDto> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ActivityDtos.CreateActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(user, request));
    }

    @GetMapping("/activities")
    public List<ActivityDtos.ActivitySummaryDto> list(@AuthenticationPrincipal AuthUser user) {
        return activityService.list(user);
    }

    @GetMapping("/activities/{id}")
    public ActivityDtos.ActivitySummaryDto detail(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long id) {
        return activityService.detail(user, id);
    }

    @PostMapping("/activities/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ActivityDtos.ActivitySummaryDto enroll(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long id) {
        return activityService.enroll(user, id);
    }

    @PostMapping("/activities/{id}/award")
    @PreAuthorize("hasRole('TEACHER')")
    public ActivityDtos.AwardResponse award(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ActivityDtos.AwardRequest request) {
        return activityService.award(user, id, request);
    }

    @GetMapping("/activities/{id}/participants")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ActivityDtos.ParticipantDto> participants(@AuthenticationPrincipal AuthUser user,
                                                          @PathVariable Long id) {
        return activityService.participants(user, id);
    }

    @GetMapping("/teachers/me/budget")
    @PreAuthorize("hasRole('TEACHER')")
    public ActivityDtos.BudgetDto budget(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam Long groupId,
                                         @RequestParam(required = false) String yearMonth) {
        return activityService.budget(user, groupId, yearMonth);
    }

    @GetMapping("/teachers/me/budgets")
    @PreAuthorize("hasRole('TEACHER')")
    public ActivityDtos.BudgetOverviewDto budgets(@AuthenticationPrincipal AuthUser user,
                                                  @RequestParam(required = false) String yearMonth) {
        return activityService.budgets(user, yearMonth);
    }
}
