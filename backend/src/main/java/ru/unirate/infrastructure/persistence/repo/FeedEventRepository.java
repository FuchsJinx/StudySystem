package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.FeedEvent;

import java.util.List;

public interface FeedEventRepository extends JpaRepository<FeedEvent, Long> {

    List<FeedEvent> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    List<FeedEvent> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
