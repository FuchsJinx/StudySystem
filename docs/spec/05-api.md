# 05 — API (контракт)

Базовый префикс: `/api/v1`. Auth: `Authorization: Bearer <JWT>`.

Ошибки — единый формат:

```json
{ "error": { "code": "STRING", "message": "русский текст", "details": ["…"] } }
```

`details` — опционально (ошибки валидации полей). HTTP-коды: 400 (валидация/бизнес), 401 (нет/просрочен токен),
403 (роль/чужой ресурс/email/группа), 404, 409 (конфликт/повтор), 413 (файл), 503 (внешний сервис).

> Обновлено при реализации. Значения полей не менять без правки спецификаций модулей.

## Auth (модуль 1)

| Method | Path | Body | Ответ |
|--------|------|------|-------|
| POST | `/auth/register/student` | `email, password, fullName, facultyId, groupId` | `201 { token, user }` |
| POST | `/auth/register/teacher` | `email, password, fullName, departmentId` | `201 { token, user }` |
| POST | `/auth/login` | `email, password` | `{ token, user }` |
| POST | `/auth/verify-email` | `{ token }` | `{ verified: true }` |
| POST | `/auth/resend-verification` | — (auth) | `{ sent: true }` |
| GET | `/auth/me` | — (auth) | `user` |

`user`:

```json
{
  "id": 4, "email": "student@demo.local", "fullName": "Иван Иванов",
  "role": "STUDENT", "emailVerified": true,
  "student": {
    "id": 1, "groupId": 1, "groupName": "ИВТ-21",
    "facultyId": 1, "facultyName": "ФИТ",
    "membershipStatus": "CONFIRMED", "isGroupLead": false,
    "academicRating": 1000, "bua": 150, "boa": 75
  },
  "teacher": null
}
```

`teacher`: `{ id, departmentId, departmentName, groups: [{id,name}] }`.

## Справочники

| Method | Path | Доступ | Ответ |
|--------|------|--------|-------|
| GET | `/catalog/faculties` | public | `[{id,name}]` |
| GET | `/catalog/faculties/{id}/groups` | public | `[{id,name,facultyId}]` |
| GET | `/catalog/departments` | public | `[{id,name}]` |
| GET | `/catalog/groups` | auth | `[{id,name,facultyId,facultyName}]` |
| GET | `/catalog/groups/my` | teacher | группы преподавателя |

## Профиль и ledger (модуль 1)

| Method | Path | Доступ | Ответ |
|--------|------|--------|-------|
| GET | `/users/{id}/profile` | auth | профиль (см. ниже) |
| GET | `/users/{id}/ledger?cursor=&limit=` | self/teacher/admin | `Page<LedgerEntry>` |
| GET | `/users/me/checkpoints` | student | `[StudentCheckpoint]` |
| GET | `/users/me/activities` | student | `[StudentActivity]` |
| GET | `/users/me/summary` | auth | сводка для главного экрана |
| GET | `/users/me/notification-settings` | auth | `{emailEnabled, telegramEnabled, telegramChatId}` |
| PUT | `/users/me/notification-settings` | auth | то же |

`profile` (публичная часть — всем auth; ledger/задания только self/teacher/admin):

```json
{
  "id": 4, "fullName": "Иван Иванов", "role": "STUDENT",
  "email": "student@demo.local", "emailVerified": true,
  "groupName": "ИВТ-21", "facultyName": "ФИТ", "departmentName": null,
  "academicRating": 1000, "bua": 150, "boa": 75,
  "membershipStatus": "CONFIRMED", "isGroupLead": false,
  "ledger": [ { "id": 10, "date": "2026-11-10T10:00:00Z", "currency": "BOA", "amount": 50,
                "reason": "Помощь в организации конференции", "actorName": "Анна Наумова" } ],
  "activeCheckpoints": [ { "id": 1, "title": "Курсовая, 1 этап", "deadline": "…", "status": "IN_PROGRESS" } ],
  "completedCheckpoints": []
}
```

`Page<T>`: `{ "items": [ … ], "nextCursor": 123 | null }`.

