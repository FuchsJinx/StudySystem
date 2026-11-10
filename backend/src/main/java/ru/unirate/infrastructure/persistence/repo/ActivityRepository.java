package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.Activity;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Activity> findByCreatedByIdOrderByEventDateDesc(Long teacherProfileId);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Activity> findAllByGroupsIdOrderByEventDateDesc(Long groupId);

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    List<Activity> findAllByOrderByEventDateDesc();

    @EntityGraph(attributePaths = {"groups", "createdBy", "createdBy.user"})
    @Query("select a from Activity a where a.id = :id")
    Optional<Activity> findWithDetailsById(@Param("id") Long id);

    long countByCreatedById(Long teacherProfileId);

    boolean existsByTitle(String title);
}
