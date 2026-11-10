package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.AiDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.AiService;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/draft-checkpoint")
    @PreAuthorize("hasRole('TEACHER')")
    public AiDtos.AiTextResponse draftCheckpoint(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody AiDtos.DraftCheckpointRequest request) {
        return aiService.draftCheckpoint(user, request);
    }

    @PostMapping("/poll-summary")
    @PreAuthorize("hasRole('TEACHER')")
    public AiDtos.AiTextResponse pollSummary(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody AiDtos.PollSummaryRequest request) {
        return aiService.pollSummary(user, request.pollId());
    }

    @PostMapping("/rating-tips")
    @PreAuthorize("hasRole('STUDENT')")
    public AiDtos.RatingTipsResponse ratingTips(@AuthenticationPrincipal AuthUser user) {
        return aiService.ratingTips(user);
    }
}
