# UniRate — университетская социальная сеть (Хакатон 2026)

MVP университетской социальной сети: профили и роли, академический рейтинг (АР), баллы учебной и общественной активности (БУА/БОА), лента наград, персональная лента, опросы, подтверждение email и группы, админ-панель, уведомления, аналитика и ИИ-функции.

## Быстрый старт

```bash
docker compose up --build
```

| Сервис | URL |
|--------|-----|
| Frontend | http://localhost |
| API health | http://localhost:8080/api/v1/health |
| Mailhog (dev-почта) | http://localhost:8025 |

Миграции Flyway и демо-данные создаются автоматически при первом запуске.

### Демо-пользователи

Пароль у всех: `Demo123!`

| Логин | Роль | Что показать |
|-------|------|--------------|
| `student@demo.local` | Студент, группа ИВТ-21 | АР, задания, награды, опросы, ИИ-советы |
| `lead@demo.local` | Староста ИВТ-21 | Подтверждение одногруппников |
| `pending@demo.local` | Студент в статусе PENDING | Ограничения до подтверждения группы |
| `teacher@demo.local` | Преподаватель | Задания, проверка, активности, бюджет, аналитика |
| `teacher2@demo.local` | Преподаватель кафедры ИС | Разграничение доступа |
| `admin@demo.local` | Администратор | Справочники, импорт, семестры, настройки |

Демо-контент создаётся `DemoDataSeeder`: активная и просроченная контрольные точки, учебные/общественные активности, награды в ленте, два опроса, бюджеты и история баллов.

## Модули

1. Регистрация, JWT-авторизация, профили, ledger.
2. Академический рейтинг: контрольные точки, сдача, проверка, автосписание по настройкам.
3. Активности, запись на SOCIAL, БУА/БОА, месячный бюджет преподавателя по группам.
4. Публичная лента наград.
5. Опросы SINGLE/MULTIPLE, анонимность, агрегаты, персональные ответы для преподавателя.
6. Персональная лента событий и напоминания о дедлайнах.
7. Подтверждение email и принадлежности к группе (преподаватель/староста).
8. Админ-панель: справочники, CSV/Excel-импорт, семестры, настройки, пользователи.
9. In-app уведомления + email (Mailhog) + Telegram (mock без токена).
10. Аналитика: динамика АР, рейтинги БУА/БОА, бюджет по месяцам.
11. ИИ: черновик задания, сводка опроса, советы студенту; без ключа — детерминированный demo-fallback.

## Стек

Java 21 · Spring Boot 3.4 · Maven · React 18 · TypeScript · Vite · PostgreSQL 16 · Flyway · Docker Compose · GitHub Actions.

Архитектура и бизнес-правила: [`docs/spec/`](docs/spec/) (начните с [`00-index.md`](docs/spec/00-index.md)), БД — [`18-database.md`](docs/spec/18-database.md), API — [`05-api.md`](docs/spec/05-api.md).

## Тесты

### Backend (unit + integration на PostgreSQL)

Интеграционные тесты используют отдельную БД `unirate_test`, чтобы не зависеть от демо-данных:

```bash
docker compose up -d db
docker compose exec db psql -U unirate -d postgres -c "CREATE DATABASE unirate_test;"
# если база уже существует — пропустите создание

cd backend
mvn test
```

Переопределение: `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`.

### Frontend

```bash
cd frontend
npm install
npm run lint      # tsc --noEmit
npm test          # vitest
npm run build
```

### E2E (Playwright)

Стек должен быть запущен:

```bash
docker compose up --build -d
cd frontend
npm install
# локально используется установленный Edge; в CI — Chromium:
# npx playwright install --with-deps chromium
E2E_BASE_URL=http://localhost npm run test:e2e
```

Сценарии: жизненный цикл задания (создание → сдача → приёмка), активность и бюджет, опрос и анонимность, админ-импорт, аналитика, ИИ и уведомления.

## Переменные окружения

Скопируйте `.env.example` в `.env` (файл не коммитится). Ключи и секреты передаются только через окружение:

- `JWT_SECRET` — секрет подписи JWT;
- `LLM_API_KEY`, `LLM_BASE_URL` — OpenAI-совместимый LLM (пусто = demo-режим ИИ);
- `TELEGRAM_BOT_TOKEN` — токен бота (пусто = mock-транспорт с логом);
- `APP_URL` — публичный адрес фронтенда для ссылок подтверждения email.

## CI/CD

`.github/workflows/ci.yml`:

- backend — unit + integration тесты на PostgreSQL;
- frontend — typecheck, build, Vitest;
- docker — `docker compose build/up`, healthcheck и Playwright E2E против собранного стека.

## Документация и артефакты агентской работы

- [`AGENTS.md`](AGENTS.md) — правила и команды;
- [`docs/agent-log.md`](docs/agent-log.md) — журнал работы агента, ошибки и исправления;
- [`docs/development-progress.md`](docs/development-progress.md) — прогресс, решения и проверки;
- [`docs/qa/`](docs/qa/) — тест-кейсы, E2E-сценарии, чеклист приёмки;
- [`docs/demo-script.md`](docs/demo-script.md) — сценарий демонстрации на защите.

## Сброс демо-данных

```bash
docker compose down -v
docker compose up -d --build
```

`down -v` удаляет volume PostgreSQL; при следующем старте Flyway и `DemoDataSeeder` создадут чистые демо-данные.

## Ограничения

- Telegram-канал работает в mock-режиме без токена; email в dev перехватывается Mailhog.
- Реальный LLM не вызывается без ключа; используется детерминированный demo-fallback (честно отображается в UI).
- Файлы работ хранятся в Docker volume; внешнее объектное хранилище не подключено.
- Интеграция с реальной АСУ вуза не реализована (вне scope MVP).
