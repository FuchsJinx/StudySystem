import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type { PersonalFeedItem, PersonalEventType } from "../shared/api/feedTypes";
import type { PageResponse } from "../shared/api/types";
import { formatDateTime, timeAgo } from "../shared/format";
import { Alert, Button, Card, EmptyState, Icon, PageHeader, PageLoader } from "../shared/ui";
import type { IconName } from "../shared/ui";

const EVENT_ICONS: Record<PersonalEventType, IconName> = {
  CHECKPOINT_ASSIGNED: "book",
  DEADLINE_SOON: "clock",
  SUBMISSION_ACCEPTED: "check",
  SUBMISSION_RETURNED: "alert",
  POINTS_AWARDED: "award",
  POLL_CREATED: "poll",
  ACTIVITY_CREATED: "megaphone",
  SUBMISSION_RECEIVED: "upload",
  POLL_ENDED: "poll",
  MEMBERSHIP_CONFIRMED: "shield",
  MEMBERSHIP_REJECTED: "alert",
  SEMESTER_BONUS: "star",
};

export function PersonalFeedPage() {
  const [items, setItems] = useState<PersonalFeedItem[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (nextCursor?: number | null) => {
    if (nextCursor === undefined) setInitialLoading(true);
    else setLoadingMore(true);
    setError(null);
    try {
      const query = nextCursor ? `?limit=20&cursor=${nextCursor}` : "?limit=20";
      const response = await api<PageResponse<PersonalFeedItem>>(`/feed/personal${query}`);
      setItems((current) => (nextCursor ? [...current, ...response.items] : response.items));
      setCursor(response.nextCursor);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось загрузить ленту событий");
    } finally {
      setInitialLoading(false);
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <>
      <PageHeader
        eyebrow="Модуль 6"
        title="Моя лента"
        subtitle="Персональные события: задания, дедлайны, проверки и начисления"
      />

      {error ? (
        <Alert variant="error" title="Ошибка">{error}</Alert>
      ) : null}

      {initialLoading ? (
        <PageLoader label="Загружаем события…" />
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            icon="bell"
            title="Пока нет событий"
            description="Здесь появятся новые задания, напоминания о дедлайнах, результаты проверки и начисления баллов."
          />
        </Card>
      ) : (
        <Card>
          <div className="personal-feed">
            {items.map((item) => (
              <article className="personal-event" key={item.id}>
                <span className={`personal-event__icon personal-event__icon--${item.type.toLowerCase()}`}>
                  <EventIcon type={item.type} />
                </span>
                <div className="personal-event__body">
                  <p>{item.message}</p>
                  <small title={formatDateTime(item.createdAt)}>{timeAgo(item.createdAt)}</small>
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
          ) : (
            <p className="muted feed-end">Это все события</p>
          )}
        </Card>
      )}
    </>
  );
}

function EventIcon({ type }: { type: PersonalEventType }) {
  const name = EVENT_ICONS[type] ?? "bell";
  return <Icon name={name} size={18} />;
}
