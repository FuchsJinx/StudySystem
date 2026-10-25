---
name: agent-session
description: >-
  Starts and closes UniRate agent work sessions with docs/agent-log.md entries.
  Use at the beginning or end of an agent coding session, after failures, or when
  the user says залогируй / журнал агента / agent-log.
---

# UniRate — сессия агента

## Старт задачи

1. Назови task ID из `08-tasks-breakdown.md` или сформулируй цель.
2. Перечисли spec-файлы, которые прочитаешь.
3. Критерий Done = DoD задачи + релевантные QA IDs.

## Финиш / сбой

Добавь в `docs/agent-log.md`:

```markdown
## YYYY-MM-DD HH:MM — <кто> / <Cursor|…>

**Задача:** …
**Промпт / скилл:** …
**Результат:** ок | частично | провал
**Ошибка агента:** …
**Как обнаружили:** …
**Исправление:** …
**Что сработало:** …
```

Честные записи об ошибках агента важнее «всё ок» — это 25% оценки хакатона.
