# Стратегия тестирования

## Пирамида

```
        ╱╲
       ╱e2e╲         основные сценарии защиты (Playwright/Cypress)
      ╱──────╲
     ╱  API   ╲      интеграция endpoint + БД
    ╱──────────╲
   ╱ unit domain ╲   Rating, Budget, Poll validation — обязательно
  ╱──────────────╲
```

## Что обязательно (критерий «инженерное качество»)

| Слой | Объект |
|------|--------|
| Unit | Списание АР, cap/floor, семестр, бюджет, расчёт % баллов |
| e2e | Journeys из `e2e-scenarios.md` |
| Ручное | UI/UX, адаптив, демо-скрипт защиты |

## Роли тестовых пользователей (seed)

| Логин | Роль | Группа |
|-------|------|--------|
| `student@demo.local` | Student CONFIRMED | ИВТ-21 |
| `pending@demo.local` | Student PENDING | ИВТ-21 |
| `teacher@demo.local` | Teacher | кафедра X |
| `lead@demo.local` | Student + groupLead | ИВТ-21 |
| `admin@demo.local` | Admin | — |

Пароль единый в README seed: `Demo123!` (только dev).

## Процесс QA

1. Кейс из `test-cases-*.md` → автоматизировать если unit/api.  
2. Баг → `bug-template.md` → тикет + строка в `docs/agent-log.md` если виноват агент.  
3. Перед защитой — `acceptance-checklist.md` 100% по базе, max по опциям.  

## Скилл агента

`.cursor/skills/tester/SKILL.md` — агент читает эту папку и гоняет тесты сам.
