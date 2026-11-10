package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;

import java.util.Optional;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);
}
