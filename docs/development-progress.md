# UniRate — план и прогресс разработки

Файл для продолжения работы между сессиями. Обновляется после каждого этапа.

## Аудит исходного состояния (2026-11-10)

| Что | Состояние |
|-----|-----------|
| Git | ветка `main`, один коммит `Initialize repo` |
| Backend | каркас: `UnirateApplication`, `/api/v1/health`, `SecurityConfig` (httpBasic), `RatingEngine`, `BudgetPolicy`, пустой `ArDeductionJob` |
| БД | `V1__init.sql` (полная схема ядра), `V2__seed.sql` (справочники, demo users, settings) |
| Frontend | каркас Vite: 2 страницы, без авторизации и API-клиента |
| Тесты | 6 unit-тестов (`RatingEngineTest`, `BudgetPolicyTest`) |
| Docker | compose с db/backend/worker/frontend/mailhog; worker без логики |
| Документация | полная (`docs/spec`, `docs/qa`, `AGENTS.md`) |

**Вывод:** реализованы только A1 (частично) и A2/A3-заготовки. Требуется реализация модулей 1–11 целиком (backend + frontend + тесты).

## Архитектурные решения (фиксируются здесь)

1. **Слои**: `api` (controllers, DTO, error, security) → `application` (use-cases) → `domain` (чистые правила) → `infrastructure` (JPA, jobs, mail, LLM, storage). Существующие `RatingEngine`/`BudgetPolicy` сохраняются и расширяются.
2. **JWT HS256**, `Authorization: Bearer`. В токене `uid`, `role`, `sid` (studentProfileId), `tid` (teacherProfileId). Фильтр проверяет подпись и существование пользователя.
3. **Ошибки API**: `{"error":{"code":"...","message":"русский текст"}}`; глобальный handler для validation/RBAC/500.
4. **Пароль**: BCrypt. Требование ≥ 8 символов.
5. **Email**: Mailhog/SMTP. Токен верификации — UUID в `email_verification_tokens`, TTL 24 ч. Login до подтверждения разрешён (banner в UI), мутации заданий/баллов/голосования — 403 `EMAIL_NOT_VERIFIED`.
6. **Группа**: регистрация → `PENDING`. Подтверждает преподаватель группы или староста (или admin). До `CONFIRMED` новые checkpoint/награды не назначаются. При подтверждении назначаются актуальные (дедлайн в будущем) checkpoint-ы группы.
7. **Списание АР**: отдельный `ArDeductionJob` (profile `worker`), интервал читается из `app_settings.arDeductIntervalSec`; внутренний tick 1 c, фактический запуск не чаще интервала. Транзакция + pessimistic lock на submission и student profile. Повторный тик — штатное списание (не дубль), двойной параллельный запуск защищён блокировками.
8. **Бюджет**: атомарный PostgreSQL upsert с условием в `budget_spends`; проверка per teacher × group × календарный месяц; валюта — сумма начисленных БУА+БОА.
9. **Ленты**: `award_feed_items` (публичная, только начисления) и `feed_events` (персональная, текст рендерится при записи, поле `message`). Курсор — `id` (keyset пагинация).
10. **Уведомления**: in-app всегда; email/telegram — по настройкам пользователя, отправка асинхронно и не ломает бизнес-транзакцию. Telegram без токена — mock-транспорт с логом.
11. **AI**: OpenAI-совместимый клиент при заданном `LLM_API_KEY`+`LLM_BASE_URL`; без ключа — детерминированный demo-fallback (`provider=demo`), ошибка настроенного LLM → 503 `AI_UNAVAILABLE`. Промпты — в `domain/ai/PromptTemplates`, ПДн не отправляются.
12. **Файлы**: каталог `UPLOADS_DIR`, имена UUID, whitelist pdf/zip/doc/docx/png, ≤10 МБ; скачивание только owner/teacher/admin через контроллер с проверкой прав.
13. **Демо-данные**: миграции V1/V2 (справочники, пользователи, settings) + V3 (расширения схемы) + `DemoDataSeeder` (checkpoint/activity/poll с датами относительно запуска, идемпотентно; отключается `UNIRATE_DEMO_SEED=false` и в тестах).
14. **Настройки** — только через `app_settings`, без хардкода в сервисах; дефолты из spec 10.

