package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
