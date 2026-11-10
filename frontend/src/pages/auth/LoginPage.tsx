import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthContext";
import { ApiError } from "../../shared/api/client";
import { Alert, Button, Field, Input } from "../../shared/ui";
import { AuthLayout } from "./AuthLayout";

const DEMO_ACCOUNTS = [
  { role: "Студент", email: "student@demo.local" },
  { role: "Преподаватель", email: "teacher@demo.local" },
  { role: "Администратор", email: "admin@demo.local" },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [details, setDetails] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setDetails([]);
    setLoading(true);
    try {
      await login(email.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
        setDetails(err.details);
      } else {
        setError("Не удалось войти. Проверьте соединение с сервером.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Вход в систему"
      subtitle="Используйте университетский email и пароль"
      footer={
        <div className="auth-links">
          <span>Ещё нет аккаунта?</span>
          <Link to="/register">Регистрация студента</Link>
          <span className="auth-links__sep">·</span>
          <Link to="/register/teacher">Регистрация преподавателя</Link>
        </div>
      }
    >
      <form className="form" onSubmit={submit}>
        {error ? (
          <Alert variant="error" title="Не удалось войти">
            {error}
            {details.length > 0 ? (
              <ul className="alert__list">
                {details.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            ) : null}
          </Alert>
        ) : null}

        <Field label="Email" htmlFor="login-email" required>
          <Input
            id="login-email"
            type="email"
            autoComplete="email"
            placeholder="student@demo.local"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </Field>
        <Field label="Пароль" htmlFor="login-password" required>
          <Input
            id="login-password"
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </Field>

        <Button type="submit" loading={loading} className="button--block">
          Войти
        </Button>
      </form>

      <div className="demo-accounts">
        <p>Демо-доступы (пароль Demo123!)</p>
        <div className="demo-accounts__grid">
          {DEMO_ACCOUNTS.map((account) => (
            <button
              key={account.email}
              type="button"
              className="demo-account"
              onClick={() => {
                setEmail(account.email);
                setPassword("Demo123!");
                setError(null);
              }}
            >
              <strong>{account.role}</strong>
              <span>{account.email}</span>
            </button>
          ))}
        </div>
      </div>
    </AuthLayout>
  );
}
