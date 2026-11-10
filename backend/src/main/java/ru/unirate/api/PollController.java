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
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.PollDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.PollService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/polls")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<PollDtos.PollSummaryDto> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody PollDtos.CreatePollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pollService.create(user, request));
    }

    @GetMapping
    public List<PollDtos.PollSummaryDto> list(@AuthenticationPrincipal AuthUser user) {
        return pollService.list(user);
    }

    @GetMapping("/{id}")
    public PollDtos.PollDetailDto detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return pollService.detail(user, id);
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("hasRole('STUDENT')")
    public PollDtos.PollResultsDto vote(@AuthenticationPrincipal AuthUser user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody PollDtos.VoteRequest request) {
        return pollService.vote(user, id, request);
    }

    @GetMapping("/{id}/results")
    public PollDtos.PollResultsDto results(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return pollService.results(user, id);
    }
}
