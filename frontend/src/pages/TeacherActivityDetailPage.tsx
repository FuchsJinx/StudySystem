import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type {
  ActivitySummary,
  AwardResponse,
  BudgetOverview,
  Participant,
} from "../shared/api/activityTypes";
import { formatDate } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  PageHeader,
  PageLoader,
  ProgressBar,
  Select,
} from "../shared/ui";

const LEVELS = [25, 50, 75, 100];

export function TeacherActivityDetailPage() {
  const { id } = useParams();
  const activityId = Number(id);
  const [activity, setActivity] = useState<ActivitySummary | null>(null);
  const [participants, setParticipants] = useState<Participant[] | null>(null);
  const [budget, setBudget] = useState<BudgetOverview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<Record<number, number>>({});
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    if (!activityId || Number.isNaN(activityId)) {
      setError("Некорректная активность");
      return;
    }
    setError(null);
    api<ActivitySummary>(`/activities/${activityId}`)
      .then(setActivity)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить активность"));
    api<Participant[]>(`/activities/${activityId}/participants`)
      .then(setParticipants)
      .catch(() => setParticipants([]));
    api<BudgetOverview>("/teachers/me/budgets")
      .then(setBudget)
      .catch(() => setBudget(null));
  }, [activityId]);

  useEffect(() => {
    load();
  }, [load]);

  const groupBudgets = useMemo(() => {
    if (!activity || !budget) return [];
    const ids = new Set(activity.groups.map((group) => group.id));
    return budget.groups.filter((group) => ids.has(group.groupId));
  }, [activity, budget]);

  const awardable = useCallback(
    (participant: Participant) => {
      if (!activity || participant.awardedPoints !== null) return false;
      return activity.type === "ACADEMIC" || participant.enrolled;
    },
    [activity],
  );

  const toggle = (participant: Participant) => {
    if (!awardable(participant)) return;
    setSelected((current) => {
      if (current[participant.studentUserId] !== undefined) {
        const next = { ...current };
        delete next[participant.studentUserId];
        return next;
      }
      return { ...current, [participant.studentUserId]: 50 };
    });
  };

  const setLevel = (studentUserId: number, level: number) => {
    setSelected((current) => ({ ...current, [studentUserId]: level }));
  };

  const selectedCount = Object.keys(selected).length;

  const submitAward = async () => {
    if (!activity || selectedCount === 0) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await api<AwardResponse>(`/activities/${activity.id}/award`, {
        method: "POST",
        body: {
          awards: Object.entries(selected).map(([studentId, level]) => ({
            studentId: Number(studentId),
            level,
          })),
        },
      });
      const total = response.awarded.reduce((acc, item) => acc + item.points, 0);
      setNotice(`Начислено ${total} баллов: ${response.awarded.length} студентов`);
      setSelected({});
      setConfirmOpen(false);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось начислить баллы");
      setConfirmOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  if (error && !activity) {
    return (
      <>
        <PageHeader title="Активность" />
        <Alert variant="error" title="Активность недоступна">{error}</Alert>
        <Link to="/activities" className="text-link">← К активностям</Link>
      </>
    );
  }

  if (!activity || participants === null) {
    return <PageLoader label="Загружаем активность…" />;
  }

  return (
    <>
      <PageHeader
        eyebrow="Начисление баллов активности"
        title={activity.title}
        subtitle={`${formatDate(activity.eventDate)} · ${activity.type === "ACADEMIC" ? "учебная" : "общественная"} · до ${activity.maxPoints} баллов`}
        actions={
          <Link to="/activities">
            <Button variant="ghost">← Все активности</Button>
          </Link>
        }
      />

      {notice ? (
        <Alert variant="success" title="Готово">
          {notice}
        </Alert>
      ) : null}
      {error ? (
        <Alert variant="error" title="Ошибка">
          {error}
        </Alert>
      ) : null}

      {activity.description ? (
        <Card>
          <h2>Описание</h2>
          <p className="muted pre-wrap">{activity.description}</p>
        </Card>
      ) : null}

      <div className="budget-mini-grid">
        {groupBudgets.map((group) => (
          <Card key={group.groupId} className="budget-mini">
            <div className="section-head">
              <span>Бюджет · {group.groupName}</span>
              <strong>{group.remaining} / {group.limit}</strong>
            </div>
            <ProgressBar
              value={group.spent}
              max={group.limit}
              tone={group.remaining === 0 ? "red" : "gold"}
            />
          </Card>
        ))}
      </div>

      <Card>
        <div className="section-head">
          <div>
            <h2>Участники</h2>
            <p className="muted">
              Отметьте студентов и уровень участия — баллы рассчитываются от максимума активности.
            </p>
          </div>
          <Button disabled={selectedCount === 0} onClick={() => setConfirmOpen(true)}>
            Начислить выбранным ({selectedCount})
          </Button>
        </div>

        {participants.length === 0 ? (
          <EmptyState
            icon="users"
            title="В целевых группах нет подтверждённых студентов"
            description="Баллы можно начислять только подтверждённым студентам целевых групп."
          />
        ) : (
          <div className="table-wrap">
            <table className="table roster-table">
              <thead>
                <tr>
                  <th></th>
                  <th>Студент</th>
                  <th>Группа</th>
                  <th>Запись</th>
                  <th>Уровень</th>
                  <th>Баллы</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                {participants.map((participant) => {
                  const isSelected = selected[participant.studentUserId] !== undefined;
                  const level = isSelected ? selected[participant.studentUserId] : 50;
                  const points = Math.floor((activity.maxPoints * level) / 100);
                  const enabled = awardable(participant);
                  return (
                    <tr key={participant.studentUserId} className={isSelected ? "row--selected" : ""}>
                      <td>
                        <input
                          type="checkbox"
                          aria-label={`Выбрать ${participant.fullName}`}
                          checked={isSelected}
                          disabled={!enabled}
                          onChange={() => toggle(participant)}
                        />
                      </td>
                      <td>
                        <Link to={`/users/${participant.studentUserId}`} className="table-person">
                          {participant.fullName}
                        </Link>
                      </td>
                      <td>{participant.groupName}</td>
                      <td>
                        {activity.type === "SOCIAL" ? (
                          participant.enrolled ? (
                            <span className="text-green">Записан</span>
                          ) : (
                            <span className="text-red">Не записан</span>
                          )
                        ) : (
                          <span className="muted">Не требуется</span>
                        )}
                      </td>
                      <td>
                        <Select
                          aria-label={`Уровень участия ${participant.fullName}`}
                          value={isSelected ? level : 50}
                          disabled={!isSelected}
                          onChange={(event) =>
                            setLevel(participant.studentUserId, Number(event.target.value))
                          }
                        >
                          {LEVELS.map((value) => (
                            <option key={value} value={value}>{value} %</option>
                          ))}
                        </Select>
                      </td>
                      <td>{isSelected ? `${points}` : "—"}</td>
                      <td>
                        {participant.awardedPoints !== null ? (
                          <span className="badge badge--status-accepted">
                            Начислено {participant.awardedPoints}
                          </span>
                        ) : enabled ? (
                          <span className="muted small">Можно начислить</span>
                        ) : (
                          <span className="muted small">Нет записи</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="Начислить баллы?"
        description={`Будет начислено ${Object.entries(selected)
          .map(([, level]) => Math.floor((activity.maxPoints * level) / 100))
          .reduce((acc, value) => acc + value, 0)} баллов ${selectedCount} студентам. Бюджет групп будет уменьшен.`}
        confirmLabel="Начислить"
        loading={submitting}
        onConfirm={submitAward}
        onClose={() => !submitting && setConfirmOpen(false)}
      />
    </>
  );
}
