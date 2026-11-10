import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError, downloadFile } from "../shared/api/client";
import type { CheckpointDetail, RosterRow } from "../shared/api/checkpointTypes";
import { formatDateTime } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Modal,
  PageHeader,
  PageLoader,
  StatusBadge,
  Textarea,
} from "../shared/ui";

export function TeacherCheckpointDetailPage() {
  const { id } = useParams();
  const checkpointId = Number(id);
  const [detail, setDetail] = useState<CheckpointDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<number[]>([]);
  const [action, setAction] = useState<"accept" | "return" | null>(null);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    if (!checkpointId || Number.isNaN(checkpointId)) {
      setError("Некорректная задача");
      return;
    }
    setError(null);
    api<CheckpointDetail>(`/checkpoints/${checkpointId}/roster`)
      .then(setDetail)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить задачу"));
  }, [checkpointId]);

  useEffect(() => {
    load();
  }, [load]);

  const rows = detail?.roster ?? [];
  const selectable = useMemo(() => rows.filter((row) => row.status === "SUBMITTED"), [rows]);
  const allSelected = selectable.length > 0 && selectable.every((row) => selected.includes(row.studentUserId));

  const toggle = (row: RosterRow) => {
    if (row.status !== "SUBMITTED") return;
    setSelected((current) =>
      current.includes(row.studentUserId)
        ? current.filter((studentId) => studentId !== row.studentUserId)
        : [...current, row.studentUserId],
    );
  };

  const toggleAll = () => {
    setSelected(allSelected ? [] : selectable.map((row) => row.studentUserId));
  };

  const submitAction = async () => {
    if (!action) return;
    setSubmitting(true);
    setError(null);
    try {
      const path = action === "accept" ? "accept" : "return";
      await api(`/checkpoints/${checkpointId}/${path}`, {
        method: "POST",
        body:
          action === "accept"
            ? { studentIds: selected }
            : { studentIds: selected, comment: comment.trim() || null },
      });
      setNotice(
        action === "accept"
          ? `Принято работ: ${selected.length}`
          : `Возвращено на доработку: ${selected.length}`,
      );
      setAction(null);
      setComment("");
      setSelected([]);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось выполнить действие");
      setAction(null);
    } finally {
      setSubmitting(false);
    }
  };

  if (error && !detail) {
    return (
      <>
        <PageHeader title="Задача" />
        <Alert variant="error" title="Задача недоступна">
          {error}
        </Alert>
        <Link to="/checkpoints" className="text-link">← К списку задач</Link>
      </>
    );
  }

  if (!detail) {
    return <PageLoader label="Загружаем ростер…" />;
  }

  return (
    <>
      <PageHeader
        eyebrow="Проверка работ"
        title={detail.title}
        subtitle={`Дедлайн: ${formatDateTime(detail.deadline)} · максимум ${detail.cost} АР · группы: ${detail.groups
          .map((group) => group.name)
          .join(", ")}`}
        actions={
          <Link to="/checkpoints">
            <Button variant="ghost">← Все задачи</Button>
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

      <div className="grid grid--4">
        <Card className="stat-simple"><span>Студентов</span><strong>{detail.studentsCount}</strong></Card>
        <Card className="stat-simple"><span>На проверке</span><strong>{detail.submittedCount}</strong></Card>
        <Card className="stat-simple"><span>Принято</span><strong>{detail.acceptedCount}</strong></Card>
        <Card className="stat-simple"><span>Просрочено</span><strong>{detail.overdueCount}</strong></Card>
      </div>

      {detail.description ? (
        <Card>
          <h2>Описание</h2>
          <p className="muted pre-wrap">{detail.description}</p>
        </Card>
      ) : null}

      <Card>
        <div className="section-head">
          <div>
            <h2>Ростер задачи</h2>
            <p className="muted">Выберите работы на проверке, чтобы принять их или вернуть на доработку.</p>
          </div>
          <div className="row-actions">
            <Button
              variant="ghost"
              disabled={selected.length === 0}
              onClick={() => setAction("return")}
            >
              Вернуть на доработку
            </Button>
            <Button
              icon="check"
              disabled={selected.length === 0}
              onClick={() => setAction("accept")}
            >
              Принять выбранные ({selected.length})
            </Button>
          </div>
        </div>

        {rows.length === 0 ? (
          <EmptyState
            icon="users"
            title="В задаче нет студентов"
            description="Задача назначена группам, но в них нет подтверждённых студентов."
          />
        ) : (
          <div className="table-wrap">
            <table className="table roster-table">
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      aria-label="Выбрать все работы на проверке"
                      checked={allSelected}
                      onChange={toggleAll}
                      disabled={selectable.length === 0}
                    />
                  </th>
                  <th>Студент</th>
                  <th>Группа</th>
                  <th>Статус</th>
                  <th>Списано</th>
                  <th>Сдано</th>
                  <th>Работа</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.studentUserId} className={selected.includes(row.studentUserId) ? "row--selected" : ""}>
                    <td>
                      <input
                        type="checkbox"
                        aria-label={`Выбрать ${row.fullName}`}
                        checked={selected.includes(row.studentUserId)}
                        onChange={() => toggle(row)}
                        disabled={row.status !== "SUBMITTED"}
                      />
                    </td>
                    <td>
                      <Link to={`/users/${row.studentUserId}`} className="table-person">
                        {row.fullName}
                      </Link>
                      {row.returnComment ? (
                        <div className="table-comment">Комментарий: {row.returnComment}</div>
                      ) : null}
                    </td>
                    <td>{row.groupName}</td>
                    <td><StatusBadge status={row.status} /></td>
                    <td>
                      <span className={row.arDebited > 0 ? "text-red" : ""}>
                        {row.arDebited} / {row.cost} АР
                      </span>
                    </td>
                    <td className="nowrap">
                      {row.submittedAt ? formatDateTime(row.submittedAt) : "—"}
                    </td>
                    <td>
                      <div className="table-links">
                        {row.submissionUrl ? (
                          <a href={row.submissionUrl} target="_blank" rel="noreferrer" className="text-link">
                            Ссылка
                          </a>
                        ) : null}
                        {row.fileName ? (
                          <button
                            type="button"
                            className="text-link text-link--button"
                            onClick={() =>
                              downloadFile(`/files/submissions/${row.submissionId}`, row.fileName!)
                            }
                          >
                            {row.fileName}
                          </button>
                        ) : null}
                        {!row.submissionUrl && !row.fileName ? <span className="muted">—</span> : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        open={action !== null}
        title={
          action === "accept"
            ? `Принять работы (${selected.length})`
            : `Вернуть на доработку (${selected.length})`
        }
        onClose={() => !submitting && setAction(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setAction(null)} disabled={submitting}>
              Отмена
            </Button>
            <Button
              variant={action === "return" ? "danger" : "primary"}
              onClick={submitAction}
              loading={submitting}
            >
              {action === "accept" ? "Принять" : "Вернуть"}
            </Button>
          </>
        }
      >
        {action === "accept" ? (
          <p className="muted">
            Выбранные работы получат статус «Принято». Списание АР по ним прекратится.
          </p>
        ) : (
          <>
            <p className="muted">
              Студенты получат уведомление. Если дедлайн уже прошёл, списание АР возобновится до
              повторной сдачи.
            </p>
            <label className="field">
              <span className="field__label">Комментарий для студента</span>
              <Textarea
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                placeholder="Что нужно доработать…"
                maxLength={2000}
              />
            </label>
          </>
        )}
      </Modal>
    </>
  );
}
