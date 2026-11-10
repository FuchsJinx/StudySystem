package ru.unirate.application;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.dto.CheckpointDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.domain.model.UserRole;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.Checkpoint;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.CheckpointRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;
import ru.unirate.infrastructure.storage.FileStorage;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CheckpointService {

    private static final Set<SubmissionStatus> SUBMITTABLE = Set.of(
            SubmissionStatus.IN_PROGRESS,
            SubmissionStatus.OVERDUE,
            SubmissionStatus.RETURNED
    );

    private final CheckpointRepository checkpointRepository;
    private final StudentGroupRepository groupRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubmissionRepository submissionRepository;
    private final ProfileService profileService;
    private final StudentAccessService studentAccessService;
    private final CheckpointAssignmentService assignmentService;
    private final EventService eventService;
    private final SettingsService settingsService;
    private final FileStorage fileStorage;

    public CheckpointService(CheckpointRepository checkpointRepository,
                             StudentGroupRepository groupRepository,
                             StudentProfileRepository studentProfileRepository,
                             TeacherProfileRepository teacherProfileRepository,
                             SubmissionRepository submissionRepository,
                             ProfileService profileService,
                             StudentAccessService studentAccessService,
                             CheckpointAssignmentService assignmentService,
                             EventService eventService,
                             SettingsService settingsService,
                             FileStorage fileStorage) {
        this.checkpointRepository = checkpointRepository;
        this.groupRepository = groupRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.submissionRepository = submissionRepository;
        this.profileService = profileService;
        this.studentAccessService = studentAccessService;
        this.assignmentService = assignmentService;
        this.eventService = eventService;
        this.settingsService = settingsService;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public CheckpointDtos.CheckpointDetailDto create(AuthUser user, CheckpointDtos.CreateCheckpointRequest request) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        int arMax = settingsService.getInt(SettingsCatalog.AR_MAX);
        if (request.cost() > arMax) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Стоимость задачи не может превышать максимум АР (" + arMax + ")");
        }
        List<StudentGroup> groups = resolveGroups(request.groupIds());

        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTitle(request.title().trim());
        checkpoint.setDescription(request.description() == null ? "" : request.description().trim());
        checkpoint.setDeadline(request.deadline());
        checkpoint.setCost(request.cost());
        checkpoint.setCreatedBy(teacher);
        checkpoint.setGroups(new LinkedHashSet<>(groups));
        checkpointRepository.save(checkpoint);

        teacher.getGroups().addAll(groups);
        teacherProfileRepository.save(teacher);

        assignmentService.assignCheckpoint(checkpoint);
        return detail(user, checkpoint.getId());
    }

    @Transactional(readOnly = true)
    public List<CheckpointDtos.CheckpointSummaryDto> teacherCheckpoints(AuthUser user) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        return checkpointRepository.findByCreatedByIdOrderByDeadlineDesc(teacher.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckpointDtos.CheckpointDetailDto detail(AuthUser user, Long checkpointId) {
        Checkpoint checkpoint = requireCheckpoint(checkpointId);
        if (user.role() == UserRole.STUDENT) {
            return studentDetail(checkpoint, user);
        }
        requireOwnerOrAdmin(user, checkpoint);
        return teacherDetail(checkpoint);
    }

    @Transactional(readOnly = true)
    public CheckpointDtos.CheckpointDetailDto roster(AuthUser user, Long checkpointId) {
        Checkpoint checkpoint = requireCheckpoint(checkpointId);
        requireOwnerOrAdmin(user, checkpoint);
        return teacherDetail(checkpoint);
    }

    @Transactional(readOnly = true)
    public List<CheckpointDtos.StudentCheckpointDto> studentCheckpoints(AuthUser user) {
        StudentProfile profile = studentAccessService.requireStudent(user);
        List<Submission> submissions = submissionRepository
                .findByStudentProfileIdOrderByCheckpointDeadlineDesc(profile.getId());
        return submissions.stream().map(this::toStudentDto).toList();
    }

    @Transactional
    public CheckpointDtos.StudentCheckpointDto submit(AuthUser user, Long checkpointId,
                                                      String url, MultipartFile file) {
        StudentProfile student = studentAccessService.requireActiveStudent(user);
        Checkpoint checkpoint = requireCheckpoint(checkpointId);
        Submission submission = submissionRepository
                .findByCheckpointIdAndStudentProfileId(checkpointId, student.getId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND,
                        "Задание не назначено этому студенту"));
        if (!SUBMITTABLE.contains(submission.getStatus())) {
            throw ApiException.conflict(ErrorCodes.SUBMISSION_INVALID_STATUS,
                    submission.getStatus() == SubmissionStatus.ACCEPTED
                            ? "Работа уже принята преподавателем"
                            : "Работа уже отправлена на проверку");
        }
        String normalizedUrl = normalizeUrl(url);
        FileStorage.StoredFile stored = null;
        if (file != null && !file.isEmpty()) {
            stored = fileStorage.store(file);
        }
        if (normalizedUrl == null && stored == null) {
            throw ApiException.badRequest(ErrorCodes.SUBMISSION_EMPTY,
                    "Приложите ссылку на работу или файл");
        }
        if (normalizedUrl != null) {
            submission.setSubmissionUrl(normalizedUrl);
        }
        if (stored != null) {
            submission.setFilePath(stored.storedName());
            submission.setFileName(stored.originalName());
        }
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(Instant.now());
        submission.setUpdatedAt(Instant.now());
        submission.setReturnComment(null);
        submissionRepository.save(submission);

        StudentGroup group = student.getGroup();
        User teacherUser = checkpoint.getCreatedBy().getUser();
        String message = student.getUser().getFullName() + " сдал(а) работу по «"
                + checkpoint.getTitle() + "»" + (group == null ? "" : ", группа " + group.getName());
        eventService.feedAndNotify(teacherUser, FeedEventType.SUBMISSION_RECEIVED, message,
                "Работа сдана на проверку", message, "/checkpoints/" + checkpoint.getId());
        return toStudentDto(submission);
    }

    @Transactional
    public CheckpointDtos.CheckpointDetailDto accept(AuthUser user, Long checkpointId,
                                                     CheckpointDtos.AcceptRequest request) {
        Checkpoint checkpoint = requireCheckpoint(checkpointId);
        requireOwnerOrAdmin(user, checkpoint);
        List<Submission> submissions = resolveReviewSubmissions(checkpoint, request.studentIds());
        for (Submission submission : submissions) {
            submission.setStatus(SubmissionStatus.ACCEPTED);
            submission.setUpdatedAt(Instant.now());
            submissionRepository.save(submission);
            String message = "Работа по «" + checkpoint.getTitle() + "» принята";
            eventService.feedAndNotify(submission.getStudentProfile().getUser(),
                    FeedEventType.SUBMISSION_ACCEPTED, message, "Работа принята", message, "/tasks");
        }
        return teacherDetail(checkpoint);
    }

    @Transactional
    public CheckpointDtos.CheckpointDetailDto returnForRevision(AuthUser user, Long checkpointId,
                                                                CheckpointDtos.ReturnRequest request) {
        Checkpoint checkpoint = requireCheckpoint(checkpointId);
        requireOwnerOrAdmin(user, checkpoint);
        List<Submission> submissions = resolveReviewSubmissions(checkpoint, request.studentIds());
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        for (Submission submission : submissions) {
            submission.setStatus(SubmissionStatus.RETURNED);
            submission.setReturnComment(comment);
            submission.setUpdatedAt(Instant.now());
            submissionRepository.save(submission);
            String message = "Работа по «" + checkpoint.getTitle() + "» возвращена на доработку";
            String body = comment == null ? message : message + ". Комментарий: " + comment;
            eventService.feedAndNotify(submission.getStudentProfile().getUser(),
                    FeedEventType.SUBMISSION_RETURNED, message, "Работа на доработке", body, "/tasks");
        }
        return teacherDetail(checkpoint);
    }

    @Transactional(readOnly = true)
    public SubmissionFile downloadSubmissionFile(AuthUser user, Long submissionId) {
        Submission submission = submissionRepository.findWithDetailsById(submissionId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND,
                        "Работа не найдена"));
        boolean isOwnerStudent = user.role() == UserRole.STUDENT
                && submission.getStudentProfile().getUser().getId().equals(user.id());
        boolean isTeacher = user.role() == UserRole.TEACHER
                && submission.getCheckpoint().getCreatedBy().getUser().getId().equals(user.id());
        if (!isOwnerStudent && !isTeacher && !user.isAdmin()) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Нет доступа к этому файлу");
        }
        if (submission.getFilePath() == null) {
            throw ApiException.notFound(ErrorCodes.NOT_FOUND, "К работе не приложен файл");
        }
        Resource resource = fileStorage.load(submission.getFilePath());
        String filename = submission.getFileName() == null ? "submission" : submission.getFileName();
        return new SubmissionFile(resource, filename);
    }

    private List<Submission> resolveReviewSubmissions(Checkpoint checkpoint, List<Long> studentUserIds) {
        List<Submission> result = new ArrayList<>();
        for (Long studentUserId : new LinkedHashSet<>(studentUserIds)) {
            StudentProfile profile = studentProfileRepository.findByUserId(studentUserId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND,
                            "Студент не найден: " + studentUserId));
            Submission submission = submissionRepository
                    .findByCheckpointIdAndStudentProfileId(checkpoint.getId(), profile.getId())
                    .orElseThrow(() -> ApiException.badRequest(ErrorCodes.SUBMISSION_NOT_FOUND,
                            "У студента " + profile.getUser().getFullName() + " нет назначенной задачи"));
            if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
                throw ApiException.badRequest(ErrorCodes.SUBMISSION_INVALID_STATUS,
                        "Работа студента " + profile.getUser().getFullName()
                                + " не находится на проверке (статус: " + submission.getStatus() + ")");
            }
            result.add(submission);
        }
        if (result.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Не выбрано ни одной работы");
        }
        return result;
    }

    private CheckpointDtos.CheckpointDetailDto teacherDetail(Checkpoint checkpoint) {
        List<Submission> submissions = submissionRepository
                .findByCheckpointIdOrderByStudentProfileUserFullNameAsc(checkpoint.getId());
        List<CheckpointDtos.RosterRowDto> roster = submissions.stream().map(this::toRosterRow).toList();
        return new CheckpointDtos.CheckpointDetailDto(
                checkpoint.getId(),
                checkpoint.getTitle(),
                checkpoint.getDescription(),
                checkpoint.getDeadline(),
                checkpoint.getCost(),
                toGroups(checkpoint),
                checkpoint.getCreatedAt(),
                checkpoint.getCreatedBy().getUser().getFullName(),
                (long) submissions.size(),
                submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED).count(),
                submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED).count(),
                submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.OVERDUE).count(),
                roster,
                null
        );
    }

    private CheckpointDtos.CheckpointDetailDto studentDetail(Checkpoint checkpoint, AuthUser user) {
        StudentProfile profile = studentAccessService.requireStudent(user);
        if (!profile.isConfirmed()) {
            throw ApiException.forbidden(ErrorCodes.GROUP_NOT_CONFIRMED,
                    "Принадлежность к группе ещё не подтверждена");
        }
        Submission submission = submissionRepository
                .findByCheckpointIdAndStudentProfileId(checkpoint.getId(), profile.getId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND,
                        "Задание не назначено этому студенту"));
        return new CheckpointDtos.CheckpointDetailDto(
                checkpoint.getId(),
                checkpoint.getTitle(),
                checkpoint.getDescription(),
                checkpoint.getDeadline(),
                checkpoint.getCost(),
                toGroups(checkpoint),
                checkpoint.getCreatedAt(),
                checkpoint.getCreatedBy().getUser().getFullName(),
                null,
                null,
                null,
                null,
                null,
                toStudentDto(submission)
        );
    }

    private CheckpointDtos.CheckpointSummaryDto toSummary(Checkpoint checkpoint) {
        return new CheckpointDtos.CheckpointSummaryDto(
                checkpoint.getId(),
                checkpoint.getTitle(),
                checkpoint.getDescription(),
                checkpoint.getDeadline(),
                checkpoint.getCost(),
                toGroups(checkpoint),
                checkpoint.getCreatedAt(),
                checkpoint.getCreatedBy().getUser().getFullName(),
                submissionRepository.countByCheckpointId(checkpoint.getId()),
                submissionRepository.countByCheckpointIdAndStatus(checkpoint.getId(), SubmissionStatus.SUBMITTED),
                submissionRepository.countByCheckpointIdAndStatus(checkpoint.getId(), SubmissionStatus.ACCEPTED),
                submissionRepository.countByCheckpointIdAndStatus(checkpoint.getId(), SubmissionStatus.OVERDUE)
        );
    }

    private CheckpointDtos.RosterRowDto toRosterRow(Submission submission) {
        return new CheckpointDtos.RosterRowDto(
                submission.getStudentProfile().getUser().getId(),
                submission.getStudentProfile().getId(),
                submission.getStudentProfile().getUser().getFullName(),
                submission.getStudentProfile().getGroup().getName(),
                submission.getStatus(),
                submission.getArDebited(),
                submission.getCheckpoint().getCost(),
                submission.getSubmittedAt(),
                submission.getSubmissionUrl(),
                submission.getId(),
                submission.getFileName(),
                submission.getReturnComment()
        );
    }

    private CheckpointDtos.StudentCheckpointDto toStudentDto(Submission submission) {
        return new CheckpointDtos.StudentCheckpointDto(
                submission.getId(),
                submission.getCheckpoint().getId(),
                submission.getCheckpoint().getTitle(),
                submission.getCheckpoint().getDescription(),
                submission.getCheckpoint().getDeadline(),
                submission.getCheckpoint().getCost(),
                submission.getStatus(),
                submission.getArDebited(),
                submission.getSubmittedAt(),
                submission.getSubmissionUrl(),
                submission.getFileName(),
                submission.getReturnComment(),
                submission.getCheckpoint().getCreatedBy().getUser().getFullName()
        );
    }

    private List<CatalogDtos.GroupRef> toGroups(Checkpoint checkpoint) {
        return checkpoint.getGroups().stream()
                .map(group -> new CatalogDtos.GroupRef(group.getId(), group.getName()))
                .toList();
    }

    private List<StudentGroup> resolveGroups(List<Long> groupIds) {
        List<StudentGroup> groups = new ArrayList<>();
        for (Long groupId : new LinkedHashSet<>(groupIds)) {
            groups.add(groupRepository.findById(groupId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND,
                            "Группа не найдена: " + groupId)));
        }
        return groups;
    }

    private Checkpoint requireCheckpoint(Long checkpointId) {
        return checkpointRepository.findWithDetailsById(checkpointId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CHECKPOINT_NOT_FOUND,
                        "Задание не найдено"));
    }

    private void requireOwnerOrAdmin(AuthUser user, Checkpoint checkpoint) {
        if (user.isAdmin()) {
            return;
        }
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        if (!checkpoint.getCreatedBy().getId().equals(teacher.getId())) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Это задание создано другим преподавателем");
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.length() > 2048) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Ссылка слишком длинная");
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                        "Ссылка должна начинаться с http:// или https://");
            }
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Некорректная ссылка");
        }
        return trimmed;
    }

    public record SubmissionFile(Resource resource, String filename) {
    }
}
