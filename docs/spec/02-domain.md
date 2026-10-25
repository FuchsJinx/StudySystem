# 02 — Доменная модель

## Сущности (кратко)

```
Faculty 1──* Group
Department 1──* TeacherProfile
User 1──1 StudentProfile | TeacherProfile | AdminProfile
StudentProfile *──1 Group
StudentProfile 1──1 AcademicRating (value 0..1000)
StudentProfile 1──* PointLedgerEntry (БУА | БОА | АР_DEBIT | АР_CREDIT)
TeacherProfile *──* Group (бюджет по группе/месяцу)
Checkpoint (задача АР) *──* Group
Checkpoint 1──* Submission (per student)
Activity *──* Group
Activity 1──* ActivityEnrollment (для общественной)
Activity 1──* ActivityAward
Poll *──* Group
Poll 1──* PollQuestion 1──* PollOption
Poll 1──* PollResponse
FeedEvent / AwardFeedItem
Notification
AppSettings (key-value)
Semester
```

## Перечисления

### SubmissionStatus
`IN_PROGRESS` | `SUBMITTED` | `OVERDUE` | `ACCEPTED` | `RETURNED`

Отображение студенту: «В процессе» | «Сдано на проверку» | «Просрочено» | «Принято» (+ «На доработке» для RETURNED).

### ActivityType
`ACADEMIC` (БУА) | `SOCIAL` (БОА)

### ParticipationLevel
`25` | `50` | `75` | `100` (%)

### PointCurrency
`AR` | `BUA` | `BOA`

### FeedEventType (модуль 6)
См. [12-personal-feed.md](12-personal-feed.md).

## Инварианты

1. АР ∈ [0, 1000].
2. Списание по checkpoint ≤ `cost` этой точки; сумма списаний по точке на студента ≤ cost.
3. Начисление БУА/БОА за активность ≤ maxPoints × participation%.
4. Расход бюджета преподавателя по группе за календарный месяц ≤ monthlyLimit.
5. До подтверждения группы (модуль 7) студенту не назначаются задания и баллы.
6. Публичная лента наград — только факты начисления БУА/БОА (не списания АР), ⚠️ уточнить: показывать ли АР-кредиты семестра.

## Тестовые данные (seed)

Минимум:

- 2 факультета, ≥3 группы, ≥2 кафедры
- ≥2 преподавателя, ≥5 студентов (разные группы)
- 1 admin
- Settings с дефолтами из [10-settings.md](10-settings.md)
- 1–2 готовых checkpoint и activity для демо
