import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { ActivitySummary } from "../shared/api/activityTypes";
import { useAuth } from "../shared/auth/AuthContext";
import { formatDate } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  PageHeader,
  PageLoader,
  StatCard,
} from "../shared/ui";

function typeLabel(type: ActivitySummary["type"]) {
  return type === "ACADEMIC" ? "Учебная" : "Общественная";
}

function typeCurrency(type: ActivitySummary["type"]) {
  return type === "ACADEMIC" ? "БУА" : "БОА";
}

export function StudentActivitiesPage() {
  const { user } = useAuth();
  const [activities, setActivities] = useState<ActivitySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [enrollingId, setEnrollingId] = useState<number | null>(null);

  const load = useCallback(() => {
    setActivities(null);
    setError(null);
    api<ActivitySummary[]>("/users/me/activities")
      .then(setActivities)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить активности"));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const canParticipate = Boolean(
    user?.emailVerified && user?.student?.membershipStatus === "CONFIRMED",
  );

  const stats = useMemo(() => {
    const list = activities ?? [];
    return {
      total: list.length,
      enrolled: list.filter((item) => item.enrolled).length,
      awarded: list.filter((item) => item.myAwardedPoints !== null).length,
    };
  }, [activities]);

  const enroll = async (activity: ActivitySummary) => {
    setEnrollingId(activity.id);
    setError(null);
    setNotice(null);
    try {
      const updated = await api<ActivitySummary>(`/activities/${activity.id}/enroll`, { method: "POST" });
      setActivities((current) =>
        (current ?? []).map((item) => (item.id === updated.id ? updated : item)),
      );
      setNotice(`Вы записаны на «${activity.title}»`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось записаться");
    } finally {
      setEnrollingId(null);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль 3"
        title="Активности"
        subtitle="Участвуйте в учебных и общественных мероприятиях и получайте баллы БУА и БОА"
      />

      <div className="grid grid--3">
        <StatCard label="Доступно активностей" value={stats.total} icon="megaphone" tone="green" />
        <StatCard label="Вы записаны" value={stats.enrolled} icon="check" tone="blue" />
        <StatCard label="Получено наград" value={stats.awarded} icon="award" tone="gold" />
      </div>

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
      {!canParticipate ? (
        <Alert variant="warning" title="Участие ограничено">
          Чтобы записываться на активности, подтвердите email и принадлежность к группе.
        </Alert>
      ) : null}

      {activities === null ? (
        <PageLoader label="Загружаем активности…" />
      ) : activities.length === 0 ? (
        <Card>
          <EmptyState
            icon="megaphone"
            title="Активностей пока нет"
            description="Преподаватели создадут мероприятия для вашей группы — они появятся здесь."
          />
        </Card>
      ) : (
        <div className="activity-grid">
          {activities.map((activity) => {
            const isPast = new Date(activity.eventDate).getTime() < Date.now() - 24 * 60 * 60 * 1000;
            const canEnroll =
              activity.type === "SOCIAL" && !activity.enrolled && !isPast && canParticipate;
            return (
              <Card key={activity.id} className="activity-card">
                <div className="activity-card__top">
                  <span className={`badge activity-badge activity-badge--${activity.type.toLowerCase()}`}>
                    {typeLabel(activity.type)}
                  </span>
                  <span className="muted small">до {activity.maxPoints} {typeCurrency(activity.type)}</span>
                </div>
                <h3>{activity.title}</h3>
                {activity.description ? (
                  <p className="muted activity-card__desc">{activity.description}</p>
                ) : null}
                <div className="activity-card__meta">
                  <span>
                    <strong>Дата:</strong> {formatDate(activity.eventDate)}
                  </span>
                  <span>
                    <strong>Группы:</strong> {activity.groups.map((group) => group.name).join(", ")}
                  </span>
                  <span>
                    <strong>Преподаватель:</strong> {activity.teacherName}
                  </span>
                </div>
                <div className="activity-card__footer">
                  {activity.myAwardedPoints !== null ? (
                    <span className="badge badge--status-accepted">
                      Начислено +{activity.myAwardedPoints} {typeCurrency(activity.type)}
                      {activity.myAwardedLevel ? ` · ${activity.myAwardedLevel}%` : ""}
                    </span>
                  ) : activity.type === "SOCIAL" ? (
                    canEnroll ? (
                      <Button
                        size="sm"
                        onClick={() => enroll(activity)}
                        loading={enrollingId === activity.id}
                      >
                        Записаться
                      </Button>
                    ) : activity.enrolled ? (
                      <span className="badge badge--membership-confirmed">Вы записаны</span>
                    ) : (
                      <span className="muted small">Запись недоступна</span>
                    )
                  ) : (
                    <span className="muted small">Участие отмечает преподаватель</span>
                  )}
                  <span className="muted small">
                    {activity.enrolledCount} записано · {activity.awardedCount} награждено
                  </span>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <p className="muted page-footnote">
        Награды за активности публикуются в <Link to="/feed" className="text-link">ленте наград</Link>.
        Баллы БУА и БОА хранятся раздельно и отображаются в профиле.
      </p>
    </>
  );
}
