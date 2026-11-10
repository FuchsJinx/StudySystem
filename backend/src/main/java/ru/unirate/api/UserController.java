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
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.ProfileService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
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
