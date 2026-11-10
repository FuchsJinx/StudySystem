package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.AwardFeedItem;

import java.util.List;

public interface AwardFeedItemRepository extends JpaRepository<AwardFeedItem, Long> {

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group"})
    List<AwardFeedItem> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group"})
    List<AwardFeedItem> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group"})
    List<AwardFeedItem> findByStudentProfileIdOrderByIdDesc(Long studentProfileId, Pageable pageable);

    long countByStudentProfileId(Long studentProfileId);
}