`LedgerEntry`: `id, date, currency (AR|BUA|BOA), amount (±), reason, actorName,
refType, refId`.

Формат истории (ТЗ): дата · `[+50 БОА]` · «Активность» от «ФИО».

## Checkpoints / АР (модуль 2)

| Method | Path | Роль | Тело / ответ |
|--------|------|------|--------------|
| POST | `/checkpoints` | Teacher | `{title, description, deadline, cost, groupIds[]}` → `CheckpointDetail` |
| GET | `/checkpoints` | Teacher | свои checkpoint + счётчики |
| GET | `/checkpoints/{id}` | Teacher/Student | детали + (для teacher) roster |
| GET | `/checkpoints/{id}/roster` | Teacher | `{checkpoint, students:[RosterRow]}` |
| POST | `/checkpoints/{id}/submit` | Student | JSON `{url?}` или multipart `file?`, `url?`; минимум одно |
| POST | `/checkpoints/{id}/accept` | Teacher | `{studentIds:[…]}` |
| POST | `/checkpoints/{id}/return` | Teacher | `{studentIds:[…], comment?}` |
| GET | `/files/submissions/{submissionId}` | owner/teacher/admin | файл (download) |

`SubmittableResponse`: `id, checkpointId, title, description, deadline, cost, status, arDebited,
submittedAt, submissionUrl, fileName, returnComment, teacherName`.

`RosterRow`: `studentUserId, studentProfileId, fullName, groupName, status, arDebited, cost,
submittedAt, submissionUrl, fileName, returnComment`.

Статусы: `IN_PROGRESS`, `SUBMITTED`, `OVERDUE`, `ACCEPTED`, `RETURNED` (RU-подписи в UI:
«В процессе», «Сдано на проверку», «Просрочено», «Принято», «На доработке»).

## Activities / баллы (модуль 3)

| Method | Path | Роль | Тело / ответ |
|--------|------|------|--------------|
| POST | `/activities` | Teacher | `{title, description, type: ACADEMIC|SOCIAL, eventDate, maxPoints, groupIds[]}` |
| GET | `/activities` | auth | student — целевые, teacher — свои |
| GET | `/activities/{id}` | auth | детали + enrollment/awards |
| POST | `/activities/{id}/enroll` | Student | SOCIAL, целевая группа; 409 если уже записан |
| POST | `/activities/{id}/award` | Teacher | `{awards:[{studentId, level:25|50|75|100}]}` |
| GET | `/activities/{id}/participants` | Teacher | `[{studentUserId, fullName, groupName, enrolled, awardedPoints, level}]` |
| GET | `/teachers/me/budget?groupId=&yearMonth=YYYY-MM` | Teacher | `{yearMonth, groupId, limit, spent, remaining, awards:[…]}` |
| GET | `/teachers/me/budgets?yearMonth=` | Teacher | остаток по всем группам |

Ошибки бюджета: `400 BUDGET_EXCEEDED`; повторная награда — `409 AWARD_ALREADY_EXISTS`;
SOCIAL без записи — `400 NOT_ENROLLED`; чужая группа — `403 GROUP_NOT_TARGETED`.

## Лента наград (модуль 4)

| Method | Path | Доступ |
|--------|------|--------|
| GET | `/feed/awards?cursor=&limit=` | auth |

Элемент: `{ id, studentUserId, studentName, groupName, currency, amount, activityTitle,
teacherName, createdAt }`. Только БУА/БОА, новые сверху.

## Опросы (модуль 5)

| Method | Path | Роль | Тело / ответ |
|--------|------|------|--------------|
| POST | `/polls` | Teacher | `{title, description, endsAt, targetGroupIds[], isAnonymous, questions:[{text,type,options[]}]}` |
| GET | `/polls` | auth | список (student — целевые, teacher — свои) |
| GET | `/polls/{id}` | auth | деталь; статистика только после голоса/для teacher/после endsAt |
| POST | `/polls/{id}/vote` | Student | `{answers:[{questionId, optionIds[]}]}` |
| GET | `/polls/{id}/results` | Student (после) / Teacher | агрегаты (+ voter-детали, если !anonymous и teacher) |

