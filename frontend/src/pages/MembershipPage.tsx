import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type { MembershipRequest } from "../shared/api/types";
import { useAuth } from "../shared/auth/AuthContext";
import { formatDateTime } from "../shared/format";
import {
  Alert,
  Avatar,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  MembershipBadge,
  PageHeader,
  PageLoader,
} from "../shared/ui";

type MembershipAction = "confirm" | "reject";

interface PendingAction {
  action: MembershipAction;
  request: MembershipRequest;
}

export function MembershipPage() {
  const { user } = useAuth();
  const [requests, setRequests] = useState<MembershipRequest[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const canAccess = user?.role === "TEACHER" || user?.role === "ADMIN" ||
    (user?.role === "STUDENT" && user.student?.isGroupLead === true);

  const load = useCallback(() => {
    setRequests(null);
    setError(null);
    api<MembershipRequest[]>("/membership/requests")
      .then(setRequests)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить заявки"));
  }, []);

  useEffect(() => {
    if (canAccess) {
      load();
    }
  }, [canAccess, load]);

  if (!canAccess) {
    return (
      <>
        <PageHeader title="Заявки в группы" subtitle="Подтверждение принадлежности студентов" />
        <EmptyState
          icon="shield"
          title="Раздел доступен преподавателям и старостам"
          description="Подтверждать принадлежность к группе может преподаватель, работающий с группой, или староста этой группы."
        />
      </>
    );
  }

  const performAction = async () => {
    if (!pendingAction) return;
    setSubmitting(true);
    try {
      await api(`/membership/${pendingAction.request.studentUserId}/${pendingAction.action}`, {
        method: "POST",
      });
      setNotice(
        pendingAction.action === "confirm"
          ? `Студент ${pendingAction.request.fullName} подтверждён`
          : `Заявка ${pendingAction.request.fullName} отклонена`,
      );
      setPendingAction(null);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось выполнить действие");
      setPendingAction(null);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль подтверждения"
        title="Заявки в группы"
        subtitle="Подтвердите принадлежность студентов к вашим группам — после этого им начнут назначаться задания"
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

      {requests === null ? (
        <PageLoader label="Загружаем заявки…" />
      ) : requests.length === 0 ? (
        <Card>
          <EmptyState
            icon="users"
            title="Нет ожидающих заявок"
            description="Все студенты ваших групп уже подтверждены. Новые заявки появятся здесь после регистрации."
          />
        </Card>
      ) : (
        <div className="request-cards">
          {requests.map((request) => (
            <Card key={request.studentUserId} className="request-card">
              <div className="request-card__head">
                <Avatar name={request.fullName} size={46} />
                <div>
                  <h3>{request.fullName}</h3>
                  <p className="muted">
                    {request.facultyName} · {request.groupName}
                  </p>
                </div>
                <MembershipBadge status={request.status} />
              </div>
              <dl className="request-card__meta">
                <div>
                  <dt>Email</dt>
                  <dd>{request.email}</dd>
                </div>
                <div>
                  <dt>Заявка создана</dt>
                  <dd>{formatDateTime(request.requestedAt)}</dd>
                </div>
              </dl>
              <div className="request-card__actions">
                <Button
                  variant="ghost"
                  onClick={() => setPendingAction({ action: "reject", request })}
                >
                  Отклонить
                </Button>
                <Button
                  icon="check"
                  onClick={() => setPendingAction({ action: "confirm", request })}
                >
                  Подтвердить группу
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {requests !== null ? (
        <p className="muted page-footnote">
          После подтверждения студенту сразу назначаются актуальные задания группы.
        </p>
      ) : null}

      <ConfirmDialog
        open={pendingAction !== null}
        title={pendingAction?.action === "confirm" ? "Подтвердить студента?" : "Отклонить заявку?"}
        description={
          pendingAction
            ? pendingAction.action === "confirm"
              ? `${pendingAction.request.fullName} получит доступ к заданиям и активностям группы ${pendingAction.request.groupName}.`
              : `${pendingAction.request.fullName} не сможет получать задания и начисления группы ${pendingAction.request.groupName}.`
            : ""
        }
        confirmLabel={pendingAction?.action === "confirm" ? "Подтвердить" : "Отклонить"}
        danger={pendingAction?.action === "reject"}
        loading={submitting}
        onConfirm={performAction}
        onClose={() => !submitting && setPendingAction(null)}
      />
    </>
  );
}
