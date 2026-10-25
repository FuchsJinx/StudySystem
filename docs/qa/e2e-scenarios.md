# E2E-сценарии (сквозные)

Автоматизировать минимум E2E-01…05. Остальные — ручной прогон на защите.

## E2E-01 — Happy path задания

1. Login teacher → создать checkpoint (группа ИВТ-21, cost 100, deadline +2 мин).  
2. Login student → видит задачу.  
3. Сдать до дедлайна (ссылка).  
4. Teacher accept.  
5. Проверить: статус Принято, АР не уменьшился.

## E2E-02 — Просрочка и списание

1. Checkpoint deadline в прошлом (или wait).  
2. Дождаться ≥1 тика worker.  
3. AR студента уменьшился на amount; статус Просрочено.  
4. Submit → списание стоп.  
5. Return → снова тик списывает.

## E2E-03 — Активность и лента

1. Teacher создаёт SOCIAL, max 100.  
2. Student enroll.  
3. Teacher award 50%.  
4. Ledger +50 БОА; лента наград; бюджет −50.

## E2E-04 — Регистрация и подтверждение

1. Register new student.  
2. Verify email (dev link).  
3. Пока PENDING — нет задач.  
4. Teacher/lead confirm → задача видна.

## E2E-05 — Опрос

1. Teacher poll SINGLE, anonymous.  
2. Student vote → видит агрегаты.  
3. Повторный vote — ошибка.

## E2E-06 — Админ импорт

1. Login admin → CSV import.  
2. Новый студент логинится.

## E2E-07 — ИИ черновик

1. Teacher: тезисы → «Сгенерировать».  
2. Description заполнено (mock ok).

## E2E-08 — Уведомления и персональная лента

1. Accept работы → in-app + событие персональной ленты у студента.  
2. У teacher — SUBMISSION_RECEIVED.
