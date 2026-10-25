# 08 — Разбивка на задачи

Каждая задача: DoD = код + тесты по `docs/qa` + запись в agent-log при работе агента.

## Эпик A — Каркас

| ID | Задача | DoD |
|----|--------|-----|
| A1 | Docker Compose: frontend, backend, db, worker | `up` без ошибок, `/health` |
| A2 | Seed справочников и пользователей | Логин demo student/teacher/admin |
| A3 | Auth register/login + RBAC | Тесты auth |

## Эпик B — База

| ID | Задача | DoD |
|----|--------|-----|
| B1 | Профиль + ledger API/UI | W2 |
| B2 | RatingService + unit тесты правил | 03-rating-rules 100% веток |
| B3 | Checkpoint CRUD + submit/accept/return | W3–W4 |
| B4 | ArDeductionJob | e2e/integration: тик списывает |
| B5 | Activities + budget | 04 + W5 |
| B6 | Лента наград | W6 |

## Эпик C — Опции

| ID | Задача | Модуль |
|----|--------|--------|
| C1 | Polls | 5 |
| C2 | Personal feed events | 6 |
| C3 | Email verify + group confirm | 7 |
| C4 | Admin CRUD + CSV/Excel + settings/semester | 8 |
| C5 | Notifications email/Telegram | 9 |
| C6 | Analytics dashboards | 10 |
| C7 | LLM endpoints + UI hooks | 11 |

## Эпик D — Качество

| ID | Задача |
|----|--------|
| D1 | e2e сценарии из `docs/qa/e2e-scenarios.md` |
| D2 | Acceptance checklist зелёный |
| D3 | README + AGENTS актуализированы под реальный стек |
| D4 | Презентация + скриншоты |

## Порядок исполнения (рекомендуемый)

`A1→A2→A3→C3(минимально)→B2→B3→B4→B1→B5→B6→C4→C1→C2→C5→C6→C7→D*`

C3 рано: без подтверждения группы остальная логика блокируется.
