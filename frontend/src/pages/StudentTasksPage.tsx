import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError, downloadFile } from "../shared/api/client";
import type { StudentCheckpoint } from "../shared/api/checkpointTypes";
import { useAuth } from "../shared/auth/AuthContext";
import { formatDateTime, plural } from "../shared/format";
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
  ProgressBar,
  StatusBadge,
} from "../shared/ui";

const ACTIVE_STATUSES = ["IN_PROGRESS", "SUBMITTED", "OVERDUE", "RETURNED"];

export function StudentTasksPage() {
  const { user } = useAuth();
  const [checkpoints, setCheckpoints] = useState<StudentCheckpoint[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [submitTarget, setSubmitTarget] = useState<StudentCheckpoint | null>(null);
  const [url, setUrl] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const allowed = Boolean(user?.emailVerified && user?.student?.membershipStatus === "CONFIRMED");

  const load = useCallback(() => {
    setError(null);
    api<StudentCheckpoint[]>("/users/me/checkpoints")
      .then(setCheckpoints)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить задания"));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const active = useMemo(
    () => (checkpoints ?? []).filter((item) => ACTIVE_STATUSES.includes(item.status)),
    [checkpoints],
  );
  const completed = useMemo(
    () => (checkpoints ?? []).filter((item) => item.status === "ACCEPTED"),
    [checkpoints],
  );
  const overdueCount = useMemo(
    () => (checkpoints ?? []).filter((item) => item.status === "OVERDUE").length,
    [checkpoints],
  );

  const openSubmit = (checkpoint: StudentCheckpoint) => {
    setSubmitTarget(checkpoint);
    setUrl(checkpoint.submissionUrl ?? "");
    setFile(null);
    setFormError(null);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!submitTarget) return;
    if (!url.trim() && !file) {
      setFormError("Приложите ссылку на работу или файл");
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      if (file) {
        const formData = new FormData();
        if (url.trim()) formData.append("url", url.trim());
        formData.append("file", file);
        await api(`/checkpoints/${submitTarget.checkpointId}/submit`, {
          method: "POST",
          form: formData,
        });
      } else {
        await api(`/checkpoints/${submitTarget.checkpointId}/submit`, {
          method: "POST",
          body: { url: url.trim() },
        });
      }
      setSubmitTarget(null);
      setNotice(`Работа по «${submitTarget.title}» отправлена на проверку`);
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Не удалось отправить работу");
    } finally {
      setSubmitting(false);
    }
  };

  const renderCard = (checkpoint: StudentCheckpoint) => {
    const canSubmit = allowed && ["IN_PROGRESS", "OVERDUE", "RETURNED"].includes(checkpoint.status);
    const deadlinePassed = new Date(checkpoint.deadline).getTime() < Date.now();
    return (
      <Card key={checkpoint.submissionId} className="task-card">
        <div className="task-card__top">
          <StatusBadge status={checkpoint.status} />
          <span className="muted small">
            {checkpoint.cost} АР · {checkpoint.teacherName}
          </span>
        </div>
        <h3>{checkpoint.title}</h3>
        {checkpoint.description ? <p className="muted task-card__desc">{checkpoint.description}</p> : null}
        <div className="task-card__deadline">
          <span className={deadlinePassed ? "text-red" : ""}>
            Дедлайн: {formatDateTime(checkpoint.deadline)}
          </span>
          {deadlinePassed && !["ACCEPTED", "SUBMITTED"].includes(checkpoint.status) ? (
            <span className="text-red">просрочено</span>
          ) : null}
        </div>
        <ProgressBar
          value={checkpoint.arDebited}
          max={checkpoint.cost}
          tone={checkpoint.arDebited >= checkpoint.cost ? "red" : "gold"}
          label="Списано АР"
        />
        {checkpoint.returnComment ? (
          <Alert variant="warning" title="Комментарий преподавателя">
            {checkpoint.returnComment}
          </Alert>
        ) : null}
        {checkpoint.submittedAt ? (
          <p className="muted small">Сдано: {formatDateTime(checkpoint.submittedAt)}</p>
        ) : null}
        <div className="task-card__links">
          {checkpoint.submissionUrl ? (
            <a href={checkpoint.submissionUrl} target="_blank" rel="noreferrer" className="text-link">
              Открыть ссылку на работу
            </a>
          ) : null}
          {checkpoint.fileName ? (
            <button
              type="button"
              className="text-link text-link--button"
              onClick={() => downloadFile(`/files/submissions/${checkpoint.submissionId}`, checkpoint.fileName!)}
            >
              Скачать {checkpoint.fileName}
            </button>
          ) : null}
        </div>
        {canSubmit ? (
          <Button icon="upload" onClick={() => openSubmit(checkpoint)}>
            {checkpoint.status === "RETURNED" ? "Отправить повторно" : "Сдать работу"}
          </Button>
        ) : null}
      </Card>
    );
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль академического рейтинга"
        title="Мои задачи"
        subtitle="Сдайте работу до дедлайна, чтобы сохранить академический рейтинг"
      />

      {notice ? (
        <Alert variant="success" title="Готово">
          {notice}
        </Alert>
      ) : null}
      {!allowed ? (
        <Alert variant="warning" title="Сдача работ пока недоступна">
          Для сдачи работы необходимо подтвердить email и принадлежность к группе.
        </Alert>
      ) : null}
      {error ? (
        <Alert variant="error" title="Ошибка">
          {error}
        </Alert>
      ) : null}

      {checkpoints === null ? (
        <PageLoader label="Загружаем задания…" />
      ) : checkpoints.length === 0 ? (
        <Card>
          <EmptyState
            icon="book"
            title="Заданий пока нет"
            description="Когда преподаватель создаст контрольную точку для вашей группы, она появится здесь."
          />
        </Card>
      ) : (
        <>
          <div className="section-title-row">
            <h2>Активные задания</h2>
            <span className="muted">
              {active.length} {plural(active.length, "задание", "задания", "заданий")}
              {overdueCount > 0 ? ` · ${overdueCount} просрочено` : ""}
            </span>
          </div>
          {active.length === 0 ? (
            <Card>
              <EmptyState
                icon="check"
                title="Все задания выполнены"
                description="Активных задач нет. Завершённые работы доступны ниже."
              />
            </Card>
          ) : (
            <div className="task-grid">{active.map(renderCard)}</div>
          )}

          {completed.length > 0 ? (
            <>
              <div className="section-title-row">
                <h2>Завершённые</h2>
                <span className="muted">{completed.length}</span>
              </div>
              <div className="task-grid">{completed.map(renderCard)}</div>
            </>
          ) : null}
        </>
      )}

      <p className="muted page-footnote">
        <Link className="text-link" to="/profile">История списаний АР</Link> доступна в профиле. Если
        работа возвращена после дедлайна, списание возобновляется до повторной сдачи.
      </p>

      <Modal
        open={submitTarget !== null}
        title={submitTarget ? `Сдача: ${submitTarget.title}` : "Сдача работы"}
        onClose={() => !submitting && setSubmitTarget(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSubmitTarget(null)} disabled={submitting}>
              Отмена
            </Button>
            <Button form="submit-work-form" type="submit" loading={submitting}>
              Отправить на проверку
            </Button>
          </>
        }
      >
        {submitTarget ? (
          <form id="submit-work-form" className="form" onSubmit={submit}>
            {formError ? <Alert variant="error">{formError}</Alert> : null}
            <Field
              label="Ссылка на работу"
              htmlFor="submission-url"
              hint="Например, ссылка на GitHub, облачный диск или документ"
            >
              <Input
                id="submission-url"
                type="url"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                placeholder="https://…"
              />
            </Field>
            <Field
              label="Файл"
              htmlFor="submission-file"
              hint="PDF, ZIP, DOC, DOCX или PNG, не более 10 МБ"
            >
              <input
                id="submission-file"
                type="file"
                className="file-input"
                accept=".pdf,.zip,.doc,.docx,.png"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
            </Field>
            <p className="form-note">
              Достаточно ссылки или файла. После отправки списание АР по этой задаче остановится.
            </p>
          </form>
        ) : null}
      </Modal>
    </>
  );
}
