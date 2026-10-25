# 14 — Модуль 8: Администрирование

## Роль Admin

- CRUD: Faculty, Group (facultyId, name), Department.
- Управление семестрами и Settings ([10-settings.md](10-settings.md)).
- Импорт студентов/групп из CSV или Excel.

## Импорт CSV (макет колонок)

```csv
email,fullName,facultyName,groupName,passwordOptional
student1@uni.local,Иванов Иван,ФИТ,ИВТ-21,
```

Поведение:

- Нет факультета/группы → создать или ошибка строки ⚠️ РЕШЕНИЕ: автосоздание группы под факультетом.
- Отчёт: created / skipped / errors[] по строкам.
- Excel: тот же набор колонок, лист 1.

## Семестры

- Создать семестр → «Активировать» → начислить `semesterBonusAR` всем студентам с cap 1000.
- Повторная активация того же семестра запрещена.

## UI

См. W9 в [07-ui-flows.md](07-ui-flows.md).
