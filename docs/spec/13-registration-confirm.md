# 13 — Модуль 7: Подтверждение регистрации

## Email

1. После register — письмо со ссылкой/кодом.
2. До verify: ⚠️ РЕШЕНИЕ: login разрешён, но banner «подтвердите email»; API мутаций заданий/баллов — 403 `EMAIL_NOT_VERIFIED`.
3. Dev: письмо в Mailhog; токен в логах seed-пользователей уже verified.

## Подтверждение группы

- Статус `groupMembership`: `PENDING` | `CONFIRMED` | `REJECTED`.
- Подтверждает: преподаватель, связанный с группой, **или** староста (`isGroupLead`).
- UI: список заявок «ожидают».
- Пока `PENDING`/`REJECTED`: студенту **не назначаются** новые checkpoint/activity awards; не видит чужие начисления как участник.

## Seed

Демо-студенты — `CONFIRMED` + email verified. Один студент `PENDING` для демо модуля.
