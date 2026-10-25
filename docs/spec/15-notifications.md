# 15 — Модуль 9: Уведомления

Каналы: **email** и/или **Telegram** (пользователь привязывает chatId / включает email).

## События → каналы

| Событие | Кому |
|---------|------|
| Дедлайн через `deadlineReminderHours` | Student |
| Работа принята / возвращена | Student |
| Начислены баллы | Student |
| Новая активность / опрос группе | Student |
| Студент сдал работу | Teacher |

In-app список уведомлений обязателен даже без внешних каналов (fallback).

## Настройки пользователя

`emailEnabled`, `telegramEnabled`, `telegramChatId`.

## Dev

- Email → Mailhog.
- Telegram → mock transport + лог, либо тестовый бот-токен в `.env.example` без секрета в git.
