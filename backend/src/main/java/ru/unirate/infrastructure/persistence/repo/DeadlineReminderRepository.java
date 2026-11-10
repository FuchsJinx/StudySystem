package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.DeadlineReminder;

public interface DeadlineReminderRepository extends JpaRepository<DeadlineReminder, Long> {

    boolean existsByStudentProfileIdAndCheckpointId(Long studentProfileId, Long checkpointId);
}
