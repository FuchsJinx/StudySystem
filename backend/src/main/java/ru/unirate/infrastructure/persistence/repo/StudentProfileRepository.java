package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.domain.model.MembershipStatus;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    @EntityGraph(attributePaths = {"user", "group", "group.faculty"})
    Optional<StudentProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "group", "group.faculty"})
    @Query("select sp from StudentProfile sp where sp.id = :id")
    Optional<StudentProfile> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sp from StudentProfile sp where sp.id = :id")
    Optional<StudentProfile> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "group"})
    List<StudentProfile> findAllByGroupIdAndMembershipStatus(Long groupId, MembershipStatus status);

    @EntityGraph(attributePaths = {"user", "group"})
    List<StudentProfile> findAllByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"user", "group", "group.faculty"})
    List<StudentProfile> findAllByMembershipStatus(MembershipStatus status);

    @EntityGraph(attributePaths = {"user", "group"})
    List<StudentProfile> findAllByGroupIdInAndMembershipStatus(List<Long> groupIds, MembershipStatus status);

    @EntityGraph(attributePaths = {"user", "group"})
    List<StudentProfile> findAllByGroupIdIn(List<Long> groupIds);

    @EntityGraph(attributePaths = {"user", "group"})
    List<StudentProfile> findAll();

    long countByMembershipStatus(MembershipStatus status);

    long countByGroupId(Long groupId);

    long countByGroupIdAndMembershipStatus(Long groupId, MembershipStatus status);
}
