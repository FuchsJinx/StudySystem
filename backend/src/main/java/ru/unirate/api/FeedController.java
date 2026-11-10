package ru.unirate.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.FeedDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.AwardFeedService;
import ru.unirate.application.PersonalFeedService;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final AwardFeedService awardFeedService;
    private final PersonalFeedService personalFeedService;

    public FeedController(AwardFeedService awardFeedService, PersonalFeedService personalFeedService) {
        this.awardFeedService = awardFeedService;
        this.personalFeedService = personalFeedService;
    }

    @GetMapping("/awards")
    public CommonDtos.PageResponse<FeedDtos.AwardFeedItemDto> awards(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return awardFeedService.page(cursor, limit);
    }

    @GetMapping("/personal")
    public CommonDtos.PageResponse<FeedDtos.PersonalFeedItemDto> personal(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return personalFeedService.page(user.id(), cursor, limit);
    }
}
