# Тест-кейсы: Академический рейтинг (модуль 2)

| ID | Название | Предусловия | Ожидание |
|----|----------|-------------|----------|
| AR-01 | Старт 1000 | новая регистрация | AR=1000 |
| AR-02 | Создание checkpoint | teacher, группы, cost, deadline | студенты CONFIRMED видят задачу IN_PROGRESS |
| AR-03 | Сдача до дедлайна | submit url | SUBMITTED, списаний 0 |
| AR-04 | Accept | | ACCEPTED, списание не идёт |
| AR-05 | Тик после дедлайна | interval=60, amount=20, ждать 1 тик | AR−20, ledger debit |
| AR-06 | Несколько тиков | 3 тика, cost=100 | AR−60, debited=60 |
| AR-07 | Cap по cost | cost=50, много тиков | debited=50 max, дальше стоп |
| AR-08 | Floor 0 | AR=15, amount=20 | AR=0, debit=15 |
| AR-09 | Submit останавливает | OVERDUE + submit | дальше тики не списывают |
| AR-10 | Return после дедлайна | RETURNED | списание возобновляется |
| AR-11 | Return до дедлайна | now < deadline | списания нет до дедлайна |
| AR-12 | Batch accept | несколько studentIds | все ACCEPTED |
| AR-13 | Статус Просрочено | now>deadline, не сдано | UI OVERDUE |
| AR-14 | Семестровый bonus | AR=900, bonus=200 | AR=1000 (cap) |
| AR-15 | Семестровый bonus без cap | AR=700, bonus=200 | AR=900 |
| AR-16 | Чужой студент не сдаёт | | 403 |
| AR-17 | Файл или ссылка | только файл / только url | ок; оба пусто — 400 |
| AR-18 | Настройки interval/amount | изменить settings | джоба использует новые |
| AR-19 | Не подтверждённая группа | PENDING student | checkpoint не назначен |
| AR-20 | Unit: pure function debit | таблица входов/выходов | см. 03-rating-rules |
