import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, ApiError } from "../../shared/api/client";
import { useAuth } from "../../shared/auth/AuthContext";
import { Alert, Button, Field, Input, PageLoader } from "../../shared/ui";
import { AuthLayout } from "./AuthLayout";

type VerificationState = "idle" | "loading" | "success" | "error";

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const { user, refreshUser } = useAuth();
  const [token, setToken] = useState(searchParams.get("token") ?? "");
  const [state, setState] = useState<VerificationState>(
    searchParams.get("token") ? "loading" : "idle",
  );
  const [message, setMessage] = useState<string | null>(null);
  const started = useRef(false);

  const verify = async (value: string) => {
    setState("loading");
    setMessage(null);
    try {
      const response = await api<{ message: string }>("/auth/verify-email", {
        method: "POST",
        body: { token: value.trim() },
      });
      setState("success");
      setMessage(response.message);
      if (user) {
        await refreshUser();
      }
    } catch (error) {
      setState("error");
      setMessage(error instanceof ApiError ? error.message : "Не удалось подтвердить email");
    }
  };

  useEffect(() => {
    const initial = searchParams.get("token");
    if (initial && !started.current) {
      started.current = true;
      void verify(initial);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  return (
    <AuthLayout
      title="Подтверждение email"
      subtitle="Откройте ссылку из письма или вставьте код вручную"
      footer={
        <div className="auth-links">
          <span>Куда приходит письмо?</span>
          <a href="http://localhost:8025" target="_blank" rel="noreferrer">
            Mailhog (dev)
          </a>
          <span className="auth-links__sep">·</span>
          <Link to="/login">Войти</Link>
        </div>
      }
    >
      {state === "loading" ? (
        <PageLoader label="Подтверждаем email…" />
      ) : state === "success" ? (
        <div className="verify-result">
          <Alert variant="success" title="Email подтверждён">
            {message}
          </Alert>
          <p className="muted">
            Подтверждение email открывает сдачу работ и участие в опросах. Принадлежность к группе
            подтверждается отдельно.
          </p>
          <Link className="button button--primary button--block" to={user ? "/" : "/login"}>
            {user ? "На главную" : "Войти в систему"}
          </Link>
        </div>
      ) : (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            void verify(token);
          }}
        >
          {state === "error" ? (
            <Alert variant="error" title="Не удалось подтвердить email">
              {message}
            </Alert>
          ) : null}
          <Field label="Код подтверждения" htmlFor="verify-token" required>
            <Input
              id="verify-token"
              value={token}
              onChange={(event) => setToken(event.target.value)}
              placeholder="Скопируйте токен из письма"
              required
            />
          </Field>
          <Button type="submit" className="button--block" disabled={!token.trim()}>
            Подтвердить email
          </Button>
          <p className="form-note">
            В dev-окружении письма перехватывает Mailhog — откройте{" "}
            <a href="http://localhost:8025" target="_blank" rel="noreferrer">
              localhost:8025
            </a>
            .
          </p>
        </form>
      )}
    </AuthLayout>
  );
}
