# 19 — Пользовательские сценарии и потоки

Связь с UI: [07-ui-flows.md](07-ui-flows.md). E2E: `docs/qa/e2e-scenarios.md`.

---

## UC-01 Регистрация и допуск студента

**Акторы:** Student, Teacher/GroupLead, система (email).

```mermaid
sequenceDiagram
  actor S as Student
  participant API as Backend
  participant MH as Mailhog
  actor T as Teacher/Lead

  S->>API: POST /auth/register/student
  API-->>S: 201, membership=PENDING
  API->>MH: verification email
  S->>API: POST /auth/verify-email
  API-->>S: emailVerified=true
  Note over S,API: задания/баллы ещё закрыты
  T->>API: confirm group membership
  API-->>S: membership=CONFIRMED
  Note over S,API: назначаются checkpoint/activity
```

```mermaid
flowchart TD
  A[Регистрация] --> B{Email verified?}
  B -->|нет| C[Banner + 403 на мутации]
  B -->|да| D{Group CONFIRMED?}
  D -->|нет| E[Ждёт Teacher/Lead]
  D -->|да| F[Полный доступ студента]
```

---

## UC-02 Контрольная точка и списание АР

**Акторы:** Teacher, Student, Worker.

```mermaid
sequenceDiagram
  actor T as Teacher
  actor S as Student
  participant API as Backend
  participant W as Worker

  T->>API: POST /checkpoints
  API-->>S: задача IN_PROGRESS (если CONFIRMED)
  alt сдано до дедлайна
    S->>API: POST submit
    API-->>S: SUBMITTED
    T->>API: accept / return
  else дедлайн прошёл без сдачи
    loop каждые arDeductIntervalSec
      W->>API: ArDeductionJob
      API-->>S: AR-=min(amount, left, AR)
    end
    S->>API: submit → стоп списания
    T->>API: return после дедлайна → списание снова
  end
```

```mermaid
stateDiagram-v2
  [*] --> IN_PROGRESS
  IN_PROGRESS --> SUBMITTED: submit
  IN_PROGRESS --> OVERDUE: deadline passed
  OVERDUE --> SUBMITTED: submit
  SUBMITTED --> ACCEPTED: accept
  SUBMITTED --> RETURNED: return
  RETURNED --> SUBMITTED: re-submit
  ACCEPTED --> [*]
  note right of OVERDUE: worker списывает АР
  note right of RETURNED: если now>deadline — списание
```

---

## UC-03 Активность и бюджет

```mermaid
sequenceDiagram
  actor T as Teacher
  actor S as Student
  participant API as Backend

  T->>API: POST /activities
  alt SOCIAL
    S->>API: enroll
  end
  T->>API: award(level %)
  API->>API: check budget month/group
  alt OK
    API-->>S: ledger + feed + personal event
  else exceeded
    API-->>T: 400 BUDGET_EXCEEDED
  end
```

---

## UC-04 Опрос

```mermaid
flowchart TD
  A[Teacher создаёт poll] --> B[Студенты целевых групп]
  B --> C{endsAt прошёл?}
  C -->|да| D[Голос запрещён]
  C -->|нет| E[Студент голосует 1 раз]
  E --> F[Видит агрегаты]
  F --> G{anonymous?}
  G -->|да| H[Без ФИО в student UI]
  G -->|нет| I[Teacher видит кто как]
```

---

## UC-05 Админ: импорт и семестр

```mermaid
sequenceDiagram
  actor A as Admin
  participant API as Backend
  participant DB as Postgres

  A->>API: POST /admin/import/students CSV
  API->>DB: upsert users/groups
  API-->>A: report created/skipped/errors
  A->>API: activate semester
  API->>DB: AR = min(1000, AR + bonus) для всех
```

---

## UC-06 Уведомления и персональная лента

```mermaid
flowchart LR
  Ev[Доменное событие] --> FE[feed_events]
  Ev --> N[notifications in-app]
  Ev --> Ch{каналы user}
  Ch -->|email| MH[Mailhog/SMTP]
  Ch -->|telegram| TG[Bot API]
```

---

## Матрица ролей × сценарии

| Сценарий | Student | Teacher | Admin | Worker |
|----------|:-------:|:-------:|:-----:|:------:|
| UC-01 регистрация | ● | confirm | | |
| UC-02 АР | submit | CRUD/review | settings | deduct |
| UC-03 баллы | enroll | create/award | | |
| UC-04 опрос | vote | create | | poll-end |
| UC-05 админ | | | ● | |
| UC-06 нотификации | read | read | | remind |
| Лента наград | read | read | | |
| Аналитика | | ● | | |
| ИИ-черновик | tips | draft/summary | | |
