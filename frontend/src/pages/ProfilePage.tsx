import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { AiRatingTips } from "../shared/api/aiTypes";
import type { NotificationSettings, ProfileResponse } from "../shared/api/types";
import { useAuth } from "../shared/auth/AuthContext";
import { formatAmount, formatDateTime, initials, roleLabels, statusLabels } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  PageHeader,
  PageLoader,
  ProgressBar,
  StatusBadge,
  MembershipBadge,
} from "../shared/ui";

export function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const params = useParams();
  const targetId = params.id ? Number(params.id) : user?.id;
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [settings, setSettings] = useState<NotificationSettings | null>(null);
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsMessage, setSettingsMessage] = useState<string | null>(null);
  const [telegramChatId, setTelegramChatId] = useState("");
  const [tips, setTips] = useState<AiRatingTips | null>(null);
  const [tipsLoading, setTipsLoading] = useState(false);
  const [tipsError, setTipsError] = useState<string | null>(null);

  const isOwn = Boolean(user && targetId === user.id);

  useEffect(() => {
    if (!targetId || Number.isNaN(targetId)) {
      setError("Некорректный профиль");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    api<ProfileResponse>(`/users/${targetId}/profile`)
      .then(setProfile)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить профиль"))
      .finally(() => setLoading(false));
  }, [targetId]);

  useEffect(() => {
    if (!isOwn) {
      setSettings(null);
      return;
    }
    api<NotificationSettings>("/users/me/notification-settings")
      .then((value) => {
        setSettings(value);
        setTelegramChatId(value.telegramChatId ?? "");
      })
      .catch(() => setSettings(null));
  }, [isOwn]);

  const saveSettings = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!settings) return;
    setSettingsLoading(true);
    setSettingsMessage(null);
    try {
      const updated = await api<NotificationSettings>("/users/me/notification-settings", {
        method: "PUT",
        body: {
          emailEnabled: settings.emailEnabled,
          telegramEnabled: settings.telegramEnabled,
          telegramChatId: telegramChatId.trim() || null,
        },
      });
      setSettings(updated);
      setSettingsMessage("Настройки уведомлений сохранены");
      if (user) await refreshUser();
    } catch (err) {
      setSettingsMessage(err instanceof ApiError ? err.message : "Не удалось сохранить настройки");
    } finally {
      setSettingsLoading(false);
    }
  };

  if (loading) {
    return <PageLoader label="Загружаем профиль…" />;
  }
  if (error || !profile) {
    return (
      <>
        <PageHeader title="Профиль" />
        <Alert variant="error" title="Профиль недоступен">
          {error ?? "Пользователь не найден"}
        </Alert>
      </>
    );
  }

  const isStudent = profile.role === "STUDENT";
  const maxRating = 1000;

  const loadTips = async () => {
    setTipsLoading(true);
    setTipsError(null);
    try {
      const response = await api<AiRatingTips>("/ai/rating-tips", { method: "POST" });
      setTips(response);
    } catch (err) {
      setTipsError(err instanceof ApiError ? err.message : "Сервис ИИ временно недоступен");
    } finally {
      setTipsLoading(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow={isOwn ? "Мой профиль" : "Профиль пользователя"}
        title={profile.fullName}
        subtitle={
          isStudent
            ? `${profile.facultyName ?? "—"} · группа ${profile.groupName ?? "—"}`
            : profile.role === "TEACHER"
              ? profile.departmentName ?? "Преподаватель"
              : "Администратор"
        }
        actions={
          !isOwn ? (
            <Link to="/">
              <Button variant="ghost" icon="arrowRight">
                На главную
              </Button>
            </Link>
          ) : null
        }
      />

      <Card className="profile-head">
        <div className="profile-head__avatar">{initials(profile.fullName)}</div>
        <div className="profile-head__main">
          <div className="profile-head__name">
            <h2>{profile.fullName}</h2>
            <span className="badge badge--role">{roleLabels[profile.role]}</span>
            {profile.isGroupLead ? <span className="badge badge--gold">Староста</span> : null}
          </div>
          <p className="muted">
            {isStudent
              ? `${profile.facultyName ?? "—"} · ${profile.groupName ?? "—"}`
              : profile.departmentName ?? "—"}
          </p>
          {profile.email ? <p className="muted">Email: {profile.email}</p> : null}
        </div>
        {isStudent && profile.membershipStatus ? (
          <div className="profile-head__status">
            <span className="muted">Принадлежность к группе</span>
            <MembershipBadge status={profile.membershipStatus} />
          </div>
        ) : null}
      </Card>

      {isStudent ? (
        <>
          <div className="grid grid--3">
            <Card className="rating-summary">
              <span className="stat-card__label">Академический рейтинг</span>
              <div className="rating-summary__value">
                {profile.academicRating}
                <span> / {maxRating}</span>
              </div>
              <ProgressBar
                value={profile.academicRating ?? 0}
                max={maxRating}
                tone={(profile.academicRating ?? 0) >= 700 ? "green" : "red"}
              />
            </Card>
            <Card className="stat-simple stat-simple--blue">
              <span>БУА</span>
              <strong>{profile.bua ?? 0}</strong>
              <small>Учебная активность</small>
            </Card>
            <Card className="stat-simple stat-simple--gold">
              <span>БОА</span>
              <strong>{profile.boa ?? 0}</strong>
              <small>Общественная активность</small>
            </Card>
          </div>

          {isOwn ? (
            <Card className="ai-panel">
              <div className="ai-panel__head">
                <strong>Как поднять рейтинг?</strong>
                <span className="badge badge--gold">Модуль 11</span>
              </div>
              <p className="muted">
                Персональные советы по вашим агрегированным показателям: АР, БУА, БОА и просрочкам.
              </p>
              <div className="ai-panel__actions">
                <Button
                  type="button"
                  variant="secondary"
                  icon="sparkles"
                  loading={tipsLoading}
                  onClick={loadTips}
                >
                  Получить советы
                </Button>
                {tips ? (
                  <span className="muted small">
                    {tips.provider === "demo" ? "Демо-режим ИИ (ключ не настроен)" : "Советы сгенерированы ИИ"}
                  </span>
                ) : null}
              </div>
              {tipsError ? <Alert variant="warning">{tipsError}</Alert> : null}
              {tips ? (
                <ol className="ai-tips">
                  {tips.tips.map((tip, index) => (
                    <li key={index}>{tip}</li>
                  ))}
                </ol>
              ) : null}
            </Card>
          ) : null}

          <div className="dashboard-grid">
            <Card>
              <div className="section-head">
                <h2>История начислений</h2>
                <span className="muted">Показаны последние операции</span>
              </div>
              {profile.ledger.length === 0 ? (
                <EmptyState
                  icon="award"
                  title="История пока пуста"
                  description="Баллы за активности и списания АР будут отображаться здесь."
                />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Дата</th>
                        <th>Операция</th>
                        <th>Баллы</th>
                        <th>Кто</th>
                      </tr>
                    </thead>
                    <tbody>
                      {profile.ledger.map((entry) => (
                        <tr key={entry.id}>
                          <td className="nowrap">{formatDateTime(entry.date)}</td>
                          <td>{entry.reason}</td>
                          <td>
                            <span className={`ledger-amount ledger-amount--${entry.currency.toLowerCase()}`}>
                              {formatAmount(entry.currency, entry.amount)}
                            </span>
                          </td>
                          <td>{entry.actorName ?? "Система"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>

            <Card>
              <h2>Задания</h2>
              {profile.activeCheckpoints.length === 0 && profile.completedCheckpoints.length === 0 ? (
                <EmptyState
                  icon="book"
                  title="Заданий пока нет"
                  description="Когда преподаватель создаст контрольную точку для вашей группы, она появится у вас."
                />
              ) : (
                <div className="profile-tasks">
                  {profile.activeCheckpoints.map((checkpoint) => (
                    <div className="profile-task" key={`active-${checkpoint.id}`}>
                      <div>
                        <strong>{checkpoint.title}</strong>
                        <small>
                          до {formatDateTime(checkpoint.deadline)} · списано {checkpoint.arDebited} /{" "}
                          {checkpoint.cost} АР
                        </small>
                      </div>
                      <StatusBadge status={checkpoint.status} />
                    </div>
                  ))}
                  {profile.completedCheckpoints.map((checkpoint) => (
                    <div className="profile-task profile-task--done" key={`done-${checkpoint.id}`}>
                      <div>
                        <strong>{checkpoint.title}</strong>
                        <small>{statusLabels[checkpoint.status]}</small>
                      </div>
                      <StatusBadge status={checkpoint.status} />
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      ) : null}

      {isOwn ? (
        <Card>
          <div className="section-head">
            <div>
              <h2>Уведомления</h2>
              <p className="muted">In-app уведомления приходят всегда. Email и Telegram — по желанию.</p>
            </div>
          </div>
          {settings ? (
            <form className="notification-form" onSubmit={saveSettings}>
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={settings.emailEnabled}
                  onChange={(event) =>
                    setSettings({ ...settings, emailEnabled: event.target.checked })
                  }
                />
                <span>
                  <strong>Email</strong>
                  <small>Письма на {user?.email}</small>
                </span>
              </label>
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={settings.telegramEnabled}
                  onChange={(event) =>
                    setSettings({ ...settings, telegramEnabled: event.target.checked })
                  }
                />
                <span>
                  <strong>Telegram</strong>
                  <small>Требуется chat ID</small>
                </span>
              </label>
              <Field
                label="Telegram chat ID"
                htmlFor="telegram-chat"
                hint="Например, 123456789. В демо-режиме сообщения пишутся в лог."
              >
                <Input
                  id="telegram-chat"
                  value={telegramChatId}
                  onChange={(event) => setTelegramChatId(event.target.value)}
                  placeholder="123456789"
                  disabled={!settings.telegramEnabled}
                />
              </Field>
              {settingsMessage ? (
                <Alert variant={settingsMessage.includes("сохранены") ? "success" : "error"}>
                  {settingsMessage}
                </Alert>
              ) : null}
              <div>
                <Button type="submit" loading={settingsLoading}>
                  Сохранить настройки
                </Button>
              </div>
            </form>
          ) : (
            <p className="muted">Настройки уведомлений недоступны.</p>
          )}
        </Card>
      ) : null}

      {isStudent && profile.membershipStatus === "PENDING" ? (
        <Alert variant="info" title="Группа ожидает подтверждения">
          До подтверждения принадлежности к группе новые задания и начисления не будут назначаться.
        </Alert>
      ) : null}
    </>
  );
}
