package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.Checkpoint;

import java.util.List;
import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Checkpoint> findByCreatedByIdOrderByDeadlineDesc(Long teacherProfileId);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    @Query("select c from Checkpoint c where c.id = :id")
    Optional<Checkpoint> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Checkpoint> findAllByGroupsIdOrderByDeadlineDesc(Long groupId);

    long countByCreatedById(Long teacherProfileId);
}
