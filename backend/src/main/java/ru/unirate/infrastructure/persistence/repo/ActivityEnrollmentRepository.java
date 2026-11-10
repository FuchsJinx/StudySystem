package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.ActivityEnrollment;

import java.util.List;
import java.util.Optional;

public interface ActivityEnrollmentRepository extends JpaRepository<ActivityEnrollment, Long> {

    boolean existsByActivityIdAndStudentProfileId(Long activityId, Long studentProfileId);

    Optional<ActivityEnrollment> findByActivityIdAndStudentProfileId(Long activityId, Long studentProfileId);

    List<ActivityEnrollment> findByActivityId(Long activityId);

    long countByActivityId(Long activityId);
}
