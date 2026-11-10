import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type { BudgetDetail, BudgetOverview } from "../shared/api/activityTypes";
import { formatAmount, formatDateTime, toDateInputValue } from "../shared/format";
import {
  Alert,
  Card,
  EmptyState,
  Field,
  Input,
  PageHeader,
  PageLoader,
  ProgressBar,
  StatCard,
} from "../shared/ui";

function currentMonth(): string {
  return toDateInputValue(new Date()).slice(0, 7);
}

export function BudgetPage() {
  const [yearMonth, setYearMonth] = useState(currentMonth());
  const [overview, setOverview] = useState<BudgetOverview | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [detail, setDetail] = useState<BudgetDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);

  const load = useCallback(() => {
    setOverview(null);
    setError(null);
    api<BudgetOverview>(`/teachers/me/budgets?yearMonth=${yearMonth}`)
      .then((value) => {
        setOverview(value);
        setSelectedGroup((current) => {
          if (current && value.groups.some((group) => group.groupId === current)) {
            return current;
          }
          return value.groups[0]?.groupId ?? null;
        });
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить бюджет"));
  }, [yearMonth]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!selectedGroup) {
      setDetail(null);
      return;
    }
    setDetailError(null);
    api<BudgetDetail>(`/teachers/me/budget?groupId=${selectedGroup}&yearMonth=${yearMonth}`)
      .then(setDetail)
      .catch((err) => setDetailError(err instanceof ApiError ? err.message : "Не удалось загрузить детали"));
  }, [selectedGroup, yearMonth]);

  return (
    <>
      <PageHeader
        eyebrow="Бюджет активности"
        title="Бюджет преподавателя"
        subtitle="Месячный лимит баллов на каждую группу. Лимит восстанавливается в новом календарном месяце."
        actions={
          <div className="month-picker">
            <Field label="Месяц" htmlFor="budget-month">
              <Input
                id="budget-month"
                type="month"
                value={yearMonth}
                onChange={(event) => event.target.value && setYearMonth(event.target.value)}
              />
            </Field>
          </div>
        }
      />

      {error ? (
        <Alert variant="error" title="Ошибка">{error}</Alert>
      ) : null}

      {overview === null ? (
        <PageLoader label="Загружаем бюджет…" />
      ) : overview.groups.length === 0 ? (
        <Card>
          <EmptyState
            icon="wallet"
            title="Нет привязанных групп"
            description="Группы появятся здесь после создания задания или активности для группы."
          />
        </Card>
      ) : (
        <>
          <div className="grid grid--3">
            <StatCard label="Общий лимит" value={overview.totalLimit} hint={`Месяц ${overview.yearMonth}`} icon="wallet" tone="green" />
            <StatCard label="Израсходовано" value={overview.totalSpent} icon="chart" tone="gold" />
            <StatCard label="Осталось" value={overview.totalRemaining} icon="check" tone="blue" />
          </div>

          <div className="budget-layout">
            <Card>
              <h2>Группы</h2>
              <div className="budget-groups">
                {overview.groups.map((group) => (
                  <button
                    type="button"
                    key={group.groupId}
                    className={`budget-group ${selectedGroup === group.groupId ? "budget-group--active" : ""}`}
                    onClick={() => setSelectedGroup(group.groupId)}
                  >
                    <div className="budget-group__head">
                      <strong>{group.groupName}</strong>
                      <span>{group.remaining} / {group.limit}</span>
                    </div>
                    <ProgressBar
                      value={group.spent}
                      max={group.limit}
                      tone={group.remaining === 0 ? "red" : group.remaining < group.limit * 0.2 ? "gold" : "green"}
                    />
                    <span className="muted small">Израсходовано {group.spent} баллов</span>
                  </button>
                ))}
              </div>
            </Card>

            <Card>
              {detailError ? <Alert variant="error">{detailError}</Alert> : null}
              {detail === null ? (
                <PageLoader label="Загружаем начисления…" />
              ) : (
                <>
                  <div className="section-head">
                    <div>
                      <h2>Начисления · {detail.groupName}</h2>
                      <p className="muted">
                        Остаток: {detail.remaining} из {detail.limit} баллов
                      </p>
                    </div>
                  </div>
                  <ProgressBar
                    value={detail.spent}
                    max={detail.limit}
                    tone={detail.remaining === 0 ? "red" : "gold"}
                    label="Использовано"
                  />
                  {detail.awards.length === 0 ? (
                    <EmptyState
                      icon="award"
                      title="Начислений в этом месяце нет"
                      description="Баллы, начисленные вами за активности, появятся здесь."
                    />
                  ) : (
                    <div className="table-wrap">
                      <table className="table">
                        <thead>
                          <tr>
                            <th>Дата</th>
                            <th>Студент</th>
                            <th>Активность</th>
                            <th>Баллы</th>
                          </tr>
                        </thead>
                        <tbody>
                          {detail.awards.map((award) => (
                            <tr key={award.awardId}>
                              <td className="nowrap">{formatDateTime(award.date)}</td>
                              <td>{award.studentName}</td>
                              <td>{award.activityTitle}</td>
                              <td>
                                <span className={`ledger-amount ledger-amount--${award.currency.toLowerCase()}`}>
                                  {formatAmount(award.currency, award.points)}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              )}
            </Card>
          </div>
        </>
      )}
    </>
  );
}
