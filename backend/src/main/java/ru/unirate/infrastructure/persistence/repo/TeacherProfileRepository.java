package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;

import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    @EntityGraph(attributePaths = {"user", "department", "groups"})
    Optional<TeacherProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "department", "groups"})
    @Query("select tp from TeacherProfile tp where tp.id = :id")
    Optional<TeacherProfile> findWithDetailsById(@Param("id") Long id);
}
