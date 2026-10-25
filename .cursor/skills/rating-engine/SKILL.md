---
name: rating-engine
description: >-
  Implements and verifies academic rating (АР) deduction, semester bonus, and
  checkpoint statuses for UniRate. Use when working on module 2, ArDeductionJob,
  RatingService, checkpoint submit/accept/return, or AR unit tests.
---

# UniRate — движок академического рейтинга

Источник правды: `docs/spec/03-rating-rules.md` + settings в `10-settings.md`.  
Кейсы: `docs/qa/test-cases-rating.md` (AR-01…AR-20).

## Обязательные ветки в unit-тестах

| Сценарий | Ожидание |
|----------|----------|
| Старт | AR = `arStartOnRegister` (1000) |
| Тик после deadline, не сдано | −`arDeductAmount`, ledger debit |
| debited → cost | стоп, даже если тики идут |
| AR < amount | debit = AR, итог 0 |
| SUBMITTED / ACCEPTED | тики не списывают |
| RETURNED и now > deadline | списание снова |
| semester bonus | `min(arMax, AR + bonus)` |

## Статусы

`IN_PROGRESS` | `SUBMITTED` | `OVERDUE` | `ACCEPTED` | `RETURNED`  
UI-подписи — в `02-domain.md`.

## Job

- Интервал и amount **только** из AppSettings.
- Идемпотентность тика: не списывать больше cost; не уходить ниже 0.
- Логировать тик (для демо на защите).

## Запрещено

Хардкод `20` / `60` в сервисе; «упростить» списание раз в час без правки spec.
