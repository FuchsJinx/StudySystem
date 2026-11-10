package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.PollQuestion;

public interface PollQuestionRepository extends JpaRepository<PollQuestion, Long> {
}
