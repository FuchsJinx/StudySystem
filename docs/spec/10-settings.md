# 10 — Настройки (AppSettings)

Все числовые параметры механик — здесь. Seed задаёт дефолты. Админ (модуль 8) меняет через UI.

| Ключ | Тип | Дефолт | Описание |
|------|-----|--------|----------|
| `semesterBonusAR` | int | 200 | Начисление АР в начале семестра |
| `arMax` | int | 1000 | Потолок АР |
| `arStartOnRegister` | int | 1000 | Стартовый АР |
| `arDeductIntervalSec` | int | 60 | Интервал джобы списания |
| `arDeductAmount` | int | 20 | Списание за тик |
| `teacherMonthlyBudgetPerGroup` | int | 1000 | Бюджет преподавателя |
| `deadlineReminderHours` | int | 24 | Порог «скоро дедлайн» (лента/уведомления) |

## Семестры

Сущность `Semester`: name, startAt, endAt, isActive.  
Старт семестра (начисление bonus) — действие админа «Активировать семестр».
