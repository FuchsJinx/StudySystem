import { useEffect } from "react";
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { Icon, type IconName } from "./icons";

export { Icon } from "./icons";
export type { IconName } from "./icons";
import type { MembershipStatus, SubmissionStatus } from "../api/types";
import { membershipLabels, statusLabels } from "../format";

export function cx(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}

export function Spinner({ size = 20 }: { size?: number }) {
  return (
    <span
      className="spinner"
      style={{ width: size, height: size }}
      role="status"
      aria-label="Загрузка"
    />
  );
}

export function PageLoader({ label = "Загрузка…" }: { label?: string }) {
  return (
    <div className="page-loader">
      <Spinner size={28} />
      <span>{label}</span>
    </div>
  );
}

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "gold";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  loading?: boolean;
  icon?: IconName;
  size?: "sm" | "md";
}

export function Button({
  variant = "primary",
  loading = false,
  icon,
  size = "md",
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      className={cx("button", `button--${variant}`, size === "sm" && "button--sm", className)}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? <Spinner size={16} /> : icon ? <Icon name={icon} size={17} /> : null}
      {children}
    </button>
  );
}

interface FieldProps {
  label: string;
  htmlFor?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  children: ReactNode;
}

export function Field({ label, htmlFor, hint, error, required, children }: FieldProps) {
  return (
    <label className={cx("field", error && "field--error")} htmlFor={htmlFor}>
      <span className="field__label">
        {label}
        {required ? <span className="field__required"> *</span> : null}
      </span>
      {children}
      {hint && !error ? <span className="field__hint">{hint}</span> : null}
      {error ? <span className="field__error">{error}</span> : null}
    </label>
  );
}

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx("input", className)} {...rest} />;
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cx("input", "select", className)} {...rest}>
      {children}
    </select>
  );
}

export function Textarea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cx("input", "textarea", className)} {...rest} />;
}

export function Card({
  children,
  className,
  as: Tag = "section",
}: {
  children: ReactNode;
  className?: string;
  as?: "section" | "div" | "article";
}) {
  return <Tag className={cx("card", className)}>{children}</Tag>;
}

export function PageHeader({
  title,
  subtitle,
  actions,
  eyebrow,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  eyebrow?: string;
}) {
  return (
    <header className="page-header">
      <div>
        {eyebrow ? <p className="page-header__eyebrow">{eyebrow}</p> : null}
        <h1>{title}</h1>
        {subtitle ? <p className="page-header__subtitle">{subtitle}</p> : null}
      </div>
      {actions ? <div className="page-header__actions">{actions}</div> : null}
    </header>
  );
}

type AlertVariant = "info" | "success" | "warning" | "error";

export function Alert({
  variant = "info",
  title,
  children,
  actions,
}: {
  variant?: AlertVariant;
  title?: string;
  children?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div className={cx("alert", `alert--${variant}`)} role={variant === "error" ? "alert" : "status"}>
      <Icon name={variant === "error" ? "alert" : variant === "success" ? "check" : "bell"} size={18} />
      <div className="alert__body">
        {title ? <strong>{title}</strong> : null}
        {children ? <div>{children}</div> : null}
      </div>
      {actions ? <div className="alert__actions">{actions}</div> : null}
    </div>
  );
}

export function EmptyState({
  icon = "book",
  title,
  description,
  actions,
}: {
  icon?: IconName;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon">
        <Icon name={icon} size={26} />
      </span>
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
      {actions ? <div className="empty-state__actions">{actions}</div> : null}
    </div>
  );
}

export function Avatar({ name, size = 44 }: { name: string; size?: number }) {
  const parts = name.trim().split(/\s+/);
  const text = `${parts[0]?.[0] ?? ""}${parts[1]?.[0] ?? ""}`.toUpperCase();
  return (
    <span
      className="avatar"
      style={{ width: size, height: size, fontSize: Math.round(size * 0.36) }}
      aria-hidden="true"
    >
      {text}
    </span>
  );
}

export function StatusBadge({ status }: { status: SubmissionStatus }) {
  return <span className={cx("badge", `badge--status-${status.toLowerCase()}`)}>{statusLabels[status]}</span>;
}

export function MembershipBadge({ status }: { status: MembershipStatus }) {
  return <span className={cx("badge", `badge--membership-${status.toLowerCase()}`)}>{membershipLabels[status]}</span>;
}

export function ProgressBar({
  value,
  max = 100,
  tone = "green",
  label,
}: {
  value: number;
  max?: number;
  tone?: "green" | "gold" | "red" | "blue";
  label?: string;
}) {
  const percent = Math.max(0, Math.min(100, (value / Math.max(1, max)) * 100));
  return (
    <div className="progress">
      {label ? (
        <div className="progress__meta">
          <span>{label}</span>
          <span>
            {value} / {max}
          </span>
        </div>
      ) : null}
      <div className="progress__track">
        <div className={cx("progress__fill", `progress__fill--${tone}`)} style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

export function Modal({
  open,
  title,
  onClose,
  children,
  footer,
  wide,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  wide?: boolean;
}) {
  useEffect(() => {
    if (!open) return;
    const handler = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handler);
    document.body.classList.add("modal-open");
    return () => {
      document.removeEventListener("keydown", handler);
      document.body.classList.remove("modal-open");
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className={cx("modal", wide && "modal--wide")} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal__header">
          <h2>{title}</h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Закрыть">
            <Icon name="close" size={20} />
          </button>
        </header>
        <div className="modal__body">{children}</div>
        {footer ? <footer className="modal__footer">{footer}</footer> : null}
      </div>
    </div>
  );
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Подтвердить",
  cancelLabel = "Отмена",
  danger,
  loading,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button variant={danger ? "danger" : "primary"} onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="muted">{description}</p>
    </Modal>
  );
}

export function StatCard({
  label,
  value,
  hint,
  tone = "green",
  icon,
}: {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: "green" | "gold" | "blue" | "red";
  icon?: IconName;
}) {
  return (
    <div className={cx("stat-card", `stat-card--${tone}`)}>
      <div className="stat-card__top">
        <span>{label}</span>
        {icon ? <Icon name={icon} size={18} /> : null}
      </div>
      <div className="stat-card__value">{value}</div>
      {hint ? <div className="stat-card__hint">{hint}</div> : null}
    </div>
  );
}
