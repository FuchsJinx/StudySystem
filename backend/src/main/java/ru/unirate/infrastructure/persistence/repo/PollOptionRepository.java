package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.PollOption;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
}
