package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.domain.model.PointCurrency;

import java.time.Instant;
import java.util.List;

public interface PointLedgerRepository extends JpaRepository<PointLedgerEntry, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<PointLedgerEntry> findByStudentProfileIdOrderByIdDesc(Long studentProfileId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor"})
    List<PointLedgerEntry> findByStudentProfileIdAndIdLessThanOrderByIdDesc(Long studentProfileId, Long cursor, Pageable pageable);

    @Query("select coalesce(sum(l.amount), 0) from PointLedgerEntry l where l.studentProfile.id = :studentId and l.currency = :currency and l.amount > 0")
    long sumCredits(@Param("studentId") Long studentId, @Param("currency") PointCurrency currency);

    @Query("""
            select l from PointLedgerEntry l
            where l.currency = :currency and l.createdAt >= :from
            order by l.createdAt asc
            """)
    List<PointLedgerEntry> findSince(@Param("currency") PointCurrency currency, @Param("from") Instant from);

    @Query("""
            select l from PointLedgerEntry l
            where l.currency = :currency and l.studentProfile.id = :studentId and l.createdAt > :after
            """)
    List<PointLedgerEntry> findAfter(@Param("currency") PointCurrency currency,
                                     @Param("studentId") Long studentId,
                                     @Param("after") Instant after);

    @Query("""
            select l from PointLedgerEntry l
            join fetch l.studentProfile sp
            where sp.group.id = :groupId and l.currency = :currency and l.createdAt >= :from
            order by l.createdAt asc
            """)
    List<PointLedgerEntry> findForGroupSince(@Param("groupId") Long groupId,
                                             @Param("currency") PointCurrency currency,
                                             @Param("from") Instant from);
}
