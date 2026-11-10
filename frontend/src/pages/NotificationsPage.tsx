import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { NotificationItem, NotificationPage, UnreadCount } from "../shared/api/notificationTypes";
import { formatDateTime, timeAgo } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Icon,
  PageHeader,
  PageLoader,
} from "../shared/ui";

function notifyChanged() {
  window.dispatchEvent(new CustomEvent("unirate:notifications-changed"));
}

export function NotificationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [unread, setUnread] = useState(0);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async (nextCursor?: number | null) => {
    if (nextCursor === undefined) setInitialLoading(true);
    else setLoadingMore(true);
    setError(null);
    try {
      const query = nextCursor ? `?limit=20&cursor=${nextCursor}` : "?limit=20";
      const response = await api<NotificationPage>(`/notifications${query}`);
      setItems((current) => (nextCursor ? [...current, ...response.items] : response.items));
      setCursor(response.nextCursor);
      const unreadResponse = await api<UnreadCount>("/notifications/unread-count");
      setUnread(unreadResponse.count);
      notifyChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось загрузить уведомления");
    } finally {
      setInitialLoading(false);
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const markRead = async (item: NotificationItem) => {
    if (!item.read) {
      try {
        await api<NotificationItem>(`/notifications/${item.id}/read`, { method: "PATCH" });
        setItems((current) =>
          current.map((existing) => (existing.id === item.id ? { ...existing, read: true } : existing)),
        );
        setUnread((current) => Math.max(0, current - 1));
        notifyChanged();
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Не удалось отметить уведомление");
        return;
      }
    }
    if (item.link) {
      navigate(item.link);
    }
  };

  const markAll = async () => {
    setBusy(true);
    setNotice(null);
    try {
      await api<UnreadCount>("/notifications/read-all", { method: "POST" });
      setItems((current) => current.map((item) => ({ ...item, read: true })));
      setUnread(0);
      setNotice("Все уведомления прочитаны");
      notifyChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось отметить уведомления");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль 9"
        title="Уведомления"
        subtitle="In-app уведомления всегда доступны; email и Telegram настраиваются в профиле"
        actions={
          <Button
            variant="secondary"
            disabled={unread === 0}
            loading={busy}
            onClick={markAll}
          >
            Прочитать все
          </Button>
        }
      />

      {notice ? <Alert variant="success">{notice}</Alert> : null}
      {error ? <Alert variant="error" title="Ошибка">{error}</Alert> : null}

      {initialLoading ? (
        <PageLoader label="Загружаем уведомления…" />
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            icon="bell"
            title="Уведомлений пока нет"
            description="Здесь появятся новые задания, проверки работ, начисления баллов и напоминания о дедлайнах."
          />
        </Card>
      ) : (
        <Card>
          <div className="notification-list">
            {items.map((item) => (
              <article
                key={item.id}
                className={`notification-item ${item.read ? "notification-item--read" : "notification-item--unread"}`}
              >
                <span className="notification-item__dot" aria-hidden="true" />
                <div className="notification-item__body">
                  <div className="notification-item__title">
                    <strong>{item.title}</strong>
                    <small title={formatDateTime(item.createdAt)}>{timeAgo(item.createdAt)}</small>
                  </div>
                  <p>{item.body}</p>
                  <div className="notification-item__actions">
                    {item.link ? (
                      <button type="button" className="text-link text-link--button" onClick={() => markRead(item)}>
                        Открыть <Icon name="arrowRight" size={14} />
                      </button>
                    ) : null}
                    {!item.read ? (
                      <button type="button" className="text-link text-link--button" onClick={() => markRead(item)}>
                        Отметить прочитанным
                      </button>
                    ) : (
                      <span className="muted small">Прочитано</span>
                    )}
                  </div>
                </div>
              </article>
            ))}
          </div>
          {cursor ? (
            <div className="feed-more">
              <Button variant="secondary" loading={loadingMore} onClick={() => load(cursor)}>
                Показать ещё
              </Button>
            </div>
          ) : null}
        </Card>
      )}
    </>
  );
}
