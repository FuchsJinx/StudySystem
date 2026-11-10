import { useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type {
  ActivityRanking,
  ArDynamics,
  BudgetStats,
  MonthBudget,
  RankRow,
  TeacherOverview,
} from "../shared/api/analyticsTypes";
import type { Group } from "../shared/api/types";
import { formatShortDate } from "../shared/format";
import {
  Alert,
  Card,
  EmptyState,
  Field,
  PageHeader,
  PageLoader,
  ProgressBar,
  Select,
  StatCard,
} from "../shared/ui";

function ArChart({ dynamics }: { dynamics: ArDynamics }) {
  const points = dynamics.points;
  if (points.length === 0) return null;
  const width = 820;
  const height = 260;
  const padding = { left: 44, right: 18, top: 18, bottom: 34 };
  const values = points.flatMap((point) => [point.average, point.median]);
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const min = Math.max(0, Math.floor((rawMin - 10) / 10) * 10);
  const max = Math.min(1000, Math.max(min + 20, Math.ceil((rawMax + 10) / 10) * 10));
  const x = (index: number) =>
    padding.left + (index * (width - padding.left - padding.right)) / Math.max(points.length - 1, 1);
  const y = (value: number) =>
    height - padding.bottom - ((value - min) / (max - min)) * (height - padding.top - padding.bottom);

  const averagePath = points.map((point, index) => `${x(index)},${y(point.average)}`).join(" ");
  const medianPath = points.map((point, index) => `${x(index)},${y(point.median)}`).join(" ");
  const areaPath = `M ${x(0)},${height - padding.bottom} L ${points
    .map((point, index) => `${x(index)},${y(point.average)}`)
    .join(" L ")} L ${x(points.length - 1)},${height - padding.bottom} Z`;
  const grid = [0, 0.25, 0.5, 0.75, 1];

  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${width} ${height}`} className="line-chart" role="img" aria-label="Динамика среднего АР">
        {grid.map((fraction) => {
          const value = min + (max - min) * fraction;
          const yPosition = y(value);
          return (
            <g key={fraction}>
              <line
                x1={padding.left}
                x2={width - padding.right}
                y1={yPosition}
                y2={yPosition}
                className="chart-grid-line"
              />
              <text x={8} y={yPosition + 4} className="chart-axis-label">
                {Math.round(value)}
              </text>
            </g>
          );
        })}
        <path d={areaPath} className="chart-area" />
        <polyline points={medianPath} className="chart-line chart-line--median" />
        <polyline points={averagePath} className="chart-line chart-line--average" />
        {[0, Math.floor(points.length / 2), points.length - 1].map((index) => (
          <text
            key={index}
            x={x(index)}
            y={height - 10}
            textAnchor={index === 0 ? "start" : index === points.length - 1 ? "end" : "middle"}
            className="chart-axis-label"
          >
            {formatShortDate(points[index].date)}
          </text>
        ))}
      </svg>
      <div className="chart-legend">
        <span><i className="chart-dot chart-dot--average" /> Среднее АР</span>
        <span><i className="chart-dot chart-dot--median" /> Медиана</span>
      </div>
    </div>
  );
}

function BudgetBars({ stats }: { stats: BudgetStats }) {
  const months = [...stats.months].reverse();
  const max = Math.max(1, ...months.map((month) => month.spent));
  return (
    <div className="bar-chart" role="img" aria-label="Расход бюджета по месяцам">
      {months.map((month) => (
        <div className="bar-chart__column" key={month.yearMonth}>
          <span className="bar-chart__value">{month.spent}</span>
          <div className="bar-chart__track">
            <div
              className="bar-chart__bar"
              style={{ height: `${Math.max(4, (month.spent / max) * 100)}%` }}
            />
          </div>
          <span className="bar-chart__label">{monthLabel(month.yearMonth)}</span>
        </div>
      ))}
    </div>
  );
}

function monthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split("-");
  const names = ["янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"];
  return `${names[Number(month) - 1]} ${year.slice(2)}`;
}

function RankingTable({ rows, sortKey }: { rows: RankRow[]; sortKey: "bua" | "boa" }) {
  const sorted = [...rows].sort((a, b) => b[sortKey] - a[sortKey]);
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>#</th>
            <th>Студент</th>
            <th>{sortKey === "bua" ? "БУА" : "БОА"}</th>
            <th>Всего</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((row, index) => (
            <tr key={row.studentUserId}>
              <td>{index + 1}</td>
              <td>{row.fullName}</td>
              <td>
                <strong>{row[sortKey]}</strong>
              </td>
              <td>{row.total}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function AnalyticsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [groupId, setGroupId] = useState<number | null>(null);
  const [overview, setOverview] = useState<TeacherOverview | null>(null);
  const [dynamics, setDynamics] = useState<ArDynamics | null>(null);
  const [ranking, setRanking] = useState<ActivityRanking | null>(null);
  const [budgetStats, setBudgetStats] = useState<BudgetStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api<Group[]>("/catalog/groups/my")
      .then((list) => {
        setGroups(list);
        setGroupId((current) => current ?? list[0]?.id ?? null);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить группы"));
    api<TeacherOverview>("/analytics/teachers/me/overview")
      .then(setOverview)
      .catch(() => setOverview(null));
    api<BudgetStats>("/analytics/teachers/me/budget-stats?months=6")
      .then(setBudgetStats)
      .catch(() => setBudgetStats(null));
  }, []);

  const loadGroup = useCallback(() => {
    if (!groupId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    Promise.all([
      api<ArDynamics>(`/analytics/groups/${groupId}/ar-dynamics?days=30`),
      api<ActivityRanking>(`/analytics/groups/${groupId}/activity-ranking`),
    ])
      .then(([dynamicsValue, rankingValue]) => {
        setDynamics(dynamicsValue);
        setRanking(rankingValue);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить аналитику"))
      .finally(() => setLoading(false));
  }, [groupId]);

  useEffect(() => {
    loadGroup();
  }, [loadGroup]);

  const currentMonthBudget = useMemo<MonthBudget | null>(
    () => budgetStats?.months[0] ?? null,
    [budgetStats],
  );

  return (
    <>
      <PageHeader
        eyebrow="Модуль 10"
        title="Аналитика"
        subtitle="Динамика академического рейтинга, рейтинги активности и бюджет преподавателя"
        actions={
          <div className="month-picker">
            <Field label="Группа" htmlFor="analytics-group">
              <Select
                id="analytics-group"
                value={groupId ?? ""}
                onChange={(event) => setGroupId(Number(event.target.value))}
              >
                {groups.map((group) => (
                  <option key={group.id} value={group.id}>{group.name}</option>
                ))}
              </Select>
            </Field>
          </div>
        }
      />

      {error ? <Alert variant="error" title="Ошибка">{error}</Alert> : null}

      {overview ? (
        <div className="grid grid--4">
          <StatCard label="Студенты" value={overview.students} hint={`${overview.pendingMemberships} ждут подтверждения`} icon="users" tone="green" />
          <StatCard label="Задания" value={overview.checkpoints} icon="book" tone="blue" />
          <StatCard label="Активности" value={overview.activities} icon="megaphone" tone="gold" />
          <StatCard label="Опросы" value={overview.polls} icon="poll" tone="red" />
        </div>
      ) : null}

      {loading ? (
        <PageLoader label="Строим графики…" />
      ) : !dynamics || !ranking ? (
        <Card>
          <EmptyState
            icon="chart"
            title="Нет данных для аналитики"
            description="Создайте задания и активности для группы — здесь появится динамика."
          />
        </Card>
      ) : (
        <>
          <Card>
            <div className="section-head">
              <div>
                <h2>Динамика АР · {dynamics.groupName}</h2>
                <p className="muted">Среднее и медиана академического рейтинга за 30 дней</p>
              </div>
              <span className="muted small">{dynamics.students.length} студентов</span>
            </div>
            <ArChart dynamics={dynamics} />
            {dynamics.students.length > 0 ? (
              <div className="chip-list">
                {dynamics.students.map((student) => (
                  <span className="chip" key={student.studentUserId}>
                    {student.fullName}: {student.academicRating} АР
                  </span>
                ))}
              </div>
            ) : null}
          </Card>

          <div className="dashboard-grid">
            <Card>
              <h2>Рейтинг по БУА</h2>
              {ranking.students.length === 0 ? (
                <EmptyState title="Нет студентов" />
              ) : (
                <RankingTable rows={ranking.students} sortKey="bua" />
              )}
            </Card>
            <Card>
              <h2>Рейтинг по БОА</h2>
              {ranking.students.length === 0 ? (
                <EmptyState title="Нет студентов" />
              ) : (
                <RankingTable rows={ranking.students} sortKey="boa" />
              )}
            </Card>
          </div>

          <div className="dashboard-grid">
            <Card>
              <h2>Расход бюджета по месяцам</h2>
              {budgetStats ? <BudgetBars stats={budgetStats} /> : <EmptyState title="Нет данных" />}
            </Card>
            <Card>
              <h2>Бюджет текущего месяца</h2>
              {currentMonthBudget ? (
                <>
                  <p className="muted">
                    Израсходовано {currentMonthBudget.spent} из {currentMonthBudget.limit} баллов
                  </p>
                  <ProgressBar
                    value={currentMonthBudget.spent}
                    max={Math.max(1, currentMonthBudget.limit)}
                    tone="gold"
                  />
                  <div className="budget-month-groups">
                    {currentMonthBudget.groups.map((group) => (
                      <div key={group.groupId} className="budget-month-group">
                        <span>{group.groupName}</span>
                        <strong>{group.spent} / {group.limit}</strong>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <EmptyState title="Нет данных о бюджете" />
              )}
            </Card>
          </div>
        </>
      )}
    </>
  );
}
