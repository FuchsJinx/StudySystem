package ru.unirate.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.unirate.api.dto.ActivityDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.domain.model.ActivityType;
import ru.unirate.domain.model.PointCurrency;

import java.sql.Timestamp;
import java.util.List;

/** Atomic budget spend + read model. Budget invariant: sum per teacher/group/month <= limit. */
@Component
public class BudgetStore {

    /**
     * Conditional upsert: increments the spend only when the month limit allows it and
     * returns the new value. An exceeded budget yields no row and never mutates the ledger.
     * Concurrent awards serialize on the unique (teacher, group, month) row.
     */
    private static final String SPEND_SQL = """
            INSERT INTO budget_spends (teacher_profile_id, group_id, year_month, amount)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (teacher_profile_id, group_id, year_month)
            DO UPDATE SET amount = budget_spends.amount + EXCLUDED.amount
            WHERE budget_spends.amount + EXCLUDED.amount <= ?
            RETURNING amount
            """;

    private static final String RECENT_AWARDS_SQL = """
            SELECT a.id, u.full_name, act.title, a.points, act.type, a.created_at
            FROM activity_awards a
            JOIN student_profiles sp ON sp.id = a.student_profile_id
            JOIN users u ON u.id = sp.user_id
            JOIN activities act ON act.id = a.activity_id
            WHERE a.awarded_by = ? AND sp.group_id = ? AND to_char(a.created_at, 'YYYY-MM') = ?
            ORDER BY a.created_at DESC
            LIMIT 20
            """;

    private final JdbcTemplate jdbcTemplate;

    public BudgetStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int spendOrThrow(Long teacherProfileId, Long groupId, String yearMonth,
                            int amount, int limit, String groupName) {
        if (amount == 0) {
            return limit - currentSpent(teacherProfileId, groupId, yearMonth);
        }
        int spent = currentSpent(teacherProfileId, groupId, yearMonth);
        if (amount > limit || spent + amount > limit) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    ErrorCodes.BUDGET_EXCEEDED,
                    "Бюджет группы «" + groupName + "» на этот месяц исчерпан. "
                            + "Доступно: " + Math.max(0, limit - spent) + " баллов.");
        }
        java.util.List<Integer> rows = jdbcTemplate.query(
                SPEND_SQL, (rs, rowNum) -> rs.getInt("amount"),
                teacherProfileId, groupId, yearMonth, amount, limit);
        if (rows.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.BUDGET_EXCEEDED,
                    "Бюджет группы «" + groupName + "» на этот месяц исчерпан");
        }
        return limit - rows.get(0);
    }

    public int currentSpent(Long teacherProfileId, Long groupId, String yearMonth) {
        Integer amount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM budget_spends "
                        + "WHERE teacher_profile_id = ? AND group_id = ? AND year_month = ?",
                Integer.class, teacherProfileId, groupId, yearMonth);
        return amount == null ? 0 : amount;
    }

    public List<ActivityDtos.BudgetAwardDto> recentAwards(Long teacherProfileId, Long groupId, String yearMonth) {
        return jdbcTemplate.query(RECENT_AWARDS_SQL, (rs, rowNum) -> {
            ActivityType type = ActivityType.valueOf(rs.getString("type"));
            PointCurrency currency = type == ActivityType.ACADEMIC ? PointCurrency.BUA : PointCurrency.BOA;
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new ActivityDtos.BudgetAwardDto(
                    rs.getLong("id"),
                    rs.getString("full_name"),
                    rs.getString("title"),
                    rs.getInt("points"),
                    currency,
                    createdAt == null ? null : createdAt.toInstant()
            );
        }, teacherProfileId, groupId, yearMonth);
    }
}
