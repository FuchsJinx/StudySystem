package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.infrastructure.persistence.entity.Checkpoint;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.repo.CheckpointRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Assigns checkpoints to confirmed students (spec 03 / module 7). */
@Service
public class CheckpointAssignmentService {

    private final CheckpointRepository checkpointRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SubmissionRepository submissionRepository;
    private final EventService eventService;

    public CheckpointAssignmentService(CheckpointRepository checkpointRepository,
                                       StudentProfileRepository studentProfileRepository,
                                       SubmissionRepository submissionRepository,
                                       EventService eventService) {
        this.checkpointRepository = checkpointRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.submissionRepository = submissionRepository;
        this.eventService = eventService;
    }

    @Transactional
    public int assignCheckpoint(Checkpoint checkpoint) {
        Set<StudentProfile> targets = new HashSet<>();
        for (StudentGroup group : checkpoint.getGroups()) {
            targets.addAll(studentProfileRepository.findAllByGroupIdAndMembershipStatus(
                    group.getId(), MembershipStatus.CONFIRMED));
        }
        Set<Long> existing = new HashSet<>();
        submissionRepository.findByCheckpointIdOrderByStudentProfileUserFullNameAsc(checkpoint.getId())
                .forEach(submission -> existing.add(submission.getStudentProfile().getId()));

        int created = 0;
        for (StudentProfile profile : targets) {
            if (existing.contains(profile.getId())) {
                continue;
            }
            Submission submission = new Submission();
            submission.setCheckpoint(checkpoint);
            submission.setStudentProfile(profile);
            submission.setStatus(SubmissionStatus.IN_PROGRESS);
            submissionRepository.save(submission);
            notifyAssignment(profile, checkpoint);
            created++;
        }
        return created;
    }

    /**
     * Called when a pending student becomes CONFIRMED: every still-relevant checkpoint
     * of the group is assigned immediately (E2E-04).
     */
    @Transactional
    public int assignUpcomingToStudent(StudentProfile profile) {
        if (!profile.isConfirmed()) {
            return 0;
        }
        List<Checkpoint> checkpoints = checkpointRepository
                .findAllByGroupsIdOrderByDeadlineDesc(profile.getGroup().getId());
        Instant now = Instant.now();
        int created = 0;
        for (Checkpoint checkpoint : checkpoints) {
            if (checkpoint.getDeadline().isBefore(now)) {
                continue;
            }
            if (submissionRepository.findByCheckpointIdAndStudentProfileId(
                    checkpoint.getId(), profile.getId()).isPresent()) {
                continue;
            }
            Submission submission = new Submission();
            submission.setCheckpoint(checkpoint);
            submission.setStudentProfile(profile);
            submission.setStatus(SubmissionStatus.IN_PROGRESS);
            submissionRepository.save(submission);
            notifyAssignment(profile, checkpoint);
            created++;
        }
        return created;
    }

    private void notifyAssignment(StudentProfile profile, Checkpoint checkpoint) {
        String message = "Преподаватель назначил задачу «" + checkpoint.getTitle() + "» вашей группе";
        String body = message + ". Дедлайн: " + DateFormats.deadline(checkpoint.getDeadline())
                + ". Максимальное списание: " + checkpoint.getCost() + " АР.";
        eventService.feedAndNotify(profile.getUser(), FeedEventType.CHECKPOINT_ASSIGNED,
                message, "Новая задача", body, "/tasks");
    }
}
