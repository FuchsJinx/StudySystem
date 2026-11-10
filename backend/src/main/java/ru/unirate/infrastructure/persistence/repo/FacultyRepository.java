package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.Faculty;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
