package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.EmailVerificationToken;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findTopByUserIdAndUsedAtIsNullOrderByIdDesc(Long userId);

    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
