# Спецификация — оглавление

Источник правды для разработки и агентов. Пишется **до** основной разработки; код следует за spec.

## База (модули 1–4)

| Файл | Содержание |
|------|------------|
| [01-overview.md](01-overview.md) | Цель, scope, роли, критерии |
| [02-domain.md](02-domain.md) | Сущности и связи |
| [03-rating-rules.md](03-rating-rules.md) | Академический рейтинг (ядро) |
| [04-activity-rules.md](04-activity-rules.md) | БУА / БОА |
| [05-api.md](05-api.md) | Контракт API (макет) |
| [06-architecture.md](06-architecture.md) | Слои, Docker, jobs |
| [07-ui-flows.md](07-ui-flows.md) | Сценарии + wireframe-макеты |
| [08-tasks-breakdown.md](08-tasks-breakdown.md) | Задачи и DoD |
| [10-settings.md](10-settings.md) | Ключи настроек и дефолты |
| [18-database.md](18-database.md) | ER + таблицы PostgreSQL |
| [19-user-flows.md](19-user-flows.md) | UC + Mermaid sequence/state |
| [20-cicd.md](20-cicd.md) | GitHub Actions CI/CD |

## Опции (модули 5–11) — все в scope

| Файл | Модуль |
|------|--------|
| [11-polls.md](11-polls.md) | 5. Опросы |
| [12-personal-feed.md](12-personal-feed.md) | 6. Персональная лента |
| [13-registration-confirm.md](13-registration-confirm.md) | 7. Подтверждение регистрации |
| [14-admin.md](14-admin.md) | 8. Администрирование |
| [15-notifications.md](15-notifications.md) | 9. Уведомления |
| [16-analytics.md](16-analytics.md) | 10. Аналитика |
| [17-ai-feature.md](17-ai-feature.md) | 11. ИИ в продукте |

## Правила изменения

1. Сначала правки в spec → потом код и тесты.
2. Противоречие между файлами: побеждает более специфичный (rules > overview).
3. Открытые решения помечены `⚠️ РЕШЕНИЕ`.
