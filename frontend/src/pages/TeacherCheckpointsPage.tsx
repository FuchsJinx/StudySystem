import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { AiTextResponse } from "../shared/api/aiTypes";
import type { CheckpointSummary } from "../shared/api/checkpointTypes";
import type { Group } from "../shared/api/types";
import { formatDateTime, toDateTimeInputValue, toIsoFromLocalInput } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  PageLoader,
  StatCard,
  Textarea,
} from "../shared/ui";

function defaultDeadline(): string {
  const date = new Date();
  date.setDate(date.getDate() + 3);
  date.setHours(18, 0, 0, 0);
  return toDateTimeInputValue(date);
}

export function TeacherCheckpointsPage() {
  const navigate = useNavigate();
  const [checkpoints, setCheckpoints] = useState<CheckpointSummary[] | null>(null);
  const [groups, setGroups] = useState<Group[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [aiTheses, setAiTheses] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiProvider, setAiProvider] = useState<"demo" | "openai" | null>(null);
  const [form, setForm] = useState({
    title: "",
    description: "",
    deadline: defaultDeadline(),
    cost: "100",
    groupIds: [] as number[],
  });

  const load = useCallback(() => {
    setCheckpoints(null);
    setError(null);
    api<CheckpointSummary[]>("/checkpoints")
      .then(setCheckpoints)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить задачи"));
  }, []);

  useEffect(() => {
    load();
    api<Group[]>("/catalog/groups").then(setGroups).catch(() => setGroups([]));
  }, [load]);

  const totals = useMemo(() => {
    const list = checkpoints ?? [];
    return {
      total: list.length,
      submitted: list.reduce((acc, item) => acc + item.submittedCount, 0),
      overdue: list.reduce((acc, item) => acc + item.overdueCount, 0),
    };
  }, [checkpoints]);

  const toggleGroup = (id: number) => {
    setForm((current) => ({
      ...current,
      groupIds: current.groupIds.includes(id)
        ? current.groupIds.filter((groupId) => groupId !== id)
        : [...current.groupIds, id],
    }));
  };

  const generateDescription = async () => {
    if (!form.title.trim()) {
      setAiError("Сначала укажите название задачи");
      return;
    }
    if (!aiTheses.trim()) {
      setAiError("Опишите тезисы задания");
      return;
    }
    setAiLoading(true);
    setAiError(null);
    try {
      const response = await api<AiTextResponse>("/ai/draft-checkpoint", {
        method: "POST",
        body: { title: form.title.trim(), theses: aiTheses.trim() },
      });
      setForm((current) => ({ ...current, description: response.text }));
      setAiProvider(response.provider);
    } catch (err) {
      setAiError(err instanceof ApiError ? err.message : "Сервис ИИ временно недоступен");
    } finally {
      setAiLoading(false);
    }
  };

  const createCheckpoint = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);
    if (form.groupIds.length === 0) {
      setFormError("Выберите хотя бы одну группу");
      return;
    }
    setCreating(true);
    try {
      const created = await api<{ id: number }>("/checkpoints", {
        method: "POST",
        body: {
          title: form.title.trim(),
          description: form.description.trim(),
          deadline: toIsoFromLocalInput(form.deadline),
          cost: Number(form.cost),
          groupIds: form.groupIds,
        },
      });
      setCreateOpen(false);
      navigate(`/checkpoints/${created.id}`);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Не удалось создать задачу");
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль академического рейтинга"
        title="Мои задачи"
        subtitle="Контрольные точки, дедлайны и проверка работ студентов"
        actions={
          <Button icon="book" onClick={() => setCreateOpen(true)}>
            Создать задачу
          </Button>
        }
      />

      <div className="grid grid--3">
        <StatCard label="Всего задач" value={totals.total} icon="book" tone="green" />
        <StatCard label="На проверке" value={totals.submitted} icon="clock" tone="gold" />
        <StatCard label="Просрочено" value={totals.overdue} icon="alert" tone="red" />
      </div>

      {error ? <Alert variant="error" title="Ошибка">{error}</Alert> : null}

      {checkpoints === null ? (
        <PageLoader label="Загружаем задачи…" />
      ) : checkpoints.length === 0 ? (
        <Card>
          <EmptyState
            icon="book"
            title="Задач пока нет"
            description="Создайте контрольную точку для группы: студенты увидят её сразу, а по дедлайну начнётся списание АР."
            actions={<Button onClick={() => setCreateOpen(true)}>Создать первую задачу</Button>}
          />
        </Card>
      ) : (
        <div className="checkpoint-grid">
          {checkpoints.map((checkpoint) => {
            const overdue = new Date(checkpoint.deadline).getTime() < Date.now();
            return (
              <Card key={checkpoint.id} className="checkpoint-card">
                <div className="checkpoint-card__top">
                  <span className={`badge ${overdue ? "badge--status-overdue" : "badge--status-in_progress"}`}>
                    {overdue ? "Дедлайн прошёл" : "Активна"}
                  </span>
                  <span className="muted small">{checkpoint.cost} АР</span>
                </div>
                <Link to={`/checkpoints/${checkpoint.id}`} className="checkpoint-card__title">
                  {checkpoint.title}
                </Link>
                {checkpoint.description ? (
                  <p className="muted checkpoint-card__description">{checkpoint.description}</p>
                ) : null}
                <div className="checkpoint-card__meta">
                  <span>
                    <strong>Дедлайн:</strong> {formatDateTime(checkpoint.deadline)}
                  </span>
                  <span>
                    <strong>Группы:</strong> {checkpoint.groups.map((group) => group.name).join(", ")}
                  </span>
                </div>
                <div className="checkpoint-card__stats">
                  <span>{checkpoint.studentsCount} студентов</span>
                  <span className="text-gold">{checkpoint.submittedCount} на проверке</span>
                  <span className="text-red">{checkpoint.overdueCount} просрочено</span>
                </div>
                <Link className="button button--secondary button--sm" to={`/checkpoints/${checkpoint.id}`}>
                  Открыть ростер
                </Link>
              </Card>
            );
          })}
        </div>
      )}

      <p className="muted page-footnote">
        Студенты видят задачу сразу после создания. Списание АР выполняется фоновой задачей по
        настройкам университета.
      </p>

      <Modal
        open={createOpen}
        title="Новая контрольная точка"
        onClose={() => !creating && setCreateOpen(false)}
        wide
        footer={
          <>
            <Button variant="ghost" onClick={() => setCreateOpen(false)} disabled={creating}>
              Отмена
            </Button>
            <Button form="create-checkpoint-form" type="submit" loading={creating}>
              Создать задачу
            </Button>
          </>
        }
      >
        <form id="create-checkpoint-form" className="form" onSubmit={createCheckpoint}>
          {formError ? <Alert variant="error">{formError}</Alert> : null}
          <Field label="Название" htmlFor="cp-title" required>
            <Input
              id="cp-title"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              placeholder="Курсовая работа, 1 этап"
              required
              maxLength={255}
            />
          </Field>
          <div className="ai-panel">
            <div className="ai-panel__head">
              <strong>ИИ-помощник</strong>
              <span className="badge badge--gold">Модуль 11</span>
            </div>
            <Field
              label="Тезисы задания"
              htmlFor="cp-ai-theses"
              hint="Кратко перечислите цель, шаги и требования — ИИ соберёт черновик описания"
            >
              <Textarea
                id="cp-ai-theses"
                value={aiTheses}
                onChange={(event) => setAiTheses(event.target.value)}
                placeholder="Цель: первый раздел курсовой. Шаги: введение, обзор литературы, постановка задачи…"
                maxLength={3000}
              />
            </Field>
            <div className="ai-panel__actions">
              <Button
                type="button"
                variant="secondary"
                icon="sparkles"
                loading={aiLoading}
                onClick={generateDescription}
              >
                Сгенерировать описание
              </Button>
              {aiProvider ? (
                <span className="muted small">
                  {aiProvider === "demo" ? "Демо-режим ИИ (ключ не настроен)" : "Сгенерировано ИИ"}
                </span>
              ) : null}
            </div>
            {aiError ? <Alert variant="warning">{aiError}</Alert> : null}
          </div>
          <Field
            label="Описание"
            htmlFor="cp-description"
            hint="Что нужно сделать и как будет проверяться работа"
          >
            <Textarea
              id="cp-description"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              placeholder="Подготовьте первый раздел курсовой работы…"
              maxLength={5000}
            />
          </Field>
          <div className="form-row">
            <Field label="Дедлайн" htmlFor="cp-deadline" required>
              <Input
                id="cp-deadline"
                type="datetime-local"
                value={form.deadline}
                onChange={(event) => setForm({ ...form, deadline: event.target.value })}
                required
              />
            </Field>
            <Field label="Максимальное списание, АР" htmlFor="cp-cost" required>
              <Input
                id="cp-cost"
                type="number"
                min={1}
                max={1000}
                value={form.cost}
                onChange={(event) => setForm({ ...form, cost: event.target.value })}
                required
              />
            </Field>
          </div>
          <div className="field">
            <span className="field__label">
              Группы<span className="field__required"> *</span>
            </span>
            <div className="group-picker">
              {groups.map((group) => (
                <label
                  key={group.id}
                  className={`group-picker__item ${form.groupIds.includes(group.id) ? "group-picker__item--active" : ""}`}
                >
                  <input
                    type="checkbox"
                    checked={form.groupIds.includes(group.id)}
                    onChange={() => toggleGroup(group.id)}
                  />
                  <span>{group.name}</span>
                  <small>{group.facultyName}</small>
                </label>
              ))}
            </div>
            <span className="field__hint">Студенты подтверждённых групп получат задачу автоматически.</span>
          </div>
        </form>
      </Modal>
    </>
  );
}
