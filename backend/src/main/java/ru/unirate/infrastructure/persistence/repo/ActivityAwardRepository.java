package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.ActivityAward;

import java.util.List;
import java.util.Optional;

public interface ActivityAwardRepository extends JpaRepository<ActivityAward, Long> {

    boolean existsByActivityIdAndStudentProfileId(Long activityId, Long studentProfileId);

    Optional<ActivityAward> findByActivityIdAndStudentProfileId(Long activityId, Long studentProfileId);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group"})
    List<ActivityAward> findByActivityIdOrderByIdAsc(Long activityId);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group", "activity"})
    List<ActivityAward> findByActivityIdAndCreatedAtBetweenOrderByIdDesc(Long activityId, java.time.Instant from, java.time.Instant to, Pageable pageable);

    long countByActivityId(Long activityId);
}
