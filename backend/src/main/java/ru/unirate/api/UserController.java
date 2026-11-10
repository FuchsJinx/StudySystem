package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.ActivityDtos;
import ru.unirate.api.dto.CheckpointDtos;
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.ActivityService;
import ru.unirate.application.CheckpointService;
import ru.unirate.application.ProfileService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ProfileService profileService;
    private final CheckpointService checkpointService;
    private final ActivityService activityService;

    public UserController(ProfileService profileService, CheckpointService checkpointService,
                          ActivityService activityService) {
        this.profileService = profileService;
        this.checkpointService = checkpointService;
        this.activityService = activityService;
    }

    @GetMapping("/me/activities")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ActivityDtos.ActivitySummaryDto> myActivities(
            @AuthenticationPrincipal AuthUser viewer) {
        return activityService.list(viewer);
    }

    @GetMapping("/me/checkpoints")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CheckpointDtos.StudentCheckpointDto> myCheckpoints(
            @AuthenticationPrincipal AuthUser viewer) {
        return checkpointService.studentCheckpoints(viewer);
    }

    @GetMapping("/{id}/profile")
    public ProfileDtos.ProfileResponse profile(@AuthenticationPrincipal AuthUser viewer, @PathVariable Long id) {
        return profileService.profile(viewer, id);
    }

    @GetMapping("/{id}/ledger")
    public List<ProfileDtos.LedgerEntryDto> ledger(@AuthenticationPrincipal AuthUser viewer,
                                                   @PathVariable Long id,
                                                   @RequestParam(required = false) Long cursor,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return profileService.ledger(viewer, id, cursor, limit);
    }

    @GetMapping("/me/notification-settings")
    public ProfileDtos.NotificationSettingsDto notificationSettings(@AuthenticationPrincipal AuthUser viewer) {
        return profileService.notificationSettings(viewer.id());
    }

    @PutMapping("/me/notification-settings")
    public ProfileDtos.NotificationSettingsDto updateNotificationSettings(
            @AuthenticationPrincipal AuthUser viewer,
            @Valid @RequestBody ProfileDtos.UpdateNotificationSettingsRequest request) {
        return profileService.updateNotificationSettings(viewer.id(), request);
    }
}
