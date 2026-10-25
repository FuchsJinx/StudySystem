# 05 — API (макет контракта)

Базовый префикс: `/api/v1`. Auth: `Authorization: Bearer <token>` (⚠️ JWT — рабочая гипотеза).  
Ошибки: `{ "error": { "code": "STRING", "message": "русский текст" } }`.

## Auth

| Method | Path | Описание |
|--------|------|----------|
| POST | `/auth/register/student` | email, password, fullName, facultyId, groupId |
| POST | `/auth/register/teacher` | email, password, fullName, departmentId |
| POST | `/auth/login` | → token + role |
| POST | `/auth/verify-email` | token из письма (модуль 7) |
| GET | `/auth/me` | текущий пользователь |

## Справочники

| Method | Path |
|--------|------|
| GET | `/catalog/faculties` |
| GET | `/catalog/faculties/{id}/groups` |
| GET | `/catalog/departments` |

## Профиль

| Method | Path | Описание |
|--------|------|----------|
| GET | `/users/{id}/profile` | Публичный профиль (auth) |
| GET | `/users/{id}/ledger` | История баллов |
| GET | `/users/me/checkpoints` | Задачи студента |
| GET | `/users/me/activities` | Активности студента |

## Checkpoints (АР)

| Method | Path | Role |
|--------|------|------|
| POST | `/checkpoints` | Teacher |
| GET | `/checkpoints/{id}` | Teacher/Student |
| POST | `/checkpoints/{id}/submit` | Student (url?, file?) |
| POST | `/checkpoints/{id}/accept` | Teacher `{ studentIds: [] }` |
| POST | `/checkpoints/{id}/return` | Teacher `{ studentIds: [] }` |
| GET | `/checkpoints/{id}/roster` | Teacher |

## Activities

| Method | Path | Role |
|--------|------|------|
| POST | `/activities` | Teacher |
| POST | `/activities/{id}/enroll` | Student (SOCIAL) |
| POST | `/activities/{id}/award` | Teacher `{ awards: [{studentId, level}] }` |
| GET | `/teachers/me/budget?groupId=&yearMonth=` | Teacher |

## Feed

| Method | Path |
|--------|------|
| GET | `/feed/awards?cursor=` | Модуль 4 |
| GET | `/feed/personal?cursor=` | Модуль 6 |

## Polls (5)

| Method | Path |
|--------|------|
| POST | `/polls` | Teacher |
| POST | `/polls/{id}/vote` | Student |
| GET | `/polls/{id}/results` | Student (после голоса) / Teacher detail |

## Admin (8)

| Method | Path |
|--------|------|
| CRUD | `/admin/faculties`, `/groups`, `/departments` |
| POST | `/admin/import/students` | CSV/Excel |
| PATCH | `/admin/settings` | |
| POST | `/admin/semesters/{id}/activate` | |

## Notifications (9)

| Method | Path |
|--------|------|
| GET | `/notifications` | |
| PATCH | `/notifications/{id}/read` | |
| PUT | `/users/me/notification-settings` | telegram/email |

## Analytics (10)

| Method | Path |
|--------|------|
| GET | `/analytics/groups/{id}/ar-dynamics` | Teacher |
| GET | `/analytics/groups/{id}/activity-ranking` | |
| GET | `/analytics/teachers/me/budget-stats` | |

## AI (11)

| Method | Path |
|--------|------|
| POST | `/ai/draft-checkpoint` | Teacher: тезисы → описание |
| POST | `/ai/poll-summary` | Teacher |
| POST | `/ai/rating-tips` | Student |

Полные JSON Schema — дополнять по мере реализации; не менять смысл полей без правки этого файла.
