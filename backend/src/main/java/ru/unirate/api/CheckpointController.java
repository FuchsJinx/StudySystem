package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.unirate.api.dto.CheckpointDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.CheckpointService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkpoints")
public class CheckpointController {

    private final CheckpointService checkpointService;

    public CheckpointController(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CheckpointDtos.CheckpointDetailDto> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CheckpointDtos.CreateCheckpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkpointService.create(user, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<CheckpointDtos.CheckpointSummaryDto> list(@AuthenticationPrincipal AuthUser user) {
        return checkpointService.teacherCheckpoints(user);
    }

    @GetMapping("/{id}")
    public CheckpointDtos.CheckpointDetailDto detail(@AuthenticationPrincipal AuthUser user,
                                                     @PathVariable Long id) {
        return checkpointService.detail(user, id);
    }

    @GetMapping("/{id}/roster")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public CheckpointDtos.CheckpointDetailDto roster(@AuthenticationPrincipal AuthUser user,
                                                     @PathVariable Long id) {
        return checkpointService.roster(user, id);
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public CheckpointDtos.StudentCheckpointDto submitJson(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody CheckpointDtos.SubmitRequest request) {
        return checkpointService.submit(user, id, request.url(), null);
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public CheckpointDtos.StudentCheckpointDto submitMultipart(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @RequestParam(value = "url", required = false) String url,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return checkpointService.submit(user, id, url, file);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public CheckpointDtos.CheckpointDetailDto accept(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody CheckpointDtos.AcceptRequest request) {
        return checkpointService.accept(user, id, request);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public CheckpointDtos.CheckpointDetailDto returnForRevision(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody CheckpointDtos.ReturnRequest request) {
        return checkpointService.returnForRevision(user, id, request);
    }
}
