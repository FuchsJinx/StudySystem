package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.Poll;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Poll> findByCreatedByIdOrderByCreatedAtDesc(Long teacherProfileId);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Poll> findAllByGroupsIdOrderByEndsAtDesc(Long groupId);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Poll> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    @Query("select p from Poll p where p.id = :id")
    Optional<Poll> findWithDetailsById(@Param("id") Long id);

    List<Poll> findByEndsAtBeforeAndEndedNotifiedFalse(Instant now);

    long countByCreatedById(Long teacherProfileId);

    boolean existsByTitle(String title);
}
