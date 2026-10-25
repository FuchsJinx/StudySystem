# UniRate — университетская социальная сеть (Хакатон 2026)

MVP: академический рейтинг, баллы активности, ленты, опросы, админка, уведомления, аналитика, ИИ.

## Стек

Java 21 / Spring Boot · React (Vite) · PostgreSQL · Docker Compose · GitHub Actions

## Быстрый старт

```bash
docker compose up --build
```

| Сервис | URL |
|--------|-----|
| Frontend | http://localhost |
| API health | http://localhost:8080/api/v1/health |
| Mailhog | http://localhost:8025 |

Демо-пользователи (пароль `Demo123!`): `student@demo.local`, `teacher@demo.local`, `admin@demo.local`.

## Документация

| Для кого | Путь |
|----------|------|
| Агенты | [`AGENTS.md`](AGENTS.md) |
| Архитектура | [`docs/spec/06-architecture.md`](docs/spec/06-architecture.md) |
| БД | [`docs/spec/18-database.md`](docs/spec/18-database.md) |
| Сценарии / диаграммы | [`docs/spec/19-user-flows.md`](docs/spec/19-user-flows.md) |
| Spec index | [`docs/spec/00-index.md`](docs/spec/00-index.md) |
| QA | [`docs/qa/00-index.md`](docs/qa/00-index.md) |

## Локальная разработка без Docker (опционально)

```bash
# БД
docker compose up -d db mailhog

# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

## CI/CD

При push/PR: unit-тесты backend, lint+build frontend, `docker compose build` + healthcheck.  
Workflow: `.github/workflows/ci.yml`.

## Сдача

Репозиторий + артефакты агентов · демо · ТГ [@ReshTatyana](https://t.me/ReshTatyana) «Хакатон» + ФИО · защита 19.11.2026.
