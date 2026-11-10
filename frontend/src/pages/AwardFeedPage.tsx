import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type { AwardFeedItem } from "../shared/api/activityTypes";
import type { PageResponse } from "../shared/api/types";
import { formatDateTime, formatAmount, timeAgo } from "../shared/format";
import { Alert, Avatar, Button, Card, EmptyState, PageHeader, PageLoader } from "../shared/ui";

export function AwardFeedPage() {
  const [items, setItems] = useState<AwardFeedItem[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (nextCursor?: number | null) => {
    if (nextCursor === undefined) {
      setInitialLoading(true);
    } else {
      setLoadingMore(true);
    }
    setError(null);
    try {
      const query = nextCursor ? `?limit=20&cursor=${nextCursor}` : "?limit=20";
      const response = await api<PageResponse<AwardFeedItem>>(`/feed/awards${query}`);
      setItems((current) => (nextCursor ? [...current, ...response.items] : response.items));
      setCursor(response.nextCursor);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось загрузить ленту наград");
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
        eyebrow="Модуль 4"
        title="Лента наград"
        subtitle="Публичные достижения студентов: баллы за учебные и общественные активности"
      />

      {error ? (
        <Alert variant="error" title="Ошибка">
          {error}
        </Alert>
      ) : null}

      {initialLoading ? (
        <PageLoader label="Загружаем достижения…" />
      ) : items.length === 0 ? (
        <Card>
          <EmptyState
            icon="award"
            title="Пока нет наград"
            description="Как только преподаватель начислит баллы за активность, запись появится в публичной ленте."
          />
        </Card>
      ) : (
        <Card className="feed-card">
          <div className="award-timeline">
            {items.map((item) => (
              <article className="award-item" key={item.id}>
                <Avatar name={item.studentName} size={46} />
                <div className="award-item__main">
                  <div className="award-item__line">
                    <strong>{item.studentName}</strong>
                    <span className={`ledger-amount ledger-amount--${item.currency.toLowerCase()}`}>
                      {formatAmount(item.currency, item.amount)}
                    </span>
                  </div>
                  <p>
                    «{item.activityTitle}» · {item.groupName}
                  </p>
                  <small>
                    Начислил(а) {item.teacherName} · <span title={formatDateTime(item.createdAt)}>{timeAgo(item.createdAt)}</span>
                  </small>
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
            <p className="muted feed-end">Вы дошли до конца ленты</p>
          )}
        </Card>
      )}
    </>
  );
}
