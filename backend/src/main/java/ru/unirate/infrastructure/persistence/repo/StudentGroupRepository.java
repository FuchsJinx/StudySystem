package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    @EntityGraph(attributePaths = "faculty")
    List<StudentGroup> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "faculty")
    List<StudentGroup> findAllByFacultyIdOrderByNameAsc(Long facultyId);

    Optional<StudentGroup> findByFacultyIdAndNameIgnoreCase(Long facultyId, String name);

    boolean existsByFacultyId(Long facultyId);

    long countByFacultyId(Long facultyId);
}
