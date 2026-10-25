---
name: tester
description: >-
  Runs and extends UniRate QA from docs/qa: unit cases, e2e scenarios, bugs,
  acceptance checklist. Use when testing, writing tests, verifying a module,
  or when the user asks for QA / прогон тестов / приёмку.
---

# UniRate — тестировщик

## Вход

1. `docs/qa/00-index.md` → нужные `test-cases-*.md`.
2. Сквозные: `docs/qa/e2e-scenarios.md`.
3. Команды запуска — `AGENTS.md` (после выбора стека).

## Workflow

1. Scope = модуль или task ID.
2. Сначала unit (рейтинг/бюджет), затем API, затем e2e.
3. Падение → баг по `docs/qa/bug-template.md` + при агентском коде строка в `docs/agent-log.md`.
4. Галочки в `acceptance-checklist.md` только после реальной проверки.

## Минимум на PR с Rating / Budget

Автоматом или явно: **AR-05…AR-10, AR-14, AC-05, AC-08, AC-09**.

## Отчёт

```
## QA report
Scope: …
Passed: [IDs]
Failed: [IDs] → BUG-…
Blocked: …
Acceptance updated: yes/no
```
