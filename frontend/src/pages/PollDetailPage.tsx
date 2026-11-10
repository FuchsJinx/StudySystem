import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type {
  PollDetail,
  PollQuestionResult,
  PollResults,
  VoteAnswer,
} from "../shared/api/pollTypes";
import { useAuth } from "../shared/auth/AuthContext";
import { formatDateTime } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  PageHeader,
  PageLoader,
} from "../shared/ui";

function ResultBars({ results }: { results: PollQuestionResult[] }) {
  return (
    <div className="poll-results">
      {results.map((question) => (
        <div className="poll-result" key={question.questionId}>
          <div className="poll-result__head">
            <h3>{question.text}</h3>
            <span className="muted small">
              {question.type === "SINGLE" ? "один вариант" : "несколько вариантов"} ·{" "}
              {question.totalVotes} ответов
            </span>
          </div>
          <div className="poll-result__options">
            {question.options.map((option) => (
              <div className="poll-result__option" key={option.optionId}>
                <div className="poll-result__label">
                  <span>{option.text}</span>
                  <strong>{option.count} · {option.percent}%</strong>
                </div>
                <div className="poll-result__track">
                  <div className="poll-result__fill" style={{ width: `${Math.max(option.percent, 0)}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

export function PollDetailPage() {
  const { id } = useParams();
  const pollId = Number(id);
  const { user } = useAuth();
  const isTeacher = user?.role === "TEACHER";
  const [poll, setPoll] = useState<PollDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Record<number, number[]>>({});
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!pollId || Number.isNaN(pollId)) {
      setError("Некорректный опрос");
      return;
    }
    setError(null);
    api<PollDetail>(`/polls/${pollId}`)
      .then(setPoll)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить опрос"));
  }, [pollId]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleOption = (questionId: number, optionId: number, multiple: boolean) => {
    setAnswers((current) => {
      const selected = current[questionId] ?? [];
      if (!multiple) {
        return { ...current, [questionId]: [optionId] };
      }
      return {
        ...current,
        [questionId]: selected.includes(optionId)
          ? selected.filter((id) => id !== optionId)
          : [...selected, optionId],
      };
    });
  };

  const submitVote = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!poll) return;
    setError(null);
    const voteAnswers: VoteAnswer[] = [];
    for (const question of poll.questions) {
      const selected = answers[question.id] ?? [];
      if (selected.length === 0) {
        setError(`Ответьте на вопрос «${question.text}»`);
        return;
      }
      voteAnswers.push({ questionId: question.id, optionIds: selected });
    }
    setSubmitting(true);
    try {
      const results = await api<PollResults>(`/polls/${poll.id}/vote`, {
        method: "POST",
        body: { answers: voteAnswers },
      });
      setNotice(`Голос принят. Всего ответов: ${results.totalResponses}`);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось отправить голос");
    } finally {
      setSubmitting(false);
    }
  };

  if (error && !poll) {
    return (
      <>
        <PageHeader title="Опрос" />
        <Alert variant="error" title="Опрос недоступен">{error}</Alert>
        <Link to="/polls" className="text-link">← К опросам</Link>
      </>
    );
  }
  if (!poll) {
    return <PageLoader label="Загружаем опрос…" />;
  }

  const totalResponses = poll.results?.[0]?.totalVotes ?? 0;
  const showResults = poll.results !== null;

  return (
    <>
      <PageHeader
        eyebrow={isTeacher ? "Статистика опроса" : "Опрос"}
        title={poll.title}
        subtitle={`До ${formatDateTime(poll.endsAt)} · ${poll.targetGroups.map((group) => group.name).join(", ")}`}
        actions={
          <Link to="/polls">
            <Button variant="ghost">← Все опросы</Button>
          </Link>
        }
      />

      <div className="poll-meta">
        <span className={`badge ${poll.status === "ACTIVE" ? "badge--status-accepted" : "badge--status-overdue"}`}>
          {poll.status === "ACTIVE" ? "Активен" : "Завершён"}
        </span>
        {poll.isAnonymous ? <span className="badge">Анонимный</span> : <span className="badge">Открытый</span>}
        <span className="muted small">Автор: {poll.teacherName}</span>
        {poll.hasVoted ? <span className="badge badge--gold">Вы проголосовали</span> : null}
        {showResults ? <span className="muted small">Ответов: {totalResponses}</span> : null}
      </div>

      {notice ? (
        <Alert variant="success" title="Голос принят">
          {notice}
        </Alert>
      ) : null}
      {error ? (
        <Alert variant="error" title="Ошибка">{error}</Alert>
      ) : null}

      {poll.description ? (
        <Card>
          <p className="muted pre-wrap">{poll.description}</p>
        </Card>
      ) : null}

      {poll.canVote ? (
        <Card>
          <h2>Ваш голос</h2>
          <form className="form" onSubmit={submitVote}>
            {poll.questions.map((question) => {
              const multiple = question.type === "MULTIPLE";
              const selected = answers[question.id] ?? [];
              return (
                <fieldset className="poll-question" key={question.id}>
                  <legend>{question.text}</legend>
                  <p className="muted small">
                    {multiple ? "Можно выбрать несколько вариантов" : "Выберите один вариант"}
                  </p>
                  <div className="poll-options">
                    {question.options.map((option) => (
                      <label
                        key={option.id}
                        className={`poll-option ${selected.includes(option.id) ? "poll-option--selected" : ""}`}
                      >
                        <input
                          type={multiple ? "checkbox" : "radio"}
                          name={`question-${question.id}`}
                          checked={selected.includes(option.id)}
                          onChange={() => toggleOption(question.id, option.id, multiple)}
                        />
                        <span>{option.text}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
              );
            })}
            <Button type="submit" loading={submitting}>Отправить голос</Button>
          </form>
        </Card>
      ) : showResults && poll.results ? (
        <Card>
          <div className="section-head">
            <h2>Результаты</h2>
            <span className="muted small">
              {poll.status === "ENDED" ? "Опрос завершён" : "Вы уже проголосовали"}
            </span>
          </div>
          <ResultBars results={poll.results} />
        </Card>
      ) : (
        <Card>
          <EmptyState
            icon="poll"
            title="Голосование недоступно"
            description={
              poll.status === "ENDED"
                ? "Опрос завершён. Результаты появятся после обработки."
                : "Для голосования нужно подтвердить email и принадлежность к группе."
            }
          />
        </Card>
      )}

      {isTeacher ? (
        <>
          {poll.isAnonymous ? (
            <Alert variant="info" title="Анонимный опрос">
              Персональные ответы скрыты. Доступны только агрегированные результаты по вариантам.
            </Alert>
          ) : (
            <Card>
              <h2>Кто как ответил</h2>
              {poll.voters.length === 0 ? (
                <EmptyState
                  icon="users"
                  title="Ответов пока нет"
                  description="Персональные ответы появятся, когда студенты проголосуют."
                />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Студент</th>
                        <th>Группа</th>
                        {poll.questions.map((question) => (
                          <th key={question.id}>{question.text}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {poll.voters.map((voter, voterIndex) => (
                        <tr key={`${voter.studentName}-${voterIndex}`}>
                          <td>{voter.studentName}</td>
                          <td>{voter.groupName}</td>
                          {poll.questions.map((question) => {
                            const answer = voter.answers.find((item) => item.questionId === question.id);
                            return <td key={question.id}>{answer?.options.join(", ") || "—"}</td>;
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          )}
        </>
      ) : null}
    </>
  );
}
