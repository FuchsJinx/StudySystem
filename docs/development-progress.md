# UniRate — план и прогресс разработки

Файл для продолжения работы между сессиями. Обновлён после завершения реализации.

## Итоговый статус

Все 11 модулей реализованы, интегрированы и проверены. Проект запускается одной командой
`docker compose up --build`, демо-данные создаются автоматически.

| # | Модуль | Состояние | Проверка |
|---|--------|-----------|----------|
| 1 | Регистрация, авторизация, профили, ledger | ✅ | AuthIntegrationTest, UI |
| 2 | Академический рейтинг, задания, проверка, автосписание | ✅ | RatingIntegrationTest (11), E2E |
| 3 | БУА/БОА, активности, бюджеты | ✅ | ActivityIntegrationTest (13), E2E |
| 4 | Лента наград | ✅ | ActivityIntegrationTest, E2E |
| 5 | Опросы и голосования | ✅ | PollIntegrationTest (9), E2E |
| 6 | Персональная лента, напоминания | ✅ | FeedIntegrationTest (4), E2E |
| 7 | Email/группа, подтверждение | ✅ | Auth/Membership tests, UI |
| 8 | Админ-панель, импорт CSV/Excel, семестры | ✅ | AdminIntegrationTest (8), E2E |
| 9 | Уведомления in-app/email/Telegram | ✅ | NotificationIntegrationTest, Mailhog |
| 10 | Аналитика и статистика | ✅ | AnalyticsIntegrationTest (5), E2E |
| 11 | ИИ-функции с fallback | ✅ | AiIntegrationTest, E2E |

## Аудит исходного состояния (2026-11-10)

| Что | Было |
|-----|------|
| Git | ветка `main`, один коммит `Initialize repo` |
| Backend | каркас: health, `SecurityConfig` (httpBasic), `RatingEngine`, `BudgetPolicy`, пустой `ArDeductionJob` |
| БД | `V1__init.sql`, `V2__seed.sql` |
| Frontend | 2 страницы, без авторизации и API-клиента |
| Тесты | 6 unit-тестов |
| Docker | compose с db/backend/worker/frontend/mailhog без логики worker |

## Архитектурные решения

1. **Слои**: `api` → `application` → `domain` → `infrastructure`; доменные правила АР/бюджета — чистые классы с unit-тестами.
2. **JWT HS256**; claims `uid/role/sid/tid`; фильтр проверяет пользователя в БД. RBAC — `@PreAuthorize` + правила в сервисах.
3. **Ошибки API** единого формата `{error:{code,message,details}}`; русские сообщения.
4. **Email-верификация**: UUID-токен, TTL 24 ч, Mailhog в dev; login разрешён, мутации закрыты до подтверждения.
5. **Группа**: `PENDING → CONFIRMED/REJECTED`; подтверждает преподаватель группы, староста или admin; при подтверждении назначаются актуальные будущие checkpoint-ы.
6. **Списание АР**: worker-профиль, интервал из `app_settings.arDeductIntervalSec`, внутренний тик 1 с, pessimistic lock на submission/student, журнал в `point_ledger`.
7. **Бюджет**: атомарный PostgreSQL upsert `INSERT … ON CONFLICT … WHERE amount + EXCLUDED <= limit`; превышение не изменяет строку и возвращает 400 `BUDGET_EXCEEDED`.
8. **Ленты**: `award_feed_items` (публичная, только БУА/БОА) и `feed_events` (персональная, keyset-пагинация по id).
9. **Уведомления**: in-app всегда, внешние каналы асинхронно после commit; Telegram без токена — mock с логом; email через Mailhog.
10. **ИИ**: OpenAI-совместимый клиент при ключе; без ключа — детерминированный fallback `provider=demo`; ошибка настроенного LLM → 503 `AI_UNAVAILABLE`.
11. **Файлы**: `UPLOADS_DIR`, UUID-имена, whitelist pdf/zip/doc/docx/png, ≤10 МБ, проверка прав при скачивании.
12. **Демо-данные**: миграции V1–V3 + `DemoDataSeeder` с датами относительно старта, идемпотентно, отключается env и не работает в worker/тестах.
13. **Настройки** только через `app_settings`; дефолты по spec 10.
14. **Тесты интеграции** — отдельная БД `unirate_test`, чтобы демо/E2E-данные не влияли на assertions.

## Решения по неоднозначностям spec

| Вопрос | Решение |
|--------|---------|
| Повторная награда за активность | 409 `AWARD_ALREADY_EXISTS` |
| Повторный голос | 409 `ALREADY_VOTED`, изменение запрещено |
| Accept/Return | только для `SUBMITTED`; batch атомарный |
| Подача работы | хотя бы ссылка или файл; оба разрешены |
| Импорт студентов | доверенный: email verified, membership CONFIRMED |
| Публичный профиль | AR/БУА/БОА/группа видны всем auth; ledger и задания — self/teacher/admin |
| Семестровый бонус | `min(arMax, AR+bonus)`, ledger только при delta>0 |
| Кто подтверждает группу | teacher группы, староста группы, admin |
| Привязка teacher↔group | автоматически при создании checkpoint/activity |
| Анонимный опрос | преподаватель видит только агрегаты; ответы хранятся для защиты от повторного голоса |
| AI без ключа | demo-fallback, честно помечается в UI |

## Git-история (основные ветки)

`main` → `develop` → тематические ветки: `feature/auth-profiles`, `feature/academic-rating`,
`feature/activity-budget`, `feature/polls`, `feature/personal-feed`, `feature/admin-panel`,
`feature/notifications-analytics-ai`. Интеграция — merge --no-ff в `develop`.

Ключевые коммиты: `feat(infra)`, `feat(security)`, `feat(auth)`, `feat(rating)`, `feat(activity)`,
`feat(feed)`, `feat(polls)`, `feat(admin)`, `feat(notifications)`, `feat(analytics)`, `feat(ai)`,
`test(...)`, `docs(...)`, `ci: ...`.

## Проверки (финальные)

| Проверка | Результат |
|----------|-----------|
| `mvn test` (unit + integration, PostgreSQL) | **88 passed, 0 failed** |
| `npm test` (Vitest) | **13 passed** |
| `npm run build` | OK |
| `npm run test:e2e` против Docker-стека | **8 passed** |
| `docker compose up --build` | стек healthy: frontend :80, backend :8080, db, mailhog, worker |
| Worker `ArDeductionJob` | списание 20 АР за тик, остановка на cost; логи INFO |
| Mailhog | письма верификации и уведомлений доставлены |
| Адаптив | проверен скриншотами 1440px и 390px |
| CI | настроены backend/frontend/docker+E2E job-ы |

## Известные ограничения

- Реальный LLM и Telegram требуют ключей в env; без них — честный demo/mock режим.
- Файлы хранятся в локальном Docker volume, без S3.
- Нет интеграции с реальной АСУ вуза (вне scope MVP).
- Презентация и QA-слайд к защите — вне репозитория; сценарий демо: `docs/demo-script.md`.

## Следующие действия (защита)

1. `docker compose down -v && docker compose up -d --build` — чистые демо-данные.
2. Пройти `docs/demo-script.md`.
3. Проверить `docs/qa/acceptance-checklist.md` (база 100%).
4. Подготовить слайд с архитектурой/агентским процессом и скриншоты.
