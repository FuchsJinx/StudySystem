package ru.unirate.infrastructure.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.unirate.infrastructure.persistence.entity.BudgetSpend;

import java.util.List;
import java.util.Optional;

public interface BudgetSpendRepository extends JpaRepository<BudgetSpend, Long> {

    Optional<BudgetSpend> findByTeacherIdAndGroupIdAndYearMonth(Long teacherId, Long groupId, String yearMonth);

    List<BudgetSpend> findByTeacherIdAndYearMonth(Long teacherId, String yearMonth);

    List<BudgetSpend> findByTeacherIdOrderByYearMonthDesc(Long teacherId);
}
