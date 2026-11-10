import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { PollSummary } from "../shared/api/pollTypes";
import { useAuth } from "../shared/auth/AuthContext";
import { formatDateTime } from "../shared/format";
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader } from "../shared/ui";

export function PollsPage() {
  const { user } = useAuth();
  const isTeacher = user?.role === "TEACHER";
  const [polls, setPolls] = useState<PollSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setPolls(null);
    setError(null);
    api<PollSummary[]>("/polls")
      .then(setPolls)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить опросы"));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <>
      <PageHeader
        eyebrow="Модуль 5"
        title="Опросы"
        subtitle={
          isTeacher
            ? "Создавайте опросы и смотрите детальную статистику ответов"
            : "Голосуйте в опросах вашей группы и смотрите агрегированные результаты"
        }
        actions={
          isTeacher ? (
            <Link to="/polls/new">
              <Button icon="poll">Создать опрос</Button>
            </Link>
          ) : null
        }
      />

      {error ? (
        <Alert variant="error" title="Ошибка">{error}</Alert>
      ) : null}

      {polls === null ? (
        <PageLoader label="Загружаем опросы…" />
      ) : polls.length === 0 ? (
        <Card>
          <EmptyState
            icon="poll"
            title="Опросов пока нет"
            description={
              isTeacher
                ? "Создайте первый опрос для группы — студенты увидят его в этом разделе."
                : "Преподаватель ещё не создал опросы для вашей группы."
            }
            actions={
              isTeacher ? (
                <Link to="/polls/new">
                  <Button>Создать опрос</Button>
                </Link>
              ) : null
            }
          />
        </Card>
      ) : (
        <div className="poll-grid">
          {polls.map((poll) => (
            <Card key={poll.id} className="poll-card">
              <div className="poll-card__top">
                <span className={`badge ${poll.status === "ACTIVE" ? "badge--status-accepted" : "badge--status-overdue"}`}>
                  {poll.status === "ACTIVE" ? "Активен" : "Завершён"}
                </span>
                <div className="poll-card__chips">
                  {poll.isAnonymous ? <span className="badge">Анонимный</span> : null}
                  {poll.hasVoted ? <span className="badge badge--gold">Вы проголосовали</span> : null}
                </div>
              </div>
              <Link to={`/polls/${poll.id}`} className="poll-card__title">{poll.title}</Link>
              <div className="poll-card__meta">
                <span>До {formatDateTime(poll.endsAt)}</span>
                <span>{poll.questionCount} вопросов</span>
                <span>{poll.targetGroups.map((group) => group.name).join(", ")}</span>
              </div>
              <Link className="button button--secondary button--sm" to={`/polls/${poll.id}`}>
                {isTeacher ? "Статистика" : poll.hasVoted || poll.status === "ENDED" ? "Результаты" : "Проголосовать"}
              </Link>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
