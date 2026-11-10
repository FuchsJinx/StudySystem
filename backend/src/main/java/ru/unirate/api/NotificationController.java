package ru.unirate.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.NotificationDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public CommonDtos.PageResponse<NotificationDtos.NotificationDto> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return notificationService.page(user.id(), cursor, limit);
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountDto unreadCount(@AuthenticationPrincipal AuthUser user) {
        return new NotificationDtos.UnreadCountDto(notificationService.unreadCount(user.id()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDtos.NotificationDto markRead(@AuthenticationPrincipal AuthUser user,
                                                     @PathVariable Long id) {
        return notificationService.markRead(user.id(), id);
    }

    @PostMapping("/read-all")
    public NotificationDtos.UnreadCountDto markAllRead(@AuthenticationPrincipal AuthUser user) {
        notificationService.markAllRead(user.id());
        return new NotificationDtos.UnreadCountDto(0);
    }
}
