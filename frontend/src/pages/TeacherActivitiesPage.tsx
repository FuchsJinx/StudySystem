import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { ActivitySummary, ActivityType } from "../shared/api/activityTypes";
import type { Group } from "../shared/api/types";
import { formatDate, toDateInputValue } from "../shared/format";
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

function defaultEventDate(): string {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return toDateInputValue(date);
}

function typeCurrency(type: ActivityType): string {
  return type === "ACADEMIC" ? "БУА" : "БОА";
}

export function TeacherActivitiesPage() {
  const navigate = useNavigate();
  const [activities, setActivities] = useState<ActivitySummary[] | null>(null);
  const [groups, setGroups] = useState<Group[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({
    title: "",
    description: "",
    type: "ACADEMIC" as ActivityType,
    eventDate: defaultEventDate(),
    maxPoints: "100",
    groupIds: [] as number[],
  });

  const load = useCallback(() => {
    setActivities(null);
    setError(null);
    api<ActivitySummary[]>("/activities")
      .then(setActivities)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить активности"));
  }, []);

  useEffect(() => {
    load();
    api<Group[]>("/catalog/groups").then(setGroups).catch(() => setGroups([]));
  }, [load]);

  const stats = useMemo(() => {
    const list = activities ?? [];
    return {
      total: list.length,
      awarded: list.reduce((acc, item) => acc + item.awardedCount, 0),
      enrolled: list.reduce((acc, item) => acc + item.enrolledCount, 0),
    };
  }, [activities]);

  const toggleGroup = (id: number) => {
    setForm((current) => ({
      ...current,
      groupIds: current.groupIds.includes(id)
        ? current.groupIds.filter((groupId) => groupId !== id)
        : [...current.groupIds, id],
    }));
  };

  const createActivity = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);
    if (form.groupIds.length === 0) {
      setFormError("Выберите хотя бы одну группу");
      return;
    }
    setCreating(true);
    try {
      const created = await api<{ id: number }>("/activities", {
        method: "POST",
        body: {
          title: form.title.trim(),
          description: form.description.trim(),
          type: form.type,
          eventDate: form.eventDate,
          maxPoints: Number(form.maxPoints),
          groupIds: form.groupIds,
        },
      });
      setCreateOpen(false);
      navigate(`/activities/${created.id}`);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Не удалось создать активность");
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль 3"
        title="Активности"
        subtitle="Учебные и общественные мероприятия, начисление БУА и БОА в рамках бюджета"
        actions={
          <Button icon="megaphone" onClick={() => setCreateOpen(true)}>
            Создать активность
          </Button>
        }
      />

      <div className="grid grid--3">
        <StatCard label="Всего активностей" value={stats.total} icon="megaphone" tone="green" />
        <StatCard label="Записалось студентов" value={stats.enrolled} icon="users" tone="blue" />
        <StatCard label="Награждено" value={stats.awarded} icon="award" tone="gold" />
      </div>

      {error ? (
        <Alert variant="error" title="Ошибка">
          {error}
        </Alert>
      ) : null}

      {activities === null ? (
        <PageLoader label="Загружаем активности…" />
      ) : activities.length === 0 ? (
        <Card>
          <EmptyState
            icon="megaphone"
            title="Активностей пока нет"
            description="Создайте мероприятие: студенты увидят его и узнают, сколько баллов можно получить."
            actions={<Button onClick={() => setCreateOpen(true)}>Создать активность</Button>}
          />
        </Card>
      ) : (
        <div className="activity-grid">
          {activities.map((activity) => (
            <Card key={activity.id} className="activity-card">
              <div className="activity-card__top">
                <span className={`badge activity-badge activity-badge--${activity.type.toLowerCase()}`}>
                  {activity.type === "ACADEMIC" ? "Учебная" : "Общественная"}
                </span>
                <span className="muted small">до {activity.maxPoints} {typeCurrency(activity.type)}</span>
              </div>
              <Link to={`/activities/${activity.id}`} className="activity-card__title">
                {activity.title}
              </Link>
              {activity.description ? (
                <p className="muted activity-card__desc">{activity.description}</p>
              ) : null}
              <div className="activity-card__meta">
                <span><strong>Дата:</strong> {formatDate(activity.eventDate)}</span>
                <span>
                  <strong>Группы:</strong> {activity.groups.map((group) => group.name).join(", ")}
                </span>
              </div>
              <div className="activity-card__footer">
                <span className="muted small">
                  {activity.enrolledCount} записано · {activity.awardedCount} награждено
                </span>
                <Link className="button button--secondary button--sm" to={`/activities/${activity.id}`}>
                  Начислить баллы
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={createOpen}
        title="Новая активность"
        onClose={() => !creating && setCreateOpen(false)}
        wide
        footer={
          <>
            <Button variant="ghost" onClick={() => setCreateOpen(false)} disabled={creating}>
              Отмена
            </Button>
            <Button form="create-activity-form" type="submit" loading={creating}>
              Создать
            </Button>
          </>
        }
      >
        <form id="create-activity-form" className="form" onSubmit={createActivity}>
          {formError ? <Alert variant="error">{formError}</Alert> : null}
          <Field label="Название" htmlFor="act-title" required>
            <Input
              id="act-title"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              placeholder="Помощь в организации конференции"
              maxLength={255}
              required
            />
          </Field>
          <Field label="Описание" htmlFor="act-description" hint="Что предстоит сделать студентам">
            <Textarea
              id="act-description"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              maxLength={5000}
            />
          </Field>
          <div className="field">
            <span className="field__label">Тип активности<span className="field__required"> *</span></span>
            <div className="type-picker">
              <button
                type="button"
                className={`type-picker__item ${form.type === "ACADEMIC" ? "type-picker__item--active" : ""}`}
                onClick={() => setForm({ ...form, type: "ACADEMIC" })}
              >
                <strong>Учебная</strong>
                <small>Начисляются БУА</small>
              </button>
              <button
                type="button"
                className={`type-picker__item ${form.type === "SOCIAL" ? "type-picker__item--active" : ""}`}
                onClick={() => setForm({ ...form, type: "SOCIAL" })}
              >
                <strong>Общественная</strong>
                <small>Начисляются БОА, нужна запись</small>
              </button>
            </div>
          </div>
          <div className="form-row">
            <Field label="Дата проведения" htmlFor="act-date" required>
              <Input
                id="act-date"
                type="date"
                value={form.eventDate}
                onChange={(event) => setForm({ ...form, eventDate: event.target.value })}
                required
              />
            </Field>
            <Field label="Максимум баллов" htmlFor="act-max" required>
              <Input
                id="act-max"
                type="number"
                min={1}
                max={1000}
                value={form.maxPoints}
                onChange={(event) => setForm({ ...form, maxPoints: event.target.value })}
                required
              />
            </Field>
          </div>
          <div className="field">
            <span className="field__label">Группы<span className="field__required"> *</span></span>
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
          </div>
        </form>
      </Modal>
    </>
  );
}
