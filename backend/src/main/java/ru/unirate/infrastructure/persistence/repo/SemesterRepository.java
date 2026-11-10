package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.Semester;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findAllByOrderByStartAtDesc();

    Optional<Semester> findByActiveTrue();
}