## Решения по неоднозначностям spec

| Вопрос | Решение |
|--------|---------|
| Повторная награда за одну активность | запрещена, 409 `AWARD_ALREADY_EXISTS` (идемпотентность баллов) |
| Повторный голос | 409 `ALREADY_VOTED`, изменение запрещено |
| Accept/Return | только для `SUBMITTED`; batch атомарный |
| Подача работы: url и файл | разрешены оба, но хотя бы один обязателен |
| Import students | admin-импорт считается доверенным: email verified, membership CONFIRMED |
| Публичный профиль | AR/БУА/БОА/группа/кафедра видны всем auth; ledger и задания — только self/teacher/admin |
| Семестровый бонус | `min(arMax, AR+bonus)`, запись в ledger только при delta>0 |
| Чужие баллы | студент видит только свои начисления в ledger; лента наград публична по ТЗ |
| Кто подтверждает группу | teacher, привязанный к группе, староста этой группы, admin |
| Привязка teacher↔group | автоматически при создании checkpoint/activity на группу |

## Этапы (ветки от develop)

| # | Ветка | Модули | Критерий готовности |
|---|-------|--------|---------------------|
| 0 | `docs/implementation-plan` | — | план, API-контракт, решения зафиксированы |
| 1 | `feature/auth-profiles` | 1, 7 (email/группа) + каркас | register/login/JWT/RBAC, профиль, ledger, verify, confirm; integration tests |
| 2 | `feature/academic-rating` | 2 | checkpoint CRUD, submit/accept/return, job списания, UI W3–W4, тесты AR-01…20 |
| 3 | `feature/activity-budget` | 3, 4 | активности, enroll, award %, бюджет, лента наград, UI W5–W6, тесты AC/FD |
| 4 | `feature/polls` | 5 | опросы SINGLE/MULTIPLE/анонимность, UI W8, тесты PL |
| 5 | `feature/personal-feed` | 6 | события student/teacher, дедлайн-джоба, UI W7, тесты PF |
| 6 | `feature/admin-panel` | 8 | CRUD справочников, CSV/Excel импорт, семестры, settings, UI W9, тесты AD |
| 7 | `feature/notifications` | 9 | in-app + email + telegram mock, настройки, тесты NT |
| 8 | `feature/analytics` | 10 | AR-динамика, рейтинги, бюджет, UI W10, тесты AN |
| 9 | `feature/ai-integration` | 11 | 3 AI-точки с fallback, UI W11, тесты AI |
| 10 | `test/e2e-and-polish` | все | E2E Playwright, compose up, acceptance, README/docs, merge → main |

## Статус

| Этап | Статус |
|------|--------|
| Аудит | ✅ завершён |
| План/Git | ✅ `develop` создана |
| 1 Auth/профили | ✅ реализовано и проверено (34 backend-теста, 13 frontend-тестов) |
| 2 АР | ⏳ backend + UI реализованы, интеграционные тесты 11/11 |
| 3 Баллы/лента | — |
| 4 Опросы | — |
| 5 Персональная лента | — |
| 6 Админка | — |
| 7 Уведомления | — |
| 8 Аналитика | — |
| 9 ИИ | — |
| 10 E2E/полировка | — |

## Проверки

- `backend`: `mvn test` (unit + integration на PostgreSQL, env `SPRING_DATASOURCE_URL`).
- `frontend`: `npm run lint`, `npm test`, `npm run build`.
- `docker compose up --build` + smoke-скрипт.
- E2E: `npm run test:e2e` (Playwright, локально `channel: msedge`; в CI — chromium после установки).

## Последние проверки

| Дата | Что | Результат |
|------|-----|-----------|
| 2026-11-10 | `mvn test` (auth, membership, rating) | 34 passed |
| 2026-11-10 | `npm test` | 13 passed |
| 2026-11-10 | `npm run build` | OK |
| 2026-11-10 | Playwright: teacher create → student submit → teacher accept | 2/2 passed |
| 2026-11-10 | Mailhog: письмо подтверждения | доставлено |

## Известные ограничения на старте

- Docker daemon локально может быть не запущен (запускается Docker Desktop).
- Vite watcher падал на временных файлах редактора; добавлен `server.watch.ignored`.
- В CI нет Playwright-шага — будет добавлен.
