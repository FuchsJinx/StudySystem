package ru.unirate.domain.ai;

/** Versioned prompt templates. No passwords, tokens or personal data are included. */
public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static final String DRAFT_SYSTEM = """
            Ты — методист университета. Составь понятное описание контрольной точки
            для студентов на русском языке. Структура: цель работы, что нужно сделать,
            требования к результату и критерии оценки. Не выдумывай факты, которых нет
            в тезисах. Объём — 120–220 слов. Верни только текст описания.
            """;

    public static final String POLL_SUMMARY_SYSTEM = """
            Ты — аналитик университета. На основе агрегированных результатов опроса
            сделай краткую сводку на русском языке: сколько ответов получено, какие
            варианты лидируют, какой вывод можно сделать. Не добавляй персональные данные
            и не выдумывай числа. Объём — 80–160 слов. Верни только текст сводки.
            """;

    public static final String RATING_TIPS_SYSTEM = """
            Ты — наставник студента. На основе агрегированных показателей профиля
            (академический рейтинг, баллы активности, просрочки) дай 3–5 конкретных
            советов на русском языке, как улучшить рейтинг. Каждый совет — одна строка
            без нумерации. Не упоминай персональные данные и не гарантируй результат.
            Верни только список советов.
            """;

    public static String draftCheckpoint(String title, String theses) {
        return "Название контрольной точки: " + title + "\nТезисы преподавателя:\n" + theses;
    }

    public static String pollSummary(String aggregateText) {
        return "Агрегированные результаты опроса:\n" + aggregateText;
    }

    public static String ratingTips(int academicRating, long bua, long boa,
                                    long overdue, long activeTasks, String nearestDeadline) {
        return """
                Академический рейтинг: %d из 1000
                Баллы учебной активности (БУА): %d
                Баллы общественной активности (БОА): %d
                Просроченных заданий: %d
                Активных заданий: %d
                Ближайший дедлайн: %s
                """.formatted(academicRating, bua, boa, overdue, activeTasks,
                nearestDeadline == null ? "нет" : nearestDeadline);
    }
}
