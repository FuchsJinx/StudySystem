import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../shared/api/client";
import type { StudentCheckpoint } from "../shared/api/checkpointTypes";
import type { LedgerEntry, MembershipRequest } from "../shared/api/types";
import { useAuth } from "../shared/auth/AuthContext";
import { formatAmount, formatDateTime, membershipLabels, plural } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Icon,
  PageHeader,
  ProgressBar,
  Spinner,
  StatCard,
} from "../shared/ui";

export function DashboardPage() {
  const { user } = useAuth();
  const [pending, setPending] = useState<MembershipRequest[] | null>(null);
  const [ledger, setLedger] = useState<LedgerEntry[] | null>(null);
  const [tasks, setTasks] = useState<StudentCheckpoint[] | null>(null);

  useEffect(() => {
    if (!user) return;
    if (user.role === "STUDENT") {
      api<LedgerEntry[]>(`/users/${user.id}/ledger?limit=6`)
        .then(setLedger)
        .catch(() => setLedger([]));
      api<StudentCheckpoint[]>("/users/me/checkpoints")
        .then(setTasks)
        .catch(() => setTasks([]));
    }
    if (user.role === "TEACHER" || user.role === "ADMIN" ||
        (user.role === "STUDENT" && user.student?.isGroupLead)) {
      api<MembershipRequest[]>("/membership/requests")
        .then(setPending)
        .catch(() => setPending([]));
    }
  }, [user]);

  if (!user) return null;

  const today = new Intl.DateTimeFormat("ru-RU", {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date());

  if (user.role === "STUDENT") {
    const student = user.student;
    if (!student) {
      return <Alert variant="error">Профиль студента не найден.</Alert>;
    }
    return (
      <>
        <PageHeader
          eyebrow={today}
          title={`Здравствуйте, ${user.fullName.split(" ").slice(0, 2).join(" ")}!`}
          subtitle={`${student.facultyName} · группа ${student.groupName}`}
          actions={
            <Link to="/profile">
              <Button variant="secondary" icon="user">
                Мой профиль
              </Button>
            </Link>
          }
        />

        <div className="grid grid--4">
          <StatCard
            label="Академический рейтинг"
            value={`${student.academicRating} / 1000`}
            hint="Списывается за просроченные задания"
            icon="star"
            tone="green"
          />
          <StatCard
            label="Баллы учебной активности"
            value={student.bua}
            hint="БУА за учебные активности"
            icon="book"
            tone="blue"
          />
          <StatCard
            label="Баллы общественной активности"
            value={student.boa}
            hint="БОА за общественные активности"
            icon="award"
            tone="gold"
          />
          <StatCard
            label="Активные задания"
            value={
              tasks === null
                ? "…"
                : tasks.filter((task) => task.status !== "ACCEPTED").length
            }
            hint={
              tasks === null
                ? "Загружаем…"
                : `${tasks.filter((task) => task.status === "OVERDUE").length} просрочено`
            }
            icon="clock"
            tone="red"
          />
        </div>

        <div className="dashboard-grid">
          <Card className="rating-card">
            <div className="section-head">
              <div>
                <h2>Академический рейтинг</h2>
                <p className="muted">Максимальное значение — 1000 АР</p>
              </div>
              <span className="rating-bubble">{student.academicRating}</span>
            </div>
            <ProgressBar value={student.academicRating} max={1000} tone="green" />
            <div className="rating-card__legend">
              <span><i className="dot dot--danger" /> Ниже 400 — риск академической задолженности</span>
              <span><i className="dot dot--green" /> 700+ — высокий результат</span>
            </div>
            <p className="muted rating-card__hint">
              Рейтинг снижается автоматически, если работа не сдана до дедлайна. Сдача останавливает
              списание, а возврат на доработку после дедлайна возобновляет.
            </p>
          </Card>

          <Card>
            <h2>Статус профиля</h2>
            <ul className="status-list">
              <li>
                <span>Email</span>
                <strong className={user.emailVerified ? "text-green" : "text-gold"}>
                  {user.emailVerified ? "Подтверждён" : "Не подтверждён"}
                </strong>
              </li>
              <li>
                <span>Группа</span>
                <strong>{student.groupName}</strong>
              </li>
              <li>
                <span>Принадлежность к группе</span>
                <strong
                  className={
                    student.membershipStatus === "CONFIRMED"
                      ? "text-green"
                      : student.membershipStatus === "REJECTED"
                        ? "text-red"
                        : "text-gold"
                  }
                >
                  {membershipLabels[student.membershipStatus]}
                </strong>
              </li>
              <li>
                <span>Староста</span>
                <strong>{student.isGroupLead ? "Да" : "Нет"}</strong>
              </li>
            </ul>
            {student.membershipStatus === "PENDING" ? (
              <Alert variant="info">
                Пока группа не подтверждена, задания не назначаются. Подтверждение выполняет
                преподаватель или староста.
              </Alert>
            ) : null}
          </Card>
        </div>

        <div className="dashboard-grid">
          <Card>
            <div className="section-head">
              <h2>Последние начисления</h2>
              <Link to="/profile" className="text-link">
                Вся история <Icon name="arrowRight" size={15} />
              </Link>
            </div>
            {ledger === null ? (
              <div className="inline-loader"><Spinner /> Загрузка…</div>
            ) : ledger.length === 0 ? (
              <EmptyState
                icon="award"
                title="Пока нет начислений"
                description="Здесь появятся баллы за активности и изменения академического рейтинга."
              />
            ) : (
              <ul className="ledger-compact">
                {ledger.map((entry) => (
                  <li key={entry.id}>
                    <span className={`ledger-amount ledger-amount--${entry.currency.toLowerCase()}`}>
                      {formatAmount(entry.currency, entry.amount)}
                    </span>
                    <span className="ledger-compact__reason">{entry.reason}</span>
                    <span className="ledger-compact__date">{formatDateTime(entry.date)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card>
            <h2>Быстрые действия</h2>
            <div className="quick-actions">
              <Link to="/tasks" className="quick-action">
                <Icon name="book" />
                <span>
                  <strong>Мои задания</strong>
                  <small>
                    {tasks === null
                      ? "Загрузка…"
                      : `${tasks.filter((task) => task.status !== "ACCEPTED").length} активных, ${tasks.filter((task) => task.status === "OVERDUE").length} просрочено`}
                  </small>
                </span>
                <Icon name="arrowRight" size={16} />
              </Link>
              <Link to="/profile" className="quick-action">
                <Icon name="user" />
                <span>
                  <strong>Профиль</strong>
                  <small>Рейтинг и история баллов</small>
                </span>
                <Icon name="arrowRight" size={16} />
              </Link>
              {student.isGroupLead ? (
                <Link to="/membership" className="quick-action">
                  <Icon name="users" />
                  <span>
                    <strong>Заявки группы</strong>
                    <small>Подтверждение одногруппников</small>
                  </span>
                  <Icon name="arrowRight" size={16} />
                </Link>
              ) : null}
            </div>
          </Card>
        </div>
      </>
    );
  }

  if (user.role === "TEACHER") {
    const teacher = user.teacher;
    return (
      <>
        <PageHeader
          eyebrow={today}
          title={`Здравствуйте, ${user.fullName}!`}
          subtitle={teacher ? teacher.departmentName : "Преподаватель"}
          actions={
            <>
              <Link to="/membership">
                <Button variant="secondary" icon="users">
                  Заявки в группы
                </Button>
              </Link>
              <Link to="/checkpoints">
                <Button icon="book">Мои задачи</Button>
              </Link>
            </>
          }
        />
        <div className="grid grid--3">
          <StatCard
            label="Учебные группы"
            value={teacher?.groups.length ?? 0}
            hint={teacher?.groups.map((group) => group.name).join(", ") || "Группы появятся после создания заданий"}
            icon="users"
            tone="green"
          />
          <StatCard
            label="Заявок на подтверждение"
            value={pending === null ? "…" : pending.length}
            hint="Студенты, ожидающие подтверждения группы"
            icon="clock"
            tone="gold"
          />
          <StatCard
            label="Кафедра"
            value={teacher?.departmentName ?? "—"}
            hint="Профиль преподавателя"
            icon="shield"
            tone="blue"
          />
        </div>
        <div className="dashboard-grid">
          <Card>
            <h2>Заявки в группы</h2>
            {pending === null ? (
              <div className="inline-loader"><Spinner /> Загрузка…</div>
            ) : pending.length === 0 ? (
              <EmptyState
                icon="users"
                title="Нет ожидающих заявок"
                description="Когда студент зарегистрируется и выберет вашу группу, заявка появится здесь."
              />
            ) : (
              <ul className="request-list">
                {pending.slice(0, 4).map((request) => (
                  <li key={request.studentUserId}>
                    <div>
                      <strong>{request.fullName}</strong>
                      <small>{request.groupName} · {request.email}</small>
                    </div>
                    <Link to="/membership" className="text-link">
                      Проверить <Icon name="arrowRight" size={15} />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>
          <Card>
            <h2>Ваши группы</h2>
            {teacher && teacher.groups.length > 0 ? (
              <div className="chip-list">
                {teacher.groups.map((group) => (
                  <span className="chip" key={group.id}>{group.name}</span>
                ))}
              </div>
            ) : (
              <p className="muted">
                Группы привязываются автоматически, когда вы создаёте для них задание или активность.
              </p>
            )}
            <div className="divider" />
            <p className="muted">
              {plural(pending?.length ?? 0, "заявка", "заявки", "заявок")} ждёт вашего решения.
            </p>
          </Card>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        eyebrow={today}
        title={`Здравствуйте, ${user.fullName}!`}
        subtitle="Панель администратора"
      />
      <Card>
        <h2>Администрирование</h2>
        <p className="muted">
          Раздел управления справочниками, импортом студентов, семестрами и настройками будет
          доступен в панели администратора.
        </p>
      </Card>
    </>
  );
}
