package ru.unirate.domain.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Single source of truth for all configurable mechanics (spec 10). */
public final class SettingsCatalog {

    private SettingsCatalog() {
    }

    public static final String SEMESTER_BONUS_AR = "semesterBonusAR";
    public static final String AR_MAX = "arMax";
    public static final String AR_START_ON_REGISTER = "arStartOnRegister";
    public static final String AR_DEDUCT_INTERVAL_SEC = "arDeductIntervalSec";
    public static final String AR_DEDUCT_AMOUNT = "arDeductAmount";
    public static final String TEACHER_MONTHLY_BUDGET_PER_GROUP = "teacherMonthlyBudgetPerGroup";
    public static final String DEADLINE_REMINDER_HOURS = "deadlineReminderHours";

    public static final List<SettingDefinition> ALL = List.of(
            new SettingDefinition(SEMESTER_BONUS_AR, "Бонус АР за семестр",
                    "Начисление академического рейтинга в начале семестра", "200", 0, 1000, "АР"),
            new SettingDefinition(AR_MAX, "Максимум АР",
                    "Верхняя граница академического рейтинга", "1000", 100, 10000, "АР"),
            new SettingDefinition(AR_START_ON_REGISTER, "Стартовый АР",
                    "Академический рейтинг при регистрации студента", "1000", 0, 10000, "АР"),
            new SettingDefinition(AR_DEDUCT_INTERVAL_SEC, "Интервал списания АР",
                    "Период фоновой проверки просроченных работ", "60", 1, 86400, "сек"),
            new SettingDefinition(AR_DEDUCT_AMOUNT, "Размер списания АР",
                    "Сколько АР списывается за один тик", "20", 1, 1000, "АР"),
            new SettingDefinition(TEACHER_MONTHLY_BUDGET_PER_GROUP, "Бюджет на группу",
                    "Месячный лимит баллов активности на каждую группу", "1000", 0, 100000, "баллов"),
            new SettingDefinition(DEADLINE_REMINDER_HOURS, "Напоминание о дедлайне",
                    "За сколько часов до дедлайна отправлять напоминание", "24", 1, 720, "ч")
    );

    private static final Map<String, SettingDefinition> BY_KEY;

    static {
        Map<String, SettingDefinition> map = new LinkedHashMap<>();
        ALL.forEach(definition -> map.put(definition.key(), definition));
        BY_KEY = Map.copyOf(map);
    }

    public static Map<String, SettingDefinition> byKey() {
        return BY_KEY;
    }

    public static Optional<SettingDefinition> find(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static String defaultValue(String key) {
        return BY_KEY.get(key).defaultValue();
    }

    public static int defaultInt(String key) {
        return Integer.parseInt(defaultValue(key));
    }
}
