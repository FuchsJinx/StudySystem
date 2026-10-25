# 03 — Академический рейтинг (модуль 2)

Ядро продукта. Unit-тесты обязательны на каждое правило ниже.

## Термины

| Термин | Описание |
|--------|----------|
| Checkpoint | Контрольная точка / задача преподавателя |
| cost | Макс. АР, которое можно списать по этой задаче у одного студента |
| AcademicRating | Текущий АР студента, 0…1000 |

## Жизненный цикл Checkpoint

1. Преподаватель создаёт: название, описание, группы (1+), deadline (дата+время), cost.
2. Система назначает задачу всем студентам целевых групп со статусом `IN_PROGRESS` (если группа подтверждена — модуль 7).
3. Студент сдаёт: ссылка и/или файл → статус `SUBMITTED`; списание останавливается.
4. Преподаватель: Accept → `ACCEPTED` (списание стоп) или Return → `RETURNED`.
5. При `RETURNED` и `now > deadline` — списание **возобновляется**.
6. Если `now > deadline` и статус не `SUBMITTED`/`ACCEPTED` — статус `OVERDUE` (или остаётся RETURNED с просрочкой); списание идёт.

## Стартовый рейтинг

При регистрации студента: **AR = 1000**.

## Начало семестра

Всем студентам: `AR = min(1000, AR + semesterBonusAR)`  
`semesterBonusAR` из Settings (дефолт **200**).  
Триггер: действие админа «открыть семестр» / крон (⚠️ РЕШЕНИЕ: ручной старт админом для MVP).

## Автосписание

Фоновая джоба с периодом `arDeductIntervalSec` (дефолт **60**):

Для каждой пары (student, checkpoint), где:
- deadline прошёл;
- статус ∈ {`IN_PROGRESS`, `OVERDUE`, `RETURNED`};
- уже списано по этой точке `< cost`;

выполнить:

```
debit = min(arDeductAmount, cost - alreadyDebited, currentAR)
AR -= debit
alreadyDebited += debit
ledger: AR debit, reason=checkpoint, amount=debit
```

`arDeductAmount` дефолт **20**.  
AR не ниже **0**.  
Списание прекращается при сдаче (`SUBMITTED`/`ACCEPTED`) или когда `alreadyDebited == cost`.

## Сдача работы

- Обязательно: хотя бы ссылка **или** файл (⚠️ оба сразу разрешены).
- Повторная сдача после RETURNED — снова `SUBMITTED`.

## Экран преподавателя

Список назначенных студентов + статусы + действия: принять одного / нескольких, вернуть.

## Экран студента

Список задач: дедлайн, статус, текущее списание по точке (уже списано / cost).

## Псевдокод для тестов

```text
given student AR=1000, checkpoint cost=100, deadline=T0
when now=T0+3min, no submit, interval=60s, amount=20
then AR=940, debited=60
when student submits
then no further debit
when teacher returns and now>deadline
then debit resumes until cost or accept
```
