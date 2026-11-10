import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../shared/auth/AuthContext";
import { api } from "../shared/api/client";
import { Avatar, Button, Icon } from "../shared/ui";
import type { IconName } from "../shared/ui/icons";

interface NavItem {
  to: string;
  label: string;
  icon: IconName;
  roles?: Array<"STUDENT" | "TEACHER" | "ADMIN">;
}

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Главная", icon: "home" },
  { to: "/feed", label: "Лента наград", icon: "award" },
  { to: "/tasks", label: "Мои задачи", icon: "book", roles: ["STUDENT"] },
  { to: "/checkpoints", label: "Задачи", icon: "book", roles: ["TEACHER"] },
  { to: "/activities", label: "Активности", icon: "megaphone", roles: ["STUDENT", "TEACHER"] },
  { to: "/budget", label: "Бюджет", icon: "wallet", roles: ["TEACHER"] },
  { to: "/profile", label: "Профиль", icon: "user" },
  { to: "/membership", label: "Заявки в группы", icon: "users", roles: ["TEACHER", "ADMIN", "STUDENT"] },
];

export function Layout() {
  const { user, logout, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  if (!user) {
    return null;
  }

  const items = NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(user.role));

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const handleResend = async () => {
    setResending(true);
    setResendMessage(null);
    try {
      const response = await api<{ message: string }>("/auth/resend-verification", { method: "POST" });
      setResendMessage(response.message);
      await refreshUser();
    } catch (error) {
      setResendMessage(error instanceof Error ? error.message : "Не удалось отправить письмо");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar__brand">
          <Link to="/" className="brand" onClick={() => setMobileOpen(false)}>
            <span className="brand__mark">UR</span>
            <span className="brand__text">
              UniRate
              <small>Университетская сеть</small>
            </span>
          </Link>
          <button
            type="button"
            className="icon-button sidebar__close"
            onClick={() => setMobileOpen(false)}
            aria-label="Закрыть меню"
          >
            <Icon name="close" />
          </button>
        </div>

        <nav className="sidebar__nav" aria-label="Основная навигация">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
              onClick={() => setMobileOpen(false)}
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar__footer">
          <Link to="/profile" className="sidebar__user" onClick={() => setMobileOpen(false)}>
            <Avatar name={user.fullName} size={38} />
            <span>
              <strong>{user.fullName}</strong>
              <small>{user.student?.groupName ?? user.teacher?.departmentName ?? "Администратор"}</small>
            </span>
          </Link>
          <button type="button" className="sidebar__logout" onClick={handleLogout}>
            <Icon name="logout" size={18} />
            Выйти
          </button>
        </div>
      </aside>

      {mobileOpen ? <div className="sidebar-overlay" onClick={() => setMobileOpen(false)} /> : null}

      <div className="app-main">
        <header className="topbar">
          <button
            type="button"
            className="icon-button topbar__menu"
            onClick={() => setMobileOpen(true)}
            aria-label="Открыть меню"
          >
            <Icon name="menu" />
          </button>
          <div className="topbar__spacer" />
          <Link to="/profile" className="topbar__user">
            <Avatar name={user.fullName} size={34} />
            <span>{user.fullName}</span>
          </Link>
        </header>

        <main className="content">
          {!user.emailVerified ? (
            <div className="banner banner--warning">
              <div>
                <strong>Подтвердите email.</strong> Письмо отправлено на {user.email}. Ссылка действует 24 часа.
                {resendMessage ? <div className="banner__note">{resendMessage}</div> : null}
              </div>
              <Button variant="secondary" size="sm" loading={resending} onClick={handleResend}>
                Отправить ещё раз
              </Button>
            </div>
          ) : null}

          {user.role === "STUDENT" && user.student?.membershipStatus === "PENDING" ? (
            <div className="banner banner--info">
              <div>
                <strong>Группа ожидает подтверждения.</strong> Задания и начисления откроются после того, как
                преподаватель или староста подтвердит вашу принадлежность к группе {user.student.groupName}.
              </div>
            </div>
          ) : null}

          {user.role === "STUDENT" && user.student?.membershipStatus === "REJECTED" ? (
            <div className="banner banner--error">
              <div>
                <strong>Заявка отклонена.</strong> Обратитесь к преподавателю или старосте группы{" "}
                {user.student.groupName}.
              </div>
            </div>
          ) : null}

          <Outlet />
        </main>
      </div>
    </div>
  );
}
