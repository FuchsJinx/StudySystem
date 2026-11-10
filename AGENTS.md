# AGENTS.md — инструкции для ИИ-агентов

## Проект

Университетская социальная сеть UniRate: профили и роли, академический рейтинг (АР), баллы учебной и общественной активности (БУА/БОА), лента наград, персональная лента, опросы, подтверждение email/группы, админка, уведомления, аналитика, LLM-функции.

**Срок:** 22.10.2026 – 12.11.2026 · **Защита:** 19.11.2026

## Стек (зафиксирован)

| Слой | Выбор |
|------|--------|
| Backend | Java 21 · Spring Boot 3.4 · Maven |
| Frontend | React 18 · Vite · TypeScript |
| БД | PostgreSQL 16 · Flyway |
| Auth | JWT (HS256) |
| Запуск | `docker compose up --build` |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |

Корень: `backend/`, `frontend/`, `docker-compose.yml`. Spec БД: `docs/spec/18-database.md`. Сценарии: `docs/spec/19-user-flows.md`.

## Документы — источник правды

1. `docs/spec/` — требования и архитектура. **Менять правила рейтинга/баллов только через spec, потом код.**
2. `docs/qa/` — стратегия и кейсы тестирования.
3. `docs/agent-log.md` — журнал: задачи агенту, ошибки, исправления.
4. `docs/development-progress.md` — план, принятые решения, текущий статус и проверки.
5. Этот файл — как запускать, тестировать, соглашения.

## Обязательный объём

Реализованы **все** модули 1–11 (база + опции). При конфликте сроков приоритет: **1 → 2 → 3 → 4 → 8 → 7 → 5 → 6 → 9 → 10 → 11**.

## Как работать агенту

1. Перед задачей прочитай релевантный файл из `docs/spec/`, `docs/development-progress.md` и DoD задачи в `08-tasks-breakdown.md`.
2. Пиши тесты **до или вместе** с кодом (TDD для домена рейтинга и бюджета).
3. Не выдумывай бизнес-правила: если в spec нет — спроси человека или зафиксируй вопрос в `agent-log.md`.
4. После сессии добавь запись в `docs/agent-log.md` и обнови `development-progress.md`.
5. Не коммить секреты (`.env` с паролями, API keys). Ключи — только env (`LLM_API_KEY`, `TELEGRAM_BOT_TOKEN`, `JWT_SECRET`).

## Соглашения по коду

- Язык UI и сообщений API ошибок: **русский** (для демо вуза).
- Имена в коде: английский (`academicRating`, `activityPoints`).
- Слои: `api` → `application` → `domain`; инфраструктура — JPA/Jobs/Mail/LLM/Storage.
- Ошибки API: `{"error":{"code":"CODE","message":"русский текст","details":[]}}`.
- Миграции БД версионируются (`V1__init`, `V2__seed`, `V3__schema_extensions`); seed обязателен при `docker compose up`.
- Демо-контент с относительными датами создаёт `DemoDataSeeder` (идемпотентно, отключается `UNIRATE_DEMO_SEED_ENABLED=false`, не работает в профиле `worker` и в тестах).
- Настройки списания/бюджета — только через `app_settings`, не хардкод в сервисах.
- Фоновая джоба списания АР: период из `app_settings.arDeductIntervalSec` (дефолт 60 с).
- Файлы работ: только pdf/zip/doc/docx/png ≤10 МБ, имена UUID, скачивание с проверкой прав.
- Идентификация пользователя: `@AuthenticationPrincipal AuthUser`; проверка прав — на backend.

## Запуск и тесты

```bash
# всё приложение (чистая БД + seed + demo seeder)
docker compose up --build

# backend: unit + integration (PostgreSQL, отдельная БД unirate_test)
docker compose up -d db
docker compose exec db psql -U unirate -d postgres -c "CREATE DATABASE unirate_test;"  # один раз
cd backend && mvn test

# frontend
cd frontend && npm install
npm run lint && npm test && npm run build

# e2e (стек должен быть запущен)
cd frontend
E2E_BASE_URL=http://localhost npm run test:e2e
```

CI: backend-тесты получают БД `unirate_test` через PostgreSQL service; docker-job поднимает compose и прогоняет Playwright на Chromium.

## Настройки окружения

| Переменная | Назначение |
|------------|------------|
| `JWT_SECRET` | секрет JWT (>=32 символов) |
| `APP_URL` | публичный URL фронтенда для ссылок подтверждения email |
| `LLM_API_KEY`, `LLM_BASE_URL` | OpenAI-совместимый LLM; пусто = demo-fallback |
| `TELEGRAM_BOT_TOKEN` | Telegram-бот; пусто = mock-транспорт в лог |
| `UPLOADS_DIR` | каталог файлов работ (в compose — `/data/uploads`) |
| `UNIRATE_DEMO_SEED_ENABLED` | демо-контент при старте backend |
| `TEST_DATABASE_URL` | БД интеграционных тестов (дефолт `unirate_test`) |

## Субагенты / скиллы

Проектные скиллы в `.cursor/skills/` (агент подхватывает по description):

| Скилл | Когда |
|-------|--------|
| `unirate-dev` | Код фич, Docker, seed, задачи из breakdown |
| `spec-first` | Смена требований / API / ⚠️ РЕШЕНИЕ |
| `rating-engine` | Модуль 2, джоба списания АР |
| `activity-budget` | Модуль 3, БУА/БОА, бюджет |
| `reviewer` | Ревью диффа / PR |
| `tester` | Прогон и расширение QA |
| `agent-session` | Старт/финиш сессии, `agent-log` |
| `defense-prep` | Презентация и демо к защите |

Явный вызов: «используй скилл reviewer» / «по unirate-dev сделай B3».

## MCP

Проектные MCP-серверы не обязательны. В ходе разработки для ручной проверки UI использовался `agent-desktop` (скриншоты/окна браузера); Playwright E2E не требует MCP и работает в CI.

## Запрещено агенту

- Менять критерии оценки / «упрощать» автосписание «для удобства» без правки spec.
- Реализовывать только happy-path без статусов «Просрочено» / возврата на доработку.
- Хардкодить секреты, ключи, пароли в репозитории.
- Отвечать на защите «так написал агент» — человек должен понимать каждое решение.