`PollDetail`: `id, title, description, endsAt, isAnonymous, status (ACTIVE|ENDED),
hasVoted, canVote, questions:[{id,text,type,options:[{id,text}]}], results?: [QuestionResult]`.
`QuestionResult`: `questionId, text, type, totalVotes, options:[{optionId,text,count,percent}]`.

## Персональная лента (модуль 6)

| Method | Path | Доступ |
|--------|------|--------|
| GET | `/feed/personal?cursor=&limit=` | auth |

Элемент: `{ id, type, message, createdAt }`. Типы из `12-personal-feed.md`.

## Подтверждение группы (модуль 7)

| Method | Path | Роль | Описание |
|--------|------|------|----------|
| GET | `/membership/requests` | Teacher/Lead/Admin | ожидающие в доступных группах |
| POST | `/membership/{studentUserId}/confirm` | Teacher группы/Lead/Admin | `CONFIRMED` + назначение задач |
| POST | `/membership/{studentUserId}/reject` | Teacher группы/Lead/Admin | `REJECTED` |

## Администрирование (модуль 8)

| Method | Path | Описание |
|--------|------|----------|
| GET/POST | `/admin/faculties` | список/создать `{name}` |
| PUT/DELETE | `/admin/faculties/{id}` | изменить/удалить |
| GET/POST | `/admin/groups` | `{name, facultyId}` |
| PUT/DELETE | `/admin/groups/{id}` | |
| GET/POST | `/admin/departments` | `{name}` |
| PUT/DELETE | `/admin/departments/{id}` | |
| POST | `/admin/import/students` | multipart `file` CSV/XLSX, отчёт `{created, skipped, errors:[{row,message}]}` |
| GET | `/admin/settings` | `[{key,value,title,type}]` |
| PATCH | `/admin/settings` | `{key:value,…}` |
| GET/POST | `/admin/semesters` | `{name,startAt,endAt}` |
| POST | `/admin/semesters/{id}/activate` | начисление бонуса, 409 при повторе |
| GET | `/admin/users?role=` | пользователи |
| GET | `/admin/stats` | счётчики для панели |

## Уведомления (модуль 9)

| Method | Path |
|--------|------|
| GET | `/notifications?cursor=&limit=` |
| GET | `/notifications/unread-count` |
| PATCH | `/notifications/{id}/read` |
| POST | `/notifications/read-all` |
| GET/PUT | `/users/me/notification-settings` |

`Notification`: `{id, title, body, kind, link, read, createdAt}`.

## Аналитика (модуль 10)

| Method | Path | Роль |
|--------|------|------|
| GET | `/analytics/groups/{id}/ar-dynamics?days=30` | Teacher группы |
| GET | `/analytics/groups/{id}/activity-ranking?days=90` | Teacher группы |
| GET | `/analytics/teachers/me/budget-stats?months=6` | Teacher |
| GET | `/analytics/teachers/me/overview` | Teacher |

`ar-dynamics`: `{groupId, groupName, points:[{date, average, median}], students:[{…}]}`.
`activity-ranking`: `{students:[{studentUserId, fullName, groupName, bua, boa, total}]}`.
`budget-stats`: `{months:[{yearMonth, limit, spent, groups:[{groupId,groupName,spent,limit}]}]}`.

## ИИ (модуль 11)

| Method | Path | Роль | Тело → ответ |
|--------|------|------|--------------|
| POST | `/ai/draft-checkpoint` | Teacher | `{title, theses}` → `{text, provider}` |
| POST | `/ai/poll-summary` | Teacher | `{pollId}` → `{text, provider}` |
| POST | `/ai/rating-tips` | Student | `{}` → `{tips:[…], provider}` |

`provider`: `openai` | `demo`. Без ключа используется demo-fallback; при ошибке настроенного
LLM — `503 AI_UNAVAILABLE`, UI показывает сообщение и форма остаётся рабочей.

## Health

| Method | Path | Ответ |
|--------|------|-------|
| GET | `/health` | `{"status":"UP"}` |
