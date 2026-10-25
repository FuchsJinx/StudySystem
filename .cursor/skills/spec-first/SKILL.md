---
name: spec-first
description: >-
  Updates UniRate docs/spec and docs/qa before changing business rules or API
  contracts. Use when the user changes requirements, resolves a ⚠️ РЕШЕНИЕ,
  adds fields, or when code would diverge from spec.
---

# UniRate — сначала spec

## Правило

**Spec → тесты/кейсы → код.** Обратный порядок запрещён для доменных правил.

## Workflow

1. Определи затронутые файлы через `docs/spec/00-index.md`.
2. Внеси правку в spec (формулировка однозначная, без «потом уточним»).
3. Синхронизируй:
   - `docs/qa/test-cases-*.md` / e2e / acceptance при новых ветках поведения;
   - `docs/spec/05-api.md` при смене контракта;
   - `docs/spec/10-settings.md` при новых ключах.
4. Сними или закрой `⚠️ РЕШЕНИЕ` явным выбором.
5. Только потом меняй код.
6. Строка в `docs/agent-log.md`: что изменили в spec и почему.

## Конфликт файлов

Побеждает более специфичный: `03-rating-rules` / `04-activity-rules` > `01-overview`.
