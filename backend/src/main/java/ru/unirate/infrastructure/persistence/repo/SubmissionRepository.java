package ru.unirate.infrastructure.persistence.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.domain.model.SubmissionStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @EntityGraph(attributePaths = {"checkpoint", "checkpoint.createdBy", "checkpoint.createdBy.user", "studentProfile", "studentProfile.user", "studentProfile.group"})
    Optional<Submission> findByCheckpointIdAndStudentProfileId(Long checkpointId, Long studentProfileId);

    @EntityGraph(attributePaths = {"checkpoint", "checkpoint.createdBy", "checkpoint.createdBy.user"})
    List<Submission> findByStudentProfileIdOrderByCheckpointDeadlineDesc(Long studentProfileId);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group"})
    List<Submission> findByCheckpointIdOrderByStudentProfileUserFullNameAsc(Long checkpointId);

    @EntityGraph(attributePaths = {"checkpoint", "checkpoint.createdBy", "checkpoint.createdBy.user",
            "studentProfile", "studentProfile.user", "studentProfile.group"})
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Submission s set s.status = :overdue where s.status = :inProgress and s.checkpoint.deadline < :now")
    int markOverdue(@Param("inProgress") SubmissionStatus inProgress,
                    @Param("overdue") SubmissionStatus overdue,
                    @Param("now") Instant now);

    @Query("""
            select s from Submission s
            join fetch s.checkpoint c
            join fetch s.studentProfile sp
            join fetch sp.user
            where c.deadline < :now
              and s.status in :statuses
              and s.arDebited < c.cost
            order by s.id
            """)
    List<Submission> findDebitCandidates(@Param("now") Instant now,
                                         @Param("statuses") Collection<SubmissionStatus> statuses);

    @Query("""
            select s from Submission s
            join fetch s.checkpoint c
            join fetch s.studentProfile sp
            join fetch sp.user
            where s.status in :statuses
              and c.deadline > :now
              and c.deadline <= :threshold
            """)
    List<Submission> findDeadlineSoon(@Param("now") Instant now,
                                      @Param("threshold") Instant threshold,
                                      @Param("statuses") Collection<SubmissionStatus> statuses);

    long countByCheckpointId(Long checkpointId);

    long countByCheckpointIdAndStatus(Long checkpointId, SubmissionStatus status);

    long countByStudentProfileIdAndStatusIn(Long studentProfileId, Collection<SubmissionStatus> statuses);

    long countByStudentProfileIdAndStatus(Long studentProfileId, SubmissionStatus status);

    @Query("select coalesce(sum(s.arDebited), 0) from Submission s where s.checkpoint.id = :checkpointId")
    long sumDebitedByCheckpoint(@Param("checkpointId") Long checkpointId);
}
