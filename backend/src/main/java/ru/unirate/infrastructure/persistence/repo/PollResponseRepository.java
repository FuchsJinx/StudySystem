package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.PollResponse;

import java.util.List;
import java.util.Optional;

public interface PollResponseRepository extends JpaRepository<PollResponse, Long> {

    Optional<PollResponse> findByPollIdAndStudentProfileId(Long pollId, Long studentProfileId);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.group", "selectedOptions"})
    List<PollResponse> findByPollId(Long pollId);

    boolean existsByPollIdAndStudentProfileId(Long pollId, Long studentProfileId);

    long countByPollId(Long pollId);
}
