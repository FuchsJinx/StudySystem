---
name: unirate-dev
description: >-
  Implements UniRate hackathon features against docs/spec with TDD and agent-log.
  Use when coding modules 1–11, scaffolding backend/frontend, Docker, seed, or any
  feature task from docs/spec/08-tasks-breakdown.md.
---

# UniRate — разработка фичи

## Перед кодом

1. Прочитай `AGENTS.md`.
2. Найди задачу в `docs/spec/08-tasks-breakdown.md` (ID: A*/B*/C*/D*).
3. Открой связанные spec-файлы (не выдумывай правила рейтинга/бюджета).
4. Если правила нет в spec → спроси человека или запиши вопрос в `docs/agent-log.md`. Не импровизируй домен.

## Порядок (TDD для домена)

Для Rating / Budget / Participation %:

1. Напиши failing unit-тест по кейсу из `docs/qa/test-cases-*.md`.
2. Реализуй минимум.
3. Прогони тест.
4. Подключи API/UI.

Для UI/API без тяжёлой логики: контракт из `docs/spec/05-api.md` + wireframe из `07-ui-flows.md`.

## Инварианты (не ломать)

- АР ∈ [0, 1000]; списание ≤ cost; тик из Settings.
- БУА/БОА раздельно; бюджет преподавателя по группе/месяцу.
- PENDING-группа / unverified email → нет заданий и начислений.
- UI и `error.message` — русский; идентификаторы в коде — английский.
- Секреты не в git.

## После сессии

Добавь запись в `docs/agent-log.md` (формат в файле). Обнови DoD задачи, если закрыл.

## Приоритет при нехватке времени

`1 → 2 → 3 → 4 → 8 → 7 → 5 → 6 → 9 → 10 → 11`
