---
name: activity-budget
description: >-
  Implements UniRate activity points (БУА/БОА), enrollments, participation
  percent, and teacher monthly budget. Use for module 3, ActivityService,
  budget checks, award ledger, or activity test cases AC-*.
---

# UniRate — баллы активности и бюджет

Spec: `docs/spec/04-activity-rules.md`, settings: `teacherMonthlyBudgetPerGroup`.  
QA: `docs/qa/test-cases-activity.md`.

## Правила

- `ACADEMIC` → БУА; `SOCIAL` → БОА.
- SOCIAL: award только при enrollment.
- `awarded = floor(maxPoints * level / 100)`, level ∈ {25,50,75,100}.
- Бюджет: сумма начисленных баллов активности по группе за календарный месяц ≤ лимит.
- Новый месяц → лимит сбрасывается (без переноса остатка).
- После award: ledger + лента наград (модуль 4) + событие персональной ленты (6).

## Тесты минимум

AC-04, AC-05, AC-08, AC-09, AC-10, AC-11.

## Формат истории

`[Дата] - [+50 БОА] - «…» от [ФИО преподавателя]`
