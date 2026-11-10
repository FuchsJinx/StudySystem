package ru.unirate.api;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.CheckpointService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final CheckpointService checkpointService;

    public FileController(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<Resource> submissionFile(@AuthenticationPrincipal AuthUser user,
                                                   @PathVariable Long submissionId) {
        CheckpointService.SubmissionFile file = checkpointService.downloadSubmissionFile(user, submissionId);
        MediaType mediaType = MediaTypeFactory.getMediaType(file.filename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
