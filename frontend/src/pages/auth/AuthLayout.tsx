import type { ReactNode } from "react";
import { Link } from "react-router-dom";

export function AuthLayout({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="auth-page">
      <aside className="auth-hero">
        <Link to="/" className="brand brand--light">
          <span className="brand__mark">UR</span>
          <span className="brand__text">
            UniRate
            <small>Университетская сеть</small>
          </span>
        </Link>
        <div className="auth-hero__content">
          <p className="auth-hero__eyebrow">БГТУ · Хакатон 2026</p>
          <h1>Успехи студентов — на виду</h1>
          <p className="auth-hero__lead">
            Академический рейтинг, баллы за активности и публичная лента достижений в одной
            университетской сети.
          </p>
          <ul className="auth-hero__features">
            <li>
              <span>01</span> Академический рейтинг и контрольные точки
            </li>
            <li>
              <span>02</span> БУА и БОА за учебные и общественные активности
            </li>
            <li>
              <span>03</span> Задания, опросы и персональные события
            </li>
          </ul>
        </div>
        <p className="auth-hero__footer">UniRate · StudySystem · 2026</p>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="auth-card__brand">
            <span className="brand__mark">UR</span>
            <span>UniRate</span>
          </div>
          <h2>{title}</h2>
          {subtitle ? <p className="auth-card__subtitle">{subtitle}</p> : null}
          {children}
          {footer ? <div className="auth-card__footer">{footer}</div> : null}
        </div>
      </main>
    </div>
  );
}
